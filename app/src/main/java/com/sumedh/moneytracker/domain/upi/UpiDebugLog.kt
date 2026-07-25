package com.sumedh.moneytracker.domain.upi

import android.util.Log

/**
 * Debug-only logging for the QR → UPI payment flow.
 * Filter Logcat with tag: UPI_DEBUG
 */
object UpiDebugLog {
    const val TAG = "UPI_DEBUG"

    fun banner(title: String) {
        Log.d(TAG, "========================")
        Log.d(TAG, "UPI PAYMENT DEBUG — $title")
        Log.d(TAG, "========================")
    }

    fun section(name: String) {
        Log.d(TAG, "")
        Log.d(TAG, "[$name]")
    }

    fun line(message: String) {
        Log.d(TAG, message)
    }

    fun field(key: String, value: String?) {
        when {
            value == null -> Log.d(TAG, "  $key = <null>")
            value.isEmpty() -> Log.d(TAG, "  $key = <empty>")
            value.isBlank() -> Log.d(TAG, "  $key = <blank> \"$value\"")
            else -> Log.d(TAG, "  $key = $value")
        }
    }

    fun check(label: String, pass: Boolean, detail: String = "") {
        val status = if (pass) "PASS" else "FAIL"
        val suffix = if (detail.isNotBlank()) " ($detail)" else ""
        Log.d(TAG, "  [$status] $label$suffix")
    }
}
