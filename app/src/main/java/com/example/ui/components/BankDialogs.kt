package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BankAccount
import com.example.ui.theme.PhonePeGreen
import com.example.ui.theme.PhonePePurple

val POPULAR_BANKS = listOf(
    "State Bank of India",
    "HDFC Bank",
    "ICICI Bank",
    "Punjab National Bank",
    "Axis Bank",
    "Bank of Baroda",
    "Canara Bank",
    "Kotak Mahindra Bank"
)

/**
 * Dialog for adding and linking a new bank account.
 */
@Composable
fun AddBankAccountDialog(
    onAddAccount: (bankName: String, accNum: String, ifsc: String, holder: String, pin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBank by remember { mutableStateOf(POPULAR_BANKS[0]) }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("SBIN0002481") }
    var accountHolder by remember { mutableStateOf("Abdul Khadar") }
    var upiPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PhonePePurple.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PhonePePurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Bank Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PhonePePurple
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_bank_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Bank",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Bank selector chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rows = POPULAR_BANKS.chunked(2)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (bank in row) {
                                val isSelected = bank == selectedBank
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedBank = bank
                                            ifscCode = when {
                                                bank.contains("HDFC") -> "HDFC0001020"
                                                bank.contains("ICICI") -> "ICIC0000345"
                                                bank.contains("Axis") -> "UTIB0000889"
                                                bank.contains("Punjab") -> "PUNB0123400"
                                                bank.contains("Baroda") -> "BARB0VILMND"
                                                else -> "SBIN0002481"
                                            }
                                        }
                                        .testTag("select_bank_${bank.replace(" ", "_")}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PhonePePurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = bank,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PhonePePurple else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { if (it.length <= 18 && it.all { c -> c.isDigit() }) accountNumber = it },
                    label = { Text("Account Number") },
                    placeholder = { Text("e.g. 104928374920") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_number_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it.uppercase() },
                    label = { Text("IFSC Code") },
                    placeholder = { Text("e.g. SBIN0001234") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ifsc_code_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it },
                    label = { Text("Account Holder Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_holder_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = upiPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) upiPin = it },
                    label = { Text("Set 4 or 6 Digit UPI PIN") },
                    placeholder = { Text("••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_upi_pin_input"),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (accountNumber.length < 8) {
                            errorMessage = "Please enter a valid account number (minimum 8 digits)"
                        } else if (ifscCode.length < 6) {
                            errorMessage = "Please enter a valid IFSC code"
                        } else if (upiPin.length != 4 && upiPin.length != 6) {
                            errorMessage = "UPI PIN must be 4 or 6 digits"
                        } else {
                            onAddAccount(selectedBank, accountNumber, ifscCode, accountHolder, upiPin)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_add_bank_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PhonePePurple)
                ) {
                    Text("Link Bank & Set UPI PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Top up offline wallet reserve from bank account.
 */
@Composable
fun TopUpReserveDialog(
    bankAccounts: List<BankAccount>,
    onTopUpConfirm: (amount: Double, bankId: Int, pin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBankId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: 0) }
    var amountText by remember { mutableStateOf("1000") }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val presetAmounts = listOf("500", "1000", "2000")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PhonePeGreen.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = PhonePeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Offline Reserve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PhonePeGreen
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_topup_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pre-load offline balance into your on-device secure vault for instant zero-latency NFC & QR payments in zero-internet areas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick preset amounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (preset in presetAmounts) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { amountText = preset }
                                .testTag("topup_preset_$preset"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (amountText == preset) PhonePePurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "₹$preset",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (amountText == preset) PhonePePurple else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topup_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Select Bank Account
                Text(
                    text = "Debiting From Bank:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (bank in bankAccounts) {
                        val isSelected = bank.id == selectedBankId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBankId = bank.id }
                                .testTag("topup_select_bank_${bank.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PhonePePurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = bank.bankName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A/C ${bank.accountNumber} • Bal: ₹%.2f".format(bank.balance),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Selected",
                                        tint = PhonePePurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) enteredPin = it },
                    label = { Text("Enter Bank UPI PIN") },
                    placeholder = { Text("Default: 1234") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topup_upi_pin_input"),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val selectedBank = bankAccounts.find { it.id == selectedBankId }
                        if (amount <= 0) {
                            errorMessage = "Please enter an amount greater than 0"
                        } else if (selectedBank != null && enteredPin != selectedBank.upiPin) {
                            errorMessage = "Incorrect UPI PIN for ${selectedBank.bankName}. Default: ${selectedBank.upiPin}"
                        } else {
                            onTopUpConfirm(amount, selectedBankId, enteredPin)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_topup_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PhonePeGreen)
                ) {
                    Text("Authorize & Add to Offline Reserve", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
