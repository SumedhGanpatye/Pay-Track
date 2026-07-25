package com.sumedh.moneytracker.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class QuickAddUiState(
    val amountInput: String = "",
    val selectedCategory: String = PaymentPrimaryCategories.FOOD,
    val customCategories: List<String> = emptyList(),
    val showCustomCategoryDialog: Boolean = false,
    val note: String = "",
    val todayDisplay: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
)

data class TransactionGroup(
    val dateLabel: String,
    val items: List<ExpenseEntity>
)

class HomeViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val customCategoryStore: CustomCategoryStore
) : AndroidViewModel(application) {

    private val _quickAdd = MutableStateFlow(
        QuickAddUiState(customCategories = customCategoryStore.current())
    )
    val quickAdd: StateFlow<QuickAddUiState> = _quickAdd.asStateFlow()

    init {
        viewModelScope.launch {
            customCategoryStore.custom.collect { customs ->
                _quickAdd.update { it.copy(customCategories = customs) }
            }
        }
    }

    val transactionGroups: StateFlow<List<TransactionGroup>> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                expenses
                    .groupBy { it.date }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (date, items) ->
                        TransactionGroup(
                            dateLabel = formatDateBanner(date),
                            items = items.sortedWith(
                                compareByDescending<ExpenseEntity> { it.time }
                                    .thenByDescending { it.id }
                            )
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val recentExpenses: StateFlow<List<ExpenseEntity>> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                expenses
                    .sortedWith(
                        compareByDescending<ExpenseEntity> { it.date }
                            .thenByDescending { it.time }
                            .thenByDescending { it.id }
                    )
                    .take(RECENT_LIMIT)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val todaySpent: StateFlow<Double> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                expenses.filter { it.date == today }.sumOf { it.amount }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    val todayCount: StateFlow<Int> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                expenses.count { it.date == today }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { raw ->
                val parts = raw.split('.')
                if (parts.size <= 1) raw
                else parts.first() + "." + parts.drop(1).joinToString("").take(2)
            }
        _quickAdd.update { it.copy(amountInput = filtered) }
    }

    fun onPrimaryCategorySelected(category: String) {
        _quickAdd.update { it.copy(selectedCategory = category) }
    }

    fun onOtherCategoryClicked() {
        _quickAdd.update { it.copy(showCustomCategoryDialog = true) }
    }

    fun dismissCustomCategoryDialog() {
        _quickAdd.update { it.copy(showCustomCategoryDialog = false) }
    }

    fun onCustomCategorySaved(name: String) {
        val saved = customCategoryStore.add(name) ?: return
        _quickAdd.update {
            it.copy(
                selectedCategory = saved,
                showCustomCategoryDialog = false,
                customCategories = customCategoryStore.current()
            )
        }
    }

    fun onCustomCategorySelected(category: String) {
        _quickAdd.update { it.copy(selectedCategory = category) }
    }

    fun onNoteChange(note: String) {
        _quickAdd.update { it.copy(note = note) }
    }

    fun saveExpense() {
        val amount = _quickAdd.value.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0.0) return

        val nowDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val category = _quickAdd.value.selectedCategory
        val userNote = _quickAdd.value.note.trim()
        val noteForDb = userNote.ifBlank { category }

        viewModelScope.launch {
            repository.insert(
                ExpenseEntity(
                    amount = amount,
                    date = nowDate,
                    time = nowTime,
                    note = noteForDb,
                    category = category,
                    isReconciled = false
                )
            )
            ExpenseNotificationHelper.showExpenseAdded(
                context = getApplication(),
                amount = amount,
                category = category,
                note = userNote.ifBlank { null }
            )
            _quickAdd.update {
                it.copy(
                    amountInput = "",
                    note = "",
                    todayDisplay = LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                )
            }
        }
    }

    fun reconcile(id: Long, reconciled: Boolean = true) {
        viewModelScope.launch {
            repository.reconcile(id, reconciled)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    private fun formatDateBanner(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            when (date) {
                today -> "Today · ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                today.minusDays(1) -> "Yesterday · ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
            }
        } catch (_: Exception) {
            isoDate
        }
    }

    companion object {
        private const val RECENT_LIMIT = 12

        fun factory(
            repository: ExpenseRepository,
            customCategoryStore: CustomCategoryStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    throw IllegalStateException("Use create(modelClass, extras)")
                }

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras
                ): T {
                    val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as Application
                    return HomeViewModel(app, repository, customCategoryStore) as T
                }
            }
    }
}
