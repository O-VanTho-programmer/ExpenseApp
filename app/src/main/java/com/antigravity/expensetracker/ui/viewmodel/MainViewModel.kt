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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val selectedFilter = MutableStateFlow<TransactionType?>(null)
    val searchQuery = MutableStateFlow("")

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addAccount(name: String, type: AccountType, currency: String, balance: Double, mask: String?) {
        viewModelScope.launch {
            val account = AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                type = type.name,
                currency = currency,
                currentBalance = balance,
                identifierMask = mask?.ifBlank { null }
            )
            accountRepository.addAccount(account)
        }
    }
}
