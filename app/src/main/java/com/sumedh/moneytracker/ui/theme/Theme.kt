package com.sumedh.moneytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark-first Material 3 scheme locked to the Money Tracker emerald/charcoal system.
 * Light theme is intentionally unused — the app identity is dark.
 */
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

@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Brand is dark-first; ignore lightTheme to preserve identity.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
