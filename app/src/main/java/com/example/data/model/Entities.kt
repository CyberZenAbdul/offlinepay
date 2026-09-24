package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bankName: String,
    val accountNumber: String, // e.g. "•••• •••• 4821"
    val fullAccountNumber: String,
    val ifscCode: String,
    val accountHolder: String,
    val accountType: String = "Savings", // "Savings" or "Current"
    val upiPin: String, // 4 or 6 digit PIN
    val balance: Double,
    val isPrimary: Boolean = false,
    val colorHex: String = "#5F259F"
)

@Entity(tableName = "offline_wallet")
data class OfflineWallet(
    @PrimaryKey
    val id: Int = 1,
    val allocatedBalance: Double = 2500.0,
    val dailyLimit: Double = 10000.0,
    val spentToday: Double = 0.0,
    val maxSingleLimit: Double = 2000.0,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val deviceUpiId: String = "abdul.khadar@offlinepay",
    val deviceUserName: String = "Abdul Khadar"
)

@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey
    val id: String, // e.g. "OTXN-XXXX"
    val title: String,
    val senderUpiId: String,
    val receiverUpiId: String,
    val receiverName: String,
    val amount: Double,
    val timestamp: Long,
    val type: String, // "DEBIT", "CREDIT", "TOPUP"
    val mode: String, // "NFC_TAP", "ENCRYPTED_QR", "LOCAL_P2P"
    val status: String, // "OFFLINE_VERIFIED", "SYNCED", "PENDING_SYNC"
    val authUsed: String, // "BIOMETRIC_AND_UPI_PIN", "BIOMETRIC", "UPI_PIN"
    val cryptographicHash: String,
    val prevBlockHash: String,
    val signature: String,
    val bankLast4: String,
    val notes: String = ""
)
