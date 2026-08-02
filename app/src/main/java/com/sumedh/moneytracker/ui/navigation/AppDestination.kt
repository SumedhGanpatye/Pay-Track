package com.sumedh.moneytracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", "Home", Icons.Outlined.Home)
    data object History : AppDestination("history", "History", Icons.Outlined.DateRange)
    data object Analysis : AppDestination("analysis", "Analytics", Icons.AutoMirrored.Outlined.List)
    data object Settings : AppDestination("settings", "Settings", Icons.Outlined.Settings)
    data object CopyPay : AppDestination("copy_pay", "Copy & Pay", Icons.Outlined.Home)
}

val bottomNavDestinations: List<AppDestination> = listOf(
    AppDestination.Home,
    AppDestination.Analysis,
    AppDestination.History,
    AppDestination.Settings
)

data class ManualExpensePrefill(
    val amount: Double,
    val title: String,
    val notes: String? = null,
    val personName: String? = null,
    val groupName: String? = null,
    val draftId: String? = null
)
