package com.antigravity.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.ui.components.AccountCard
import com.antigravity.expensetracker.ui.components.AccountFormDialog
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel

@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Financial Accounts",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage your bank accounts, e-wallets, and cash reserves.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(accounts, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    onEdit = { accountToEdit = it },
                    onDelete = { accountToDelete = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddAccountDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AccountFormDialog(
            initialAccount = null,
            defaultCurrency = defaultCurrency,
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, curr, bal, mask, savingSubType ->
                viewModel.addAccount(name, type, curr, bal, mask, savingSubType)
            }
        )
    }

    // Edit Account Dialog
    accountToEdit?.let { account ->
        AccountFormDialog(
            initialAccount = account,
            onDismiss = { accountToEdit = null },
            onSave = { name, type, curr, bal, mask, savingSubType ->
                viewModel.updateAccount(
                    account.copy(
                        name = name,
                        type = type.name,
                        currency = curr,
                        currentBalance = bal,
                        identifierMask = mask,
                        savingSubType = if (type == com.antigravity.expensetracker.data.model.AccountType.SAVINGS) savingSubType else null
                    )
                )
            }
        )
    }

    // Delete Confirmation Dialog
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${account.name}\"? All associated transactions linked to this account will also be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
