package com.example

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.OfflineCryptoManager
import com.example.p2p.NfcPaymentManager
import com.example.ui.PaymentViewModel
import com.example.ui.screens.BankAccountsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.NfcTapPayScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.ScanQrScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PhonePePurple
import java.nio.charset.StandardCharsets

class MainActivity : FragmentActivity() {

    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNfcIntent(intent)

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        OfflinePayBottomBar(
                            currentScreen = currentScreen,
                            onNavigate = { screen -> viewModel.navigateTo(screen) }
                        )
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "HOME" -> HomeScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        "NFC_TAP" -> NfcTapPayScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("HOME") }
                        )
                        "SCAN_QR" -> ScanQrScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("HOME") }
                        )
                        "RECEIVE" -> ReceiveScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("HOME") }
                        )
                        "BANKS" -> BankAccountsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("HOME") }
                        )
                        "LEDGER" -> LedgerScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("HOME") }
                        )
                        else -> HomeScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val msg = rawMessages[0] as NdefMessage
                for (record in msg.records) {
                    val mimeType = String(record.type, StandardCharsets.US_ASCII)
                    if (mimeType == NfcPaymentManager.NFC_MIME_TYPE) {
                        val payload = String(record.payload, StandardCharsets.UTF_8)
                        val voucher = OfflineCryptoManager.decodeVoucherFromQrString(payload)
                        if (voucher != null) {
                            viewModel.receiveOfflineVoucher(voucher, mode = "NFC_TAP")
                            viewModel.navigateTo("LEDGER")
                        }
                    }
                }
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem("HOME", "Home", Icons.Default.Home)
    data object NfcTap : BottomNavItem("NFC_TAP", "Tap & Pay", Icons.Default.Nfc)
    data object Receive : BottomNavItem("RECEIVE", "Receive", Icons.Default.QrCode)
    data object Banks : BottomNavItem("BANKS", "Banks", Icons.Default.AccountBalance)
    data object Ledger : BottomNavItem("LEDGER", "History", Icons.Default.History)
}

@Composable
fun OfflinePayBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.NfcTap,
        BottomNavItem.Receive,
        BottomNavItem.Banks,
        BottomNavItem.Ledger
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PhonePePurple,
                    selectedTextColor = PhonePePurple,
                    indicatorColor = PhonePePurple.copy(alpha = 0.12f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("bottom_nav_${item.route.lowercase()}")
            )
        }
    }
}
