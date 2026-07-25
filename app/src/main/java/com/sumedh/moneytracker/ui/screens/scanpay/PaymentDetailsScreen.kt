package com.sumedh.moneytracker.ui.screens.scanpay

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.upi.PaymentSession
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.AmountSection
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.CategorySelector
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.CustomCategoryDialog
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.FloatingPayButton
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.MerchantCard
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.NoteInput
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentDetailsAppBar
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentMethodBottomSheet
import com.sumedh.moneytracker.ui.screens.scanpay.components.payment.PaymentMethodCard
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreen(
    repository: ExpenseRepository,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
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

    val upiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onReturnedFromUpi()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentDetailsEvent.LaunchUpi -> {
                    try {
                        upiLauncher.launch(event.intent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "Could not open ${uiState.selectedUpiApp.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        PaymentSession.clearAwaitingReturn()
                    }
                }
                PaymentDetailsEvent.NavigateSuccess -> onPaymentSuccess()
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
            if (uiState.autofocusAmount && !uiState.amountLocked) {
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
            if (event == Lifecycle.Event.ON_RESUME && PaymentSession.awaitingUpiReturn) {
                viewModel.onReturnedFromUpi()
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
                        // Keep last fields clear of the floating Pay button
                        .padding(bottom = 88.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    AmountSection(
                        amount = uiState.amountInput,
                        onAmountChange = viewModel::onAmountChange,
                        readOnly = uiState.amountLocked,
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

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 }
                    ) {
                        MerchantCard(
                            merchantName = uiState.merchantName.ifBlank { "Unknown merchant" },
                            upiId = uiState.upiId.ifBlank { "UPI ID unavailable" },
                            verified = uiState.verified && uiState.upiId.isNotBlank()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PaymentMethodCard(
                        selectedApp = uiState.selectedUpiApp,
                        onClick = viewModel::openAppSheet
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                FloatingPayButton(
                    amount = amountValue,
                    enabled = amountReady && uiState.upiId.isNotBlank(),
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
        AlertDialog(
            onDismissRequest = viewModel::dismissReturnDialog,
            title = { Text("Payment status") },
            text = {
                Text(
                    "Did you complete this payment in ${uiState.selectedUpiApp.displayName}?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onPaymentConfirmedSuccessful) {
                    Text("Yes, paid", color = NeonTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onPaymentCancelled) {
                    Text("No, cancelled", color = ErrorRed)
                }
            }
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
