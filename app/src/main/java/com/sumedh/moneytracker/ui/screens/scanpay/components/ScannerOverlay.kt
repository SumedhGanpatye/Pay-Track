package com.sumedh.moneytracker.ui.screens.scanpay.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.SoftMint

@Composable
fun ScannerOverlay(
    animateLaser: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val frameSize = minOf(maxWidth * 0.72f, 300.dp)
        val density = LocalDensity.current
        val framePx = with(density) { frameSize.toPx() }
        val cornerRadiusPx = with(density) { 28.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val left = (size.width - framePx) / 2f
            val top = (size.height - framePx) / 2f
            val hole = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(left, top),
                            size = Size(framePx, framePx)
                        ),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }

            drawRect(color = Color.Black.copy(alpha = 0.55f))
            drawPath(path = hole, color = Color.Transparent, blendMode = BlendMode.Clear)

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonTeal.copy(alpha = 0.35f),
                        SoftMint.copy(alpha = 0.12f),
                        NeonTeal.copy(alpha = 0.28f)
                    )
                ),
                topLeft = Offset(left - 2f, top - 2f),
                size = Size(framePx + 4f, framePx + 4f),
                cornerRadius = CornerRadius(cornerRadiusPx + 2f, cornerRadiusPx + 2f),
                style = Stroke(width = 6.dp.toPx())
            )

            drawRoundRect(
                color = NeonTeal.copy(alpha = 0.92f),
                topLeft = Offset(left, top),
                size = Size(framePx, framePx),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (animateLaser) {
            Box(
                modifier = Modifier
                    .size(frameSize)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                ScannerAnimation(frameHeight = frameSize)
            }
        }
    }
}

@Composable
fun ScannerAnimation(
    frameHeight: Dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scannerLaser")
    val progress by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .align(Alignment.TopCenter)
                .offset(y = frameHeight * progress - 14.dp)
        ) {
            val lineY = size.height * 0.7f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonTeal.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(size.width * 0.08f, 0f),
                size = Size(size.width * 0.84f, size.height)
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonTeal.copy(alpha = 0.2f),
                        NeonTeal.copy(alpha = 0.95f),
                        SoftMint.copy(alpha = 0.9f),
                        NeonTeal.copy(alpha = 0.95f),
                        NeonTeal.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.06f, lineY),
                end = Offset(size.width * 0.94f, lineY),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}
