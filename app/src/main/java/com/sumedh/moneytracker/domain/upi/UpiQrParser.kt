package com.sumedh.moneytracker.domain.upi

/**
 * Lightweight UPI QR validation and parsing for Scan & Pay.
 */
object UpiQrParser {

    private val TRACKED_KEYS = listOf(
        "pa", "pn", "am", "tn", "tr", "tid", "mc", "mode", "orgid", "sign",
        "cu", "url", "mam"
    )

    data class ParsedUpiQr(
        val rawValue: String,
        val payeeAddress: String,
        val payeeName: String?,
        val amount: String?,
        /** When true, amount came from QR and is shown prefilled; still editable unless locked. */
        val amountFromQr: Boolean,
        val amountLocked: Boolean = false,
        /** Full query map for debugging / forward-compat. */
        val allParams: Map<String, String> = emptyMap()
    )

    fun parse(raw: String): ParsedUpiQr? {
        val trimmed = raw.trim()

        UpiDebugLog.banner("QR PARSE")
        UpiDebugLog.section("RAW_QR")
        UpiDebugLog.line(trimmed.ifEmpty { "<empty after trim>" })
        UpiDebugLog.field("raw_length", trimmed.length.toString())
        UpiDebugLog.field("raw_unchanged_vs_input", (trimmed == raw).toString())
        if (trimmed != raw) {
            UpiDebugLog.field("input_length_before_trim", raw.length.toString())
        }

        if (trimmed.isEmpty()) {
            UpiDebugLog.line("parse_succeeded = false (empty)")
            return null
        }

        val lower = trimmed.lowercase()
        val isUpiScheme = lower.startsWith("upi://") || lower.startsWith("upi:")
        val looksLikeUpiPayload = lower.contains("pa=") && lower.contains("pn=")
        UpiDebugLog.field("is_upi_scheme", isUpiScheme.toString())
        UpiDebugLog.field("looks_like_upi_payload", looksLikeUpiPayload.toString())

        if (!isUpiScheme && !looksLikeUpiPayload) {
            UpiDebugLog.line("parse_succeeded = false (not a UPI QR)")
            return null
        }

        val query = extractQuery(trimmed)
        val params = parseQueryParams(query)

        UpiDebugLog.section("PARSED_DATA")
        TRACKED_KEYS.forEach { key ->
            if (params.containsKey(key)) {
                UpiDebugLog.field(key, params[key])
            } else {
                UpiDebugLog.field(key, null)
                UpiDebugLog.line("  (missing key: $key)")
            }
        }
        val extras = params.keys.filter { it !in TRACKED_KEYS }
        if (extras.isNotEmpty()) {
            UpiDebugLog.line("  extra_keys = $extras")
            extras.forEach { UpiDebugLog.field(it, params[it]) }
        }

        val pa = params["pa"]?.takeIf { it.isNotBlank() }
        if (pa == null) {
            UpiDebugLog.line("parse_succeeded = false (pa missing/blank)")
            return null
        }

        val amount = params["am"]?.takeIf { it.isNotBlank() && it.toDoubleOrNull() != null }
        val locked = params["mode"]?.equals("fixed", ignoreCase = true) == true

        val result = ParsedUpiQr(
            rawValue = trimmed,
            payeeAddress = pa,
            payeeName = params["pn"]?.takeIf { it.isNotBlank() },
            amount = amount,
            amountFromQr = amount != null,
            amountLocked = locked && amount != null,
            allParams = params
        )
        UpiDebugLog.line("parse_succeeded = true")
        UpiDebugLog.field("amountFromQr", result.amountFromQr.toString())
        UpiDebugLog.field("amountLocked", result.amountLocked.toString())
        return result
    }

    fun isValidUpiQr(raw: String): Boolean = parse(raw) != null

    private fun extractQuery(raw: String): String {
        val qIndex = raw.indexOf('?')
        return if (qIndex >= 0 && qIndex < raw.lastIndex) raw.substring(qIndex + 1) else raw
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&')
            .mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) return@mapNotNull null
                val key = part.substring(0, eq).lowercase()
                val value = part.substring(eq + 1)
                    .replace('+', ' ')
                    .let {
                        runCatching { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
                            .getOrDefault(it)
                    }
                key to value
            }
            .toMap()
    }
}
