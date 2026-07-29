package com.sumedh.moneytracker.domain.upi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpiPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _defaultApp = MutableStateFlow(readDefault())
    val defaultApp: StateFlow<UpiApp> = _defaultApp.asStateFlow()

    fun current(): UpiApp = _defaultApp.value

    fun setDefault(app: UpiApp) {
        prefs.edit { putString(KEY_DEFAULT_PACKAGE, app.packageName) }
        _defaultApp.value = app
    }

    private fun readDefault(): UpiApp =
        UpiApp.fromPackage(prefs.getString(KEY_DEFAULT_PACKAGE, UpiApp.GPAY.packageName))

    companion object {
        private const val PREFS = "upi_prefs"
        private const val KEY_DEFAULT_PACKAGE = "default_upi_package"
    }
}
