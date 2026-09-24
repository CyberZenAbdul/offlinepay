package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankAccount
import com.example.data.model.OfflineTransaction
import com.example.data.model.OfflineWallet
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY isPrimary DESC, id ASC")
    fun getAllBankAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryBankAccount(): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getBankAccountById(id: Int): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccount): Long

    @Update
    suspend fun updateBankAccount(account: BankAccount)

    @Query("UPDATE bank_accounts SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE bank_accounts SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: Int)

    @Query("UPDATE bank_accounts SET balance = balance - :amount WHERE id = :id")
    suspend fun deductBalance(id: Int, amount: Double)

    @Query("UPDATE bank_accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun addBalance(id: Int, amount: Double)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteBankAccount(id: Int)
}

@Dao
interface OfflineWalletDao {
    @Query("SELECT * FROM offline_wallet WHERE id = 1 LIMIT 1")
    fun getOfflineWalletFlow(): Flow<OfflineWallet?>

    @Query("SELECT * FROM offline_wallet WHERE id = 1 LIMIT 1")
    suspend fun getOfflineWallet(): OfflineWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: OfflineWallet)

    @Query("UPDATE offline_wallet SET allocatedBalance = allocatedBalance - :amount, spentToday = spentToday + :amount WHERE id = 1")
    suspend fun deductWalletBalance(amount: Double)

    @Query("UPDATE offline_wallet SET allocatedBalance = allocatedBalance + :amount WHERE id = 1")
    suspend fun addWalletBalance(amount: Double)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM offline_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<OfflineTransaction>>

    @Query("SELECT * FROM offline_transactions ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTransactions(): Flow<List<OfflineTransaction>>

    @Query("SELECT * FROM offline_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): OfflineTransaction?

    @Query("SELECT cryptographicHash FROM offline_transactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBlockHash(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: OfflineTransaction)

    @Query("UPDATE offline_transactions SET status = 'SYNCED' WHERE status = 'OFFLINE_VERIFIED' OR status = 'PENDING_SYNC'")
    suspend fun markAllSynced()

    @Query("SELECT COUNT(*) FROM offline_transactions WHERE status = 'OFFLINE_VERIFIED' OR status = 'PENDING_SYNC'")
    fun getPendingSyncCount(): Flow<Int>
}
