package com.antigravity.expensetracker.data.model

enum class SavingSubType(val displayName: String, val icon: String) {
    STOCKS("Stocks", "📈"),
    MUTUAL_FUNDS("Mutual Funds", "📊"),
    BONDS("Bonds", "📜"),
    TERM_DEPOSIT("Term Deposit", "🏦"),
    CRYPTO("Crypto", "🪙"),
    OTHER("Other Assets", "💼");

    companion object {
        fun fromString(value: String?): SavingSubType? {
            if (value.isNullOrBlank()) return null
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
