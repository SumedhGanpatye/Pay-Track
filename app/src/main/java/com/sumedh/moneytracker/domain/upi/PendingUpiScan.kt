package com.sumedh.moneytracker.domain.upi

/**
 * Holds the last successfully scanned UPI QR until the payment details
 * screen consumes it. Keeps long payloads out of navigation arguments.
 */
object PendingUpiScan {
    @Volatile
    var rawQr: String? = null
        private set

    fun set(raw: String) {
        rawQr = raw
    }

    fun consume(): String? {
        val value = rawQr
        rawQr = null
        return value
    }

    fun peek(): String? = rawQr
}
