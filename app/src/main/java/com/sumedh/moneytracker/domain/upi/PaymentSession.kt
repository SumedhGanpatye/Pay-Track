package com.sumedh.moneytracker.domain.upi

/**
 * In-memory payment draft spanning scanner → details → UPI return.
 */
data class PaymentDraft(
    val rawQr: String,
    val merchantName: String,
    val upiId: String,
    val amount: String,
    val amountLocked: Boolean,
    val category: String,
    val note: String,
    val selectedUpiApp: UpiApp,
    val verified: Boolean = true
)

object PaymentSession {
    @Volatile
    var draft: PaymentDraft? = null
        private set

    @Volatile
    var awaitingUpiReturn: Boolean = false
        private set

    fun setDraft(draft: PaymentDraft) {
        this.draft = draft
    }

    fun updateDraft(transform: (PaymentDraft) -> PaymentDraft) {
        draft = draft?.let(transform)
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
        PendingUpiScan.consume()
    }
}
