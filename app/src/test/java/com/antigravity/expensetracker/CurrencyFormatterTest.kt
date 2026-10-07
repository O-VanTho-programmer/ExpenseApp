package com.antigravity.expensetracker

import com.antigravity.expensetracker.util.CurrencyFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testUsdFormat() {
        val formatted = CurrencyFormatter.format(1250.50, "USD")
        assertTrue(formatted.contains("1,250.50") || formatted.contains("$"))
    }

    @Test
    fun testVndFormat() {
        val formatted = CurrencyFormatter.format(50000.0, "VND")
        assertTrue(formatted.contains("50.000") || formatted.contains("50,000") || formatted.contains("₫"))
    }
}
