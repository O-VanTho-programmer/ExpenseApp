package com.antigravity.expensetracker.data.model

data class ParsedTransaction(
    val rawId: String,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val counterparty: String?,
    val accountIdentifier: String?, // e.g., last 4 digits
    val timestamp: Long
)
