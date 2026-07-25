package com.sumedh.moneytracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.screens.home.HomeHeader
import com.sumedh.moneytracker.ui.screens.home.QuickAddCard
import com.sumedh.moneytracker.ui.screens.home.RecentExpensesSection
import com.sumedh.moneytracker.ui.screens.home.ScanPayHeroCard
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

/**
 * Home — premium entry surface for Pay&Track.
 *
 * Hierarchy: Header → Scan & Pay → Quick Add → Recent Expenses.
 */
@Composable
fun HomeScreen(
    repository: ExpenseRepository,
    username: String,
    onScanAndPay: () -> Unit = {},
    onViewAllExpenses: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore
        )
    )
) {
    val quickAdd by viewModel.quickAdd.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val todaySpent by viewModel.todaySpent.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

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
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -it / 6 }
                ) {
                    HomeHeader(
                        username = username,
                        todaySpent = todaySpent,
                        todayCount = todayCount
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            item(key = "scan_pay") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 10 }
                ) {
                    ScanPayHeroCard(onClick = onScanAndPay)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item(key = "quick_add") {
                QuickAddCard(
                    state = quickAdd,
                    onAmountChange = viewModel::onAmountChange,
                    onPrimaryCategorySelected = viewModel::onPrimaryCategorySelected,
                    onOtherCategoryClicked = viewModel::onOtherCategoryClicked,
                    onCustomCategorySelected = viewModel::onCustomCategorySelected,
                    onCustomCategorySaved = viewModel::onCustomCategorySaved,
                    onDismissCustomCategoryDialog = viewModel::dismissCustomCategoryDialog,
                    onNoteChange = viewModel::onNoteChange,
                    onSave = viewModel::saveExpense
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            item(key = "recent") {
                RecentExpensesSection(
                    expenses = recentExpenses,
                    onViewAll = onViewAllExpenses,
                    onExpenseLongPress = { pendingDelete = it }
                )
            }
        }
    }

    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete expense?") },
            text = {
                Text(
                    "${expense.note} · ${ExpenseAnalytics.formatInr(expense.amount)}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = NeonTeal)
                }
            }
        )
    }
}
