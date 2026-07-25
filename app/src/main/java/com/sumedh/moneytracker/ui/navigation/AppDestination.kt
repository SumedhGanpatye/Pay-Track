package com.sumedh.moneytracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : AppDestination("home", "Home", Icons.Outlined.Home)
    data object Analysis : AppDestination("analysis", "Analytics", Icons.AutoMirrored.Outlined.List)
    data object Settings : AppDestination("settings", "Settings", Icons.Outlined.Settings)
}

val bottomNavDestinations: List<AppDestination> = listOf(
    AppDestination.Home,
    AppDestination.Analysis,
    AppDestination.Settings
)

object ScanPayRoutes {
    const val SCANNER = "scan_pay/scanner"
    const val PAYMENT_DETAILS = "scan_pay/payment_details"
    const val PAYMENT_SUCCESS = "scan_pay/payment_success"
}
