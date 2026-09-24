package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.ui.PaymentViewModel
import com.example.ui.components.AddBankAccountDialog
import com.example.ui.components.UpiPinKeypadDialog
import com.example.ui.theme.PhonePeGreen
import com.example.ui.theme.PhonePePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()

    var showAddBankDialog by remember { mutableStateOf(false) }
    var verifyingBankForBalance by remember { mutableStateOf<BankAccount?>(null) }
    var revealedBalanceBankId by remember { mutableStateOf<Int?>(null) }

    if (showAddBankDialog) {
        AddBankAccountDialog(
            onAddAccount = { bankName, accNum, ifsc, holder, pin ->
                showAddBankDialog = false
                viewModel.addBankAccount(
                    bankName = bankName,
                    accountNumber = accNum,
                    ifscCode = ifsc,
                    accountHolder = holder,
                    upiPin = pin,
                    isPrimary = bankAccounts.isEmpty()
                )
            },
            onDismiss = { showAddBankDialog = false }
        )
    }

    verifyingBankForBalance?.let { bank ->
        UpiPinKeypadDialog(
            title = "Enter UPI PIN for ${bank.bankName}",
            subtitle = "Checking balance for A/C ${bank.accountNumber}",
            expectedPin = bank.upiPin,
            onPinSuccess = {
                revealedBalanceBankId = bank.id
                verifyingBankForBalance = null
            },
            onDismiss = { verifyingBankForBalance = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Linked Bank Accounts",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("bank_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PhonePePurple)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBankDialog = true },
                containerColor = PhonePePurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_bank_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bank")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PhonePeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unified Payments Interface (UPI) Secured",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PhonePeGreen
                    )
                }
            }

            items(bankAccounts) { bank ->
                val isRevealed = revealedBalanceBankId == bank.id
                val bankBgColor = try {
                    Color(android.graphics.Color.parseColor(bank.colorHex))
                } catch (e: Exception) {
                    PhonePePurple
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_card_${bank.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(bankBgColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = bankBgColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = bank.bankName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${bank.accountType} A/C ${bank.accountNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (bank.isPrimary) {
                                Box(
                                    modifier = Modifier
                                        .background(PhonePeGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Primary",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PhonePeGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "IFSC: ${bank.ifscCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Holder: ${bank.accountHolder}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isRevealed) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Available Balance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₹%.2f".format(bank.balance),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PhonePeGreen
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { verifyingBankForBalance = bank },
                                    colors = ButtonDefaults.buttonColors(containerColor = PhonePePurple.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("check_balance_bank_${bank.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = PhonePePurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check Balance",
                                        color = PhonePePurple,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!bank.isPrimary) {
                                TextButton(
                                    onClick = { viewModel.setPrimaryBank(bank.id) },
                                    modifier = Modifier.testTag("set_primary_bank_${bank.id}")
                                ) {
                                    Text("Set as Primary", color = PhonePePurple, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (bankAccounts.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteBankAccount(bank.id) },
                                    modifier = Modifier.testTag("delete_bank_${bank.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Bank",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
