package com.sumedh.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.notification.NotificationPermissionManager
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.navigation.ManualExpensePrefill
import com.sumedh.moneytracker.ui.screens.home.CopyPayHeroCard
import com.sumedh.moneytracker.ui.screens.home.HomeWelcomeHeader
import com.sumedh.moneytracker.ui.screens.home.NotificationPermissionCard
import com.sumedh.moneytracker.ui.screens.home.QuickAddCard
import com.sumedh.moneytracker.ui.screens.home.RecentExpensesSection
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

@Composable
fun HomeScreen(
    repository: ExpenseRepository,
    username: String,
    manualExpensePrefill: ManualExpensePrefill? = null,
    onPrefillConsumed: () -> Unit = {},
    onViewAllExpenses: () -> Unit = {},
    onCopyPay: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore
        )
    )
) {
    val context = LocalContext.current
    val quickAdd by viewModel.quickAdd.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val daySpendTotals by viewModel.daySpendTotals.collectAsStateWithLifecycle()
    val todaySpent by viewModel.todaySpent.collectAsStateWithLifecycle()
    var selectedExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var notificationAccessEnabled by remember {
        mutableStateOf(NotificationPermissionManager.isNotificationListenerEnabled(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled =
                    NotificationPermissionManager.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(manualExpensePrefill) {
        manualExpensePrefill?.let {
            viewModel.applyPrefill(it)
            onPrefillConsumed()
        }
    }

    AppScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "header") {
                HomeWelcomeHeader(
                    username = username,
                    todaySpent = todaySpent
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!notificationAccessEnabled) {
                item(key = "permission") {
                    NotificationPermissionCard(
                        onEnableClick = {
                            NotificationPermissionManager.openNotificationAccessSettings(context)
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            item(key = "copy_pay") {
                CopyPayHeroCard(onClick = onCopyPay)
                Spacer(modifier = Modifier.height(14.dp))
            }

            item(key = "quick_add") {
                QuickAddCard(
                    state = quickAdd,
                    onAmountChange = viewModel::onAmountChange,
                    onNotesChange = viewModel::onNotesChange,
                    onPrimaryCategorySelected = viewModel::onPrimaryCategorySelected,
                    onOtherCategoryClicked = viewModel::onOtherCategoryClicked,
                    onCustomCategorySelected = viewModel::onCustomCategorySelected,
                    onCustomCategorySaved = viewModel::onCustomCategorySaved,
                    onDismissCustomCategoryDialog = viewModel::dismissCustomCategoryDialog,
                    onRemoveCustomCategory = viewModel::removeCustomCategory,
                    onToggleExpanded = viewModel::toggleExpanded,
                    onSave = viewModel::saveExpense,
                    onCancelEdit = viewModel::cancelEdit
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            item(key = "recent") {
                RecentExpensesSection(
                    expenses = recentExpenses,
                    daySpendTotals = daySpendTotals,
                    onViewAll = onViewAllExpenses,
                    onExpenseLongPress = { selectedExpense = it }
                )
            }
        }
    }

    selectedExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { selectedExpense = null },
            title = { Text("Expense options") },
            text = {
                Text(
                    "${expense.title} · ${ExpenseAnalytics.formatInr(expense.amount)}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startEditExpense(expense)
                        selectedExpense = null
                    }
                ) {
                    Text("Edit", color = NeonTeal, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.deleteExpense(expense)
                            selectedExpense = null
                        }
                    ) {
                        Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { selectedExpense = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }
}
