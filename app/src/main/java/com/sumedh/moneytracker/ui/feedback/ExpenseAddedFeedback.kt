package com.sumedh.moneytracker.ui.feedback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-app top banner for expenses saved from the app UI (not system notifications).
 */
object ExpenseAddedFeedback {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}
