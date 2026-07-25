package com.sumedh.moneytracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.CategoryColors
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

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

    AppScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "See where your money goes",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(
                cornerRadius = 26.dp,
                contentPadding = 0.dp,
                highlighted = true
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NeonTeal.copy(alpha = 0.16f),
                                    CardBackground,
                                    SecondaryCard
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = state.scopeLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonTeal
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = ExpenseAnalytics.formatInr(state.periodTotal),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.transactionCount} transactions" +
                                (state.topCategory?.let { "  ·  top $it" } ?: ""),
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
                ModernAvgTile(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.DateRange,
                    title = "Weekly avg",
                    amount = state.weeklyAvg,
                    caption = "per day · this week",
                    selected = state.scope == AnalysisScope.THIS_WEEK,
                    onClick = viewModel::selectWeekly
                )
                ModernAvgTile(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.DateRange,
                    title = "Monthly avg",
                    amount = state.monthlyAvg,
                    caption = "per day · this month",
                    selected = state.scope == AnalysisScope.THIS_MONTH,
                    onClick = viewModel::selectMonthly
                )
            }

            AnimatedVisibility(
                visible = state.scope != AnalysisScope.ALL_TIME,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                TextButton(
                    onClick = viewModel::showAllTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Refresh,
                        contentDescription = null,
                        tint = NeonTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to all-time totals",
                        color = NeonTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Category spend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (state.scope) {
                    AnalysisScope.ALL_TIME -> "% of all-time total spend"
                    AnalysisScope.THIS_WEEK -> "% of this week's total spend"
                    AnalysisScope.THIS_MONTH -> "% of this month's total spend"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(cornerRadius = 22.dp, contentPadding = 16.dp) {
                if (state.bars.isEmpty()) {
                    Text(
                        text = "No categorized spend in this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.bars.forEach { bar ->
                            CategorySpendBar(bar = bar)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModernAvgTile(
    icon: ImageVector,
    title: String,
    amount: Double,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (selected) NeonTeal else Color.White.copy(alpha = 0.07f)
    val bgBrush = if (selected) {
        Brush.verticalGradient(
            listOf(
                NeonTeal.copy(alpha = 0.28f),
                Color(0xFF13241F)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1C232C),
                Color(0xFF141920)
            )
        )
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.5.dp, borderColor, shape),
        color = Color.Transparent,
        shape = shape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) NeonTeal.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) NeonTeal else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (selected) NeonTeal else TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ExpenseAnalytics.formatInr(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CategorySpendBar(bar: CategoryBar) {
    val animatedFraction by animateFloatAsState(
        targetValue = bar.fraction,
        animationSpec = tween(durationMillis = 480),
        label = "bar-${bar.category}"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bar.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ExpenseAnalytics.formatInr(bar.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CategoryColors.forCategory(bar.category)
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.0f%% of total", bar.percentOfTotal),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            val widthFraction = if (bar.amount > 0.0) {
                animatedFraction.coerceIn(0.03f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFraction)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CategoryColors.forCategory(bar.category).copy(alpha = 0.85f),
                                CategoryColors.forCategory(bar.category)
                            )
                        )
                    )
            )
        }
    }
}
