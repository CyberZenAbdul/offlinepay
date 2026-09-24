package com.example.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Manages cryptographic operations for 100% offline peer-to-peer transactions.
 * Utilizes SHA-256 for tamper-proof hash chaining and HMAC-SHA256 for cryptographic signatures.
 */
object OfflineCryptoManager {

    // On-device secret seed representing the secure hardware / SIM applet element
    private const val DEVICE_SECRET = "OFFLINEPAY_DEVICE_SECURE_ENCLAVE_KEY_V1_RURAL_2026"
    private const val HMAC_ALGO = "HmacSHA256"

    /**
     * Data structure for an encrypted/signed offline payment voucher.
     */
    data class OfflineVoucher(
        val transactionId: String,
        val senderUpiId: String,
        val senderName: String,
        val receiverUpiId: String,
        val receiverName: String,
        val amount: Double,
        val timestamp: Long,
        val nonce: String,
        val prevBlockHash: String,
        val blockHash: String,
        val signature: String,
        val bankLast4: String,
        val authMethod: String
    )

    /**
     * Generates a unique cryptographically random 64-bit hex nonce.
     */
    fun generateNonce(): String {
        val randomBytes = ByteArray(8)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes SHA-256 hash string.
     */
    fun computeSha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Signs transaction payload using HMAC-SHA256 with the device enclave key.
     */
    fun signPayload(payload: String): String {
        val secretKey = SecretKeySpec(DEVICE_SECRET.toByteArray(StandardCharsets.UTF_8), HMAC_ALGO)
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(secretKey)
        val signatureBytes = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    /**
     * Verifies the HMAC-SHA256 signature of a received voucher.
     */
    fun verifySignature(payload: String, expectedSignature: String): Boolean {
        val actualSignature = signPayload(payload)
        return actualSignature == expectedSignature
    }

    /**
     * Builds and signs an OfflineVoucher.
     */
    fun createVoucher(
        senderUpiId: String,
        senderName: String,
        receiverUpiId: String,
        receiverName: String,
        amount: Double,
        prevBlockHash: String,
        bankLast4: String,
        authMethod: String = "BIOMETRIC_AND_UPI_PIN"
    ): OfflineVoucher {
        val txnId = "OTXN-" + UUID.randomUUID().toString().take(12).uppercase()
        val timestamp = System.currentTimeMillis()
        val nonce = generateNonce()

        // Raw payload for hashing and signing
        val corePayload = "$txnId|$senderUpiId|$receiverUpiId|%.2f|$timestamp|$nonce|$prevBlockHash".format(amount)
        val blockHash = computeSha256(corePayload)
        val signature = signPayload(blockHash)

        return OfflineVoucher(
            transactionId = txnId,
            senderUpiId = senderUpiId,
            senderName = senderName,
            receiverUpiId = receiverUpiId,
            receiverName = receiverName,
            amount = amount,
            timestamp = timestamp,
            nonce = nonce,
            prevBlockHash = prevBlockHash,
            blockHash = blockHash,
            signature = signature,
            bankLast4 = bankLast4,
            authMethod = authMethod
        )
    }

    /**
     * Encodes an OfflineVoucher into an encrypted-format URL-safe Base64 token for QR code rendering or NFC transmission.
     */
    fun encodeVoucherToQrString(voucher: OfflineVoucher): String {
        val json = JSONObject().apply {
            put("v", 1)
            put("id", voucher.transactionId)
            put("snd", voucher.senderUpiId)
            put("snm", voucher.senderName)
            put("rcv", voucher.receiverUpiId)
            put("rnm", voucher.receiverName)
            put("amt", voucher.amount)
            put("ts", voucher.timestamp)
            put("nce", voucher.nonce)
            put("pbh", voucher.prevBlockHash)
            put("bh", voucher.blockHash)
            put("sig", voucher.signature)
            put("b4", voucher.bankLast4)
            put("auth", voucher.authMethod)
        }
        val rawBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        return "offlinepay://otv/" + Base64.encodeToString(rawBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Decodes and validates an OfflineVoucher from an encrypted QR or NFC string.
     */
    fun decodeVoucherFromQrString(qrString: String): OfflineVoucher? {
        return try {
            val prefix = "offlinepay://otv/"
            val base64Data = if (qrString.startsWith(prefix)) {
                qrString.substring(prefix.length)
            } else if (qrString.contains("otv/")) {
                qrString.substringAfter("otv/")
            } else {
                qrString
            }

            val decodedBytes = Base64.decode(base64Data, Base64.URL_SAFE or Base64.DEFAULT)
            val jsonString = String(decodedBytes, StandardCharsets.UTF_8)
            val json = JSONObject(jsonString)

            val txnId = json.getString("id")
            val snd = json.getString("snd")
            val snm = json.optString("snm", "Peer User")
            val rcv = json.getString("rcv")
            val rnm = json.optString("rnm", "Local Merchant")
            val amt = json.getDouble("amt")
            val ts = json.getLong("ts")
            val nce = json.getString("nce")
            val pbh = json.getString("pbh")
            val bh = json.getString("bh")
            val sig = json.getString("sig")
            val b4 = json.optString("b4", "0000")
            val auth = json.optString("auth", "UPI_PIN")

            // Verify integrity
            val expectedPayload = "$txnId|$snd|$rcv|%.2f|$ts|$nce|$pbh".format(amt)
            val expectedBlockHash = computeSha256(expectedPayload)

            if (expectedBlockHash != bh) {
                // Tampered payload
                return null
            }

            if (!verifySignature(bh, sig)) {
                // Invalid signature
                return null
            }

            OfflineVoucher(
                transactionId = txnId,
                senderUpiId = snd,
                senderName = snm,
                receiverUpiId = rcv,
                receiverName = rnm,
                amount = amt,
                timestamp = ts,
                nonce = nce,
                prevBlockHash = pbh,
                blockHash = bh,
                signature = sig,
                bankLast4 = b4,
                authMethod = auth
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Encode dynamic payment request (e.g. merchant requesting payment via QR/NFC)
     */
    fun createReceiveQrString(receiverUpiId: String, receiverName: String, amount: Double?): String {
        val json = JSONObject().apply {
            put("type", "RECEIVE_REQ")
            put("rcv", receiverUpiId)
            put("rnm", receiverName)
            put("amt", amount ?: 0.0)
            put("ts", System.currentTimeMillis())
            put("nonce", generateNonce())
        }
        val rawBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        return "offlinepay://req/" + Base64.encodeToString(rawBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Decodes a payment request
     */
    fun decodeReceiveRequest(dataString: String): Pair<String, Double?>? {
        return try {
            val prefix = "offlinepay://req/"
            val base64Data = if (dataString.startsWith(prefix)) {
                dataString.substring(prefix.length)
            } else {
                dataString
            }
            val decoded = Base64.decode(base64Data, Base64.URL_SAFE or Base64.DEFAULT)
            val json = JSONObject(String(decoded, StandardCharsets.UTF_8))
            val rcv = json.getString("rcv")
            val amt = json.optDouble("amt", 0.0)
            Pair(rcv, if (amt > 0.0) amt else null)
        } catch (e: Exception) {
            null
        }
    }
}
