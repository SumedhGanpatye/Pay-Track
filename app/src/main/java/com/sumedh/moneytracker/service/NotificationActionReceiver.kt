package com.sumedh.moneytracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sumedh.moneytracker.MoneyTrackerApp

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ExpenseDetectedNotificationHelper.ACTION_SAVE_CATEGORY) return

        val draftId = intent.getStringExtra(ExpenseDetectedNotificationHelper.EXTRA_DRAFT_ID) ?: return
        val category = intent.getStringExtra(ExpenseDetectedNotificationHelper.EXTRA_CATEGORY) ?: return
        val notificationId = intent.getIntExtra(
            ExpenseDetectedNotificationHelper.EXTRA_NOTIFICATION_ID,
            -1
        )

        val app = context.applicationContext as MoneyTrackerApp
        app.expenseAutoSaver.saveFromDraft(
            draftId = draftId,
            category = category,
            notificationId = notificationId
        )
    }
}
