package com.antigravity.expensetracker

import android.app.Application
import com.antigravity.expensetracker.data.local.AppDatabase
import com.antigravity.expensetracker.data.repository.AccountRepository
import com.antigravity.expensetracker.data.repository.CategoryRepository
import com.antigravity.expensetracker.data.repository.TransactionRepository

class ExpenseTrackerApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val accountRepository by lazy { AccountRepository(database.accountDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao()
        )
    }

    companion object {
        lateinit var instance: ExpenseTrackerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
