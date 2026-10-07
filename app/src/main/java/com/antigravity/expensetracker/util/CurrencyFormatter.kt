package com.antigravity.expensetracker.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

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
}
