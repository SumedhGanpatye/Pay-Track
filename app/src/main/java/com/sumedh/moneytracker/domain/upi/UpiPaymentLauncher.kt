package com.sumedh.moneytracker.domain.upi

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle

/**
 * Copies the pay amount to the clipboard, then opens the selected UPI app
 * so keyboards / UPI apps can suggest it when the amount field is focused.
 */
object UpiPaymentLauncher {

    fun isInstalled(context: Context, app: UpiApp): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(app.packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun installedApps(context: Context): List<UpiApp> {
        val installed = UpiApp.entries.filter { isInstalled(context, it) }
        return installed.ifEmpty { UpiApp.entries.toList() }
    }

    data class OpenAppResult(
        val intent: Intent,
        val clipboardAmount: String
    )

    /**
     * Copy [amount] to clipboard and open [targetApp]'s launcher.
     */
    fun createOpenAppIntent(
        context: Context,
        targetApp: UpiApp,
        amount: Double
    ): OpenAppResult {
        val amountText = formatClipboardAmount(amount)
        copyAmountToClipboard(context, amountText)

        UpiDebugLog.banner("COPY AMOUNT + OPEN UPI")
        UpiDebugLog.field("app", targetApp.displayName)
        UpiDebugLog.field("package", targetApp.packageName)
        UpiDebugLog.field("clipboard_amount", amountText)

        val launch = buildLaunchIntent(context, targetApp)
        UpiDebugLog.field("intent", launch.toUri(0))
        UpiDebugLog.field(
            "resolved_activity",
            launch.resolveActivity(context.packageManager)?.flattenToString()
        )

        return OpenAppResult(intent = launch, clipboardAmount = amountText)
    }

    /**
     * Puts a plain numeric amount on the system clipboard so Gboard / Samsung Keyboard /
     * UPI apps can show a paste suggestion when the amount field is focused.
     */
    fun copyAmountToClipboard(context: Context, amountText: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("₹$amountText", amountText)
        // Keep clip visible to other apps' paste / keyboard suggestions (not "sensitive").
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras = PersistableBundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false)
                }
            }
        }
        cm.setPrimaryClip(clip)
        PaymentSession.lastClipboardAmount = amountText
        UpiDebugLog.field("clipboard_set", amountText)
    }

    /** Re-copy draft amount (e.g. right as we leave for the UPI app). */
    fun recopyDraftAmount(context: Context): String? {
        val raw = PaymentSession.draft?.amount?.trim().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        val text = formatClipboardAmount(value)
        copyAmountToClipboard(context, text)
        return text
    }

    private fun buildLaunchIntent(context: Context, targetApp: UpiApp): Intent {
        val pm = context.packageManager
        val fromPackage = pm.getLaunchIntentForPackage(targetApp.packageName)
        if (fromPackage != null) {
            return fromPackage.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(targetApp.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    fun formatClipboardAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}
