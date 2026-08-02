package com.sumedh.moneytracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.screens.home.EmptyExpensesCard
import com.sumedh.moneytracker.ui.screens.home.ExpenseCard
import com.sumedh.moneytracker.ui.theme.ErrorRed
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

@Composable
fun HistoryScreen(
    repository: ExpenseRepository,
    scrollToTop: Boolean = false,
    onScrollToTopConsumed: () -> Unit = {},
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(repository)
    )
) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val daySpendTotals by viewModel.daySpendTotals.collectAsStateWithLifecycle()
    var selectedExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    val listState = rememberLazyListState()

    val groups = remember(expenses, daySpendTotals) {
        ExpenseAnalytics.groupByDay(expenses, dayTotalsByIso = daySpendTotals)
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            listState.scrollToItem(0)
            onScrollToTopConsumed()
        }
    }

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (expenses.isEmpty()) {
                    "All your expenses will appear here"
                } else {
                    "${expenses.size} transactions · Long-press to delete"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (expenses.isEmpty()) {
                EmptyExpensesCard()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        item(key = "header_${group.isoDate}") {
                            DayHeaderRow(
                                title = group.header,
                                dayTotal = group.dayTotal
                            )
                        }
                        items(
                            items = group.expenses,
                            key = { it.id }
                        ) { expense ->
                            ExpenseCard(
                                expense = expense,
                                onLongPress = { selectedExpense = expense }
                            )
                        }
                        item(key = "spacer_${group.isoDate}") {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    selectedExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { selectedExpense = null },
            title = { Text("Delete expense?") },
            text = {
                Text(
                    "${expense.title} · ${ExpenseAnalytics.formatInr(expense.amount)}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        selectedExpense = null
                    }
                ) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedExpense = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DayHeaderRow(
    title: String,
    dayTotal: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonTeal)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ExpenseAnalytics.formatInr(dayTotal),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
