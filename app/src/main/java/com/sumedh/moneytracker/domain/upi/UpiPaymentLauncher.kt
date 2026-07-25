package com.sumedh.moneytracker.domain.upi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.util.UUID

/**
 * Builds and launches UPI payment intents.
 *
 * Launch strategy (NPCI Linking Spec aligned):
 * 1. Prefer the original scanned URI and preserve merchant params (pa, pn, mc, tr, tid, …).
 * 2. Strip GPay camera-only `aid` (not part of the NPCI linking parameter set for Intent).
 * 3. Inject/replace only payer-controlled fields: am, cu, tn.
 * 4. Inject blank mc / synthetic tr only when the QR omitted them.
 * 5. Re-encode all values for Intent (spaces as %20); keep @ in VPA and / in names.
 */
object UpiPaymentLauncher {

    private val OVERRIDE_KEYS = setOf("am", "cu", "tn")
    private val STRIP_KEYS = setOf("aid")

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

    fun isLikelyPersonalGpayQr(originalRawQr: String?, upiId: String): Boolean {
        val raw = originalRawQr.orEmpty()
        val params = enumerateRawParams(raw).associate { it.key.lowercase() to it.rawValue }
        val hasGpayAid = params["aid"]?.startsWith("uGICAg") == true
        val mc = params["mc"]?.let { decodeParam(it) }.orEmpty()
        val looksMerchantVpa = upiId.contains("okbizaxis", ignoreCase = true) ||
            upiId.contains("@okbiz", ignoreCase = true)
        return hasGpayAid && mc.isBlank() && !looksMerchantVpa
    }

