package com.sumedh.moneytracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Money Tracker design system — V2.0
 *
 * Deep charcoal + emerald. Do not invent new brand colors.
 * Category accents are for icons / badges / chips only — never full cards.
 */

// Surfaces
val Charcoal900 = Color(0xFF0B0F13) // Background
val Charcoal800 = Color(0xFF151B22) // Card background
val Charcoal700 = Color(0xFF1A2028) // Secondary card
val Charcoal600 = Color(0xFF242B33) // Elevated / pressed

/** Alias tokens matching the official palette names. */
val AppBackground = Charcoal900
val CardBackground = Charcoal800
val SecondaryCard = Charcoal700

// Emerald accent
val NeonTeal = Color(0xFF00D6A2) // Primary emerald
val TealAccent = Color(0xFF00B488) // Dark emerald
val SoftMint = Color(0xFF5EEBC4) // Soft emerald highlight (icons / hero)
val BorderEmerald = Color(0xFF00D6A2).copy(alpha = 0.18f)

/** @deprecated Prefer SoftMint / NeonTeal — kept for compile compatibility. */
val NeonGreen = SoftMint

// Typography
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9AA4AF)

// Utility
val Divider = Color.White.copy(alpha = 0.05f)
val ErrorRed = Color(0xFFFF5C5C)

/**
 * Soft category accents — icons, badges, chips, tiny indicators only.
 */
object CategoryColors {
    val Food = Color(0xFF4ADE9A)          // soft emerald
    val Travel = Color(0xFF8B5CF6)        // violet
    val Shopping = Color(0xFFF0A35E)      // warm orange
    val Grocery = Color(0xFFF5D76E)       // soft yellow
    val Medical = Color(0xFFE57373)       // soft red
    val Fuel = Color(0xFFE6B84D)          // amber
    val Entertainment = Color(0xFFB794F6) // purple
    val Education = Color(0xFF4DD0E1)     // cyan
    val Bills = Color(0xFFC06B6B)         // soft muted maroon
    val Coffee = Color(0xFFD4A574)        // latte brown
    val Gym = Color(0xFF26C6DA)           // bright cyan (fitness)
    val Gifts = Color(0xFFCE93D8)         // soft orchid
    val Others = Color(0xFF8B959E)        // slate grey

    fun forCategory(category: String): Color = when (category.trim().lowercase()) {
        "food" -> Food
        "travel" -> Travel
        "shopping" -> Shopping
        "grocery", "groceries" -> Grocery
        "medical", "medicines", "medicine", "health", "pharmacy" -> Medical
        "coffee", "cafe", "café" -> Coffee
        "gym", "fitness", "workout" -> Gym
        "bills" -> Bills
        "education", "books" -> Education
        "fuel", "petrol", "diesel" -> Fuel
        "entertainment", "movies" -> Entertainment
        "gifts" -> Gifts
        "others", "other" -> Others
        else -> Others
    }
}
