package com.sumedh.moneytracker.ui.screens.scanpay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.expense.CustomCategoryStore
import com.sumedh.moneytracker.domain.upi.PaymentDraft
import com.sumedh.moneytracker.domain.upi.PaymentPreferences
import com.sumedh.moneytracker.domain.upi.PaymentSession
import com.sumedh.moneytracker.domain.upi.PendingUpiScan
import com.sumedh.moneytracker.domain.upi.UpiApp
import com.sumedh.moneytracker.domain.upi.UpiDebugLog
import com.sumedh.moneytracker.domain.upi.UpiPaymentLauncher
import com.sumedh.moneytracker.domain.upi.UpiQrParser
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import android.app.Activity
import androidx.activity.result.ActivityResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class PaymentDetailsUiState(
    val merchantName: String = "",
    val upiId: String = "",
    val verified: Boolean = true,
    val amountInput: String = "",
    val amountLocked: Boolean = false,
    val selectedCategory: String? = null,
    val customCategories: List<String> = emptyList(),
    val showCustomCategoryDialog: Boolean = false,
    val note: String = "",
    val selectedUpiApp: UpiApp = UpiApp.GOOGLE_PAY,
    val availableApps: List<UpiApp> = UpiApp.entries,
    val showAppSheet: Boolean = false,
    val saveAsDefaultChecked: Boolean = true,
    val askBeforeEveryPayment: Boolean = false,
    val amountError: Boolean = false,
    val categoryShake: Boolean = false,
    val showReturnDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isReady: Boolean = false,
    /** true → focus amount; false → focus note (amount already present). */
    val autofocusAmount: Boolean = true
)

sealed interface PaymentDetailsEvent {
    data class LaunchUpi(val intent: android.content.Intent) : PaymentDetailsEvent
    data object NavigateSuccess : PaymentDetailsEvent
    data object NavigateHome : PaymentDetailsEvent
    data class ShowMessage(val text: String) : PaymentDetailsEvent
}

class PaymentDetailsViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val paymentPreferences: PaymentPreferences,
    private val customCategoryStore: CustomCategoryStore
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaymentDetailsUiState())
    val uiState: StateFlow<PaymentDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaymentDetailsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PaymentDetailsEvent> = _events.asSharedFlow()

    private var pendingPayAfterSheet: Boolean = false
    /** Prevents duplicate DB inserts / notifications on double-tap of "Yes, paid". */
    private var completedSaveInFlight: Boolean = false

    init {
        hydrateFromScan()
        viewModelScope.launch {
            customCategoryStore.custom.collect { customs ->
                _uiState.update { it.copy(customCategories = customs) }
            }
        }
    }

    private fun hydrateFromScan() {
        val prefs = paymentPreferences.current()
        val apps = UpiPaymentLauncher.installedApps(getApplication())
        val selected = if (apps.contains(prefs.defaultUpiApp)) {
            prefs.defaultUpiApp
        } else {
            apps.firstOrNull() ?: UpiApp.GOOGLE_PAY
        }

        val raw = PendingUpiScan.peek().orEmpty()
        val parsed = UpiQrParser.parse(raw)
        val merchant = parsed?.payeeName?.takeIf { it.isNotBlank() }
            ?: parsed?.payeeAddress?.substringBefore('@')?.replaceFirstChar { it.uppercase() }
            ?: "Merchant"
        val upiId = parsed?.payeeAddress.orEmpty()
        val amount = parsed?.amount.orEmpty()
        val amountPresent = amount.toDoubleOrNull()?.let { it > 0 } == true

        _uiState.update {
            it.copy(
                merchantName = merchant,
                upiId = upiId,
                verified = upiId.isNotBlank(),
                amountInput = amount,
                amountLocked = parsed?.amountLocked == true,
                selectedCategory = prefs.lastCategory,
                selectedUpiApp = selected,
                availableApps = apps,
                customCategories = customCategoryStore.current(),
                saveAsDefaultChecked = true,
                askBeforeEveryPayment = prefs.askBeforeEveryPayment,
                isReady = upiId.isNotBlank(),
                autofocusAmount = !amountPresent
            )
        }

        UpiDebugLog.banner("PAYMENT DETAILS OPEN")
        UpiDebugLog.section("PAYMENT_SCREEN")
        UpiDebugLog.field("Merchant Name", merchant)
        UpiDebugLog.field("UPI ID", upiId)
        UpiDebugLog.field("Amount", amount)
        UpiDebugLog.field("Note", _uiState.value.note)
        UpiDebugLog.field("Selected Category", prefs.lastCategory)
        UpiDebugLog.field("Selected UPI App", selected.displayName)
        UpiDebugLog.field("Selected UPI Package", selected.packageName)
        UpiDebugLog.field("amountLocked", (parsed?.amountLocked == true).toString())
        UpiDebugLog.field("raw_pending_qr_length", raw.length.toString())

        syncDraft()
    }

    fun onAmountChange(value: String) {
        if (_uiState.value.amountLocked) return
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { raw ->
                val parts = raw.split('.')
                if (parts.size <= 1) raw
                else parts.first() + "." + parts.drop(1).joinToString("").take(2)
            }
        _uiState.update { it.copy(amountInput = filtered, amountError = false) }
        syncDraft()
    }

    fun onPrimaryCategorySelected(label: String) {
        paymentPreferences.setLastCategory(label)
        _uiState.update {
            it.copy(selectedCategory = label, categoryShake = false)
        }
        syncDraft()
    }

    fun onOtherCategoryClicked() {
        _uiState.update { it.copy(showCustomCategoryDialog = true, categoryShake = false) }
    }

    fun dismissCustomCategoryDialog() {
        _uiState.update { it.copy(showCustomCategoryDialog = false) }
    }

    fun onCustomCategorySaved(name: String) {
        val saved = customCategoryStore.add(name) ?: return
        paymentPreferences.setLastCategory(saved)
        _uiState.update {
            it.copy(
                selectedCategory = saved,
                showCustomCategoryDialog = false,
                categoryShake = false,
                customCategories = customCategoryStore.current()
            )
        }
        syncDraft()
    }

    fun onCustomCategorySelected(label: String) {
        paymentPreferences.setLastCategory(label)
        _uiState.update {
            it.copy(selectedCategory = label, categoryShake = false)
        }
        syncDraft()
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note.take(80)) }
        syncDraft()
    }

    fun openAppSheet() {
        val apps = UpiPaymentLauncher.installedApps(getApplication())
        UpiDebugLog.line("UPI chooser apps (${apps.size}): ${apps.joinToString { it.displayName }}")
        _uiState.update {
            it.copy(
                availableApps = apps,
                showAppSheet = true
            )
        }
    }

    fun dismissAppSheet() {
        pendingPayAfterSheet = false
        _uiState.update { it.copy(showAppSheet = false) }
    }

    fun onSaveAsDefaultChange(checked: Boolean) {
        _uiState.update { it.copy(saveAsDefaultChecked = checked) }
    }

    fun onUpiAppSelected(app: UpiApp) {
        _uiState.update {
            it.copy(selectedUpiApp = app, showAppSheet = false)
        }
        if (_uiState.value.saveAsDefaultChecked) {
            paymentPreferences.setDefaultUpiApp(app)
        }
        syncDraft()
    }

    fun onPayClicked() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull()
        var valid = true

        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(amountError = true) }
            valid = false
        }
        if (state.selectedCategory.isNullOrBlank()) {
            _uiState.update { it.copy(categoryShake = true) }
            valid = false
        }
        if (state.upiId.isBlank()) {
            _events.tryEmit(PaymentDetailsEvent.ShowMessage("Merchant UPI ID missing"))
            valid = false
        }
        if (!valid) return

        syncDraft()

        if (state.askBeforeEveryPayment) {
            pendingPayAfterSheet = true
            _uiState.update { it.copy(showAppSheet = true) }
            return
        }

        launchPayment()
    }

    fun onUpiAppSelectedAndMaybePay(app: UpiApp) {
        onUpiAppSelected(app)
        if (pendingPayAfterSheet) {
            pendingPayAfterSheet = false
            launchPayment()
        }
    }

    private fun launchPayment() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: return

        UpiDebugLog.banner("PAY CLICKED — LAUNCH")
        UpiDebugLog.section("PAYMENT_SCREEN_AT_LAUNCH")
        UpiDebugLog.field("Merchant Name", state.merchantName)
        UpiDebugLog.field("UPI ID", state.upiId)
        UpiDebugLog.field("Amount", state.amountInput)
        UpiDebugLog.field("Note", state.note)
        UpiDebugLog.field("Selected Category", state.selectedCategory)
        UpiDebugLog.field("Selected UPI App", state.selectedUpiApp.displayName)
        UpiDebugLog.field("Selected UPI Package", state.selectedUpiApp.packageName)

        val originalRawQr = PaymentSession.draft?.rawQr
            ?.takeIf { it.isNotBlank() }
            ?: PendingUpiScan.peek().orEmpty()
        UpiDebugLog.field("original_raw_qr_present", originalRawQr.isNotBlank().toString())
        UpiDebugLog.field("original_raw_qr_length", originalRawQr.length.toString())

        val personalGpayQr = UpiPaymentLauncher.isLikelyPersonalGpayQr(
            originalRawQr = originalRawQr,
            upiId = state.upiId
        )
        if (personalGpayQr && state.selectedUpiApp == UpiApp.GOOGLE_PAY) {
            _events.tryEmit(
                PaymentDetailsEvent.ShowMessage(
                    "GPay often blocks personal UPI from other apps. " +
                        "If you see a bank-limit error, try PhonePe or BHIM."
                )
            )
        }

        val intent = try {
            UpiPaymentLauncher.createPayIntent(
                upiId = state.upiId,
                merchantName = state.merchantName,
                amount = amount,
                note = state.note,
                targetApp = state.selectedUpiApp,
                originalRawQr = originalRawQr
            )
        } catch (e: Exception) {
            UpiDebugLog.line("createPayIntent threw: ${e.message} — falling back to generic")
            UpiPaymentLauncher.createGenericPayIntent(
                upiId = state.upiId,
                merchantName = state.merchantName,
                amount = amount,
                note = state.note,
                originalRawQr = originalRawQr
            )
        }
        UpiPaymentLauncher.logIntentResolution(
            context = getApplication(),
            intent = intent,
            targetPackage = intent.`package` ?: state.selectedUpiApp.packageName
        )
        PaymentSession.markAwaitingReturn()
        UpiDebugLog.line("awaitingUpiReturn = true — emitting LaunchUpi")
        _events.tryEmit(PaymentDetailsEvent.LaunchUpi(intent))
    }

    fun onReturnedFromUpi(result: ActivityResult? = null, source: String = "unknown") {
        UpiDebugLog.banner("UPI APP RETURNED")
        UpiDebugLog.section("RESULT")
        UpiDebugLog.field("source", source)
        UpiDebugLog.field("awaitingUpiReturn", PaymentSession.awaitingUpiReturn.toString())

        if (result != null) {
            val code = result.resultCode
            val codeLabel = when (code) {
                Activity.RESULT_OK -> "RESULT_OK (-1)"
                Activity.RESULT_CANCELED -> "RESULT_CANCELED (0)"
                else -> "RESULT_$code"
            }
            UpiDebugLog.field("resultCode", codeLabel)
            val data = result.data
            UpiDebugLog.field("intent_returned", data?.toString())
            UpiDebugLog.field("intent_data_uri", data?.dataString)
            val extras = data?.extras
            if (extras == null || extras.isEmpty) {
                UpiDebugLog.line("  extras = <none>")
            } else {
                UpiDebugLog.line("  extras keys = ${extras.keySet()}")
                for (key in extras.keySet()) {
                    @Suppress("DEPRECATION")
                    val value = extras.get(key)
                    UpiDebugLog.field("extra[$key]", value?.toString())
                }
            }
            // Common UPI response keys (may or may not be present)
            listOf(
                "response", "Status", "status", "txnId", "txnRef",
                "ApprovalRefNo", "responseCode", "StatusCode"
            ).forEach { key ->
                val v = extras?.getString(key)
                if (v != null) UpiDebugLog.field("upi_$key", v)
            }
        } else {
            UpiDebugLog.line("  ActivityResult = <null> (likely ON_RESUME fallback; no result extras)")
        }

        if (!PaymentSession.awaitingUpiReturn) {
            UpiDebugLog.line("ignored — not awaiting UPI return")
            return
        }
        PaymentSession.clearAwaitingReturn()
        UpiDebugLog.line("showing payment status dialog")
        _uiState.update { it.copy(showReturnDialog = true) }
    }

    fun onPaymentConfirmedSuccessful() {
        if (completedSaveInFlight) return
        completedSaveInFlight = true
        _uiState.update { it.copy(showReturnDialog = false) }
        viewModelScope.launch {
            val saved = saveExpense(status = ExpenseEntity.STATUS_COMPLETED)
            if (saved) {
                _events.emit(PaymentDetailsEvent.NavigateSuccess)
            } else {
                completedSaveInFlight = false
            }
        }
    }

    fun onPaymentCancelled() {
        _uiState.update {
            it.copy(showReturnDialog = false, showCancelDialog = true)
        }
    }

    fun discardPending() {
        _uiState.update { it.copy(showCancelDialog = false) }
        PaymentSession.clear()
        _events.tryEmit(PaymentDetailsEvent.NavigateHome)
    }

    fun saveAsPending() {
        viewModelScope.launch {
            saveExpense(status = ExpenseEntity.STATUS_PENDING)
            _uiState.update { it.copy(showCancelDialog = false) }
            _events.emit(PaymentDetailsEvent.ShowMessage("Saved as pending expense"))
            _events.emit(PaymentDetailsEvent.NavigateHome)
        }
    }

    fun dismissReturnDialog() {
        _uiState.update { it.copy(showReturnDialog = false) }
    }

    /**
     * Persists the expense. Notification is shown only for completed UPI payments
     * after a successful insert — never for cancel / pending / failed saves.
     *
     * @return true if a row was inserted
     */
    private suspend fun saveExpense(status: String): Boolean {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: return false
        if (amount <= 0.0) return false
        val category = state.selectedCategory?.trim().orEmpty().ifBlank { "Others" }
        paymentPreferences.setLastCategory(category)
        val userNote = state.note.trim()
        val baseNote = userNote.ifBlank { state.merchantName }
        val note = if (status == ExpenseEntity.STATUS_PENDING) {
            "Pending · $baseNote"
        } else {
            baseNote
        }
        val nowDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        repository.insert(
            ExpenseEntity(
                amount = amount,
                date = nowDate,
                time = nowTime,
                note = note,
                category = category,
                isReconciled = status == ExpenseEntity.STATUS_COMPLETED,
                status = status
            )
        )
        if (status == ExpenseEntity.STATUS_COMPLETED) {
            // Same helper / channel / copy as Quick Add; user note only (not merchant fallback).
            ExpenseNotificationHelper.showExpenseAdded(
                context = getApplication(),
                amount = amount,
                category = category,
                note = userNote.ifBlank { null }
            )
            PendingUpiScan.consume()
        } else {
            PaymentSession.clear()
        }
        return true
    }

    private fun syncDraft() {
        val state = _uiState.value
        if (state.upiId.isBlank()) return
        PaymentSession.setDraft(
            PaymentDraft(
                rawQr = PendingUpiScan.peek().orEmpty(),
                merchantName = state.merchantName,
                upiId = state.upiId,
                amount = state.amountInput,
                amountLocked = state.amountLocked,
                category = state.selectedCategory.orEmpty(),
                note = state.note,
                selectedUpiApp = state.selectedUpiApp,
                verified = state.verified
            )
        )
    }

    companion object {
        fun factory(
            repository: ExpenseRepository,
            paymentPreferences: PaymentPreferences,
            customCategoryStore: CustomCategoryStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras
                ): T {
                    val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as Application
                    return PaymentDetailsViewModel(
                        app,
                        repository,
                        paymentPreferences,
                        customCategoryStore
                    ) as T
                }
            }
    }
}
