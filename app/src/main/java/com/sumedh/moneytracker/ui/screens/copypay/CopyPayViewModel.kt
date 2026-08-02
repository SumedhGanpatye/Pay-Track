package com.sumedh.moneytracker.ui.screens.copypay

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
import com.sumedh.moneytracker.domain.upi.UpiApp
import com.sumedh.moneytracker.domain.upi.UpiPaymentLauncher
import com.sumedh.moneytracker.domain.upi.UpiPreferences
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CopyPayUiState(
    val amountInput: String = "",
    val note: String = "",
    val selectedCategory: String = PaymentPrimaryCategories.FOOD,
    val customCategories: List<String> = emptyList(),
    val showCustomCategoryDialog: Boolean = false,
    val selectedApp: UpiApp = UpiApp.GPAY,
    val awaitingReturn: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showSplitOptions: Boolean = false,
    val launchError: String? = null,
    val saved: Boolean = false
) {
    val amount: Double? get() = amountInput.toDoubleOrNull()?.takeIf { it > 0.0 }
    val canCopyAndPay: Boolean get() = amount != null
    val categoryChips: List<String>
        get() = PaymentPrimaryCategories.primaries + customCategories + PaymentPrimaryCategories.OTHER
}

class CopyPayViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val customCategoryStore: CustomCategoryStore,
    private val upiPreferences: UpiPreferences
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(
        CopyPayUiState(
            customCategories = customCategoryStore.current(),
            selectedApp = upiPreferences.current()
        )
    )
    val ui: StateFlow<CopyPayUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            customCategoryStore.custom.collect { customs ->
                _ui.update { it.copy(customCategories = customs) }
            }
        }
        viewModelScope.launch {
            upiPreferences.defaultApp.collect { app ->
                _ui.update { it.copy(selectedApp = app) }
            }
        }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { raw ->
                val parts = raw.split('.')
                if (parts.size <= 1) raw
                else parts.first() + "." + parts.drop(1).joinToString("").take(2)
            }
        _ui.update { it.copy(amountInput = filtered, launchError = null) }
    }

    fun onNoteChange(value: String) {
        _ui.update { it.copy(note = value) }
    }

    fun onCategorySelected(category: String) {
        if (category == PaymentPrimaryCategories.OTHER) return
        _ui.update { it.copy(selectedCategory = category) }
    }

    fun onOtherCategoryClicked() {
        _ui.update { it.copy(showCustomCategoryDialog = true) }
    }

    fun dismissCustomCategoryDialog() {
        _ui.update { it.copy(showCustomCategoryDialog = false) }
    }

    fun onCustomCategorySaved(name: String) {
        val saved = customCategoryStore.add(name) ?: return
        _ui.update {
            it.copy(
                selectedCategory = saved,
                showCustomCategoryDialog = false,
                customCategories = customCategoryStore.current()
            )
        }
    }

    fun onCustomCategorySelected(category: String) {
        _ui.update { it.copy(selectedCategory = category) }
    }

    fun removeCustomCategory(category: String) {
        val removed = customCategoryStore.remove(category)
        if (!removed) return
        _ui.update { state ->
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

    fun onAppSelected(app: UpiApp) {
        upiPreferences.setDefault(app)
        _ui.update { it.copy(selectedApp = app, launchError = null) }
    }

    fun copyAndPay() {
        val state = _ui.value
        val amount = state.amount ?: return
        val ctx = getApplication<Application>()

        UpiPaymentLauncher.copyAmountToClipboard(ctx, amount)
        val launched = UpiPaymentLauncher.launchScanOrApp(ctx, state.selectedApp)
        if (launched) {
            _ui.update {
                it.copy(
                    awaitingReturn = true,
                    showConfirmDialog = false,
                    showSplitOptions = false,
                    launchError = null
                )
            }
        } else {
            _ui.update {
                it.copy(launchError = "Couldn’t open ${state.selectedApp.displayName}.")
            }
        }
    }

    fun onReturnedFromUpi() {
        val state = _ui.value
        if (state.awaitingReturn && state.amount != null) {
            _ui.update { it.copy(showConfirmDialog = true, awaitingReturn = false) }
        }
    }

    fun dismissConfirmDialog() {
        _ui.update { it.copy(showConfirmDialog = false, showSplitOptions = false) }
    }

    fun onSplitExpenseClicked() {
        _ui.update { it.copy(showConfirmDialog = false, showSplitOptions = true) }
    }

    fun dismissSplitOptions() {
        _ui.update { it.copy(showSplitOptions = false, showConfirmDialog = true) }
    }

    fun confirmPaymentSaved(onDone: () -> Unit, splitAmong: Int = 1) {
        val state = _ui.value
        val fullAmount = state.amount ?: return
        val divisor = splitAmong.coerceAtLeast(1)
        val amount = ((fullAmount / divisor) * 100.0).toLong() / 100.0
        if (amount <= 0.0) return
        val note = state.note.trim()
        val category = state.selectedCategory
        val shareNote = when (divisor) {
            2 -> "split in 2"
            3 -> "split in 3"
            else -> null
        }
        val savedNotes = buildString {
            if (note.isNotBlank()) append(note)
            if (shareNote != null) {
                if (isNotEmpty()) append(" · ")
                append(shareNote)
            }
        }

        viewModelScope.launch {
            val nowDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            repository.insert(
                ExpenseEntity(
                    amount = amount,
                    title = note.ifBlank { category },
                    category = category,
                    expenseType = ExpenseType.PERSONAL,
                    date = nowDate,
                    time = nowTime,
                    source = ExpenseSource.MANUAL,
                    notes = savedNotes
                )
            )
            ExpenseNotificationHelper.showExpenseAdded(
                context = getApplication(),
                amount = amount,
                category = category,
                note = note.takeIf { it.isNotBlank() } ?: shareNote
            )
            _ui.update {
                CopyPayUiState(
                    customCategories = customCategoryStore.current(),
                    selectedApp = upiPreferences.current(),
                    saved = true
                )
            }
            onDone()
        }
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            customCategoryStore: CustomCategoryStore,
            upiPreferences: UpiPreferences
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras
                ): T {
                    val app = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    )
                    return CopyPayViewModel(
                        app,
                        repository,
                        customCategoryStore,
                        upiPreferences
                    ) as T
                }
            }
    }
}
