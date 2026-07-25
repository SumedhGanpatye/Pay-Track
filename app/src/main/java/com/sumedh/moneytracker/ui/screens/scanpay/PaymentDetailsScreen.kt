package com.sumedh.moneytracker.ui.screens.scanpay

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.upi.PaymentSession
import com.sumedh.moneytracker.domain.upi.UpiDebugLog
import com.sumedh.moneytracker.domain.upi.UpiPaymentLauncher
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.AmountSection
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.CategorySelector
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.CustomCategoryDialog
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.FloatingPayButton
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.NoteInput
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentDetailsAppBar
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentMethodBottomSheet
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentMethodCard
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreen(
    repository: ExpenseRepository,
    onBack: () -> Unit,
    onPaymentRecorded: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: PaymentDetailsViewModel = viewModel(
        factory = PaymentDetailsViewModel.factory(
            repository = repository,
            paymentPreferences = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .paymentPreferences,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboard = LocalSoftwareKeyboardController.current
    val amountValue = uiState.amountInput.toDoubleOrNull()
    val amountReady = amountValue != null && amountValue > 0
    val scrollState = rememberScrollState()
    val amountFocus = remember { FocusRequester() }
    val noteFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentDetailsEvent.LaunchUpi -> {
                    try {
                        // Fresh copy right before leave so UPI / keyboard can suggest paste.
                        UpiPaymentLauncher.recopyDraftAmount(context)
                        UpiDebugLog.line("startActivity → selected UPI app")
                        context.startActivity(event.intent)
                        // One more copy after launch (some OEMs clear clip during activity switch).
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            UpiPaymentLauncher.recopyDraftAmount(context.applicationContext)
                        }, 250)
                    } catch (e: Exception) {
                        UpiDebugLog.line(
                            "UPI launch EXCEPTION: ${e.javaClass.simpleName}: ${e.message}"
                        )
                        try {
                            UpiPaymentLauncher.recopyDraftAmount(context)
                            context.startActivity(
                                event.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e2: Exception) {
                            Toast.makeText(
                                context,
                                "Could not open ${uiState.selectedUpiApp.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            PaymentSession.clearAwaitingReturn()
                            UpiDebugLog.line("retry failed: ${e2.message}")
                        }
                    }
                }
                PaymentDetailsEvent.NavigateHomeAfterPaid -> onPaymentRecorded()
                PaymentDetailsEvent.NavigateHome -> onGoHome()
                is PaymentDetailsEvent.ShowMessage -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(uiState.isReady, uiState.autofocusAmount) {
        if (!uiState.isReady) return@LaunchedEffect
        delay(180)
        runCatching {
            if (uiState.autofocusAmount) {
                amountFocus.requestFocus()
                keyboard?.show()
            } else {
                noteFocus.requestFocus()
                keyboard?.show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Keep amount on clipboard while switching to UPI so paste chips appear.
                    if (PaymentSession.awaitingUpiReturn) {
                        UpiPaymentLauncher.recopyDraftAmount(context.applicationContext)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (PaymentSession.awaitingUpiReturn) {
                        viewModel.onReturnedFromUpi(result = null, source = "Lifecycle.ON_RESUME")
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PaymentDetailsAppBar(onBack = onBack)
        }
    ) { padding ->
        AppScreenBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "1. Enter amount, category & note\n" +
                            "2. Tap Copy & open — amount is copied for paste suggestions\n" +
                            "3. In the UPI app, scan the merchant QR\n" +
                            "4. Tap the amount field — choose the clipboard / paste chip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AmountSection(
                        amount = uiState.amountInput,
                        onAmountChange = viewModel::onAmountChange,
                        readOnly = false,
                        showError = uiState.amountError,
                        focusRequester = amountFocus
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CategorySelector(
                        selectedCategory = uiState.selectedCategory,
                        customCategories = uiState.customCategories,
                        onPrimarySelected = viewModel::onPrimaryCategorySelected,
                        onOtherSelected = viewModel::onOtherCategoryClicked,
                        onCustomSelected = viewModel::onCustomCategorySelected,
                        shake = uiState.categoryShake
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NoteInput(
                        note = uiState.note,
                        onNoteChange = viewModel::onNoteChange,
                        focusRequester = noteFocus
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PaymentMethodCard(
                        selectedApp = uiState.selectedUpiApp,
                        onClick = viewModel::openAppSheet
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                FloatingPayButton(
                    amount = amountValue,
                    enabled = amountReady,
                    visible = uiState.isReady,
                    onClick = viewModel::onPayClicked,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }

    if (uiState.showCustomCategoryDialog) {
        CustomCategoryDialog(
            onDismiss = viewModel::dismissCustomCategoryDialog,
            onSave = viewModel::onCustomCategorySaved
        )
    }

    if (uiState.showAppSheet) {
        PaymentMethodBottomSheet(
            apps = uiState.availableApps,
            selectedApp = uiState.selectedUpiApp,
            saveAsDefault = uiState.saveAsDefaultChecked,
            onSaveAsDefaultChange = viewModel::onSaveAsDefaultChange,
            onAppSelected = viewModel::onUpiAppSelectedAndMaybePay,
            onDismiss = viewModel::dismissAppSheet
        )
    }

    if (uiState.showReturnDialog) {
        val paidAmount = uiState.amountInput.toDoubleOrNull()
        val paidLabel = paidAmount?.let {
            com.sumedh.moneytracker.util.ExpenseAnalytics.formatInr(it)
        } ?: "this amount"
        AlertDialog(
            onDismissRequest = { /* require an explicit choice */ },
            title = {
                Text(
                    "Did you pay $paidLabel?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Confirm if the payment went through in ${uiState.selectedUpiApp.displayName}.",
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = viewModel::onPaymentConfirmedSuccessful,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonTeal,
                            contentColor = Charcoal900
                        )
                    ) {
                        Text(
                            "Yes, paid",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = viewModel::onPaymentCancelled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("No, cancelled", color = ErrorRed)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { /* force choice */ },
            title = { Text("Payment cancelled") },
            text = {
                Text(
                    "Would you like to save this as a pending expense?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveAsPending) {
                    Text("Save as Pending", color = NeonTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::discardPending) {
                    Text("Discard", color = ErrorRed)
                }
            }
        )
    }
}
