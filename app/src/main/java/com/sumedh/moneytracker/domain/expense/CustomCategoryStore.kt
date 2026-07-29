package com.sumedh.moneytracker.domain.expense

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Locally persisted user-created categories from Payment Details "Other".
 */
class CustomCategoryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _custom = MutableStateFlow(read())
    val custom: StateFlow<List<String>> = _custom.asStateFlow()

    fun current(): List<String> = _custom.value

    fun add(name: String): String? {
        val cleaned = name.trim()
            .replace(Regex("\\s+"), " ")
            .take(24)
        if (cleaned.isBlank()) return null
        val exists = PaymentPrimaryCategories.labels.any { it.equals(cleaned, true) } ||
            _custom.value.any { it.equals(cleaned, true) }
        if (exists) {
            // Still select existing match
            return _custom.value.firstOrNull { it.equals(cleaned, true) }
                ?: PaymentPrimaryCategories.labels.first { it.equals(cleaned, true) }
        }
        val updated = (_custom.value + cleaned).distinct()
        prefs.edit { putStringSet(KEY_CUSTOM, updated.toSet()) }
        _custom.value = updated
        return cleaned
    }

    fun remove(name: String): Boolean {
        val target = name.trim()
        if (target.isBlank()) return false
        val updated = _custom.value.filterNot { it.equals(target, ignoreCase = true) }
        if (updated.size == _custom.value.size) return false
        prefs.edit { putStringSet(KEY_CUSTOM, updated.toSet()) }
        _custom.value = updated
        return true
    }

    fun clear() {
        prefs.edit { remove(KEY_CUSTOM) }
        _custom.value = emptyList()
    }

    private fun read(): List<String> {
        return prefs.getStringSet(KEY_CUSTOM, emptySet())
            ?.toList()
            ?.sortedBy { it.lowercase() }
            .orEmpty()
    }

    companion object {
        private const val PREFS_NAME = "custom_categories"
        private const val KEY_CUSTOM = "names"
    }
}

object PaymentPrimaryCategories {
    const val FOOD = "Food"
    const val TRAVEL = "Travel"
    const val SHOPPING = "Shopping"
    const val BILLS = "Bills"
    const val GYM = "Gym"
    const val OTHER = "Other"

    /** Built-in chips shown before customs (Other is rendered last). */
    val primaries = listOf(FOOD, TRAVEL, SHOPPING, BILLS, GYM)

    /** Home screen category summary cards. */
    val homeSummaryCategories = listOf(FOOD, SHOPPING, TRAVEL, BILLS, GYM, OTHER)

    /** All primary labels including Other (for existence checks / analytics). */
    val labels = listOf(FOOD, TRAVEL, SHOPPING, BILLS, GYM, OTHER)

    fun idFor(label: String): String = label.trim().lowercase().replace(' ', '_')
}
