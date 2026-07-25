package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.domain.profile.UserProfileStore
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics

@Composable
fun HomeHeader(
    username: String,
    todaySpent: Double,
    todayCount: Int,
    modifier: Modifier = Modifier
) {
    val greeting = remember(username) {
        UserProfileStore.personalizedGreeting(username)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track. Pay. Save.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        TodaySummaryCard(
            amount = todaySpent,
            transactionCount = todayCount
        )
    }
}

@Composable
fun TodaySummaryCard(
    amount: Double,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = NeonTeal.copy(alpha = 0.16f),
                ambientColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(SecondaryCard, CardBackground)
                )
            )
            .border(BorderStroke(1.dp, BorderEmerald), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = NeonTeal
            )
            Text(
                text = ExpenseAnalytics.formatInr(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = when (transactionCount) {
                    0 -> "No transactions"
                    1 -> "1 Transaction"
                    else -> "$transactionCount Transactions"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
