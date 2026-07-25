package com.sumedh.moneytracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.data.ExpenseRepository
import com.sumedh.moneytracker.domain.upi.PaymentSession
import com.sumedh.moneytracker.ui.screens.AnalysisScreen
import com.sumedh.moneytracker.ui.screens.HomeScreen
import com.sumedh.moneytracker.ui.screens.SettingsScreen
import com.sumedh.moneytracker.ui.screens.account.AccountSignupScreen
import com.sumedh.moneytracker.ui.screens.scanpay.PaymentDetailsScreen
import com.sumedh.moneytracker.ui.screens.scanpay.PaymentSuccessScreen
import com.sumedh.moneytracker.ui.screens.scanpay.QrScannerScreen
import com.sumedh.moneytracker.ui.theme.BorderEmerald
import com.sumedh.moneytracker.ui.theme.CardBackground
import com.sumedh.moneytracker.ui.theme.NeonTeal
import com.sumedh.moneytracker.ui.theme.TextSecondary

private const val TRANSITION_MS = 280

private fun sharedAxisXEnter(
    towards: AnimatedContentTransitionScope.SlideDirection
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = towards,
        animationSpec = tween(TRANSITION_MS)
    ) + fadeIn(animationSpec = tween(TRANSITION_MS))
}

private fun sharedAxisXExit(
    towards: AnimatedContentTransitionScope.SlideDirection
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = towards,
        animationSpec = tween(TRANSITION_MS)
    ) + fadeOut(animationSpec = tween(TRANSITION_MS))
}

@Composable
fun MoneyTrackerNavHost(
    repository: ExpenseRepository,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as MoneyTrackerApp
    val profile by app.userProfileStore.profile.collectAsStateWithLifecycle()

    if (!profile.isSignedUp) {
        AccountSignupScreen(
            modifier = modifier,
            onContinue = { rawName ->
                app.userProfileStore.setUsername(rawName)
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavDestinations.map { it.route }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showBottomBar) Modifier.padding(bottom = 96.dp)
                    else Modifier
                ),
            enterTransition = sharedAxisXEnter(AnimatedContentTransitionScope.SlideDirection.Start),
            exitTransition = sharedAxisXExit(AnimatedContentTransitionScope.SlideDirection.Start),
            popEnterTransition = sharedAxisXEnter(AnimatedContentTransitionScope.SlideDirection.End),
            popExitTransition = sharedAxisXExit(AnimatedContentTransitionScope.SlideDirection.End)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    repository = repository,
                    username = profile.username,
                    onScanAndPay = {
                        navController.navigate(ScanPayRoutes.SCANNER)
                    },
                    onViewAllExpenses = {
                        navController.navigate(AppDestination.Analysis.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppDestination.Analysis.route) {
                AnalysisScreen(repository = repository)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(repository = repository)
            }
            composable(ScanPayRoutes.SCANNER) {
                QrScannerScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToPaymentDetails = {
                        navController.navigate(ScanPayRoutes.PAYMENT_DETAILS) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(ScanPayRoutes.PAYMENT_DETAILS) {
                PaymentDetailsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onPaymentSuccess = {
                        navController.navigate(ScanPayRoutes.PAYMENT_SUCCESS) {
                            popUpTo(ScanPayRoutes.PAYMENT_DETAILS) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoHome = {
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                    }
                )
            }
            composable(ScanPayRoutes.PAYMENT_SUCCESS) {
                PaymentSuccessScreen(
                    onDone = {
                        PaymentSession.clear()
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                    },
                    onViewExpense = {
                        PaymentSession.clear()
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                    }
                )
            }
        }

        if (showBottomBar) {
            FloatingCapsuleBottomBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun FloatingCapsuleBottomBar(
    currentRoute: String?,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = NeonTeal.copy(alpha = 0.14f),
                    ambientColor = Color.Black.copy(alpha = 0.45f)
                )
                .border(
                    width = 1.dp,
                    color = BorderEmerald,
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = CardBackground.copy(alpha = 0.98f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    BottomNavPill(
                        destination = destination,
                        selected = selected,
                        onClick = { onNavigate(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavPill(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) NeonTeal.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(220),
        label = "navPill"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) NeonTeal else TextSecondary,
        animationSpec = tween(220),
        label = "navContent"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = NeonTeal),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}
