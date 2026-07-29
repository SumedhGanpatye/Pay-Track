package com.sumedh.moneytracker.domain.notification

/**
 * Parser for Google Pay split / payment request notifications.
 *
 * Primary format:
 *   New split request in 'Amanora Flat'
 *   Pay Manas Jungade Rs 1.00 for 'maggi'
 *
 * Extracts:
 *   amount   → 1.00
 *   requestor → Manas  (first word after Pay)
 *   note     → maggi  (text after for, in quotes)
 */
object NotificationParser {

    // ₹ / Rs / Rs. / INR, with optional spaces (incl. non-breaking)
    private const val AMOUNT = """(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)"""

    private val amountRegex = Regex(AMOUNT, RegexOption.IGNORE_CASE)

    // Straight or curly quotes around the payment note
    private val forNoteRegex = Regex(
        """\bfor\s+['"‘’“”]([^'"‘’“”]+)['"‘’“”]""",
        RegexOption.IGNORE_CASE
    )

    // Also accept unquoted note after for, until end / period
    private val forNoteLooseRegex = Regex(
        """\bfor\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val payLineRegex = Regex(
        """\bPay\s+(.+?)\s+$AMOUNT""",
        RegexOption.IGNORE_CASE
    )

    private val splitGroupRegex = Regex(
        """(?:new\s+)?split\s+request\s+in\s+['"‘’“”]([^'"‘’“”]+)['"‘’“”]""",
        RegexOption.IGNORE_CASE
    )

    fun parse(raw: RawNotificationData): ParsedExpenseData? {
        val text = normalize(raw.combinedText)
        if (text.isBlank()) return null

        // Prefer the "Pay … amount … for 'note'" line — ignore the "New split request…" title.
        parsePayLine(text)?.let { return it }

        // Fallbacks for older / alternate GPay wordings
        parseRequestedOnGroupBy(text)?.let { return it }
        parseRequestedBy(text)?.let { return it }
        parseYouPaid(text)?.let { return it }
        parseAmountForNote(text)?.let { return it }

        return null
    }

    /** Test helper — parse arbitrary text. */
    fun parseText(text: String): ParsedExpenseData? {
        return parse(
            RawNotificationData(
                packageName = GPayNotificationConstants.PACKAGE,
                title = "",
                text = text,
                bigText = null,
                timestamp = System.currentTimeMillis(),
                notificationKey = "test"
            )
        )
    }

    private fun parsePayLine(text: String): ParsedExpenseData? {
        val payMatch = payLineRegex.find(text) ?: return null
        val amount = parseAmount(payMatch.groupValues[2]) ?: return null
        val fullName = payMatch.groupValues[1].trim().trimEnd('.')
        val requestor = fullName.split(Regex("\\s+"))
            .firstOrNull { it.isNotBlank() }
            ?.takeIf { !it.equals("new", ignoreCase = true) }
            ?: return null

        val note = extractForNote(text)
            ?: extractForNote(text.substring(payMatch.range.last + 1))

        return ParsedExpenseData(
            amount = amount,
            personName = requestor,
            groupName = extractSplitGroup(text),
            title = note?.takeIf { it.isNotBlank() } ?: requestor,
            note = note,
            parserUsed = "pay_line",
            originalText = text
        )
    }

    private fun parseRequestedOnGroupBy(text: String): ParsedExpenseData? {
        val regex = Regex(
            """$AMOUNT\s+requested\s+on\s+(.+?)\s+by\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val group = match.groupValues[2].trim().trimEnd('.')
        val person = match.groupValues[3].trim().trimEnd('.')
            .split(Regex("\\s+")).firstOrNull().orEmpty()
        return ParsedExpenseData(
            amount = amount,
            personName = person.takeIf { it.isNotBlank() },
            groupName = group,
            title = person.ifBlank { "Split request" },
            note = extractForNote(text),
            parserUsed = "requested_on_group_by",
            originalText = text
        )
    }

    private fun parseRequestedBy(text: String): ParsedExpenseData? {
        val regex = Regex(
            """$AMOUNT\s+requested\s+by\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val person = match.groupValues[2].trim().trimEnd('.')
            .split(Regex("\\s+")).firstOrNull().orEmpty()
        return ParsedExpenseData(
            amount = amount,
            personName = person.takeIf { it.isNotBlank() },
            groupName = extractSplitGroup(text),
            title = person.ifBlank { "Payment request" },
            note = extractForNote(text),
            parserUsed = "requested_by",
            originalText = text
        )
    }

    private fun parseYouPaid(text: String): ParsedExpenseData? {
        val regex = Regex(
            """you\s+paid\s+$AMOUNT(?:\s+to\s+(.+))?""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val person = match.groupValues.getOrNull(2)?.trim()?.trimEnd('.')
            ?.split(Regex("\\s+"))?.firstOrNull()
        return ParsedExpenseData(
            amount = amount,
            personName = person,
            groupName = null,
            title = person?.let { "Paid to $it" } ?: "Payment",
            note = extractForNote(text),
            parserUsed = "you_paid",
            originalText = text
        )
    }

    private fun parseAmountForNote(text: String): ParsedExpenseData? {
        val amountMatch = amountRegex.find(text) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        val note = extractForNote(text) ?: return null
        return ParsedExpenseData(
            amount = amount,
            personName = null,
            groupName = extractSplitGroup(text),
            title = note,
            note = note,
            parserUsed = "amount_for_note",
            originalText = text
        )
    }

    private fun extractForNote(text: String): String? {
        forNoteRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return cleanNote(it) }

        forNoteLooseRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.trimEnd('.')
            ?.takeIf { it.isNotBlank() && !it.contains("split request", ignoreCase = true) }
            ?.let { return cleanNote(it) }

        return null
    }

    private fun cleanNote(raw: String): String =
        raw.trim()
            .trim('\'', '"', '‘', '’', '“', '”', '.', ',', ' ')
            .replace(Regex("\\s+"), " ")

    private fun extractSplitGroup(text: String): String? =
        splitGroupRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace(",", "").trim()
        return cleaned.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun normalize(text: String): String =
        text
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

object GPayNotificationConstants {
    const val PACKAGE = "com.google.android.apps.nbu.paisa.user"
}
