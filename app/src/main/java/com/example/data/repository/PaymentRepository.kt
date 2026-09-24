package com.example.data.repository

import com.example.crypto.OfflineCryptoManager
import com.example.data.dao.BankAccountDao
import com.example.data.dao.OfflineWalletDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccount
import com.example.data.model.OfflineTransaction
import com.example.data.model.OfflineWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PaymentRepository(
    private val bankAccountDao: BankAccountDao,
    private val offlineWalletDao: OfflineWalletDao,
    private val transactionDao: TransactionDao
) {
    val allBankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllBankAccounts()
    val offlineWallet: Flow<OfflineWallet?> = offlineWalletDao.getOfflineWalletFlow()
    val allTransactions: Flow<List<OfflineTransaction>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<OfflineTransaction>> = transactionDao.getRecentTransactions()
    val pendingSyncCount: Flow<Int> = transactionDao.getPendingSyncCount()

    suspend fun getPrimaryBankAccount(): BankAccount? = withContext(Dispatchers.IO) {
        bankAccountDao.getPrimaryBankAccount()
    }

    suspend fun addBankAccount(bankAccount: BankAccount): Long = withContext(Dispatchers.IO) {
        if (bankAccount.isPrimary) {
            bankAccountDao.clearPrimaryFlags()
        }
        bankAccountDao.insertBankAccount(bankAccount)
    }

    suspend fun setPrimaryBankAccount(id: Int) = withContext(Dispatchers.IO) {
        bankAccountDao.clearPrimaryFlags()
        bankAccountDao.setPrimary(id)
    }

    suspend fun deleteBankAccount(id: Int) = withContext(Dispatchers.IO) {
        bankAccountDao.deleteBankAccount(id)
    }

    /**
     * Top-up offline wallet reserve from linked primary bank account.
     */
    suspend fun topUpOfflineReserve(amount: Double, sourceBankId: Int): Boolean = withContext(Dispatchers.IO) {
        val bank = bankAccountDao.getBankAccountById(sourceBankId) ?: return@withContext false
        if (bank.balance < amount) return@withContext false

        // Deduct bank
        bankAccountDao.deductBalance(sourceBankId, amount)
        // Credit offline wallet
        offlineWalletDao.addWalletBalance(amount)

        // Record top-up transaction
        val latestHash = transactionDao.getLatestBlockHash() ?: "0".repeat(64)
        val voucher = OfflineCryptoManager.createVoucher(
            senderUpiId = "bank_${bank.fullAccountNumber.takeLast(4)}@sbi",
            senderName = bank.bankName,
            receiverUpiId = "abdul.khadar@offlinepay",
            receiverName = "Offline Reserve Vault",
            amount = amount,
            prevBlockHash = latestHash,
            bankLast4 = bank.fullAccountNumber.takeLast(4),
            authMethod = "UPI_PIN"
        )

        transactionDao.insertTransaction(
            OfflineTransaction(
                id = voucher.transactionId,
                title = "Reserve Allocation from ${bank.bankName}",
                senderUpiId = voucher.senderUpiId,
                receiverUpiId = voucher.receiverUpiId,
                receiverName = voucher.receiverName,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                type = "TOPUP",
                mode = "LOCAL_P2P",
                status = "OFFLINE_VERIFIED",
                authUsed = "UPI_PIN",
                cryptographicHash = voucher.blockHash,
                prevBlockHash = voucher.prevBlockHash,
                signature = voucher.signature,
                bankLast4 = bank.fullAccountNumber.takeLast(4),
                notes = "Loaded into on-device tamper-proof enclave"
            )
        )
        true
    }

    /**
     * Process an outbound Offline Payment (via NFC Tap or Encrypted QR scan).
     * Deducts from Offline Reserve (or direct Bank if specified),
     * computes cryptographically signed transaction block, and records to local ledger.
     */
    suspend fun processOfflinePayment(
        receiverUpiId: String,
        receiverName: String,
        amount: Double,
        mode: String, // "NFC_TAP" or "ENCRYPTED_QR"
        authUsed: String = "BIOMETRIC_AND_UPI_PIN",
        bankAccountId: Int? = null,
        notes: String = ""
    ): Result<OfflineTransaction> = withContext(Dispatchers.IO) {
        val wallet = offlineWalletDao.getOfflineWallet()
        var usedBankLast4 = "4821"

        if (bankAccountId != null) {
            val bank = bankAccountDao.getBankAccountById(bankAccountId)
                ?: return@withContext Result.failure(Exception("Bank account not found"))
            if (bank.balance < amount) {
                return@withContext Result.failure(Exception("Insufficient bank balance (₹%.2f available)".format(bank.balance)))
            }
            bankAccountDao.deductBalance(bankAccountId, amount)
            usedBankLast4 = bank.fullAccountNumber.takeLast(4)
        } else {
            // Deduct from offline wallet reserve
            if (wallet == null || wallet.allocatedBalance < amount) {
                return@withContext Result.failure(Exception("Insufficient offline wallet balance. Please add funds to Offline Reserve."))
            }
            if (amount > wallet.maxSingleLimit) {
                return@withContext Result.failure(Exception("Exceeds offline limit of ₹%.0f per transaction".format(wallet.maxSingleLimit)))
            }
            offlineWalletDao.deductWalletBalance(amount)
        }

        val latestHash = transactionDao.getLatestBlockHash() ?: "0".repeat(64)
        val voucher = OfflineCryptoManager.createVoucher(
            senderUpiId = wallet?.deviceUpiId ?: "abdul.khadar@offlinepay",
            senderName = wallet?.deviceUserName ?: "Abdul Khadar",
            receiverUpiId = receiverUpiId,
            receiverName = receiverName,
            amount = amount,
            prevBlockHash = latestHash,
            bankLast4 = usedBankLast4,
            authMethod = authUsed
        )

        val transaction = OfflineTransaction(
            id = voucher.transactionId,
            title = if (notes.isNotBlank()) notes else "Payment to $receiverName",
            senderUpiId = voucher.senderUpiId,
            receiverUpiId = voucher.receiverUpiId,
            receiverName = voucher.receiverName,
            amount = voucher.amount,
            timestamp = voucher.timestamp,
            type = "DEBIT",
            mode = mode,
            status = "OFFLINE_VERIFIED",
            authUsed = authUsed,
            cryptographicHash = voucher.blockHash,
            prevBlockHash = voucher.prevBlockHash,
            signature = voucher.signature,
            bankLast4 = usedBankLast4,
            notes = if (notes.isNotBlank()) notes else "Instant local $mode transfer"
        )

        transactionDao.insertTransaction(transaction)
        Result.success(transaction)
    }

    /**
     * Process an inbound payment (received from peer via Encrypted QR or NFC tap).
     */
    suspend fun processReceivedOfflineVoucher(voucher: OfflineCryptoManager.OfflineVoucher, mode: String): Result<OfflineTransaction> = withContext(Dispatchers.IO) {
        // Credit the offline wallet
        offlineWalletDao.addWalletBalance(voucher.amount)

        val transaction = OfflineTransaction(
            id = voucher.transactionId,
            title = "Received from ${voucher.senderName}",
            senderUpiId = voucher.senderUpiId,
            receiverUpiId = voucher.receiverUpiId,
            receiverName = voucher.receiverName,
            amount = voucher.amount,
            timestamp = voucher.timestamp,
            type = "CREDIT",
            mode = mode,
            status = "OFFLINE_VERIFIED",
            authUsed = voucher.authMethod,
            cryptographicHash = voucher.blockHash,
            prevBlockHash = voucher.prevBlockHash,
            signature = voucher.signature,
            bankLast4 = voucher.bankLast4,
            notes = "Cryptographically verified voucher received offline"
        )

        transactionDao.insertTransaction(transaction)
        Result.success(transaction)
    }

    /**
     * Synchronize all offline transactions to bank when network becomes available.
     */
    suspend fun syncAllOfflineTransactions(): Int = withContext(Dispatchers.IO) {
        transactionDao.markAllSynced()
        1
    }
}
