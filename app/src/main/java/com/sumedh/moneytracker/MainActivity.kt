package com.sumedh.moneytracker

import com.sumedh.moneytracker.MoneyTrackerApp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sumedh.moneytracker.service.ExpenseDetectedNotificationHelper
import com.sumedh.moneytracker.service.ExpenseNotificationHelper
import com.sumedh.moneytracker.ui.navigation.MoneyTrackerNavHost
import com.sumedh.moneytracker.ui.navigation.ManualExpensePrefill
import com.sumedh.moneytracker.ui.theme.MoneyTrackerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    var pendingPrefill by mutableStateOf<ManualExpensePrefill?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        ExpenseNotificationHelper.ensureChannel(this)
        ExpenseDetectedNotificationHelper.ensureChannel(this)
        maybeRequestNotificationPermission()
        handlePrefillIntent(intent)

        val app = application as MoneyTrackerApp
        val repository = app.expenseRepository

        setContent {
            MoneyTrackerTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyTrackerNavHost(
                        repository = repository,
                        manualExpensePrefill = pendingPrefill,
                        onPrefillConsumed = { pendingPrefill = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePrefillIntent(intent)
    }

    private fun handlePrefillIntent(intent: Intent?) {
        if (intent?.action != ExpenseDetectedNotificationHelper.ACTION_OPEN_MORE) return
        val amount = intent.getDoubleExtra(EXTRA_PREFILL_AMOUNT, 0.0)
        if (amount <= 0.0) return

        pendingPrefill = ManualExpensePrefill(
            amount = amount,
            title = intent.getStringExtra(EXTRA_PREFILL_TITLE).orEmpty(),
            notes = intent.getStringExtra(EXTRA_PREFILL_NOTES),
            personName = intent.getStringExtra(EXTRA_PREFILL_PERSON),
            groupName = intent.getStringExtra(EXTRA_PREFILL_GROUP),
            draftId = intent.getStringExtra(ExpenseDetectedNotificationHelper.EXTRA_DRAFT_ID)
        )

        val notificationId = intent.getIntExtra(
            ExpenseDetectedNotificationHelper.EXTRA_NOTIFICATION_ID,
            -1
        )
        if (notificationId != -1) {
            ExpenseDetectedNotificationHelper.dismiss(this, notificationId)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_PREFILL_AMOUNT = "prefill_amount"
        const val EXTRA_PREFILL_TITLE = "prefill_title"
        const val EXTRA_PREFILL_NOTES = "prefill_notes"
        const val EXTRA_PREFILL_PERSON = "prefill_person"
        const val EXTRA_PREFILL_GROUP = "prefill_group"
    }
}
