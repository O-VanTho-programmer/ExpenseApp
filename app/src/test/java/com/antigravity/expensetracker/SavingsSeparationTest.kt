package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.local.dao.AccountDao
import com.antigravity.expensetracker.data.local.dao.CategoryDao
import com.antigravity.expensetracker.data.local.dao.TransactionDao
import com.antigravity.expensetracker.data.local.entity.AccountEntity
import com.antigravity.expensetracker.data.model.AccountType
import com.antigravity.expensetracker.data.model.SavingSubType
import com.antigravity.expensetracker.data.repository.AccountRepository
import com.antigravity.expensetracker.data.repository.CategoryRepository
import com.antigravity.expensetracker.data.repository.TransactionRepository
import com.antigravity.expensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsSeparationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSavingSubTypeEnumValues() {
        assertEquals("Stocks", SavingSubType.STOCKS.displayName)
        assertEquals("📈", SavingSubType.STOCKS.icon)

        assertEquals("Mutual Funds", SavingSubType.MUTUAL_FUNDS.displayName)
        assertEquals("📊", SavingSubType.MUTUAL_FUNDS.icon)

        assertEquals("Bonds", SavingSubType.BONDS.displayName)
        assertEquals("📜", SavingSubType.BONDS.icon)

        assertEquals(SavingSubType.STOCKS, SavingSubType.fromString("STOCKS"))
        assertEquals(SavingSubType.MUTUAL_FUNDS, SavingSubType.fromString("mutual_funds"))
        assertEquals(SavingSubType.BONDS, SavingSubType.fromString("BONDS"))
    }

    @Test
    fun testLiquidAndSavingsBalancesAreSeparated() = runTest {
        val testAccounts = listOf(
            AccountEntity(
                id = "acc1",
                name = "Vietcombank Checking",
                type = AccountType.CHECKING.name,
                currency = "VND",
                currentBalance = 15_000_000.0
            ),
            AccountEntity(
                id = "acc2",
                name = "MoMo Wallet",
                type = AccountType.WALLET.name,
                currency = "VND",
                currentBalance = 2_000_000.0
            ),
            AccountEntity(
                id = "acc3",
                name = "TCBS Stocks",
                type = AccountType.SAVINGS.name,
                currency = "VND",
                currentBalance = 50_000_000.0,
                savingSubType = SavingSubType.STOCKS.name
            ),
            AccountEntity(
                id = "acc4",
                name = "DCDS Mutual Fund",
                type = AccountType.SAVINGS.name,
                currency = "VND",
                currentBalance = 30_000_000.0,
                savingSubType = SavingSubType.MUTUAL_FUNDS.name
            ),
            AccountEntity(
                id = "acc5",
                name = "Corporate Bonds",
                type = AccountType.SAVINGS.name,
                currency = "VND",
                currentBalance = 20_000_000.0,
                savingSubType = SavingSubType.BONDS.name
            )
        )

        val accountDao = createAccountDaoMock(testAccounts)
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

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.accounts.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.liquidAccounts.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.savingsAccounts.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.liquidBalance.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.savingsBalance.collect() }

        testScheduler.advanceUntilIdle()

        val allAccs = viewModel.accounts.value
        assertEquals(5, allAccs.size)

        val liquidAccs = viewModel.liquidAccounts.value
        assertEquals(2, liquidAccs.size)
        assertTrue(liquidAccs.none { it.type == AccountType.SAVINGS.name })

        val savingsAccs = viewModel.savingsAccounts.value
        assertEquals(3, savingsAccs.size)
        assertTrue(savingsAccs.all { it.type == AccountType.SAVINGS.name })

        // Liquid balance = 15M + 2M = 17M VND (savings accounts NOT accumulated)
        val liquidBal = viewModel.liquidBalance.value
        assertEquals(17_000_000.0, liquidBal, 0.001)

        // Savings balance = 50M + 30M + 20M = 100M VND
        val savingsBal = viewModel.savingsBalance.value
        assertEquals(100_000_000.0, savingsBal, 0.001)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createAccountDaoMock(accounts: List<AccountEntity>): AccountDao {
        return Proxy.newProxyInstance(
            AccountDao::class.java.classLoader,
            arrayOf(AccountDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAllAccountsFlow" -> flowOf(accounts)
                "getAllAccounts" -> accounts
                else -> null
            }
        } as AccountDao
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
