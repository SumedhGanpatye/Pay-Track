package com.sumedh.moneytracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allExpenses: StateFlow<List<ExpenseEntity>> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                expenses.sortedWith(
                    compareByDescending<ExpenseEntity> { it.date }
                        .thenByDescending { it.time }
                        .thenByDescending { it.id }
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val daySpendTotals: StateFlow<Map<String, Double>> =
        repository.observeAllSortedByDateDesc()
            .map { expenses ->
                expenses.groupBy { it.date }
                    .mapValues { (_, dayExpenses) -> dayExpenses.sumOf { it.amount } }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    companion object {
        fun factory(repository: ExpenseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
