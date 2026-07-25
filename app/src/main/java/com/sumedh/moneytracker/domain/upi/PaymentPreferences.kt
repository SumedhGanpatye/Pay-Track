package com.sumedh.moneytracker.domain.upi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PaymentPrefs(
    val defaultUpiApp: UpiApp = UpiApp.GOOGLE_PAY,
    val askBeforeEveryPayment: Boolean = false,
    val lastCategory: String? = null
)

class PaymentPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<PaymentPrefs> = _state.asStateFlow()

    fun current(): PaymentPrefs = _state.value

    fun setDefaultUpiApp(app: UpiApp) {
        prefs.edit { putString(KEY_DEFAULT_UPI, app.packageName) }
        _state.value = read()
    }

    fun setAskBeforeEveryPayment(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ASK_BEFORE, enabled) }
        _state.value = read()
    }

    fun setLastCategory(category: String) {
        val cleaned = category.trim()
        if (cleaned.isBlank()) return
        prefs.edit { putString(KEY_LAST_CATEGORY, cleaned) }
        _state.value = read()
    }

    private fun read(): PaymentPrefs {
        val pkg = prefs.getString(KEY_DEFAULT_UPI, UpiApp.GOOGLE_PAY.packageName)
        val last = prefs.getString(KEY_LAST_CATEGORY, null)?.takeIf { it.isNotBlank() }
        return PaymentPrefs(
            defaultUpiApp = UpiApp.fromPackage(pkg) ?: UpiApp.GOOGLE_PAY,
            askBeforeEveryPayment = prefs.getBoolean(KEY_ASK_BEFORE, false),
            lastCategory = last
        )
    }

    companion object {
        private const val PREFS_NAME = "payment_prefs"
        private const val KEY_DEFAULT_UPI = "default_upi_package"
        private const val KEY_ASK_BEFORE = "ask_before_every_payment"
        private const val KEY_LAST_CATEGORY = "last_category"
    }
}
