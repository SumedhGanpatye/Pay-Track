package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.CategoryColors
import com.sumedh.moneytracker.ui.theme.Divider
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories

data class CategorySummary(
    val label: String,
    val amount: Double,
    val fraction: Float,
    val percentOfTotal: Float = 0f
)

@Composable
fun CategorySummarySection(
    categories: List<CategorySummary>,
    periodLabel: String,
    billsTotal: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (billsTotal > 0.0) {
            Text(
                text = "Bills",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fixed or recurring",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            BillsAmountCard(amount = billsTotal)
            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Divider)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (categories.isEmpty()) {
            if (billsTotal <= 0.0) {
                Text(
                    text = "No categorized spend $periodLabel yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { summary ->
                    CategorySummaryCard(summary = summary)
                }
            }
        }
    }
}

@Composable
private fun BillsAmountCard(amount: Double) {
    val accent = CategoryColors.forCategory(PaymentPrimaryCategories.BILLS)
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(SecondaryCard, CardBackground))
            )
            .border(1.dp, accent.copy(alpha = 0.28f), shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.category(PaymentPrimaryCategories.BILLS),
                contentDescription = PaymentPrimaryCategories.BILLS,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = PaymentPrimaryCategories.BILLS,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = ExpenseAnalytics.formatInr(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun CategorySummaryCard(summary: CategorySummary) {
    val accent = CategoryColors.forCategory(summary.label)
    val shape = RoundedCornerShape(16.dp)
    val animatedProgress by animateFloatAsState(
        targetValue = summary.fraction.coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "categoryProgress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(SecondaryCard, CardBackground))
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.category(summary.label),
                contentDescription = summary.label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ExpenseAnalytics.formatInr(summary.amount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${summary.percentOfTotal.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.08f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
