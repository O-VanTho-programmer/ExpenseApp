package com.antigravity.expensetracker.data.parser

import com.antigravity.expensetracker.data.model.TransactionType

data class ParsedOutput(
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val counterparty: String,
    val accountIdentifier: String? = null,
    val timestamp: Long
)
