package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.OfflineCryptoManager
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
fun ScanQrScreen(
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()

    var manualInput by remember { mutableStateOf("") }
    var detectedVoucher by remember { mutableStateOf<OfflineCryptoManager.OfflineVoucher?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Security Verification
    var showBiometrics by remember { mutableStateOf(false) }
    var showUpiPin by remember { mutableStateOf(false) }

    // Laser scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    if (showBiometrics) {
        BiometricAuthModal(
            title = "Authorize QR Payment",
            subtitle = "Biometric approval required to sign voucher transfer",
            amountText = "₹%.2f".format(detectedVoucher?.amount ?: 0.0),
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
        val primaryBank = bankAccounts.firstOrNull { it.isPrimary } ?: bankAccounts.firstOrNull()
        UpiPinKeypadDialog(
            title = "Enter UPI PIN",
            subtitle = "Paying ₹%.2f to %s".format(detectedVoucher?.amount ?: 0.0, detectedVoucher?.receiverName ?: "Merchant"),
            expectedPin = primaryBank?.upiPin ?: "1234",
            onPinSuccess = {
                showUpiPin = false
                detectedVoucher?.let { v ->
                    viewModel.sendOfflinePayment(
                        receiverUpiId = v.receiverUpiId,
                        receiverName = v.receiverName,
                        amount = v.amount,
                        mode = "ENCRYPTED_QR",
                        authUsed = "BIOMETRIC_AND_UPI_PIN",
                        bankAccountId = null,
                        notes = "Encrypted QR payment at " + v.receiverName
                    )
                }
            },
            onDismiss = { showUpiPin = false }
        )
    }

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
                        text = "Scan Encrypted QR",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scan_back_button")) {
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
                .background(Color(0xFF100E17))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Point Camera at Recipient's Offline QR Code",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Tamper-evident cryptographically signed vouchers",
                style = MaterialTheme.typography.bodySmall,
                color = PhonePeGreen,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Scanner Viewfinder Box
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(2.dp, PhonePePurple, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Four corner brackets
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(160.dp)
                )

                // Laser line moving up and down
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = (laserOffset - 100).dp)
                        .background(PhonePeGreen)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sample Quick Scan Vouchers for Instant Testing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B26))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Demo: Scan Sample Offline Merchant QRs",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Simulates instant QR detection without requiring a 2nd physical phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val sampleQrs = listOf(
                        Triple("Sharma Ji Kirana Store", "sharma.kirana@offlinepay", 150.0),
                        Triple("Ramesh Mandi Vegetables", "ramesh.farmer@offlinepay", 340.0),
                        Triple("Gupta Fresh Tea Stall", "gupta.tea@offlinepay", 30.0)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((name, upi, amt) in sampleQrs) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val voucher = OfflineCryptoManager.createVoucher(
                                            senderUpiId = "self@offlinepay",
                                            senderName = "Self",
                                            receiverUpiId = upi,
                                            receiverName = name,
                                            amount = amt,
                                            prevBlockHash = "0".repeat(64),
                                            bankLast4 = "4821",
                                            authMethod = "BIOMETRIC_AND_UPI_PIN"
                                        )
                                        detectedVoucher = voucher
                                        showBiometrics = true
                                    }
                                    .testTag("scan_sample_${name.replace(" ", "_")}"),
                                color = PhonePePurple.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PhonePePurple.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = PhonePeGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = name,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = upi,
                                                color = Color(0xFFB0BEC5),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Pay ₹%.0f".format(amt),
                                        color = PhonePeGreen,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Code Input Option
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B26))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paste or Enter Encrypted Token",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("offlinepay://otv/...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_qr_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val voucher = OfflineCryptoManager.decodeVoucherFromQrString(manualInput)
                            if (voucher != null) {
                                detectedVoucher = voucher
                                showBiometrics = true
                                errorMessage = ""
                            } else {
                                errorMessage = "Invalid or tampered offline payment voucher string"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("verify_manual_token_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PhonePePurple)
                    ) {
                        Text("Decode & Authorize Payment", fontWeight = FontWeight.Bold)
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
