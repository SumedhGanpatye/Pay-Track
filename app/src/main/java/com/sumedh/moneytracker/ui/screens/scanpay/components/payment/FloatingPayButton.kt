package com.sumedh.moneytracker.ui.screens.scanpay.components.payment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.ui.components.pressableScale
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TealAccent
import com.sumedh.moneytracker.util.ExpenseAnalytics

/**
 * One-handed floating Pay CTA — bottom-end, above system navigation.
 */
@Composable
fun FloatingPayButton(
    amount: Double?,
    enabled: Boolean,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (amount != null && amount > 0) {
        "Copy & open · ${ExpenseAnalytics.formatInr(amount)}"
    } else {
        "Copy & open UPI"
    }
    val haptics = LocalHapticFeedback.current
    val elevation by animateDpAsState(
        targetValue = if (enabled) 14.dp else 4.dp,
        animationSpec = tween(220),
        label = "payElevation"
    )
    val shape = RoundedCornerShape(28.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(NeonTeal, TealAccent))
    } else {
        Brush.horizontalGradient(
            listOf(NeonTeal.copy(alpha = 0.28f), TealAccent.copy(alpha = 0.2f))
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.94f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp)
                .pressableScale(pressedScale = 0.96f, enabled = enabled)
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    spotColor = NeonTeal.copy(alpha = if (enabled) 0.45f else 0.15f)
                )
                .clip(shape)
                .background(brush)
                .defaultMinSize(minWidth = 128.dp)
                .heightIn(min = 56.dp)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Charcoal900.copy(alpha = 0.22f)),
                    role = Role.Button,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Charcoal900 else Charcoal900.copy(alpha = 0.42f)
            )
        }
    }
}
