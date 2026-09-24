package com.example

import com.example.crypto.OfflineCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun testOfflineVoucherCreationAndIntegrity() {
    val prevHash = "0000000000000000000000000000000000000000000000000000000000000000"
    val voucher = OfflineCryptoManager.createVoucher(
      senderUpiId = "user@offlinepay",
      senderName = "User Test",
      receiverUpiId = "merchant@offlinepay",
      receiverName = "Merchant Test",
      amount = 150.0,
      prevBlockHash = prevHash,
      bankLast4 = "4821"
    )

    assertNotNull(voucher.transactionId)
    assertTrue(voucher.transactionId.startsWith("OTXN-"))
    assertEquals(150.0, voucher.amount, 0.001)
    assertEquals(prevHash, voucher.prevBlockHash)
    assertTrue(voucher.blockHash.isNotEmpty())
    assertTrue(voucher.signature.isNotEmpty())

    // Test URL-safe QR Encoding and Decoding
    val qrString = OfflineCryptoManager.encodeVoucherToQrString(voucher)
    assertTrue(qrString.startsWith("offlinepay://otv/"))

    val decodedVoucher = OfflineCryptoManager.decodeVoucherFromQrString(qrString)
    assertNotNull(decodedVoucher)
    assertEquals(voucher.transactionId, decodedVoucher?.transactionId)
    assertEquals(voucher.amount, decodedVoucher?.amount ?: 0.0, 0.001)
    assertEquals(voucher.blockHash, decodedVoucher?.blockHash)
    assertEquals(voucher.signature, decodedVoucher?.signature)
  }

  @Test
  fun testTamperedVoucherFailsVerification() {
    val prevHash = "0000000000000000000000000000000000000000000000000000000000000000"
    val voucher = OfflineCryptoManager.createVoucher(
      senderUpiId = "user@offlinepay",
      senderName = "User Test",
      receiverUpiId = "merchant@offlinepay",
      receiverName = "Merchant Test",
      amount = 100.0,
      prevBlockHash = prevHash,
      bankLast4 = "4821"
    )

    // 1. Tamper with the amount in the payload
    val tamperedAmountVoucher = voucher.copy(amount = 9999.0)
    val tamperedQr = OfflineCryptoManager.encodeVoucherToQrString(tamperedAmountVoucher)
    val decodedTampered = OfflineCryptoManager.decodeVoucherFromQrString(tamperedQr)
    assertTrue(decodedTampered == null)

    // 2. Tamper with the signature
    val forgedSigVoucher = voucher.copy(signature = "invalid_forged_hmac_signature")
    val forgedQr = OfflineCryptoManager.encodeVoucherToQrString(forgedSigVoucher)
    val decodedForged = OfflineCryptoManager.decodeVoucherFromQrString(forgedQr)
    assertTrue(decodedForged == null)
  }
}
