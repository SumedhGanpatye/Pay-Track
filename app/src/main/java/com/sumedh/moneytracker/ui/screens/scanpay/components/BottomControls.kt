package com.sumedh.moneytracker.ui.screens.scanpay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumedh.moneytracker.ui.components.pressableScale
import com.sumedh.moneytracker.ui.icons.AppIcons
import com.sumedh.moneytracker.ui.theme.Charcoal700
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextPrimary
import com.sumedh.moneytracker.ui.theme.TextSecondary

@Composable
fun BottomControls(
    torchEnabled: Boolean,
    onFlashClick: () -> Unit,
    onGalleryClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = NeonTeal.copy(alpha = 0.18f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Charcoal700.copy(alpha = 0.94f),
                        Color(0xFF12181F).copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlashButton(
            enabled = enabled,
            torchOn = torchEnabled,
            onClick = onFlashClick
        )
        GalleryButton(
            enabled = enabled,
            onClick = onGalleryClick
        )
    }
}

@Composable
fun FlashButton(
    torchOn: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    ScannerControlButton(
        icon = AppIcons.flash(torchOn),
        label = "Flash",
        active = torchOn,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun GalleryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    ScannerControlButton(
        icon = AppIcons.gallery(),
        label = "Gallery",
        active = false,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ScannerControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.pressableScale(enabled = enabled),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bg = if (active) NeonTeal.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
        val tint = if (active) NeonTeal else TextPrimary
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = if (active) NeonTeal.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = 1.dp,
                    color = if (active) NeonTeal.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = NeonTeal),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else TextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextSecondary else TextSecondary.copy(alpha = 0.5f)
        )
    }
}
