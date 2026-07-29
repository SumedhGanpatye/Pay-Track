package com.sumedh.moneytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    onPrimary = Charcoal900,
    primaryContainer = Color(0xFF0A2E28),
    onPrimaryContainer = SoftMint,
    secondary = TealAccent,
    onSecondary = Charcoal900,
    secondaryContainer = Color(0xFF0F2924),
    onSecondaryContainer = SoftMint,
    tertiary = SoftMint,
    onTertiary = Charcoal900,
    background = Charcoal900,
    onBackground = TextPrimary,
    surface = Charcoal800,
    onSurface = TextPrimary,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = TextSecondary,
    outline = BorderEmerald,
    outlineVariant = Divider,
    error = ErrorRed,
    onError = TextPrimary,
    surfaceContainerHighest = Charcoal600,
    surfaceContainerHigh = Charcoal700,
    surfaceContainer = Charcoal800,
    surfaceContainerLow = Charcoal800,
    surfaceContainerLowest = Charcoal900,
    scrim = Color.Black.copy(alpha = 0.72f)
)

private val LightColorScheme = lightColorScheme(
    primary = TealAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F5EB),
    onPrimaryContainer = Color(0xFF0A2E28),
    secondary = NeonTeal,
    onSecondary = Color.White,
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF0B0F13),
    surface = Color.White,
    onSurface = Color(0xFF0B0F13),
    surfaceVariant = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFF5A6570),
    outline = TealAccent.copy(alpha = 0.35f),
    outlineVariant = Color(0xFFD0D7DE),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
