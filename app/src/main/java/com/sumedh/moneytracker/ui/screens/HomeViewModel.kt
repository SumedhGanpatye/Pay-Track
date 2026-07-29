package com.sumedh.moneytracker.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.data.ExpenseSource
import com.sumedh.moneytracker.data.ExpenseType
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import com.sumedh.moneytracker.ui.navigation.ManualExpensePrefill
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
    val isExpanded: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val editingExpenseId: Long? = null,
    val todayDisplay: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
) {
    val isEditing: Boolean get() = editingExpenseId != null
}

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

    fun applyPrefill(prefill: ManualExpensePrefill) {
        _quickAdd.update {
            it.copy(
                amountInput = formatAmount(prefill.amount),
                title = prefill.title,
                notes = prefill.notes.orEmpty(),
                isExpanded = true
            )
        }
    }

    fun startEditExpense(expense: ExpenseEntity) {
        _quickAdd.update {
            QuickAddUiState(
                amountInput = formatAmount(expense.amount),
                selectedCategory = expense.category,
                customCategories = customCategoryStore.current(),
                isExpanded = true,
                title = expense.title,
                notes = expense.notes,
                editingExpenseId = expense.id,
                todayDisplay = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
            )
        }
    }

    fun cancelEdit() {
        _quickAdd.update {
            QuickAddUiState(
                customCategories = customCategoryStore.current(),
                isExpanded = true,
                todayDisplay = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
            )
        }
    }

    fun toggleExpanded() {
        _quickAdd.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun removeCustomCategory(category: String) {
        val removed = customCategoryStore.remove(category)
        if (!removed) return
        _quickAdd.update { state ->
            val nextSelected = if (state.selectedCategory.equals(category, ignoreCase = true)) {
                PaymentPrimaryCategories.FOOD
            } else {
                state.selectedCategory
            }
            state.copy(
                selectedCategory = nextSelected,
                customCategories = customCategoryStore.current()
            )
        }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { raw ->
                val parts = raw.split('.')
                if (parts.size <= 1) raw
                else parts.first() + "." + parts.drop(1).joinToString("").take(2)
            }
        _quickAdd.update { it.copy(amountInput = filtered) }
    }

    fun onNotesChange(value: String) {
        _quickAdd.update { it.copy(notes = value) }
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

    fun saveExpense() {
        val state = _quickAdd.value
        val amount = state.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0.0) return

        val category = state.selectedCategory
        val title = state.title.trim().ifBlank { category }
        val notes = state.notes.trim()

        viewModelScope.launch {
            val editingId = state.editingExpenseId
            if (editingId != null) {
                val existing = repository.getById(editingId) ?: return@launch
                repository.update(
                    existing.copy(
                        amount = amount,
                        title = title,
                        category = category,
                        notes = notes
                    )
                )
            } else {
                val nowDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                repository.insert(
                    ExpenseEntity(
                        amount = amount,
                        title = title,
                        category = category,
                        expenseType = ExpenseType.PERSONAL,
                        date = nowDate,
                        time = nowTime,
                        source = ExpenseSource.MANUAL,
                        notes = notes
                    )
                )
                ExpenseNotificationHelper.showExpenseAdded(
                    context = getApplication(),
                    amount = amount,
                    category = category,
                    note = notes.takeIf { it.isNotBlank() }
                )
            }
            cancelEdit()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.delete(expense)
            if (_quickAdd.value.editingExpenseId == expense.id) {
                cancelEdit()
            }
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

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
