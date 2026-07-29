package com.sumedh.moneytracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.R
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard
import com.sumedh.moneytracker.ui.theme.SoftMint
import com.sumedh.moneytracker.ui.theme.TealAccent
import com.sumedh.moneytracker.ui.theme.TextPrimary

@Composable
fun CopyPayHeroCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner = 18.dp
    val shape = RoundedCornerShape(corner)
    val iconShape = RoundedCornerShape(14.dp)
    val gradient = Brush.linearGradient(
        colors = listOf(
            NeonTeal.copy(alpha = 0.24f),
            SoftMint.copy(alpha = 0.08f),
            TealAccent.copy(alpha = 0.12f),
            SecondaryCard,
            CardBackground
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    val glowGradient = Brush.radialGradient(
        colors = listOf(
            NeonTeal.copy(alpha = 0.18f),
            NeonTeal.copy(alpha = 0.05f),
            CardBackground.copy(alpha = 0f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = NeonTeal.copy(alpha = 0.22f),
                ambientColor = NeonTeal.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(gradient)
            .drawBehind {
                drawRoundRect(
                    brush = glowGradient,
                    cornerRadius = CornerRadius(corner.toPx())
                )
                val stroke = Stroke(
                    width = 1.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 10f), 0f)
                )
                drawRoundRect(
                    color = NeonTeal.copy(alpha = 0.7f),
                    style = stroke,
                    cornerRadius = CornerRadius(corner.toPx()),
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = Offset(stroke.width / 2f, stroke.width / 2f)
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(iconShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NeonTeal.copy(alpha = 0.4f),
                                SoftMint.copy(alpha = 0.16f),
                                TealAccent.copy(alpha = 0.24f)
                            )
                        )
                    )
                    .border(1.5.dp, NeonTeal, iconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rupee),
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Copy & Pay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Copy amount · scan QR · auto-track",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftMint.copy(alpha = 0.88f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NeonTeal.copy(alpha = 0.18f))
                    .border(1.5.dp, NeonTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
