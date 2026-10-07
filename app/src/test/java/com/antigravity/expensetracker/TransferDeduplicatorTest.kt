package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.deduplication.DeduplicationResult
import com.antigravity.expensetracker.data.deduplication.TransferDeduplicator
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransferDeduplicatorTest {

    private lateinit var deduplicator: TransferDeduplicator

    @Before
    fun setUp() {
        // Standard 180 seconds window (180,000 ms)
        deduplicator = TransferDeduplicator(timeWindowMillis = 180_000L)
    }

    @Test
    fun testSelfTransferPairWithinWindowMergesIntoTransfer() {
        val baseTime = 1000000L

        val debitTx = TransactionEntity(
            id = "tx-debit-1",
            accountId = "acc_checking",
            destinationAccountId = null,
            categoryId = null,
            amount = 100.0,
            currency = "USD",
            type = TransactionType.DEBIT.name,
            counterparty = "Checking Outflow",
            rawText = "Debited $100.00",
            timestamp = baseTime
        )

        // Credit occurring 60 seconds later (well within 180 seconds)
        val creditTx = TransactionEntity(
            id = "tx-credit-2",
            accountId = "acc_wallet",
            destinationAccountId = null,
            categoryId = null,
            amount = 100.0,
            currency = "USD",
            type = TransactionType.CREDIT.name,
            counterparty = "Wallet Inflow",
            rawText = "Credited $100.00",
            timestamp = baseTime + 60_000L
        )

        val result = deduplicator.evaluateInMemory(creditTx, listOf(debitTx))
        assertTrue(result is DeduplicationResult.MergedAsTransfer)

        val merged = result as DeduplicationResult.MergedAsTransfer
        assertEquals(TransactionType.TRANSFER.name, merged.transferTransaction.type)
        assertEquals(100.0, merged.transferTransaction.amount, 0.001)
        assertEquals("acc_checking", merged.transferTransaction.accountId)
        assertEquals("acc_wallet", merged.transferTransaction.destinationAccountId)
        assertEquals("tx-credit-2", merged.supersededTransactionId)
    }

    @Test
    fun testTransferOutsideWindowDoesNotMerge() {
        val baseTime = 1000000L

        val debitTx = TransactionEntity(
            id = "tx-debit-1",
            accountId = "acc_checking",
            destinationAccountId = null,
            categoryId = null,
            amount = 100.0,
            currency = "USD",
            type = TransactionType.DEBIT.name,
            counterparty = "Outflow",
            rawText = "Debited $100.00",
            timestamp = baseTime
        )

        // Credit occurring 200 seconds later (> 180 seconds)
        val creditTx = TransactionEntity(
            id = "tx-credit-2",
            accountId = "acc_wallet",
            destinationAccountId = null,
            categoryId = null,
            amount = 100.0,
            currency = "USD",
            type = TransactionType.CREDIT.name,
            counterparty = "Inflow",
            rawText = "Credited $100.00",
            timestamp = baseTime + 200_000L
        )

        val result = deduplicator.evaluateInMemory(creditTx, listOf(debitTx))
        assertTrue(result is DeduplicationResult.Standalone)
    }

    @Test
    fun testUnequalAmountsDoNotMerge() {
        val baseTime = 1000000L

        val debitTx = TransactionEntity(
            id = "tx-debit-1",
            accountId = "acc_checking",
            destinationAccountId = null,
            categoryId = null,
            amount = 100.0,
            currency = "USD",
            type = TransactionType.DEBIT.name,
            counterparty = "Outflow",
            rawText = "Debited $100.00",
            timestamp = baseTime
        )

        val creditTx = TransactionEntity(
            id = "tx-credit-2",
            accountId = "acc_wallet",
            destinationAccountId = null,
            categoryId = null,
            amount = 95.0, // Different amount
            currency = "USD",
            type = TransactionType.CREDIT.name,
            counterparty = "Inflow",
            rawText = "Credited $95.00",
            timestamp = baseTime + 30_000L
        )

        val result = deduplicator.evaluateInMemory(creditTx, listOf(debitTx))
        assertTrue(result is DeduplicationResult.Standalone)
    }

    @Test
    fun testSameDirectionDebitDebitDoesNotMerge() {
        val baseTime = 1000000L

        val debitTx1 = TransactionEntity(
            id = "tx-debit-1",
            accountId = "acc_checking",
            destinationAccountId = null,
            categoryId = null,
            amount = 50.0,
            currency = "USD",
            type = TransactionType.DEBIT.name,
            counterparty = "Starbucks",
            rawText = "Debited $50.00",
            timestamp = baseTime
        )

        val debitTx2 = TransactionEntity(
            id = "tx-debit-2",
            accountId = "acc_checking",
            destinationAccountId = null,
            categoryId = null,
            amount = 50.0,
            currency = "USD",
            type = TransactionType.DEBIT.name,
            counterparty = "Uber",
            rawText = "Debited $50.00",
            timestamp = baseTime + 30_000L
        )

        val result = deduplicator.evaluateInMemory(debitTx2, listOf(debitTx1))
        assertTrue(result is DeduplicationResult.Standalone)
    }
}
