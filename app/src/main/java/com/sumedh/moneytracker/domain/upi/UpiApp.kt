package com.sumedh.moneytracker.domain.upi

/**
 * Supported UPI apps for Scan & Pay.
 */
enum class UpiApp(
    val displayName: String,
    val packageName: String
) {
    GOOGLE_PAY(
        displayName = "Google Pay",
        packageName = "com.google.android.apps.nbu.paisa.user"
    ),
    PHONEPE(
        displayName = "PhonePe",
        packageName = "com.phonepe.app"
    ),
    PAYTM(
        displayName = "Paytm",
        packageName = "net.one97.paytm"
    ),
    BHIM(
        displayName = "BHIM",
        packageName = "in.org.npci.upiapp"
    ),
    AMAZON_PAY(
        displayName = "Amazon Pay",
        packageName = "in.amazon.mShop.android.shopping"
    );

    companion object {
        fun fromPackage(packageName: String?): UpiApp? =
            entries.firstOrNull { it.packageName == packageName }

        fun fromNameOrPackage(value: String?): UpiApp =
            entries.firstOrNull {
                it.packageName == value || it.name.equals(value, ignoreCase = true)
            } ?: GOOGLE_PAY
    }
}
