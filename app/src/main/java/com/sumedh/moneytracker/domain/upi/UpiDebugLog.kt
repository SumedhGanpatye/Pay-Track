package com.sumedh.moneytracker.domain.upi

import android.util.Log

/**
 * Debug-only logging for the QR → UPI payment flow.
 * Filter Logcat with tag: **UPI_DEBUG**
 */
object UpiDebugLog {
    const val TAG = "UPI_DEBUG"

    fun banner(title: String) {
        Log.d(TAG, "")
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, " UPI PAYMENT DEBUG — $title")
        Log.d(TAG, "════════════════════════════════════════")
    }

    fun section(name: String) {
        Log.d(TAG, "")
        Log.d(TAG, "── [$name] ──")
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

    /** Hex of first [maxChars] code units — catches invisible / non-ASCII corruption. */
    fun hexPreview(label: String, value: String?, maxChars: Int = 64) {
        if (value == null) {
            field("${label}_hex", null)
            return
        }
        val slice = value.take(maxChars)
        val hex = slice.map { c ->
            String.format("%04X", c.code)
        }.joinToString(" ")
        field("${label}_len", value.length.toString())
        field("${label}_hex", hex)
        if (value.length > maxChars) {
            line("  ${label}_hex = … truncated after $maxChars chars")
        }
    }
}
