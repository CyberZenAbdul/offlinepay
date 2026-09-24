package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.p2p.NfcPaymentManager
import com.example.ui.PaymentUiState
import com.example.ui.PaymentViewModel
import com.example.ui.components.BiometricAuthModal
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.UpiPinKeypadDialog
import com.example.ui.theme.PhonePeGreen
import com.example.ui.theme.PhonePePurple
import com.example.ui.theme.PhonePePurpleDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcTapPayScreen(
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val offlineWallet by viewModel.offlineWallet.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()

    var selectedPeer by remember { mutableStateOf(NfcPaymentManager.DEMO_NEARBY_PEERS[0]) }
    var amountText by remember { mutableStateOf("250") }
    var noteText by remember { mutableStateOf("Kirana Groceries") }
    var useBankDirectly by remember { mutableStateOf(false) }
    var selectedBankId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: 0) }

    // Security Verification Steps
    var showBiometrics by remember { mutableStateOf(false) }
    var showUpiPin by remember { mutableStateOf(false) }
    var pendingPaymentPayload by remember { mutableStateOf<Triple<Double, String, Int?>?>(null) }

    // Pulsing NFC wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_waves")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_wave_scale"
    )

    // Handle dialogs
    if (showBiometrics) {
        BiometricAuthModal(
            title = "Authorize NFC Tap Payment",
            subtitle = "Place finger on sensor to sign offline token",
            amountText = "₹$amountText",
            onSuccess = {
                showBiometrics = false
                showUpiPin = true
            },
            onUsePin = {
                showBiometrics = false
                showUpiPin = true
            },
            onDismiss = { showBiometrics = false }
        )
    }

    if (showUpiPin) {
        val selectedBank = bankAccounts.find { it.id == selectedBankId }
        val expectedPin = if (useBankDirectly && selectedBank != null) selectedBank.upiPin else "1234"

        UpiPinKeypadDialog(
            title = "Enter UPI PIN",
            subtitle = "Paying ₹$amountText to ${selectedPeer.name}",
            expectedPin = expectedPin,
            onPinSuccess = {
                showUpiPin = false
                val amt = amountText.toDoubleOrNull() ?: 0.0
                viewModel.sendOfflinePayment(
                    receiverUpiId = selectedPeer.upiId,
                    receiverName = selectedPeer.name,
                    amount = amt,
                    mode = "NFC_TAP",
                    authUsed = "BIOMETRIC_AND_UPI_PIN",
                    bankAccountId = if (useBankDirectly) selectedBankId else null,
                    notes = noteText
                )
            },
            onDismiss = { showUpiPin = false }
        )
    }

    // Success receipt sheet
    if (paymentState is PaymentUiState.Success) {
        val successState = paymentState as PaymentUiState.Success
        ReceiptDetailDialog(
            transaction = successState.transaction,
            onDismiss = {
                viewModel.clearPaymentState()
                onBack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NFC Tap & Pay",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nfc_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PhonePePurple)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // NFC Antenna Sensor Area with Waves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer wave
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(waveScale)
                        .background(PhonePePurple.copy(alpha = 0.08f), CircleShape)
                )
                // Middle wave
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(waveScale * 0.95f)
                        .background(PhonePePurple.copy(alpha = 0.15f), CircleShape)
                )
                // Center NFC icon circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(PhonePePurple, CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Active",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Text(
                text = "Hold Phone Near Recipient or Terminal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PhonePePurpleDark
            )
            Text(
                text = "Low-latency local P2P wireless communication (<100ms)",
                style = MaterialTheme.typography.bodySmall,
                color = PhonePeGreen,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nearby NFC Peers Selection (Simulated & Hardware Detection)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Nearby Receiver / Terminal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(PhonePeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NFC Ready",
                                color = PhonePeGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (peer in NfcPaymentManager.DEMO_NEARBY_PEERS) {
                            val isSelected = peer.id == selectedPeer.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedPeer = peer }
                                    .testTag("select_peer_${peer.id}"),
                                color = if (isSelected) PhonePePurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PhonePePurple) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(if (isSelected) PhonePePurple else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Store,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = peer.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${peer.category} • ${peer.upiId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PhonePePurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transfer Amount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                        label = { Text("Amount (₹)") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nfc_amount_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50", "150", "250", "500").forEach { chip ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { amountText = chip }
                                    .testTag("nfc_chip_$chip"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (amountText == chip) PhonePePurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "₹$chip",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (amountText == chip) PhonePePurple else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Source Selection (Offline Reserve Vault vs Direct Bank)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payment Source",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option A: Offline Reserve
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { useBankDirectly = false }
                            .background(if (!useBankDirectly) PhonePePurple.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = PhonePePurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Offline Reserve Vault",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Available: ₹%.2f • Instant Local Tap".format(offlineWallet?.allocatedBalance ?: 0.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PhonePeGreen
                                )
                            }
                        }
                        if (!useBankDirectly) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = PhonePePurple)
                        }
                    }

                    // Option B: Bank Account
                    val primaryBank = bankAccounts.firstOrNull { it.isPrimary } ?: bankAccounts.firstOrNull()
                    if (primaryBank != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    useBankDirectly = true
                                    selectedBankId = primaryBank.id
                                }
                                .background(if (useBankDirectly) PhonePePurple.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = PhonePePurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${primaryBank.bankName} (A/C ${primaryBank.accountNumber})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Requires UPI PIN authorization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (useBankDirectly) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = PhonePePurple)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pay Button with Biometric/UPI trigger
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        showBiometrics = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("nfc_pay_now_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PhonePePurple)
            ) {
                if (paymentState is PaymentUiState.Processing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Signing Cryptographic Token...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Authenticate & Tap to Pay ₹$amountText",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (paymentState is PaymentUiState.Error) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = (paymentState as PaymentUiState.Error).errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
