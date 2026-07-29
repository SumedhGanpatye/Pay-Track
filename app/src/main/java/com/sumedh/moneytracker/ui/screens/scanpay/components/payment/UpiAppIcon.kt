package com.sumedh.moneytracker.ui.screens.scanpay.components.payment

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.sumedh.moneytracker.domain.upi.UpiApp
import com.sumedh.moneytracker.ui.theme.NeonTeal

@Composable
fun UpiAppIcon(
    app: UpiApp,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(app.packageName) {
        runCatching {
            val drawable: Drawable = context.packageManager.getApplicationIcon(app.packageName)
            drawable.toBitmap(128, 128)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.displayName,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(NeonTeal.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBox,
                contentDescription = app.displayName,
                tint = NeonTeal,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
