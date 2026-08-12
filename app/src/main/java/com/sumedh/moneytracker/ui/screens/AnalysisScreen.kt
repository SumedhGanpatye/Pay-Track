package com.sumedh.moneytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.components.ScreenHeader
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.screens.home.CategorySummarySection
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.SoftMint
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics
import java.time.YearMonth

@Composable
fun AnalysisScreen(
    repository: ExpenseRepository,
    viewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModel.factory(
            repository = repository,
            customCategoryStore = (LocalContext.current.applicationContext as MoneyTrackerApp)
                .customCategoryStore
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isWeekSelected = state.selection is AnalysisSelection.ThisWeek
    val isThisMonthSelected = state.selection is AnalysisSelection.ThisMonth
    val isAllTimeSelected = state.selection is AnalysisSelection.AllTime
    val selectedMonth = (state.selection as? AnalysisSelection.Month)?.yearMonth

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            ScreenHeader(
                title = "Analytics",
                subtitle = "See where your money goes"
            )

            state.trackingSinceLabel?.let { label ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonTeal.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(
                modifier = Modifier.clickable { viewModel.selectWeek() },
                cornerRadius = 24.dp,
                contentPadding = 0.dp,
                highlighted = isWeekSelected
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NeonTeal.copy(alpha = 0.14f),
                                    CardBackground,
                                    SecondaryCard
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "WEEKLY SPEND",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ExpenseAnalytics.formatInr(state.weeklyTotalWithBills),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                append("${state.weeklyTransactionCount} transactions")
                                if (isWeekSelected) {
                                    state.topCategory?.let { append("  ·  top $it") }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.DateRange,
                    title = "Monthly spend",
                    amount = state.monthlyTotalExcludingBills,
                    amountWithBills = state.monthlyTotalWithBills,
                    caption = state.monthlyCaption,
                    selected = isThisMonthSelected,
                    onClick = viewModel::selectThisMonth
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Wallet,
                    title = "Total spend",
                    amount = state.allTimeTotal,
                    caption = "${state.allTimeTransactionCount} all time",
                    selected = isAllTimeSelected,
                    onClick = viewModel::selectAllTime
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            MonthCompareDropdown(
                options = state.monthOptions,
                selectedMonth = selectedMonth,
                onSelect = viewModel::selectMonth
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.categorySummaries.isEmpty() && state.billsTotal <= 0.0) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard {
                    Text(
                        text = "No categorized spend ${state.periodLabel} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                CategorySummarySection(
                    categories = state.categorySummaries,
                    periodLabel = state.periodLabel,
                    billsTotal = state.billsTotal
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MonthCompareDropdown(
    options: List<MonthOption>,
    selectedMonth: YearMonth?,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.yearMonth == selectedMonth }
    val isSelected = selectedOption != null
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (isSelected) NeonTeal.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Compare months",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, borderColor, shape),
                color = Color.Transparent,
                shape = shape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    if (isSelected) NeonTeal.copy(alpha = 0.18f) else SecondaryCard,
                                    CardBackground
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedOption?.label ?: "Choose a month",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selectedOption != null) {
                                Text(
                                    text = ExpenseAnalytics.formatInr(selectedOption.total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftMint
                                )
                            } else {
                                Text(
                                    text = "Previous 12 months",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonTeal.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = SoftMint
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(
                        Brush.verticalGradient(listOf(SecondaryCard, CardBackground)),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, NeonTeal.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            ) {
                options.forEach { option ->
                    val selected = option.yearMonth == selectedMonth
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (selected) {
                                            Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NeonTeal.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        } else {
                                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        }
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) SoftMint else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = ExpenseAnalytics.formatInr(option.total),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftMint
                                )
                            }
                        },
                        onClick = {
                            onSelect(option.yearMonth)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    title: String,
    amount: Double,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountWithBills: Double? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = if (selected) NeonTeal else Color.White.copy(alpha = 0.07f)
    val bgBrush = if (selected) {
        Brush.verticalGradient(listOf(NeonTeal.copy(alpha = 0.2f), CardBackground))
    } else {
        Brush.verticalGradient(listOf(SecondaryCard, CardBackground))
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape),
        color = Color.Transparent,
        shape = shape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonTeal.copy(alpha = if (selected) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ExpenseAnalytics.formatInr(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (amountWithBills != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ExpenseAnalytics.formatInr(amountWithBills)} with Bills",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftMint
                )
            }
            Text(text = caption, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
