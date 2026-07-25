package com.sumedh.moneytracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.util.DateRanges
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data class ExportReady(val csv: String) : SettingsEvent()
}

enum class ResetType {
    COMPLETE,
    THIS_WEEK,
    THIS_MONTH,
    THIS_DAY
}

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val customCategoryStore: CustomCategoryStore
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun performReset(type: ResetType) {
        viewModelScope.launch {
            val today = DateRanges.today()
            val deleted = when (type) {
                ResetType.COMPLETE -> {
                    repository.deleteAll()
                    customCategoryStore.clear()
                    -1
                }
                ResetType.THIS_DAY -> {
                    repository.deleteByDate(DateRanges.todayIso())
                }
                ResetType.THIS_WEEK -> {
                    repository.deleteByDateRange(
                        DateRanges.toIso(DateRanges.weekStart(today)),
                        DateRanges.toIso(today)
                    )
                }
                ResetType.THIS_MONTH -> {
                    repository.deleteByDateRange(
                        DateRanges.toIso(DateRanges.monthStart(today)),
                        DateRanges.toIso(today)
                    )
                }
            }

            val message = when (type) {
                ResetType.COMPLETE -> "All expenses and custom categories deleted"
                ResetType.THIS_DAY -> "Deleted $deleted expense(s) from today"
                ResetType.THIS_WEEK -> "Deleted $deleted expense(s) from this week"
                ResetType.THIS_MONTH -> "Deleted $deleted expense(s) from this month"
            }
            _events.emit(SettingsEvent.Message(message))
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val rows = repository.getAllOnce()
            val header = "id,amount,date,time,note,category,isReconciled,status"
            val body = rows.joinToString("\n") { e ->
                listOf(
                    e.id.toString(),
                    e.amount.toString(),
                    e.date,
                    e.time,
                    "\"${e.note.replace("\"", "\"\"")}\"",
                    e.category,
                    e.isReconciled.toString(),
                    e.status
                ).joinToString(",")
            }
            _events.emit(SettingsEvent.ExportReady("$header\n$body"))
        }
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            customCategoryStore: CustomCategoryStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, customCategoryStore) as T
                }
            }
    }
}
