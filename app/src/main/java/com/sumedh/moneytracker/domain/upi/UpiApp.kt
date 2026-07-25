package com.sumedh.moneytracker.domain.upi

/**
 * Supported UPI apps for Scan & Pay.
 * Declaration order = chooser display order.
 */
enum class UpiApp(
    val displayName: String,
    val packageName: String
) {
    GOOGLE_PAY(
        displayName = "Google Pay",
        packageName = "com.google.android.apps.nbu.paisa.user"
    ),
    BHIM(
        displayName = "BHIM",
        packageName = "in.org.npci.upiapp"
    ),
    PAYTM(
        displayName = "Paytm",
        packageName = "net.one97.paytm"
    ),
    SUPER_MONEY(
        displayName = "SuperMoney",
        packageName = "money.super.payments"
    ),
    AMAZON_PAY(
        displayName = "Amazon Pay",
        packageName = "in.amazon.mShop.android.shopping"
    ),
    PHONEPE(
        displayName = "PhonePe",
        packageName = "com.phonepe.app"
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
