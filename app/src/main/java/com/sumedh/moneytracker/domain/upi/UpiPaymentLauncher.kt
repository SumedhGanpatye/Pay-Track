package com.sumedh.moneytracker.domain.upi

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap

enum class UpiApp(
    val displayName: String,
    val packageName: String,
    val scanUris: List<String>
) {
    GPAY(
        displayName = "GPay",
        packageName = "com.google.android.apps.nbu.paisa.user",
        scanUris = listOf("tez://upi/scan", "gpay://upi/scan", "upi://scan")
    ),
    BHIM(
        displayName = "BHIM",
        packageName = "in.org.npci.upiapp",
        scanUris = listOf("bhim://upi/scan", "upi://scan")
    ),
    PHONEPE(
        displayName = "PhonePe",
        packageName = "com.phonepe.app",
        scanUris = listOf("phonepe://scan", "phonepe://pay", "upi://scan")
    ),
    POP(
        displayName = "Pop UPI",
        packageName = "com.popclub.android",
        scanUris = listOf("popclubapp://upi/pay", "upi://scan")
    ),
    PAYTM(
        displayName = "Paytm",
        packageName = "net.one97.paytm",
        scanUris = listOf("paytmmp://cash_wallet", "paytm://upi/scan", "upi://scan")
    );

    companion object {
        val defaults: List<UpiApp> = entries

        fun fromPackage(packageName: String?): UpiApp =
            entries.firstOrNull { it.packageName == packageName } ?: GPAY
    }
}

object UpiIconLoader {

    fun isInstalled(context: Context, app: UpiApp): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(app.packageName, 0)
            true
        }.getOrDefault(false)

    /** Official app icon from the device when the UPI app is installed. */
    fun loadDrawable(context: Context, app: UpiApp): Drawable? =
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
        }.getOrNull()

    fun loadBitmap(context: Context, app: UpiApp, sizePx: Int = 128): android.graphics.Bitmap? {
        val drawable = loadDrawable(context, app) ?: return null
        return runCatching { drawable.toBitmap(sizePx, sizePx) }.getOrNull()
    }

    /**
     * Apps that can handle a UPI pay intent on this device (for discovery).
     */
    fun resolveUpiHandlers(context: Context): List<String> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .distinct()
    }
}

object UpiPaymentLauncher {

    fun isInstalled(context: Context, app: UpiApp): Boolean =
        UpiIconLoader.isInstalled(context, app)

    fun copyAmountToClipboard(context: Context, amount: Double): String {
        val text = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("amount", text))
        return text
    }

    fun launchScanOrApp(context: Context, app: UpiApp): Boolean {
        for (uri in app.scanUris) {
            if (tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                    setPackage(app.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })) {
                return true
            }
        }

        if (tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("upi://scan")).apply {
                setPackage(app.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })) {
            return true
        }

        if (tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay")).apply {
                setPackage(app.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })) {
            return true
        }

        val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return tryStart(context, launch)
        }

        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${app.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryStart(context, market)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
