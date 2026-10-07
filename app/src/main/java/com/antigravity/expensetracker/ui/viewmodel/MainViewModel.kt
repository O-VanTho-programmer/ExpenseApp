package com.antigravity.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.local.entity.CategoryEntity
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.data.model.AccountType
import com.antigravity.expensetracker.data.model.TransactionType
import com.antigravity.expensetracker.data.repository.AccountRepository
import com.antigravity.expensetracker.data.repository.CategoryRepository
import com.antigravity.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import com.antigravity.expensetracker.data.settings.SettingsManager

class MainViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsManager: SettingsManager? = null
) : ViewModel() {

    private val _fallbackCurrency = MutableStateFlow("VND")
    val defaultCurrency: StateFlow<String> = settingsManager?.defaultCurrency ?: _fallbackCurrency.asStateFlow()

    fun setDefaultCurrency(currency: String) {
        if (settingsManager != null) {
            settingsManager.setDefaultCurrency(currency)
        } else {
            _fallbackCurrency.value = currency.trim().uppercase()
        }
    }

    val selectedFilter = MutableStateFlow<TransactionType?>(null)
    val searchQuery = MutableStateFlow("")

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liquidAccounts: StateFlow<List<AccountEntity>> = accounts
        .map { list -> list.filter { it.type != AccountType.SAVINGS.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsAccounts: StateFlow<List<AccountEntity>> = accounts
        .map { list -> list.filter { it.type == AccountType.SAVINGS.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liquidBalance: StateFlow<Double> = accounts
        .map { list -> list.filter { it.type != AccountType.SAVINGS.name }.sumOf { it.currentBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val savingsBalance: StateFlow<Double> = accounts
        .map { list -> list.filter { it.type == AccountType.SAVINGS.name }.sumOf { it.currentBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = transactionRepository.getTotalExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = transactionRepository.getTotalIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val allTransactions = transactionRepository.getAllTransactions()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        selectedFilter,
        searchQuery
    ) { transactions, filter, query ->
        transactions.filter { tx ->
            val matchesFilter = filter == null || tx.type == filter.name
            val matchesSearch = query.isBlank() ||
                tx.counterparty?.contains(query, ignoreCase = true) == true ||
                tx.rawText.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TransactionType?) {
        selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun simulateNotification(packageName: String, title: String, content: String) {
        viewModelScope.launch {
            transactionRepository.processNotification(
                packageName = packageName,
                title = title,
                content = content,
                postTime = System.currentTimeMillis()
            )
        }
    }

    fun addManualTransaction(
        accountId: String,
        amount: Double,
        currency: String,
        type: TransactionType,
        counterparty: String,
        categoryId: String? = null,
        destinationAccountId: String? = null
    ) {
        viewModelScope.launch {
            transactionRepository.insertManualTransaction(
                accountId = accountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                amount = amount,
                currency = currency,
                type = type,
                counterparty = counterparty
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    val expenseBreakdownByCategory: StateFlow<List<CategoryBreakdownItem>> = combine(
        allTransactions,
        categories
    ) { transactions, cats ->
        val expenseTxs = transactions.filter { it.type == "DEBIT" && it.status != "EXCLUDED" }
        val totalExpense = expenseTxs.sumOf { it.amount }
        if (totalExpense <= 0.0) return@combine emptyList()

        val catMap = cats.associateBy { it.id }
        val grouped = expenseTxs.groupBy { it.categoryId }

        grouped.map { (catId, txs) ->
            val cat = catId?.let { catMap[it] }
            val sum = txs.sumOf { it.amount }
            val percent = if (totalExpense > 0) ((sum / totalExpense) * 100).toFloat() else 0f
            CategoryBreakdownItem(
                categoryId = catId,
                categoryName = cat?.name ?: "Uncategorized",
                colorHex = cat?.colorHex ?: "#78909C",
                totalAmount = sum,
                percentage = percent
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }

    fun addCategory(name: String, type: String, iconKey: String? = null, colorHex: String? = null) {
        viewModelScope.launch {
            val category = CategoryEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                iconKey = iconKey,
                colorHex = colorHex
            )
            categoryRepository.addCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        currency: String,
        balance: Double,
        mask: String?,
        savingSubType: String? = null
    ) {
        viewModelScope.launch {
            val account = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                type = type.name,
                currency = currency,
                currentBalance = balance,
                identifierMask = mask?.ifBlank { null },
                savingSubType = if (type == AccountType.SAVINGS) savingSubType else null
            )
            accountRepository.addAccount(account)
        }
    }
}

data class CategoryBreakdownItem(
    val categoryId: String?,
    val categoryName: String,
    val colorHex: String?,
    val totalAmount: Double,
    val percentage: Float
)
