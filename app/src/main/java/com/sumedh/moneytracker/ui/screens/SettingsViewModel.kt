package com.sumedh.moneytracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.data.ExpenseSource
import com.sumedh.moneytracker.data.ExpenseType
import com.sumedh.moneytracker.data.NotificationDebugRepository
import com.sumedh.moneytracker.data.ParsedNotificationEntity
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.notification.NotificationParser
import com.sumedh.moneytracker.util.DateRanges
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data class ExportReady(val csv: String) : SettingsEvent()
    data class BackupReady(val bytes: ByteArray, val fileName: String) : SettingsEvent()
}

enum class ResetType {
    COMPLETE,
    THIS_WEEK,
    THIS_MONTH,
    THIS_DAY
}

data class ParserTestResult(
    val amount: Double?,
    val person: String?,
    val group: String?,
    val parserUsed: String?
)

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val customCategoryStore: CustomCategoryStore,
    private val debugRepository: NotificationDebugRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val _lastParsed = MutableStateFlow<ParsedNotificationEntity?>(null)
    val lastParsed: StateFlow<ParsedNotificationEntity?> = _lastParsed.asStateFlow()

    init {
        viewModelScope.launch {
            _lastParsed.value = debugRepository.getLatest()
        }
    }

    fun performReset(type: ResetType) {
        viewModelScope.launch {
            val today = DateRanges.today()
            val deleted = when (type) {
                ResetType.COMPLETE -> {
                    repository.deleteAll()
                    customCategoryStore.clear()
                    debugRepository.clearAll()
                    -1
                }
                ResetType.THIS_DAY -> repository.deleteByDate(DateRanges.todayIso())
                ResetType.THIS_WEEK -> repository.deleteByDateRange(
                    DateRanges.toIso(DateRanges.weekStart(today)),
                    DateRanges.toIso(today)
                )
                ResetType.THIS_MONTH -> repository.deleteByDateRange(
                    DateRanges.toIso(DateRanges.monthStart(today)),
                    DateRanges.toIso(today)
                )
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
            val header = "id,amount,title,category,expenseType,personName,groupName,date,time,source,notes,isReconciled,status"
            val body = rows.joinToString("\n") { e ->
                listOf(
                    e.id.toString(),
                    e.amount.toString(),
                    "\"${e.title.replace("\"", "\"\"")}\"",
                    e.category,
                    e.expenseType,
                    e.personName.orEmpty(),
                    e.groupName.orEmpty(),
                    e.date,
                    e.time,
                    e.source,
                    "\"${e.notes.replace("\"", "\"\"")}\"",
                    e.isReconciled.toString(),
                    e.status
                ).joinToString(",")
            }
            _events.emit(SettingsEvent.ExportReady("$header\n$body"))
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            val lines = csv.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) {
                _events.emit(SettingsEvent.Message("No data rows found in CSV"))
                return@launch
            }
            var imported = 0
            lines.drop(1).forEach { line ->
                val parsed = parseCsvLine(line) ?: return@forEach
                repository.insert(parsed)
                imported++
            }
            _events.emit(SettingsEvent.Message("Imported $imported expense(s)"))
        }
    }

    fun testParser(text: String): ParserTestResult {
        val result = NotificationParser.parseText(text)
        return ParserTestResult(
            amount = result?.amount,
            person = result?.personName,
            group = result?.groupName,
            parserUsed = result?.parserUsed
        )
    }

    fun refreshLastParsed() {
        viewModelScope.launch {
            _lastParsed.value = debugRepository.getLatest()
        }
    }

    private fun parseCsvLine(line: String): ExpenseEntity? {
        return try {
            val parts = splitCsv(line)
            if (parts.size < 8) return null
            val hasNewSchema = parts.size >= 13
            if (hasNewSchema) {
                ExpenseEntity(
                    amount = parts[1].toDouble(),
                    title = parts[2].trim('"'),
                    category = parts[3],
                    expenseType = parts[4].ifBlank { ExpenseType.PERSONAL },
                    personName = parts[5].ifBlank { null },
                    groupName = parts[6].ifBlank { null },
                    date = parts[7],
                    time = parts[8],
                    source = parts[9].ifBlank { ExpenseSource.MANUAL },
                    notes = parts[10].trim('"'),
                    isReconciled = parts[11].toBoolean(),
                    status = parts[12]
                )
            } else {
                // Legacy CSV: id,amount,date,time,note,category,isReconciled,status
                ExpenseEntity(
                    amount = parts[1].toDouble(),
                    title = parts[4].trim('"'),
                    category = parts[5],
                    date = parts[2],
                    time = parts[3],
                    notes = "",
                    isReconciled = parts[6].toBoolean(),
                    status = parts[7]
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        line.forEach { ch ->
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            customCategoryStore: CustomCategoryStore,
            debugRepository: NotificationDebugRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, customCategoryStore, debugRepository) as T
                }
            }
    }
}

class NotificationDebugViewModel(
    debugRepository: NotificationDebugRepository
) : ViewModel() {

    val entries = debugRepository.observeRecent(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    companion object {
        fun factory(debugRepository: NotificationDebugRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NotificationDebugViewModel(debugRepository) as T
                }
            }
    }
}
