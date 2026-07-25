package com.sumedh.moneytracker.ui.screens.scanpay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.domain.upi.PaymentSession
import com.sumedh.moneytracker.ui.components.AppScreenBackground
import com.sumedh.moneytracker.ui.components.GlassCard
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary
import com.sumedh.moneytracker.util.ExpenseAnalytics
import kotlinx.coroutines.delay

@Composable
fun PaymentSuccessScreen(
    onDone: () -> Unit,
    onViewExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft = remember { PaymentSession.draft }
    val merchant = draft?.merchantName ?: "Merchant"
    val amount = draft?.amount?.toDoubleOrNull() ?: 0.0
    val category = draft?.category?.ifBlank { "Others" } ?: "Others"

    LaunchedEffect(Unit) {
        delay(2_000)
        PaymentSession.clear()
        onDone()
    }

    AppScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SuccessCheckmark()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Payment Successful",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(cornerRadius = 22.dp, contentPadding = 18.dp, highlighted = true) {
                SuccessRow("Merchant", merchant)
                Spacer(modifier = Modifier.height(12.dp))
                SuccessRow("Amount", ExpenseAnalytics.formatInr(amount))
                Spacer(modifier = Modifier.height(12.dp))
                SuccessRow("Category", category)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Expense added successfully.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    PaymentSession.clear()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTeal,
                    contentColor = Charcoal900
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    PaymentSession.clear()
                    onViewExpense()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("View Expense", color = NeonTeal, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = {
                PaymentSession.clear()
                onDone()
            }) {
                Text("Skip", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SuccessRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SuccessCheckmark() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(NeonTeal.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(
                color = NeonTeal.copy(alpha = 0.9f),
                style = stroke,
                radius = size.minDimension / 2f
            )
            val p = progress.value
            if (p > 0f) {
                val start = Offset(size.width * 0.22f, size.height * 0.52f)
                val mid = Offset(size.width * 0.42f, size.height * 0.70f)
                val end = Offset(size.width * 0.78f, size.height * 0.32f)
                val firstLeg = (p * 2f).coerceAtMost(1f)
                val secondLeg = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                drawLine(
                    color = NeonTeal,
                    start = start,
                    end = Offset(
                        start.x + (mid.x - start.x) * firstLeg,
                        start.y + (mid.y - start.y) * firstLeg
                    ),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                if (secondLeg > 0f) {
                    drawLine(
                        color = NeonTeal,
                        start = mid,
                        end = Offset(
                            mid.x + (end.x - mid.x) * secondLeg,
                            mid.y + (end.y - mid.y) * secondLeg
                        ),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
