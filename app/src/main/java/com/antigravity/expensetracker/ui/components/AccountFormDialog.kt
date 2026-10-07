package com.antigravity.expensetracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.model.AccountType
import com.antigravity.expensetracker.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormDialog(
    initialAccount: AccountEntity? = null,
    defaultCurrency: String = "VND",
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, currency: String, balance: Double, mask: String?) -> Unit
) {
    val isEditing = initialAccount != null
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(
            try {
                AccountType.valueOf(initialAccount?.type ?: "CHECKING")
            } catch (e: Exception) {
                AccountType.CHECKING
            }
        )
    }
    var selectedCurrency by remember { mutableStateOf(initialAccount?.currency ?: defaultCurrency) }
    var balanceText by remember {
        mutableStateOf(
            if (initialAccount != null) {
                if (initialAccount.currentBalance % 1.0 == 0.0) {
                    initialAccount.currentBalance.toLong().toString()
                } else {
                    initialAccount.currentBalance.toString()
                }
            } else {
                "0"
            }
        )
    }
    var maskText by remember { mutableStateOf(initialAccount?.identifierMask ?: "") }

    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    // Validation errors state
    var showErrors by remember { mutableStateOf(false) }

    val isNameValid = name.isNotBlank()
    val parsedBalance = CurrencyFormatter.parseInputToAmount(balanceText)
    val isBalanceValid = parsedBalance != null && parsedBalance >= 0.0
    val isMaskValid = maskText.isBlank() || maskText.trim().length in 2..8

    val canSubmit = isNameValid && isBalanceValid && isMaskValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Account" else "Add New Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name *") },
                    placeholder = { Text("e.g. Vietcombank, Cash Wallet, Chase") },
                    singleLine = true,
                    isError = showErrors && !isNameValid,
                    supportingText = {
                        if (showErrors && !isNameValid) {
                            Text(text = "Account name is required", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Account Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Currency Dropdown (Select Input)
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                ) {
                    val currentOption = CurrencyFormatter.SUPPORTED_CURRENCIES.firstOrNull { it.code == selectedCurrency }
                    OutlinedTextField(
                        value = currentOption?.displayName ?: selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        CurrencyFormatter.SUPPORTED_CURRENCIES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    selectedCurrency = option.code
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance with Live Currency Formatting & Preview
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(if (isEditing) "Current Balance *" else "Initial Balance *") },
                    placeholder = { Text("e.g. 5,000,000 or 150.00") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    isError = showErrors && !isBalanceValid,
                    supportingText = {
                        Column {
                            if (showErrors && !isBalanceValid) {
                                Text(
                                    text = "Please enter a valid balance (≥ 0)",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "Preview: ${CurrencyFormatter.formatPreview(balanceText, selectedCurrency)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Identifier Mask
                OutlinedTextField(
                    value = maskText,
                    onValueChange = { maskText = it },
                    label = { Text("Account Identifier / Tail (Optional)") },
                    placeholder = { Text("e.g. 4242 or 8899") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    isError = showErrors && !isMaskValid,
                    supportingText = {
                        if (showErrors && !isMaskValid) {
                            Text(
                                text = "Identifier mask must be 2 to 8 characters",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Last digits matched against incoming bank notifications",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        val amount = parsedBalance ?: 0.0
                        onSave(
                            name.trim(),
                            selectedType,
                            selectedCurrency,
                            amount,
                            maskText.trim().ifBlank { null }
                        )
                        onDismiss()
                    } else {
                        showErrors = true
                    }
                }
            ) {
                Text(if (isEditing) "Save Changes" else "Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
