package com.antigravity.expensetracker.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _defaultCurrency = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY_VALUE) ?: DEFAULT_CURRENCY_VALUE
    )
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun setDefaultCurrency(currency: String) {
        val cleanCurrency = currency.trim().uppercase()
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, cleanCurrency).apply()
        _defaultCurrency.value = cleanCurrency
    }

    companion object {
        private const val PREFS_NAME = "expense_app_settings"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        const val DEFAULT_CURRENCY_VALUE = "VND"
    }
}
