package com.sumedh.moneytracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.util.DateRanges
import com.sumedh.moneytracker.util.ExpenseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class AnalysisScope {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH
}

data class CategoryBar(
    val category: String,
    val amount: Double,
    val fraction: Float,
    val percentOfTotal: Double = 0.0
)

data class AnalysisUiState(
    val scope: AnalysisScope = AnalysisScope.ALL_TIME,
    val weeklyAvg: Double = 0.0,
    val monthlyAvg: Double = 0.0,
    val periodTotal: Double = 0.0,
    val transactionCount: Int = 0,
    val bars: List<CategoryBar> = emptyList(),
    val topCategory: String? = null,
    val scopeLabel: String = "All-time totals"
)

class AnalysisViewModel(
    repository: ExpenseRepository,
    customCategoryStore: CustomCategoryStore
) : ViewModel() {

    private val selectedScope = MutableStateFlow(AnalysisScope.ALL_TIME)

    val uiState: StateFlow<AnalysisUiState> = combine(
        repository.observeAllSortedByDateDesc(),
        selectedScope,
        customCategoryStore.custom
    ) { expenses, currentScope, customs ->
        buildState(expenses, currentScope, customs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState()
    )

    fun selectWeekly() {
        selectedScope.update { AnalysisScope.THIS_WEEK }
    }

    fun selectMonthly() {
        selectedScope.update { AnalysisScope.THIS_MONTH }
    }

    fun showAllTime() {
        selectedScope.update { AnalysisScope.ALL_TIME }
    }

    private fun buildState(
        expenses: List<ExpenseEntity>,
        currentScope: AnalysisScope,
        customCategories: List<String>
    ): AnalysisUiState {
        val today = DateRanges.today()
        val filtered = when (currentScope) {
            AnalysisScope.ALL_TIME -> expenses
            AnalysisScope.THIS_WEEK -> ExpenseAnalytics.inInclusiveRange(
                expenses,
                DateRanges.weekStart(today),
                today
            )
            AnalysisScope.THIS_MONTH -> ExpenseAnalytics.inInclusiveRange(
                expenses,
                DateRanges.monthStart(today),
                today
            )
        }

        val byCategory = ExpenseAnalytics.sumByCategory(filtered)
        val periodTotal = filtered.sumOf { it.amount }
        val ordered = ExpenseAnalytics.orderedCategoriesForChart(filtered, customCategories)
        val bars = ordered.mapNotNull { category ->
            val amount = byCategory[category] ?: 0.0
            if (amount <= 0.0) return@mapNotNull null
            val percentOfTotal = if (periodTotal > 0.0) {
                (amount / periodTotal) * 100.0
            } else {
                0.0
            }
            CategoryBar(
                category = category,
                amount = amount,
                fraction = (if (periodTotal > 0.0) amount / periodTotal else 0.0)
                    .toFloat()
                    .coerceIn(0f, 1f),
                percentOfTotal = percentOfTotal
            )
        }
        val top = bars.maxByOrNull { it.amount }
            ?.takeIf { it.amount > 0 }
            ?.category

        val scopeLabel = when (currentScope) {
            AnalysisScope.ALL_TIME -> "All-time totals"
            AnalysisScope.THIS_WEEK -> "This week · Mon–today"
            AnalysisScope.THIS_MONTH -> "This month · 1st–today"
        }

        return AnalysisUiState(
            scope = currentScope,
            weeklyAvg = ExpenseAnalytics.weeklyDailyAverage(expenses, today),
            monthlyAvg = ExpenseAnalytics.monthlyDailyAverage(expenses, today),
            periodTotal = periodTotal,
            transactionCount = filtered.size,
            bars = bars,
            topCategory = top,
            scopeLabel = scopeLabel
        )
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            customCategoryStore: CustomCategoryStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AnalysisViewModel(repository, customCategoryStore) as T
                }
            }
    }
}
