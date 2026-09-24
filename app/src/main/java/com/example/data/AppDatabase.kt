package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.crypto.OfflineCryptoManager
import com.example.data.dao.BankAccountDao
import com.example.data.dao.OfflineWalletDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccount
import com.example.data.model.OfflineTransaction
import com.example.data.model.OfflineWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [BankAccount::class, OfflineWallet::class, OfflineTransaction::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun offlineWalletDao(): OfflineWalletDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offlinepay_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial data on background coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedInitialData(database)
                    }
                }
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val bankDao = database.bankAccountDao()
            val walletDao = database.offlineWalletDao()
            val txnDao = database.transactionDao()

            // 1. Seed Bank Accounts
            bankDao.insertBankAccount(
                BankAccount(
                    bankName = "State Bank of India",
                    accountNumber = "•••• •••• 4821",
                    fullAccountNumber = "104928374821",
                    ifscCode = "SBIN0001234",
                    accountHolder = "Abdul Khadar",
                    accountType = "Savings",
                    upiPin = "1234",
                    balance = 48500.00,
                    isPrimary = true,
                    colorHex = "#1A237E"
                )
            )

            bankDao.insertBankAccount(
                BankAccount(
                    bankName = "HDFC Bank",
                    accountNumber = "•••• •••• 9104",
                    fullAccountNumber = "50100492839104",
                    ifscCode = "HDFC0000456",
                    accountHolder = "Abdul Khadar",
                    accountType = "Savings",
                    upiPin = "9876",
                    balance = 12350.00,
                    isPrimary = false,
                    colorHex = "#004B87"
                )
            )

            // 2. Seed Offline Wallet
            walletDao.insertOrUpdateWallet(
                OfflineWallet(
                    id = 1,
                    allocatedBalance = 2500.00,
                    dailyLimit = 10000.00,
                    spentToday = 380.00,
                    maxSingleLimit = 2000.00,
                    lastSyncedAt = System.currentTimeMillis() - 86400000,
                    deviceUpiId = "abdul.khadar@offlinepay",
                    deviceUserName = "Abdul Khadar"
                )
            )

            // 3. Seed Seed Cryptographic Block Chain Transactions
            val genesisHash = "0000000000000000000000000000000000000000000000000000000000000000"
            val voucher1 = OfflineCryptoManager.createVoucher(
                senderUpiId = "abdul.khadar@offlinepay",
                senderName = "Abdul Khadar",
                receiverUpiId = "sharma.kirana@offlinepay",
                receiverName = "Sharma Ji Kirana Store",
                amount = 230.00,
                prevBlockHash = genesisHash,
                bankLast4 = "4821",
                authMethod = "BIOMETRIC_AND_UPI_PIN"
            )
            txnDao.insertTransaction(
                OfflineTransaction(
                    id = voucher1.transactionId,
                    title = "Grocery & Spices",
                    senderUpiId = voucher1.senderUpiId,
                    receiverUpiId = voucher1.receiverUpiId,
                    receiverName = voucher1.receiverName,
                    amount = voucher1.amount,
                    timestamp = System.currentTimeMillis() - 7200000,
                    type = "DEBIT",
                    mode = "NFC_TAP",
                    status = "OFFLINE_VERIFIED",
                    authUsed = voucher1.authMethod,
                    cryptographicHash = voucher1.blockHash,
                    prevBlockHash = voucher1.prevBlockHash,
                    signature = voucher1.signature,
                    bankLast4 = "4821",
                    notes = "Offline NFC tap transfer at village grocery"
                )
            )

            val voucher2 = OfflineCryptoManager.createVoucher(
                senderUpiId = "abdul.khadar@offlinepay",
                senderName = "Abdul Khadar",
                receiverUpiId = "krishna.dairy@offlinepay",
                receiverName = "Krishna Fresh Milk & Dairy",
                amount = 150.00,
                prevBlockHash = voucher1.blockHash,
                bankLast4 = "4821",
                authMethod = "BIOMETRIC_AND_UPI_PIN"
            )
            txnDao.insertTransaction(
                OfflineTransaction(
                    id = voucher2.transactionId,
                    title = "Daily Milk Supply",
                    senderUpiId = voucher2.senderUpiId,
                    receiverUpiId = voucher2.receiverUpiId,
                    receiverName = voucher2.receiverName,
                    amount = voucher2.amount,
                    timestamp = System.currentTimeMillis() - 3600000,
                    type = "DEBIT",
                    mode = "ENCRYPTED_QR",
                    status = "OFFLINE_VERIFIED",
                    authUsed = voucher2.authMethod,
                    cryptographicHash = voucher2.blockHash,
                    prevBlockHash = voucher2.prevBlockHash,
                    signature = voucher2.signature,
                    bankLast4 = "4821",
                    notes = "Encrypted QR voucher scan"
                )
            )
        }
    }
}
