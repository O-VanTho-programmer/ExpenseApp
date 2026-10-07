package com.antigravity.expensetracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun format(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeOnlyFormat.format(Date(timestamp))
    }
}
