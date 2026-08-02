package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.data.ExpenseEntity
import com.sumedh.moneytracker.data.ExpenseType
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.CategoryColors
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

@Composable
fun RecentExpensesSection(
    expenses: List<ExpenseEntity>,
    onViewAll: () -> Unit,
    onExpenseLongPress: (ExpenseEntity) -> Unit,
    daySpendTotals: Map<String, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val groups = remember(expenses, daySpendTotals) {
        ExpenseAnalytics.groupByDay(expenses, dayTotalsByIso = daySpendTotals)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (expenses.isEmpty()) {
                        "Your latest activity will appear here"
                    } else {
                        "Long-press to delete"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (expenses.isNotEmpty()) {
                TextButton(onClick = onViewAll) {
                    Text(
                        text = "View All",
                        color = NeonTeal,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (expenses.isEmpty()) {
            EmptyExpensesCard()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                groups.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayHeader(
                            title = group.header,
                            dayTotal = group.dayTotal
                        )
                        group.expenses.forEachIndexed { index, expense ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(240 + index * 20)) +
                                    slideInVertically(tween(240 + index * 20)) { it / 12 }
                            ) {
                                ExpenseCard(
                                    expense = expense,
                                    onLongPress = { onExpenseLongPress(expense) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseCard(
    expense: ExpenseEntity,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = CategoryColors.forCategory(expense.category)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (pressed) 8.dp else 3.dp,
        animationSpec = tween(160),
        label = "expenseElevation"
    )
    val typeLabel = if (expense.expenseType == ExpenseType.SPLIT) "Split" else "Personal"
    val typeColor = if (expense.expenseType == ExpenseType.SPLIT) NeonTeal else TextSecondary
    val headline = when {
        expense.notes.isNotBlank() -> expense.notes.trim().replaceFirstChar { it.titlecase() }
        expense.title.isNotBlank() &&
            !expense.title.equals(expense.category, ignoreCase = true) -> expense.title
        else -> expense.category
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = accent.copy(alpha = 0.16f),
                ambientColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(SecondaryCard, CardBackground)
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = shape
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.26f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryBadge(label = expense.category, color = accent)
                    TypeBadge(label = typeLabel, color = typeColor)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.date} · ${expense.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.85f)
                )
            }

            Text(
                text = ExpenseAnalytics.formatInr(expense.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun CategoryBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun TypeBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
fun EmptyExpensesCard() {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(SecondaryCard.copy(alpha = 0.95f), CardBackground)
                )
            )
            .border(1.dp, BorderEmerald, shape)
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonTeal.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Wallet,
                contentDescription = null,
                tint = NeonTeal,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No expenses yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use Add Expense above to log your first spend, or enable notification access for auto-detection.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun categoryIcon(category: String): ImageVector = AppIcons.category(category)
