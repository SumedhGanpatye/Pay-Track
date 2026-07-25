package com.sumedh.moneytracker.domain.upi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

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

    fun createPayIntent(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String,
        targetApp: UpiApp
    ): Intent {
        val uri = buildUpiUri(upiId, merchantName, amount, note)
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(targetApp.packageName)
        }
    }

    fun createGenericPayIntent(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String
    ): Intent = Intent(Intent.ACTION_VIEW, buildUpiUri(upiId, merchantName, amount, note))

    private fun buildUpiUri(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String
    ): Uri {
        // UPI `tn`: use Payment Details "Add Note" when the user typed one;
        // only fall back to Pay&Track when that field is empty/whitespace.
        val tn = note.trim().ifBlank { "Pay&Track" }.take(50)
        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", merchantName)
            .appendQueryParameter("am", formatAmount(amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", tn)
            .build()
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}
