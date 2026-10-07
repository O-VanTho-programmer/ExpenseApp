package com.antigravity.expensetracker.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.local.entity.CategoryEntity
import com.antigravity.expensetracker.data.model.TransactionType
import com.antigravity.expensetracker.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    defaultCurrency: String = "VND",
    onDismiss: () -> Unit,
    onAddTransaction: (
        accountId: String,
        amount: Double,
        currency: String,
        type: TransactionType,
        counterparty: String,
        categoryId: String?,
        destinationAccountId: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var amountText by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }

    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var selectedDestinationAccount by remember {
        mutableStateOf(accounts.getOrNull(1))
    }

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.CREDIT) it.type == "INCOME"
        else it.type == "EXPENSE"
    }

    var selectedCategory by remember {
        mutableStateOf<CategoryEntity?>(filteredCategories.firstOrNull())
    }

    var accountExpanded by remember { mutableStateOf(false) }
    var destAccountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val activeCurrency = selectedAccount?.currency ?: defaultCurrency
    val parsedAmount = CurrencyFormatter.parseInputToAmount(amountText)
    val isAmountValid = parsedAmount != null && parsedAmount > 0.0
    val isCounterpartyValid = counterparty.isNotBlank()
    val isAccountValid = selectedAccount != null
    val canSubmit = isAmountValid && isCounterpartyValid && isAccountValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Transaction",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Transaction Type Selector (Expense, Income, Transfer) - Custom Pill Segmented Control
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf(
                            Triple(TransactionType.DEBIT, "Expense", Color(0xFFE53935)),
                            Triple(TransactionType.CREDIT, "Income", Color(0xFF43A047)),
                            Triple(TransactionType.TRANSFER, "Transfer", MaterialTheme.colorScheme.primary)
                        ).forEach { (type, label, activeColor) ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) activeColor
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedType = type
                                        if (type == TransactionType.DEBIT) {
                                            selectedCategory = categories.firstOrNull { it.type == "EXPENSE" }
                                        } else if (type == TransactionType.CREDIT) {
                                            selectedCategory = categories.firstOrNull { it.type == "INCOME" }
                                        } else {
                                            selectedCategory = null
                                        }
                                    }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Field with Live Currency Preview
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount *") },
                    placeholder = { Text("e.g. 50,000 or 15.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showErrors && !isAmountValid,
                    supportingText = {
                        if (showErrors && !isAmountValid) {
                            Text(text = "Please enter an amount > 0", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                text = "Preview: ${CurrencyFormatter.formatPreview(amountText, activeCurrency)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description / Counterparty
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = {
                        Text(
                            when (selectedType) {
                                TransactionType.CREDIT -> "Income Source / Note *"
                                TransactionType.TRANSFER -> "Transfer Description *"
                                else -> "Merchant / Note *"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                TransactionType.CREDIT -> "e.g. Salary, Freelance, Cash gift"
                                TransactionType.TRANSFER -> "e.g. Transfer to savings"
                                else -> "e.g. Highlands Coffee, Grab, Grocery"
                            }
                        )
                    },
                    singleLine = true,
                    isError = showErrors && !isCounterpartyValid,
                    supportingText = {
                        if (showErrors && !isCounterpartyValid) {
                            Text(text = "Please enter a description", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Account Selection
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (selectedType == TransactionType.TRANSFER) "From Account *" else "Account *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(acc.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${acc.type} • ${CurrencyFormatter.format(acc.currentBalance, acc.currency)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAccount = acc
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // If Transfer: Destination Account
                if (selectedType == TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = destAccountExpanded,
                        onExpandedChange = { destAccountExpanded = !destAccountExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDestinationAccount?.name ?: "Select Destination Account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("To Account *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destAccountExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = destAccountExpanded,
                            onDismissRequest = { destAccountExpanded = false }
                        ) {
                            accounts.filter { it.id != selectedAccount?.id }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedDestinationAccount = acc
                                        destAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category Selection (Optional / Filtered by type)
                if (selectedType != TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Uncategorized",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                selectedCategory?.let { cat ->
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex ?: ""))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                }
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Uncategorized)") },
                                onClick = {
                                    selectedCategory = null
                                    categoryExpanded = false
                                }
                            )
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val catColor = try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex ?: ""))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat.name)
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit && selectedAccount != null) {
                        val amount = parsedAmount ?: 0.0
                        onAddTransaction(
                            selectedAccount!!.id,
                            amount,
                            activeCurrency,
                            selectedType,
                            counterparty.trim(),
                            selectedCategory?.id,
                            if (selectedType == TransactionType.TRANSFER) selectedDestinationAccount?.id else null
                        )
                        onDismiss()
                    } else {
                        showErrors = true
                    }
                }
            ) {
                Text("Add Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
