package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.local.dao.AccountDao
import com.antigravity.expensetracker.data.local.dao.CategoryDao
import com.antigravity.expensetracker.data.local.dao.TransactionDao
import com.antigravity.expensetracker.data.repository.AccountRepository
import com.antigravity.expensetracker.data.repository.CategoryRepository
import com.antigravity.expensetracker.data.repository.TransactionRepository
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

class MainViewModelCurrencyTest {

    @Test
    fun testDefaultCurrencyDefaultsToVndAndUpdates() {
        val accountDao = createMock(AccountDao::class.java)
        val categoryDao = createMock(CategoryDao::class.java)
        val transactionDao = createMock(TransactionDao::class.java)

        val accountRepo = AccountRepository(accountDao)
        val categoryRepo = CategoryRepository(categoryDao)
        val txRepo = TransactionRepository(transactionDao, accountDao, categoryDao)

        val viewModel = MainViewModel(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            settingsManager = null
        )

        // Initial default currency is VND
        assertEquals("VND", viewModel.defaultCurrency.value)

        // Update default currency to EUR
        viewModel.setDefaultCurrency("EUR")
        assertEquals("EUR", viewModel.defaultCurrency.value)

        // Update default currency to USD
        viewModel.setDefaultCurrency("USD")
        assertEquals("USD", viewModel.defaultCurrency.value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> createMock(clazz: Class<T>): T {
        return Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz)
        ) { _, method, _ ->
            when (method.returnType) {
                kotlinx.coroutines.flow.Flow::class.java -> flowOf(emptyList<Any>())
                List::class.java -> emptyList<Any>()
                Double::class.javaObjectType, Double::class.javaPrimitiveType -> 0.0
                else -> null
            }
        } as T
    }
}
