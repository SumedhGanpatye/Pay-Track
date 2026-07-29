package com.sumedh.moneytracker.domain.notification

data class ParsedExpenseData(
    val amount: Double,
    val personName: String?,
    val groupName: String?,
    val title: String,
    val note: String? = null,
    val parserUsed: String,
    val originalText: String
)

data class RawNotificationData(
    val packageName: String,
    val title: String,
    val text: String,
    val bigText: String?,
    val timestamp: Long,
    val notificationKey: String
) {
    val combinedText: String
        get() = listOf(title, text, bigText)
            .filterNot { it.isNullOrBlank() }
            .joinToString(" ")
            .trim()
}
