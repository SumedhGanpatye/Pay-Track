package com.sumedh.moneytracker.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.ui.theme.AppBackground
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.Charcoal900
import com.sumedh.moneytracker.ui.theme.Divider
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SecondaryCard

@Composable
fun AppScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppBackground,
                        Color(0xFF0C1218),
                        Charcoal900
                    )
                )
            )
            .statusBarsPadding(),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 18.dp,
    highlighted: Boolean = false,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (highlighted) {
        NeonTeal.copy(alpha = 0.32f)
    } else {
        BorderEmerald
    }
    val container = if (highlighted) {
        Brush.linearGradient(
            listOf(
                NeonTeal.copy(alpha = 0.12f),
                SecondaryCard,
                CardBackground
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                SecondaryCard.copy(alpha = 0.98f),
                CardBackground
            )
        )
    }
    val elevation by animateDpAsState(
        targetValue = when {
            !elevated -> 0.dp
            highlighted -> 10.dp
            else -> 6.dp
        },
        animationSpec = tween(120),
        label = "cardElevation"
    )

    var cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (elevated) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    spotColor = if (highlighted) {
                        NeonTeal.copy(alpha = 0.18f)
                    } else {
                        Color.Black.copy(alpha = 0.32f)
                    },
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(container)
        .border(width = 1.dp, color = borderColor, shape = shape)

    if (onClick != null) {
        cardModifier = cardModifier.pressableScale(pressedScale = 0.985f)
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            color = Color.Transparent,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    } else {
        Column(
            modifier = cardModifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Subtle press scale for premium tactile feedback (≈95%).
 */
fun Modifier.pressableScale(
    pressedScale: Float = 0.95f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "pressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/** Hairline divider matching the design system. */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider)
    )
}
