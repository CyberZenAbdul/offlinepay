package com.example.p2p

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.example.crypto.OfflineCryptoManager
import java.nio.charset.StandardCharsets

class NfcPaymentManager(private val context: Context) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isNfcSupported(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Data class representing a discoverable nearby offline peer or terminal.
     */
    data class OfflinePeer(
        val id: String,
        val name: String,
        val upiId: String,
        val category: String,
        val distanceMeters: String,
        val terminalType: String // "SMART_SOUNDBOX", "MERCHANT_POS", "PEER_PHONE"
    )

    companion object {
        const val NFC_MIME_TYPE = "application/vnd.com.offlinepay.p2p"

        val DEMO_NEARBY_PEERS = listOf(
            OfflinePeer(
                id = "PEER_KIRANA_01",
                name = "Sharma Ji Kirana Store",
                upiId = "sharma.kirana@offlinepay",
                category = "Groceries & Daily Essentials",
                distanceMeters = "0.2 cm (Tap Range)",
                terminalType = "SMART_SOUNDBOX"
            ),
            OfflinePeer(
                id = "PEER_MANDI_02",
                name = "Ramesh Kumar (Kisan Mandi)",
                upiId = "ramesh.farmer@offlinepay",
                category = "Fresh Farm Produce",
                distanceMeters = "0.5 cm (Tap Range)",
                terminalType = "PEER_PHONE"
            ),
            OfflinePeer(
                id = "PEER_DAIRY_03",
                name = "Krishna Dairy & Sweets",
                upiId = "krishna.dairy@offlinepay",
                category = "Milk, Ghee & Curd",
                distanceMeters = "Tap to Connect",
                terminalType = "MERCHANT_POS"
            ),
            OfflinePeer(
                id = "PEER_CHAI_04",
                name = "Gupta Tea Stall",
                upiId = "gupta.tea@offlinepay",
                category = "Tea & Snacks",
                distanceMeters = "Tap to Connect",
                terminalType = "SMART_SOUNDBOX"
            )
        )
    }

    /**
     * Starts listening for hardware NFC tags using Android NfcAdapter Reader Mode.
     */
    fun startNfcReaderMode(
        activity: Activity,
        onVoucherDetected: (OfflineCryptoManager.OfflineVoucher) -> Unit,
        onError: (String) -> Unit
    ) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled) {
            return
        }

        try {
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

            nfcAdapter.enableReaderMode(activity, { tag: Tag ->
                val voucher = readVoucherFromTag(tag)
                activity.runOnUiThread {
                    if (voucher != null) {
                        onVoucherDetected(voucher)
                    } else {
                        onError("Non-OfflinePay NFC Tag Detected")
                    }
                }
            }, flags, null)
        } catch (e: Exception) {
            onError("NFC reader error: ${e.message}")
        }
    }

    fun stopNfcReaderMode(activity: Activity) {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readVoucherFromTag(tag: Tag): OfflineCryptoManager.OfflineVoucher? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            val message = ndef.ndefMessage ?: return null
            ndef.close()

            for (record in message.records) {
                val mimeType = String(record.type, StandardCharsets.US_ASCII)
                if (mimeType == NFC_MIME_TYPE) {
                    val payloadStr = String(record.payload, StandardCharsets.UTF_8)
                    return OfflineCryptoManager.decodeVoucherFromQrString(payloadStr)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Prepares an NDEF message with an encrypted offline voucher payload.
     */
    fun createNdefMessage(voucherString: String): NdefMessage {
        val record = NdefRecord.createMime(
            NFC_MIME_TYPE,
            voucherString.toByteArray(StandardCharsets.UTF_8)
        )
        return NdefMessage(arrayOf(record))
    }
}
