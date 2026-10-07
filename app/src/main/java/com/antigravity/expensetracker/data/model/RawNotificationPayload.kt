package com.antigravity.expensetracker.data.model

data class RawNotificationPayload(
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long
)
