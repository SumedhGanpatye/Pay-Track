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
import com.sumedh.moneytracker.domain.notification.ParsedExpenseData
import com.sumedh.moneytracker.domain.expense.PaymentPrimaryCategories
import com.sumedh.moneytracker.util.ExpenseAnalytics
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shows "Expense Detected" notification with quick category actions.
 */
object ExpenseDetectedNotificationHelper {

    const val CHANNEL_ID = "expense_detected"
    private val nextId = AtomicInteger(5100)

    const val ACTION_SAVE_CATEGORY = "com.sumedh.moneytracker.ACTION_SAVE_CATEGORY"
    const val ACTION_OPEN_MORE = "com.sumedh.moneytracker.ACTION_OPEN_MORE"
    const val EXTRA_DRAFT_ID = "draft_id"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expense detection",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Quick actions when Google Pay split expenses are detected"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, parsed: ParsedExpenseData, draftId: String) {
        ensureChannel(context)
        val notificationId = nextId.incrementAndGet()

        val subtitle = buildString {
            append("Rs ${ExpenseAnalytics.formatIndianNumber(parsed.amount)}")
            parsed.note?.takeIf { it.isNotBlank() }?.let {
                append(" for $it")
            }
            parsed.personName?.takeIf { it.isNotBlank() }?.let {
                append("\nRequested by $it")
            }
        }

        val contentLine = buildString {
            append("Rs ${ExpenseAnalytics.formatIndianNumber(parsed.amount)}")
            parsed.note?.takeIf { it.isNotBlank() }?.let { append(" for $it") }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Expense Detected")
            .setContentText(contentLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(
                openAppPendingIntent(context, draftId, parsed, notificationId)
            )
            .addAction(
                R.drawable.ic_category_food,
                "🍔 Food",
                categoryPendingIntent(context, draftId, PaymentPrimaryCategories.FOOD, notificationId)
            )
            .addAction(
                R.drawable.ic_category_coffee,
                "🚕 Travel",
                categoryPendingIntent(context, draftId, PaymentPrimaryCategories.TRAVEL, notificationId)
            )
            .addAction(
                R.drawable.ic_category_grocery,
                "🥬 Grocery",
                categoryPendingIntent(context, draftId, PaymentPrimaryCategories.GROCERY, notificationId)
            )
            .addAction(
                R.drawable.ic_category_shopping,
                "🛒 Shopping",
                categoryPendingIntent(context, draftId, PaymentPrimaryCategories.SHOPPING, notificationId)
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS may be denied
        }
    }

    fun dismiss(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun categoryPendingIntent(
        context: Context,
        draftId: String,
        category: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SAVE_CATEGORY
            putExtra(EXTRA_DRAFT_ID, draftId)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            (draftId + category).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun morePendingIntent(
        context: Context,
        draftId: String,
        parsed: ParsedExpenseData,
        notificationId: Int
    ): PendingIntent = openAppPendingIntent(context, draftId, parsed, notificationId)

    private fun openAppPendingIntent(
        context: Context,
        draftId: String,
        parsed: ParsedExpenseData,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_OPEN_MORE
            putExtra(EXTRA_DRAFT_ID, draftId)
            putExtra(MainActivity.EXTRA_PREFILL_AMOUNT, parsed.amount)
            putExtra(MainActivity.EXTRA_PREFILL_TITLE, parsed.title)
            putExtra(MainActivity.EXTRA_PREFILL_NOTES, buildPrefillNotes(parsed))
            putExtra(MainActivity.EXTRA_PREFILL_PERSON, parsed.personName)
            putExtra(MainActivity.EXTRA_PREFILL_GROUP, parsed.groupName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getActivity(
            context,
            (draftId + "open").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildPrefillNotes(parsed: ParsedExpenseData): String {
        // Only the payment note (e.g. maggi) — never the raw GPay notification text
        return parsed.note?.trim().orEmpty()
    }
}