    fun createPayIntent(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String,
        targetApp: UpiApp,
        originalRawQr: String? = null
    ): Intent {
        val uri = resolveLaunchUri(upiId, merchantName, amount, note, originalRawQr)
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(targetApp.packageName)
        }
    }

    fun createGenericPayIntent(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String,
        originalRawQr: String? = null
    ): Intent {
        val uri = resolveLaunchUri(upiId, merchantName, amount, note, originalRawQr)
        return Intent(Intent.ACTION_VIEW, uri)
    }

    @Suppress("UNUSED_PARAMETER")
    fun logIntentResolution(context: Context, intent: Intent, targetPackage: String?) {
        // Kept for call-site compatibility.
    }

    private fun resolveLaunchUri(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String,
        originalRawQr: String?
    ): Uri {
        val tn = note.trim().ifBlank { "Pay&Track" }.take(50)
        val am = formatAmount(amount)
        val cu = "INR"
        val original = originalRawQr?.trim().orEmpty()
        val originalParams = enumerateRawParams(original)
        val originalMc = originalParams
            .firstOrNull { it.key.equals("mc", ignoreCase = true) }
            ?.let { decodeParam(it.rawValue) }
        val originalTr = originalParams
            .firstOrNull { it.key.equals("tr", ignoreCase = true) }
            ?.let { decodeParam(it.rawValue) }
            ?.takeIf { it.isNotBlank() }

        val trToInject = originalTr ?: newTransactionRef()

        val launchString = if (isUsableOriginalUpi(original)) {
            patchOriginalUpiUri(
                original = original,
                am = am,
                cu = cu,
                tn = tn,
                injectMcIfAbsent = originalMc == null,
                mcIfAbsent = "",
                injectTrIfAbsent = originalTr == null,
                trIfAbsent = trToInject
            )
        } else {
            buildFallbackUpiString(
                upiId = upiId,
                merchantName = merchantName,
                am = am,
                cu = cu,
                tn = tn,
                mc = "",
                tr = trToInject
            )
        }

        val parsed = Uri.parse(launchString)
        UpiDebugLog.banner("URI LAUNCH STRATEGY")
        UpiDebugLog.section("DIAGNOSIS")
        UpiDebugLog.field("ORIGINAL_SCANNED", original.ifEmpty { "<none>" })
        UpiDebugLog.field("tr_policy", if (originalTr != null) "PRESERVE merchant tr" else "INJECT synthetic tr")
        UpiDebugLog.field("LAUNCH_URI", launchString)
        UpiDebugLog.field("has_raw_spaces", launchString.contains(' ').toString())
        UpiDebugLog.field("parsed_pa", parsed.getQueryParameter("pa"))
        UpiDebugLog.field("parsed_pn", parsed.getQueryParameter("pn"))
        UpiDebugLog.field("parsed_tr", parsed.getQueryParameter("tr"))
        UpiDebugLog.field("parsed_mc", parsed.getQueryParameter("mc"))
        UpiDebugLog.field("parsed_am", parsed.getQueryParameter("am"))

        return parsed
    }

    /**
     * Build an Intent-safe UPI URI from the scanned QR:
     * - keep every original key (except stripped keys like aid)
     * - never overwrite merchant tr/mc when present
     * - always set/replace am, cu, tn
     * - re-encode ALL values for Intent (NPCI: spaces must be %20)
     */
    fun patchOriginalUpiUri(
        original: String,
        am: String,
        cu: String,
        tn: String,
        injectMcIfAbsent: Boolean,
        mcIfAbsent: String,
        injectTrIfAbsent: Boolean,
        trIfAbsent: String
    ): String {
        val trimmed = original.trim()
        val qIndex = trimmed.indexOf('?')
        val base = if (qIndex >= 0) trimmed.substring(0, qIndex) else trimmed
        val query = if (qIndex >= 0 && qIndex < trimmed.lastIndex) {
            trimmed.substring(qIndex + 1)
        } else {
            ""
        }

        val overridesDecoded = linkedMapOf(
            "am" to am,
            "cu" to cu,
            "tn" to tn
        )
        val seen = mutableSetOf<String>()
        val parts = mutableListOf<String>()

        if (query.isNotBlank()) {
            query.split('&').filter { it.isNotEmpty() }.forEach { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) return@forEach
                val key = part.substring(0, eq)
                val keyLower = key.lowercase()
                if (keyLower in STRIP_KEYS) return@forEach

                val decoded = when {
                    keyLower in OVERRIDE_KEYS -> overridesDecoded.getValue(keyLower)
                    else -> decodeParam(part.substring(eq + 1))
                }
                parts.add("$key=${encodeUpiQueryValue(decoded)}")
                seen.add(keyLower)
            }
        }

        overridesDecoded.forEach { (key, value) ->
            if (key !in seen) {
                parts.add("$key=${encodeUpiQueryValue(value)}")
                seen.add(key)
            }
        }
        if (injectMcIfAbsent && "mc" !in seen) {
            parts.add("mc=${encodeUpiQueryValue(mcIfAbsent)}")
        }
        if (injectTrIfAbsent && "tr" !in seen) {
            parts.add("tr=${encodeUpiQueryValue(trIfAbsent)}")
        }

        return if (parts.isEmpty()) base else "$base?${parts.joinToString("&")}"
    }

    private fun buildFallbackUpiString(
        upiId: String,
        merchantName: String,
        am: String,
        cu: String,
        tn: String,
        mc: String,
        tr: String
    ): String {
        val q = listOf(
            "pa=${encodeUpiQueryValue(upiId)}",
            "pn=${encodeUpiQueryValue(merchantName)}",
            "mc=${encodeUpiQueryValue(mc)}",
            "tr=${encodeUpiQueryValue(tr)}",
            "tn=${encodeUpiQueryValue(tn)}",
            "am=${encodeUpiQueryValue(am)}",
            "cu=${encodeUpiQueryValue(cu)}"
        ).joinToString("&")
        return "upi://pay?$q"
    }

    private fun isUsableOriginalUpi(original: String): Boolean {
        if (original.isBlank()) return false
        val lower = original.lowercase()
        return lower.startsWith("upi://") || lower.startsWith("upi:") ||
            (lower.contains("pa=") && lower.contains("pn="))
    }

    private data class RawParam(val key: String, val rawValue: String)

    private fun enumerateRawParams(uriString: String): List<RawParam> {
        if (uriString.isBlank()) return emptyList()
        val qIndex = uriString.indexOf('?')
        val query = if (qIndex >= 0 && qIndex < uriString.lastIndex) {
            uriString.substring(qIndex + 1)
        } else if (uriString.contains('=') && !uriString.contains("://")) {
            uriString
        } else {
            return emptyList()
        }
        if (query.isBlank()) return emptyList()
        return query.split('&')
            .mapNotNull { part ->
                if (part.isEmpty()) return@mapNotNull null
                val eq = part.indexOf('=')
                if (eq <= 0) return@mapNotNull null
                RawParam(
                    key = part.substring(0, eq),
                    rawValue = part.substring(eq + 1)
                )
            }
    }

    private fun decodeParam(raw: String): String {
        return runCatching {
            java.net.URLDecoder.decode(raw.replace('+', ' '), Charsets.UTF_8.name())
        }.getOrDefault(raw)
    }

    /**
     * NPCI Linking Spec: spaces must be %20 for generated Intent/QR URIs.
     * Keep @ (VPA) and / (names like M/S.) unencoded — common merchant QR practice.
     */
    private fun encodeUpiQueryValue(value: String): String = Uri.encode(value, "@/:._-")

    private fun formatAmount(amount: Double): String = String.format("%.2f", amount)

    private fun newTransactionRef(): String =
        "PT" + UUID.randomUUID().toString().replace("-", "").take(18)
}
