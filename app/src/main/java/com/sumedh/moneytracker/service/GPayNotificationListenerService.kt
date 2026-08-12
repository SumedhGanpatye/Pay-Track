package com.sumedh.moneytracker.service

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sumedh.moneytracker.MoneyTrackerApp
import com.sumedh.moneytracker.domain.notification.GPayNotificationConstants
import com.sumedh.moneytracker.domain.notification.RawNotificationData

class GPayNotificationListenerService : NotificationListenerService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Catch splits that arrived while the listener was unbound (common after
        // app updates / OEM process kills). Dedup in NotificationRepository.
        try {
            activeNotifications
                ?.filter { it.packageName == GPayNotificationConstants.PACKAGE }
                ?.forEach { handlePosted(it) }
        } catch (_: SecurityException) {
            // Listener can briefly be unbound during reconnect races.
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Ask the system to bind us again — Samsung often drops NLS bindings.
        mainHandler.postDelayed({
            requestRebind(
                ComponentName(this, GPayNotificationListenerService::class.java)
            )
        }, 1_500L)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handlePosted(sbn)
    }

    private fun handlePosted(sbn: StatusBarNotification) {
        if (sbn.packageName != GPayNotificationConstants.PACKAGE) return

        val extras = sbn.notification.extras ?: return
        val title = extras.charSeq(Notification.EXTRA_TITLE)
        val text = extras.charSeq(Notification.EXTRA_TEXT)
        val bigText = extras.charSeq(Notification.EXTRA_BIG_TEXT).takeIf { it.isNotBlank() }
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim()?.takeIf { line -> line.isNotBlank() } }
            .orEmpty()
        val summary = extras.charSeq(Notification.EXTRA_SUMMARY_TEXT)
        val subText = extras.charSeq(Notification.EXTRA_SUB_TEXT)

        // Merge all available text so "Pay … for 'maggi'" is not missed
        val bodyParts = buildList {
            add(text)
            if (!bigText.isNullOrBlank()) add(bigText)
            addAll(lines)
            if (summary.isNotBlank()) add(summary)
            if (subText.isNotBlank()) add(subText)
        }.filter { it.isNotBlank() }.distinct()

        val raw = RawNotificationData(
            packageName = sbn.packageName,
            title = title,
            text = bodyParts.joinToString("\n"),
            bigText = bigText,
            timestamp = sbn.postTime,
            notificationKey = sbn.key ?: "${sbn.id}:${sbn.postTime}"
        )

        val app = applicationContext as? MoneyTrackerApp ?: return
        app.notificationRepository.onNotificationReceived(raw)
    }

    private fun Bundle.charSeq(key: String): String =
        getCharSequence(key)?.toString().orEmpty()
}
