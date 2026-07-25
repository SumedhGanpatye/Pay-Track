package com.sumedh.moneytracker.util

import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateRanges {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().format(iso)

    fun weekStart(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekEnd(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun monthStart(date: LocalDate = today()): LocalDate =
        date.withDayOfMonth(1)

    fun monthEnd(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.lastDayOfMonth())

    fun toIso(date: LocalDate): String = date.format(iso)

    fun parseIso(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, iso) }.getOrNull()
}

object ExpenseAnalytics {
    /** Primary chart order before custom categories. */
    val primaryCategories = listOf(
        PaymentPrimaryCategories.FOOD,
        PaymentPrimaryCategories.TRAVEL,
        PaymentPrimaryCategories.SHOPPING
    )

    fun normalizeCategory(label: String): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return PaymentPrimaryCategories.OTHER
        if (trimmed.equals("others", ignoreCase = true) ||
            trimmed.equals("other", ignoreCase = true)
        ) {
            return PaymentPrimaryCategories.OTHER
        }
        PaymentPrimaryCategories.labels
            .firstOrNull { it.equals(trimmed, ignoreCase = true) }
            ?.let { return it }
        return trimmed
    }

    fun inInclusiveRange(
        expenses: List<ExpenseEntity>,
        start: LocalDate,
        end: LocalDate
    ): List<ExpenseEntity> {
        return expenses.filter { expense ->
            val date = DateRanges.parseIso(expense.date) ?: return@filter false
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    fun sumByCategory(expenses: List<ExpenseEntity>): Map<String, Double> {
        val totals = linkedMapOf<String, Double>()
        expenses.forEach { expense ->
            val key = normalizeCategory(expense.category)
            totals[key] = (totals[key] ?: 0.0) + expense.amount
        }
        return totals
    }

    /**
     * Ordered categories for analytics bars:
     * Food → Travel → Shopping → custom (A–Z) → leftover spent categories.
     * Excludes "Other" — that bucket is not shown on the chart.
     */
    fun orderedCategoriesForChart(
        expenses: List<ExpenseEntity>,
        customCategories: List<String>
    ): List<String> {
        val spent = sumByCategory(expenses).keys
        val ordered = linkedSetOf<String>()
        primaryCategories.forEach { ordered.add(it) }
        customCategories
            .map { normalizeCategory(it) }
            .filter {
                it != PaymentPrimaryCategories.OTHER &&
                    it !in PaymentPrimaryCategories.labels
            }
            .sortedBy { it.lowercase() }
            .forEach { ordered.add(it) }
        spent
            .filter {
                it != PaymentPrimaryCategories.OTHER &&
                    !it.equals("others", ignoreCase = true) &&
                    it !in ordered
            }
            .sortedBy { it.lowercase() }
            .forEach { ordered.add(it) }
        return ordered.toList()
    }

    /** Average daily spend for current calendar week (Mon–today). */
    fun weeklyDailyAverage(expenses: List<ExpenseEntity>, today: LocalDate = DateRanges.today()): Double {
        val start = DateRanges.weekStart(today)
        val weekItems = inInclusiveRange(expenses, start, today)
        val daysElapsed = (start.until(today).days + 1).coerceAtLeast(1)
        return weekItems.sumOf { it.amount } / daysElapsed
    }

    /** Average daily spend for current calendar month (1st–today). */
    fun monthlyDailyAverage(expenses: List<ExpenseEntity>, today: LocalDate = DateRanges.today()): Double {
        val start = DateRanges.monthStart(today)
        val monthItems = inInclusiveRange(expenses, start, today)
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)
        return monthItems.sumOf { it.amount } / daysElapsed
    }

    /** Indian grouping: 5,00,000 not 500,000. Whole rupees omit decimals (₹1). */
    fun formatIndianNumber(amount: Double): String {
        val nf = java.text.NumberFormat.getNumberInstance(Locale("en", "IN"))
        if (kotlin.math.abs(amount % 1.0) < 0.000_001) {
            nf.maximumFractionDigits = 0
            nf.minimumFractionDigits = 0
            return nf.format(amount.toLong())
        }
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 2
        return nf.format(amount)
    }

    fun formatInr(amount: Double): String = "₹${formatIndianNumber(amount)}"
}
