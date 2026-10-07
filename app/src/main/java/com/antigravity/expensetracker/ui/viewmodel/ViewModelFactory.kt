package com.antigravity.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antigravity.expensetracker.ExpenseTrackerApp

class ViewModelFactory(private val app: ExpenseTrackerApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                transactionRepository = app.transactionRepository,
                accountRepository = app.accountRepository,
                categoryRepository = app.categoryRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
