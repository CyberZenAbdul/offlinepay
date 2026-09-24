package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.model.OfflineTransaction
import com.example.ui.PaymentViewModel
import com.example.ui.components.BiometricAuthModal
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.TopUpReserveDialog
import com.example.ui.components.UpiPinKeypadDialog
import com.example.ui.theme.OfflineOrange
import com.example.ui.theme.PhonePeGreen
import com.example.ui.theme.PhonePePurple
import com.example.ui.theme.PhonePePurpleDark
import com.example.ui.theme.PhonePePurpleLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: PaymentViewModel,
    modifier: Modifier = Modifier
) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val offlineWallet by viewModel.offlineWallet.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isSoundboxEnabled by viewModel.isSoundboxEnabled.collectAsStateWithLifecycle()
    val selectedTxn by viewModel.selectedTransaction.collectAsStateWithLifecycle()

    var showTopUpDialog by remember { mutableStateOf(false) }
    var showCheckBalanceAuth by remember { mutableStateOf(false) }
    var showUpiPinBalanceCheck by remember { mutableStateOf(false) }
    var revealedBalanceAccount by remember { mutableStateOf<BankAccount?>(null) }

    val primaryBank = bankAccounts.firstOrNull { it.isPrimary } ?: bankAccounts.firstOrNull()

    // Dialogs
    if (showTopUpDialog && bankAccounts.isNotEmpty()) {
        TopUpReserveDialog(
            bankAccounts = bankAccounts,
            onTopUpConfirm = { amount, bankId, _ ->
                showTopUpDialog = false
                viewModel.topUpReserve(amount, bankId)
            },
            onDismiss = { showTopUpDialog = false }
        )
    }

    if (showCheckBalanceAuth && primaryBank != null) {
        BiometricAuthModal(
            title = "Verify Identity to Check Balance",
            subtitle = "Touch sensor or use UPI PIN to reveal account balance",
            onSuccess = {
                showCheckBalanceAuth = false
                revealedBalanceAccount = primaryBank
            },
            onUsePin = {
                showCheckBalanceAuth = false
                showUpiPinBalanceCheck = true
            },
            onDismiss = { showCheckBalanceAuth = false }
        )
    }

    if (showUpiPinBalanceCheck && primaryBank != null) {
        UpiPinKeypadDialog(
            title = "Enter UPI PIN for ${primaryBank.bankName}",
            subtitle = "A/C ${primaryBank.accountNumber}",
            expectedPin = primaryBank.upiPin,
            onPinSuccess = {
                showUpiPinBalanceCheck = false
                revealedBalanceAccount = primaryBank
            },
            onDismiss = { showUpiPinBalanceCheck = false }
        )
    }

    selectedTxn?.let { txn ->
        ReceiptDetailDialog(
            transaction = txn,
            onDismiss = { viewModel.setSelectedTransaction(null) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. PhonePe Signature Header
        item {
            HeaderSection(
                userName = offlineWallet?.deviceUserName ?: "Abdul Khadar",
                upiId = offlineWallet?.deviceUpiId ?: "abdul.khadar@offlinepay",
                isSoundboxEnabled = isSoundboxEnabled,
                onToggleSoundbox = { viewModel.toggleSoundbox() },
                onScanClick = { viewModel.navigateTo("SCAN_QR") }
            )
        }

        // 2. Offline Mode Status Bar (Rural Focus)
        item {
            OfflineStatusBar(
                pendingSync = pendingSyncCount,
                onSyncClick = { viewModel.syncOfflineLedger() }
            )
        }

        // 3. Offline Reserve & Bank Balance Card
        item {
            WalletReserveCard(
                reserveBalance = offlineWallet?.allocatedBalance ?: 0.0,
                primaryBank = primaryBank,
                revealedBank = revealedBalanceAccount,
                onAddReserve = { showTopUpDialog = true },
                onCheckBankBalance = { showCheckBalanceAuth = true }
            )
        }

        // 4. PhonePe Money Transfer Grid
        item {
            TransferMoneyGrid(
                onScanQr = { viewModel.navigateTo("SCAN_QR") },
                onNfcTap = { viewModel.navigateTo("NFC_TAP") },
                onReceive = { viewModel.navigateTo("RECEIVE") },
                onBankAccounts = { viewModel.navigateTo("BANKS") },
                onLedger = { viewModel.navigateTo("LEDGER") }
            )
        }

        // 5. Rural Security Guarantee Banner
        item {
            RuralOfflineBanner()
        }

        // 6. Recent Transactions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Offline Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.navigateTo("LEDGER") },
                    modifier = Modifier.testTag("view_all_transactions_button")
                ) {
                    Text(text = "View Ledger", color = PhonePePurple, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(5)) { txn ->
                TransactionListItem(
                    transaction = txn,
                    onClick = { viewModel.setSelectedTransaction(txn) }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    userName: String,
    upiId: String,
    isSoundboxEnabled: Boolean,
    onToggleSoundbox: () -> Unit,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PhonePePurpleDark, PhonePePurple)
                )
            )
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile & ID
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString() ?: "U",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = userName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = upiId,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick actions: Soundbox voice toggle & Scan QR
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Soundbox speaker toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSoundboxEnabled) PhonePeGreen else Color.White.copy(alpha = 0.15f))
                        .clickable { onToggleSoundbox() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("soundbox_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Voice Soundbox",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSoundboxEnabled) "Voice ON" else "Muted",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("header_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineStatusBar(
    pendingSync: Int,
    onSyncClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF263238),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = PhonePeGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "100% Offline Mode Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Zero internet required • Local NFC & QR ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                }
            }

            if (pendingSync > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OfflineOrange)
                        .clickable { onSyncClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("sync_pending_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$pendingSync Pending",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletReserveCard(
    reserveBalance: Double,
    primaryBank: BankAccount?,
    revealedBank: BankAccount?,
    onAddReserve: () -> Unit,
    onCheckBankBalance: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Offline Vault Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = PhonePePurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Offline Reserve Vault",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PhonePePurple
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%.2f".format(reserveBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Instant low-latency local transfers (<100ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = PhonePeGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PhonePePurple.copy(alpha = 0.12f))
                        .clickable { onAddReserve() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("add_reserve_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PhonePePurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Money",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PhonePePurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Primary Bank Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = primaryBank?.bankName ?: "No Bank Linked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (primaryBank != null) "A/C ${primaryBank.accountNumber}" else "Tap to add bank",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (revealedBank != null && primaryBank != null && revealedBank.id == primaryBank.id) {
                    Text(
                        text = "₹%.2f".format(revealedBank.balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PhonePeGreen
                    )
                } else {
                    TextButton(
                        onClick = onCheckBankBalance,
                        modifier = Modifier.testTag("check_bank_balance_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PhonePePurple
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Check Balance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PhonePePurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferMoneyGrid(
    onScanQr: () -> Unit,
    onNfcTap: () -> Unit,
    onReceive: () -> Unit,
    onBankAccounts: () -> Unit,
    onLedger: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transfer Money (Offline)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionTile(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan QR",
                    badge = "Encrypted",
                    color = PhonePePurple,
                    onClick = onScanQr,
                    tag = "action_scan_qr"
                )

                ActionTile(
                    icon = Icons.Default.Nfc,
                    label = "Tap & Pay",
                    badge = "NFC P2P",
                    color = PhonePePurple,
                    onClick = onNfcTap,
                    tag = "action_nfc_tap"
                )

                ActionTile(
                    icon = Icons.Default.CallReceived,
                    label = "Receive",
                    badge = "QR & NFC",
                    color = PhonePeGreen,
                    onClick = onReceive,
                    tag = "action_receive"
                )

                ActionTile(
                    icon = Icons.Default.AccountBalance,
                    label = "Bank A/C",
                    badge = "UPI PIN",
                    color = PhonePePurple,
                    onClick = onBankAccounts,
                    tag = "action_bank_accounts"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary row: Offline Ledger & Sync
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onLedger() }
                    .testTag("action_open_ledger"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PhonePeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Encrypted On-Device Ledger",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SHA-256 Hash Chaining • Anti-tamper proof",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    badge: String,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RuralOfflineBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PhonePePurple.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(PhonePePurple.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = PhonePePurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Engineered for Rural & Remote India",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PhonePePurpleDark
                )
                Text(
                    text = "Pay at rural mandis, grocery shops, and weekly haats without mobile data or telecom coverage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: OfflineTransaction,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.timestamp))
    val isCredit = transaction.type == "CREDIT" || transaction.type == "TOPUP"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isCredit) PhonePeGreen.copy(alpha = 0.12f) else PhonePePurple.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.mode == "NFC_TAP") Icons.Default.Nfc else Icons.Default.QrCode,
                        contentDescription = null,
                        tint = if (isCredit) PhonePeGreen else PhonePePurple,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isCredit) transaction.senderUpiId else transaction.receiverName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "$formattedDate • ${if (transaction.mode == "NFC_TAP") "NFC" else "QR"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCredit) "+₹" else "-₹") + "%.2f".format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCredit) PhonePeGreen else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (transaction.status == "SYNCED") "Synced" else "Offline Verified",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (transaction.status == "SYNCED") PhonePeGreen else OfflineOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
