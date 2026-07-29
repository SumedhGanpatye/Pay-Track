package com.sumedh.moneytracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sumedh.moneytracker.MainActivity
import com.sumedh.moneytracker.R
import com.sumedh.moneytracker.util.ExpenseAnalytics
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared system notification for successful expense saves.
 */
object ExpenseNotificationHelper {

    private const val CHANNEL_ID = "expense_added_system"
    private val nextId = AtomicInteger(4300)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expense alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "System notifications when an expense is recorded"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts one expense-recorded notification with a single sentence only:
     * "Rs 1 was paid for Food to Rishabh."
     */
    fun showExpenseAdded(
        context: Context,
        amount: Double,
        category: String,
        note: String? = null,
        requestor: String? = null
    ) {
        ensureChannel(context)
        val message = buildExpenseAddedMessage(amount, category, note, requestor)

        val openApp = PendingIntent.getActivity(
            context,
            nextId.get(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message)
            .setContentText(null)
            .setStyle(null)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(nextId.incrementAndGet(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS may be denied on Android 13+
        }
    }

    /**
     * "Rs 1 was paid for testing to Rishabh."
     * Uses note when present, otherwise category. Adds "to {requestor}" when known.
     */
    fun buildExpenseAddedMessage(
        amount: Double,
        category: String,
        note: String?,
        requestor: String? = null
    ): String {
        val amountLabel = "Rs ${ExpenseAnalytics.formatIndianNumber(amount)}"
        val categoryLabel = when (category.trim().lowercase(Locale.US)) {
            "others", "other" -> "Other"
            else -> category.trim().ifBlank { "Other" }
        }
        val forLabel = note?.trim()?.takeIf { it.isNotBlank() } ?: categoryLabel
        val requestorLabel = requestor?.trim()?.takeIf { it.isNotBlank() }

        return if (requestorLabel != null) {
            "$amountLabel was paid for $forLabel to $requestorLabel."
        } else {
            "$amountLabel was paid for $forLabel."
        }
    }
}
