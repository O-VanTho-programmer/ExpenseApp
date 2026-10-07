package com.antigravity.expensetracker.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val displayName: String,
    val symbol: String,
    val isZeroDecimal: Boolean = false
)

object CurrencyFormatter {

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyOption("VND", "VND - Vietnamese Đồng (₫)", "₫", isZeroDecimal = true),
        CurrencyOption("USD", "USD - US Dollar ($)", "$", isZeroDecimal = false),
        CurrencyOption("EUR", "EUR - Euro (€)", "€", isZeroDecimal = false),
        CurrencyOption("GBP", "GBP - British Pound (£)", "£", isZeroDecimal = false),
        CurrencyOption("JPY", "JPY - Japanese Yen (¥)", "¥", isZeroDecimal = true),
        CurrencyOption("SGD", "SGD - Singapore Dollar (S$)", "S$", isZeroDecimal = false),
        CurrencyOption("AUD", "AUD - Australian Dollar (A$)", "A$", isZeroDecimal = false),
        CurrencyOption("CAD", "CAD - Canadian Dollar (C$)", "C$", isZeroDecimal = false)
    )

    fun format(amount: Double, currency: String): String {
        return when (currency.uppercase(Locale.ROOT)) {
            "USD", "$" -> {
                val formatter = DecimalFormat("$#,##0.00", DecimalFormatSymbols(Locale.US))
                formatter.format(amount)
            }
            "EUR", "€" -> {
                val formatter = DecimalFormat("€#,##0.00", DecimalFormatSymbols(Locale.GERMANY))
                formatter.format(amount)
            }
            "GBP", "£" -> {
                val formatter = DecimalFormat("£#,##0.00", DecimalFormatSymbols(Locale.UK))
                formatter.format(amount)
            }
            "JPY", "¥" -> {
                val formatter = DecimalFormat("¥#,##0", DecimalFormatSymbols(Locale.JAPAN))
                formatter.format(amount)
            }
            "VND", "₫" -> {
                val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
                    groupingSeparator = '.'
                }
                val formatter = DecimalFormat("#,##0", symbols)
                "${formatter.format(amount)} ₫"
            }
            else -> {
                val formatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
                "${formatter.format(amount)} $currency"
            }
        }
    }

    /**
     * Parses user numeric input safely, handling comma/dot decimal separators.
     */
    fun parseInputToAmount(input: String): Double? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Remove currency symbols and whitespace
        val clean = trimmed.replace(Regex("[^0-9.,]"), "")
        if (clean.isEmpty()) return null

        return try {
            // If contains both comma and dot, determine which is decimal
            if (clean.contains(',') && clean.contains('.')) {
                val lastComma = clean.lastIndexOf(',')
                val lastDot = clean.lastIndexOf('.')
                if (lastDot > lastComma) {
                    // 1,000.50 -> 1000.50
                    clean.replace(",", "").toDouble()
                } else {
                    // 1.000,50 -> 1000.50
                    clean.replace(".", "").replace(',', '.').toDouble()
                }
            } else if (clean.contains(',')) {
                // If contains single comma with 1 or 2 digits after, likely decimal (e.g. 50,5)
                // Otherwise thousands separator (e.g. 1,000,000)
                val parts = clean.split(',')
                if (parts.size == 2 && parts[1].length <= 2) {
                    clean.replace(',', '.').toDouble()
                } else {
                    clean.replace(",", "").toDouble()
                }
            } else if (clean.contains('.')) {
                val parts = clean.split('.')
                if (parts.size == 2 && parts[1].length <= 2) {
                    clean.toDouble()
                } else if (parts.size > 2) {
                    // e.g. 1.000.000
                    clean.replace(".", "").toDouble()
                } else {
                    clean.toDouble()
                }
            } else {
                clean.toDouble()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats live preview of user input based on the chosen currency.
     */
    fun formatPreview(rawInput: String, currency: String): String {
        val parsed = parseInputToAmount(rawInput) ?: 0.0
        return format(parsed, currency)
    }
}
