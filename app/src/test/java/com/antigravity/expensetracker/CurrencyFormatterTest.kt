package com.antigravity.expensetracker

import com.antigravity.expensetracker.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testSupportedCurrenciesList() {
        assertTrue(CurrencyFormatter.SUPPORTED_CURRENCIES.isNotEmpty())
        val vndOption = CurrencyFormatter.SUPPORTED_CURRENCIES.find { it.code == "VND" }
        assertNotNull(vndOption)
        assertEquals("₫", vndOption?.symbol)
        assertTrue(vndOption?.isZeroDecimal == true)
    }

    @Test
    fun testParseInputToAmount() {
        // Plain integer
        assertEquals(50000.0, CurrencyFormatter.parseInputToAmount("50000")!!, 0.01)

        // Decimals with dot
        assertEquals(1234.56, CurrencyFormatter.parseInputToAmount("1234.56")!!, 0.01)

        // Thousands with commas
        assertEquals(1000000.0, CurrencyFormatter.parseInputToAmount("1,000,000")!!, 0.01)

        // Dot thousands (Vietnamese / European style)
        assertEquals(1000000.0, CurrencyFormatter.parseInputToAmount("1.000.000")!!, 0.01)

        // Whitespace and currency symbols
        assertEquals(75000.0, CurrencyFormatter.parseInputToAmount(" 75,000 ₫ ")!!, 0.01)

        // Invalid strings
        assertNull(CurrencyFormatter.parseInputToAmount(""))
        assertNull(CurrencyFormatter.parseInputToAmount("abc"))
    }

    @Test
    fun testFormatPreview() {
        val vndPreview = CurrencyFormatter.formatPreview("500000", "VND")
        assertTrue(vndPreview.contains("500") && vndPreview.contains("₫"))

        val usdPreview = CurrencyFormatter.formatPreview("1250", "USD")
        assertTrue(usdPreview.contains("$") && usdPreview.contains("1,250"))
    }
}
