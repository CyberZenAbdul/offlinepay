package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.OfflineCryptoManager
import com.example.data.AppDatabase
import com.example.data.model.BankAccount
import com.example.data.model.OfflineTransaction
import com.example.data.model.OfflineWallet
import com.example.data.repository.PaymentRepository
import com.example.p2p.NfcPaymentManager
import com.example.util.SoundboxHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data class Processing(val message: String) : PaymentUiState
    data class Success(val transaction: OfflineTransaction, val message: String) : PaymentUiState
    data class Error(val errorMessage: String) : PaymentUiState
}

enum class TransactionFilter {
    ALL, DEBITS, CREDITS, PENDING_SYNC
}

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = PaymentRepository(
        database.bankAccountDao(),
        database.offlineWalletDao(),
        database.transactionDao()
    )

    val soundboxHelper = SoundboxHelper(application)
    val nfcManager = NfcPaymentManager(application)

    // Data streams
    val bankAccounts: StateFlow<List<BankAccount>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineWallet: StateFlow<OfflineWallet?> = repository.offlineWallet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<OfflineTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filter
    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<OfflineTransaction>> = combine(
        allTransactions,
        _selectedFilter
    ) { txns, filter ->
        when (filter) {
            TransactionFilter.ALL -> txns
            TransactionFilter.DEBITS -> txns.filter { it.type == "DEBIT" }
            TransactionFilter.CREDITS -> txns.filter { it.type == "CREDIT" || it.type == "TOPUP" }
            TransactionFilter.PENDING_SYNC -> txns.filter { it.status == "OFFLINE_VERIFIED" || it.status == "PENDING_SYNC" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for transactions
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    // Soundbox voice enabled toggle
    private val _isSoundboxEnabled = MutableStateFlow(true)
    val isSoundboxEnabled: StateFlow<Boolean> = _isSoundboxEnabled.asStateFlow()

    // Active Tab/Screen navigation: HOME, NFC_TAP, SCAN_QR, RECEIVE, BANKS, LEDGER
    private val _currentScreen = MutableStateFlow("HOME")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Selected Transaction for receipt detail modal
    private val _selectedTransaction = MutableStateFlow<OfflineTransaction?>(null)
    val selectedTransaction: StateFlow<OfflineTransaction?> = _selectedTransaction.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun toggleSoundbox() {
        _isSoundboxEnabled.value = !_isSoundboxEnabled.value
    }

    fun setSelectedTransaction(txn: OfflineTransaction?) {
        _selectedTransaction.value = txn
    }

    fun clearPaymentState() {
        _paymentState.value = PaymentUiState.Idle
    }

    /**
     * Executes an outbound offline payment (via NFC Tap or QR).
     */
    fun sendOfflinePayment(
        receiverUpiId: String,
        receiverName: String,
        amount: Double,
        mode: String,
        authUsed: String = "BIOMETRIC_AND_UPI_PIN",
        bankAccountId: Int? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing("Securing local cryptographic proof...")
            val result = repository.processOfflinePayment(
                receiverUpiId = receiverUpiId,
                receiverName = receiverName,
                amount = amount,
                mode = mode,
                authUsed = authUsed,
                bankAccountId = bankAccountId,
                notes = notes
            )

            result.fold(
                onSuccess = { txn ->
                    _paymentState.value = PaymentUiState.Success(txn, "Offline payment of ₹%.2f to %s verified!".format(amount, receiverName))
                    if (_isSoundboxEnabled.value) {
                        soundboxHelper.announcePayment(amount, receiverName, isCredit = false)
                    } else {
                        soundboxHelper.triggerHapticSuccess()
                    }
                },
                onFailure = { error ->
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Transaction failed")
                }
            )
        }
    }

    /**
     * Receives an offline payment voucher scanned via QR or tapped via NFC.
     */
    fun receiveOfflineVoucher(voucher: OfflineCryptoManager.OfflineVoucher, mode: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing("Verifying cryptographic signature & nonces...")
            val result = repository.processReceivedOfflineVoucher(voucher, mode)
            result.fold(
                onSuccess = { txn ->
                    _paymentState.value = PaymentUiState.Success(txn, "Received ₹%.2f offline from %s!".format(voucher.amount, voucher.senderName))
                    if (_isSoundboxEnabled.value) {
                        soundboxHelper.announcePayment(voucher.amount, voucher.senderName, isCredit = true)
                    } else {
                        soundboxHelper.triggerHapticSuccess()
                    }
                },
                onFailure = { error ->
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Invalid voucher")
                }
            )
        }
    }

    /**
     * Top-up offline reserve balance from bank account.
     */
    fun topUpReserve(amount: Double, bankId: Int) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing("Allocating offline reserve balance...")
            val success = repository.topUpOfflineReserve(amount, bankId)
            if (success) {
                _paymentState.value = PaymentUiState.Success(
                    OfflineTransaction(
                        id = "TOPUP-${System.currentTimeMillis()}",
                        title = "Reserve Allocation",
                        senderUpiId = "bank@self",
                        receiverUpiId = "wallet@self",
                        receiverName = "Offline Vault",
                        amount = amount,
                        timestamp = System.currentTimeMillis(),
                        type = "TOPUP",
                        mode = "LOCAL_P2P",
                        status = "OFFLINE_VERIFIED",
                        authUsed = "UPI_PIN",
                        cryptographicHash = "",
                        prevBlockHash = "",
                        signature = "",
                        bankLast4 = "",
                        notes = "Top-up completed"
                    ),
                    "₹%.2f successfully added to Offline Reserve!".format(amount)
                )
                soundboxHelper.triggerHapticSuccess()
            } else {
                _paymentState.value = PaymentUiState.Error("Failed to allocate reserve. Check bank balance.")
            }
        }
    }

    /**
     * Add new Bank Account
     */
    fun addBankAccount(
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        accountHolder: String,
        upiPin: String,
        initialBalance: Double = 25000.0,
        isPrimary: Boolean = false
    ) {
        viewModelScope.launch {
            val last4 = if (accountNumber.length >= 4) accountNumber.takeLast(4) else accountNumber
            val masked = "•••• •••• $last4"
            val bankColor = when {
                bankName.contains("SBI", ignoreCase = true) || bankName.contains("State Bank", ignoreCase = true) -> "#1A237E"
                bankName.contains("HDFC", ignoreCase = true) -> "#004B87"
                bankName.contains("ICICI", ignoreCase = true) -> "#A31922"
                bankName.contains("Axis", ignoreCase = true) -> "#800020"
                bankName.contains("Punjab", ignoreCase = true) || bankName.contains("PNB", ignoreCase = true) -> "#B71C1C"
                bankName.contains("Baroda", ignoreCase = true) -> "#E65100"
                else -> "#5F259F"
            }

            val newAccount = BankAccount(
                bankName = bankName,
                accountNumber = masked,
                fullAccountNumber = accountNumber,
                ifscCode = ifscCode.uppercase(),
                accountHolder = accountHolder,
                accountType = "Savings",
                upiPin = upiPin,
                balance = initialBalance,
                isPrimary = isPrimary,
                colorHex = bankColor
            )

            repository.addBankAccount(newAccount)
            soundboxHelper.triggerHapticSuccess()
        }
    }

    fun setPrimaryBank(id: Int) {
        viewModelScope.launch {
            repository.setPrimaryBankAccount(id)
        }
    }

    fun deleteBankAccount(id: Int) {
        viewModelScope.launch {
            repository.deleteBankAccount(id)
        }
    }

    /**
     * Synchronize all offline transactions to bank server when connectivity returns.
     */
    fun syncOfflineLedger() {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing("Simulating bank settlement sync...")
            kotlinx.coroutines.delay(1200) // Realistic sync delay
            repository.syncAllOfflineTransactions()
            _paymentState.value = PaymentUiState.Success(
                OfflineTransaction(
                    id = "SYNC-${System.currentTimeMillis()}",
                    title = "Ledger Synchronized",
                    senderUpiId = "all@offlinepay",
                    receiverUpiId = "npci.bank@settlement",
                    receiverName = "NPCI Offline Settlement",
                    amount = 0.0,
                    timestamp = System.currentTimeMillis(),
                    type = "SYNC",
                    mode = "LOCAL_P2P",
                    status = "SYNCED",
                    authUsed = "SYSTEM",
                    cryptographicHash = "",
                    prevBlockHash = "",
                    signature = "",
                    bankLast4 = "",
                    notes = "All local cryptographic proofs reconciled"
                ),
                "Offline transaction ledger successfully reconciled with bank!"
            )
            soundboxHelper.triggerHapticSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundboxHelper.release()
    }
}
