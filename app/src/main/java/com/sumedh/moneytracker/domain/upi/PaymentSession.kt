package com.sumedh.moneytracker.domain.upi

/**
 * In-memory payment draft: amount / category / note → open UPI app → confirm.
 */
data class PaymentDraft(
    val amount: String,
    val category: String,
    val note: String,
    val selectedUpiApp: UpiApp
)

object PaymentSession {
    @Volatile
    var draft: PaymentDraft? = null
        private set

    @Volatile
    var awaitingUpiReturn: Boolean = false
        private set

    /** Last amount we put on the clipboard for UPI paste suggestions. */
    @Volatile
    var lastClipboardAmount: String? = null

    fun setDraft(draft: PaymentDraft) {
        this.draft = draft
    }

    fun markAwaitingReturn() {
        awaitingUpiReturn = true
    }

    fun clearAwaitingReturn() {
        awaitingUpiReturn = false
    }

    fun clear() {
        draft = null
        awaitingUpiReturn = false
        lastClipboardAmount = null
    }
}
