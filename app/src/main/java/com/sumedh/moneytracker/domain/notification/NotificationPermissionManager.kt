package com.sumedh.moneytracker.domain.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.sumedh.moneytracker.service.GPayNotificationListenerService

object NotificationPermissionManager {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val component = ComponentName(
            context.packageName,
            GPayNotificationListenerService::class.java.name
        )
        return TextUtils.SimpleStringSplitter(':').let { splitter ->
            splitter.setString(enabledListeners)
            while (splitter.hasNext()) {
                val name = splitter.next()
                if (name.equals(component.flattenToString(), ignoreCase = true)) {
                    return true
                }
            }
            false
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}
