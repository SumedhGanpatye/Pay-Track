package com.sumedh.moneytracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.ui.screens.home.CategorySummary
import com.sumedh.moneytracker.util.DateRanges
import com.sumedh.moneytracker.util.ExpenseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class AnalysisSelection {
    data object ThisWeek : AnalysisSelection()
    data object ThisMonth : AnalysisSelection()
    data object AllTime : AnalysisSelection()
    data class Month(val yearMonth: YearMonth) : AnalysisSelection()
}

data class MonthOption(
    val yearMonth: YearMonth,
    val label: String,
    val total: Double
)

data class AnalysisUiState(
    val selection: AnalysisSelection = AnalysisSelection.ThisWeek,
    val periodTotal: Double = 0.0,
    val periodTransactionCount: Int = 0,
    val weeklyTotal: Double = 0.0,
    val weeklyTransactionCount: Int = 0,
    val weeklyLabel: String = "Mon – this week",
    val monthlyTotal: Double = 0.0,
    val monthlyCaption: String = "this month",
    val allTimeTotal: Double = 0.0,
    val allTimeTransactionCount: Int = 0,
    val monthOptions: List<MonthOption> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val topCategory: String? = null,
    val periodLabel: String = "this week",
    val trackingSinceLabel: String? = null
)

class AnalysisViewModel(
    repository: ExpenseRepository,
    customCategoryStore: CustomCategoryStore
) : ViewModel() {

    private val selection = MutableStateFlow<AnalysisSelection>(AnalysisSelection.ThisWeek)

    val uiState: StateFlow<AnalysisUiState> = combine(
        repository.observeAllSortedByDateDesc(),
        selection,
        customCategoryStore.custom
    ) { expenses, selected, _ ->
        buildState(expenses, selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState()
    )

    fun selectWeek() = selection.update { AnalysisSelection.ThisWeek }
    fun selectThisMonth() = selection.update { AnalysisSelection.ThisMonth }
    fun selectAllTime() = selection.update { AnalysisSelection.AllTime }
    fun selectMonth(yearMonth: YearMonth) = selection.update { AnalysisSelection.Month(yearMonth) }

    private fun buildState(
        expenses: List<ExpenseEntity>,
        selected: AnalysisSelection
    ): AnalysisUiState {
        val today = DateRanges.today()
        val weekStart = DateRanges.weekStart(today)
        val monthStart = DateRanges.monthStart(today)
        val currentYm = YearMonth.from(today)

        val weekItems = ExpenseAnalytics.inInclusiveRange(expenses, weekStart, today)
        val thisMonthItems = ExpenseAnalytics.inInclusiveRange(expenses, monthStart, today)

        val monthOptions = buildMonthOptions(expenses, currentYm)

        val periodExpenses = when (selected) {
            AnalysisSelection.ThisWeek -> weekItems
            AnalysisSelection.ThisMonth -> thisMonthItems
            AnalysisSelection.AllTime -> expenses
            is AnalysisSelection.Month -> expensesInMonth(expenses, selected.yearMonth)
        }

        val periodTotal = periodExpenses.sumOf { it.amount }
        val categoryTotals = ExpenseAnalytics.sumByCategory(periodExpenses)

        val categorySummaries = categoryTotals
            .filter { it.value > 0.0 }
            .entries
            .sortedByDescending { it.value }
            .map { (label, amount) ->
                val percent = if (periodTotal > 0.0) {
                    ((amount / periodTotal) * 100.0).toFloat()
                } else {
                    0f
                }
                CategorySummary(
                    label = label,
                    amount = amount,
                    // Bar width = share of selected period total
                    fraction = (percent / 100f).coerceIn(0f, 1f),
                    percentOfTotal = percent
                )
            }

        val trackingSince = expenses
            .mapNotNull { DateRanges.parseIso(it.date) }
            .minOrNull()
            ?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

        val daysIntoMonth = today.dayOfMonth
        val monthlyCaption = if (daysIntoMonth == 1) {
            "1 day this month"
        } else {
            "$daysIntoMonth days this month"
        }

        val periodLabel = when (selected) {
            AnalysisSelection.ThisWeek -> "this week"
            AnalysisSelection.ThisMonth -> "this month"
            AnalysisSelection.AllTime -> "all time"
            is AnalysisSelection.Month -> selected.yearMonth.format(
                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
            )
        }

        return AnalysisUiState(
            selection = selected,
            periodTotal = periodTotal,
            periodTransactionCount = periodExpenses.size,
            weeklyTotal = weekItems.sumOf { it.amount },
            weeklyTransactionCount = weekItems.size,
            weeklyLabel = "Mon – this week",
            monthlyTotal = thisMonthItems.sumOf { it.amount },
            monthlyCaption = monthlyCaption,
            allTimeTotal = expenses.sumOf { it.amount },
            allTimeTransactionCount = expenses.size,
            monthOptions = monthOptions,
            categorySummaries = categorySummaries,
            topCategory = categorySummaries.firstOrNull()?.label,
            periodLabel = periodLabel,
            trackingSinceLabel = trackingSince?.let { "Tracking since $it" }
        )
    }

    /**
     * Previous month first, then going back through 12 months
     * (e.g. Jun → May → … → Jul of previous year).
     */
    private fun buildMonthOptions(
        expenses: List<ExpenseEntity>,
        currentYm: YearMonth
    ): List<MonthOption> {
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
        return (1..12).map { offset ->
            val ym = currentYm.minusMonths(offset.toLong())
            MonthOption(
                yearMonth = ym,
                label = ym.format(formatter),
                total = expensesInMonth(expenses, ym).sumOf { it.amount }
            )
        }
    }

    private fun expensesInMonth(
        expenses: List<ExpenseEntity>,
        yearMonth: YearMonth
    ): List<ExpenseEntity> {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        return ExpenseAnalytics.inInclusiveRange(expenses, start, end)
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
