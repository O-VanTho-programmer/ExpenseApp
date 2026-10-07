package com.antigravity.expensetracker.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.model.AccountType
import com.antigravity.expensetracker.data.model.TransactionType
import com.antigravity.expensetracker.ui.components.AccountFormDialog
import com.antigravity.expensetracker.ui.components.AddTransactionDialog
import com.antigravity.expensetracker.ui.components.CategoryPieChartCard
import com.antigravity.expensetracker.ui.components.SavingsHeaderCard
import com.antigravity.expensetracker.ui.components.SavingsPortfolioDialog
import com.antigravity.expensetracker.ui.components.SimulationDialog
import com.antigravity.expensetracker.ui.components.SummaryCard
import com.antigravity.expensetracker.ui.components.TransactionCard
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import com.antigravity.expensetracker.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val liquidBalance by viewModel.liquidBalance.collectAsState()
    val savingsBalance by viewModel.savingsBalance.collectAsState()
    val savingsAccounts by viewModel.savingsAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val expenseBreakdown by viewModel.expenseBreakdownByCategory.collectAsState()

    var showSimulationDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showSavingsPortfolio by remember { mutableStateOf(false) }
    var showAddSavingsAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    var isPermissionGranted by remember {
        mutableStateOf(PermissionHelper.isNotificationAccessGranted(context))
    }

    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Notification Permission Banner
                if (!isPermissionGranted) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Access Disabled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Enable access to auto-ingest incoming bank alerts.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            ElevatedButton(
                                onClick = {
                                    PermissionHelper.openNotificationAccessSettings(context)
                                    isPermissionGranted = PermissionHelper.isNotificationAccessGranted(context)
                                },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Swipeable Header (Swipe right/left between Balance Summary and Category Pie Chart)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    if (page == 0) {
                        SummaryCard(
                            totalBalance = liquidBalance,
                            totalIncome = totalIncome,
                            totalExpenses = totalExpenses,
                            currency = defaultCurrency
                        )
                    } else {
                        CategoryPieChartCard(
                            items = expenseBreakdown,
                            currency = defaultCurrency
                        )
                    }
                }

                // Page indicator dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }

            // Savings & Investment Header Card (Isolated from liquid balance)
            item {
                SavingsHeaderCard(
                    savingsBalance = savingsBalance,
                    holdingCount = savingsAccounts.size,
                    currency = defaultCurrency,
                    onClick = { showSavingsPortfolio = true }
                )
            }

            // Quick Actions & Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search transactions, merchants...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All (${transactions.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.DEBIT,
                            onClick = { viewModel.setFilter(TransactionType.DEBIT) },
                            label = { Text("Expenses") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.CREDIT,
                            onClick = { viewModel.setFilter(TransactionType.CREDIT) },
                            label = { Text("Income") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.TRANSFER,
                            onClick = { viewModel.setFilter(TransactionType.TRANSFER) },
                            label = { Text("Transfers") }
                        )
                    }
                }
            }

            // Feed Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { showSimulationDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Simulate Bank Alert",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Transactions Feed
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No transactions yet",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button to add a transaction manually or simulate alerts.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Add Transaction Manually
        FloatingActionButton(
            onClick = { showAddTransactionDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction"
            )
        }
    }

    // Manual Transaction Dialog
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            accounts = accounts,
            categories = categories,
            defaultCurrency = defaultCurrency,
            onDismiss = { showAddTransactionDialog = false },
            onAddTransaction = { accId, amount, curr, type, counterparty, catId, destId ->
                viewModel.addManualTransaction(
                    accountId = accId,
                    amount = amount,
                    currency = curr,
                    type = type,
                    counterparty = counterparty,
                    categoryId = catId,
                    destinationAccountId = destId
                )
            }
        )
    }

    // Simulation Dialog
    if (showSimulationDialog) {
        SimulationDialog(
            onDismiss = { showSimulationDialog = false },
            onSimulate = { pkg, title, content ->
                viewModel.simulateNotification(pkg, title, content)
            }
        )
    }

    // Savings & Investment Portfolio View / Dialog
    if (showSavingsPortfolio) {
        SavingsPortfolioDialog(
            savingsAccounts = savingsAccounts,
            totalSavingsBalance = savingsBalance,
            currency = defaultCurrency,
            onDismiss = { showSavingsPortfolio = false },
            onAddNewAsset = {
                showAddSavingsAccountDialog = true
            },
            onEditAccount = { accountToEdit = it },
            onDeleteAccount = { accountToDelete = it }
        )
    }

    // Add New Savings / Investment Holding Dialog
    if (showAddSavingsAccountDialog) {
        AccountFormDialog(
            initialAccount = null,
            defaultCurrency = defaultCurrency,
            defaultType = AccountType.SAVINGS,
            onDismiss = { showAddSavingsAccountDialog = false },
            onSave = { name, type, curr, bal, mask, savingSubType ->
                viewModel.addAccount(name, type, curr, bal, mask, savingSubType)
            }
        )
    }

    // Edit Holding Dialog from Portfolio View
    accountToEdit?.let { account ->
        AccountFormDialog(
            initialAccount = account,
            defaultCurrency = defaultCurrency,
            defaultType = AccountType.SAVINGS,
            onDismiss = { accountToEdit = null },
            onSave = { name, type, curr, bal, mask, savingSubType ->
                viewModel.updateAccount(
                    account.copy(
                        name = name,
                        type = type.name,
                        currency = curr,
                        currentBalance = bal,
                        identifierMask = mask,
                        savingSubType = if (type == AccountType.SAVINGS) savingSubType else null
                    )
                )
            }
        )
    }

    // Delete Holding Confirmation Dialog from Portfolio View
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(
                    text = "Delete Asset?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to remove \"${account.name}\" from your portfolio?")
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteAccount(account)
                        accountToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
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
