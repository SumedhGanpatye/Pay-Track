package com.sumedh.moneytracker.domain.upi

/**
 * Lightweight UPI QR validation and parsing for Scan & Pay.
 */
object UpiQrParser {

    data class ParsedUpiQr(
        val rawValue: String,
        val payeeAddress: String,
        val payeeName: String?,
        val amount: String?,
        /** When true, amount came from QR and is shown prefilled; still editable unless locked. */
        val amountFromQr: Boolean,
        val amountLocked: Boolean = false
    )

    fun parse(raw: String): ParsedUpiQr? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase()
        val isUpiScheme = lower.startsWith("upi://") || lower.startsWith("upi:")
        val looksLikeUpiPayload = lower.contains("pa=") && lower.contains("pn=")
        if (!isUpiScheme && !looksLikeUpiPayload) return null

        val query = extractQuery(trimmed)
        val params = parseQueryParams(query)
        val pa = params["pa"]?.takeIf { it.isNotBlank() } ?: return null
        val amount = params["am"]?.takeIf { it.isNotBlank() && it.toDoubleOrNull() != null }
        // Some QRs use mam (minimum amount) — treat am as preferred fill.
        val locked = params["mode"]?.equals("fixed", ignoreCase = true) == true

        return ParsedUpiQr(
            rawValue = trimmed,
            payeeAddress = pa,
            payeeName = params["pn"]?.takeIf { it.isNotBlank() },
            amount = amount,
            amountFromQr = amount != null,
            amountLocked = locked && amount != null
        )
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
