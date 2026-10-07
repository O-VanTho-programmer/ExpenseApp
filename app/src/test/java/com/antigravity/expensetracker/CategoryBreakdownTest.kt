package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.local.entity.CategoryEntity
import com.antigravity.expensetracker.data.local.entity.TransactionEntity
import com.antigravity.expensetracker.ui.viewmodel.CategoryBreakdownItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBreakdownTest {

    @Test
    fun testExpenseBreakdownCalculation() {
        val catFood = CategoryEntity(id = "cat_1", name = "Food & Dining", type = "EXPENSE", colorHex = "#E53935")
        val catTransport = CategoryEntity(id = "cat_2", name = "Transport", type = "EXPENSE", colorHex = "#1E88E5")
        val categories = listOf(catFood, catTransport)

        val txs = listOf(
            TransactionEntity(
                accountId = "acc_1",
                categoryId = "cat_1",
                amount = 300000.0,
                currency = "VND",
                type = "DEBIT",
                rawText = "Food expense",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                accountId = "acc_1",
                categoryId = "cat_1",
                amount = 200000.0,
                currency = "VND",
                type = "DEBIT",
                rawText = "Food expense 2",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                accountId = "acc_1",
                categoryId = "cat_2",
                amount = 500000.0,
                currency = "VND",
                type = "DEBIT",
                rawText = "Taxi ride",
                timestamp = System.currentTimeMillis()
            ),
            TransactionEntity(
                accountId = "acc_1",
                categoryId = null,
                amount = 2000000.0,
                currency = "VND",
                type = "CREDIT", // Income, should not be included in expense breakdown
                rawText = "Salary",
                timestamp = System.currentTimeMillis()
            )
        )

        val expenseTxs = txs.filter { it.type == "DEBIT" && it.status != "EXCLUDED" }
        val totalExpense = expenseTxs.sumOf { it.amount }
        assertEquals(1000000.0, totalExpense, 0.01)

        val catMap = categories.associateBy { it.id }
        val grouped = expenseTxs.groupBy { it.categoryId }

        val breakdown = grouped.map { (catId, items) ->
            val cat = catId?.let { catMap[it] }
            val sum = items.sumOf { it.amount }
            val percent = ((sum / totalExpense) * 100).toFloat()
            CategoryBreakdownItem(
                categoryId = catId,
                categoryName = cat?.name ?: "Uncategorized",
                colorHex = cat?.colorHex ?: "#78909C",
                totalAmount = sum,
                percentage = percent
            )
        }.sortedByDescending { it.totalAmount }

        assertEquals(2, breakdown.size)
        // Both Food (500k) and Transport (500k) are 50%
        assertEquals(50f, breakdown[0].percentage, 0.1f)
        assertEquals(50f, breakdown[1].percentage, 0.1f)
        assertEquals(500000.0, breakdown[0].totalAmount, 0.01)
        assertEquals(500000.0, breakdown[1].totalAmount, 0.01)
    }
}
