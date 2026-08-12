package com.sumedh.moneytracker.domain.notification

import android.content.Context
import com.sumedh.moneytracker.data.NotificationDebugRepository
import com.sumedh.moneytracker.data.ParsedNotificationEntity
import com.sumedh.moneytracker.service.ExpenseDetectedNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Coordinates notification ingestion, deduplication, parsing, and debug logging.
 */
class NotificationRepository(
    private val context: Context,
    private val debugRepository: NotificationDebugRepository,
    private val expenseAutoSaver: ExpenseAutoSaver
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seenKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val handledPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun onNotificationReceived(raw: RawNotificationData) {
        val dedupeKey = "${raw.notificationKey}:${raw.timestamp}"
        if (!seenKeys.add(dedupeKey)) return
        // Survives process death / listener rebind so active-shade rescans don't re-notify.
        if (handledPrefs.contains(dedupeKey)) return

        scope.launch {
            val parsed = NotificationParser.parse(raw)
            debugRepository.log(
                ParsedNotificationEntity(
                    originalText = raw.combinedText,
                    notificationTitle = raw.title,
                    extractedAmount = parsed?.amount,
                    extractedPerson = parsed?.personName,
                    extractedGroup = parsed?.groupName,
                    parserUsed = parsed?.parserUsed ?: "none",
                    packageName = raw.packageName,
                    notificationKey = raw.notificationKey,
                    timestamp = raw.timestamp
                )
            )

            parsed?.let { data ->
                markHandled(dedupeKey)
                val draftId = expenseAutoSaver.storeDraft(data)
                ExpenseDetectedNotificationHelper.show(
                    context = appContext,
                    parsed = data,
                    draftId = draftId
                )
            }
        }
    }

    fun testParse(text: String): ParsedExpenseData? = NotificationParser.parseText(text)

    private fun markHandled(dedupeKey: String) {
        handledPrefs.edit().putLong(dedupeKey, System.currentTimeMillis()).apply()
        pruneHandledPrefs()
    }

    private fun pruneHandledPrefs() {
        val cutoff = System.currentTimeMillis() - HANDLED_TTL_MS
        val stale = handledPrefs.all.entries
            .mapNotNull { (key, value) ->
                val ts = value as? Long ?: return@mapNotNull key
                key.takeIf { ts < cutoff }
            }
        if (stale.isEmpty()) return
        handledPrefs.edit().apply {
            stale.forEach { remove(it) }
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "gpay_notif_handled"
        private const val HANDLED_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
