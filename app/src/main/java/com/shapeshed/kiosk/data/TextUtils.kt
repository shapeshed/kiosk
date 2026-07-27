package com.shapeshed.kiosk.data

import java.net.URI

/**
 * Pure text helpers (no Android APIs) so they can be unit-tested directly.
 */

private val TAG_REGEX = Regex("<[^>]+>")
private val MULTI_NEWLINE_REGEX = Regex("\\n{3,}")
private val NUMERIC_ENTITY_REGEX = Regex("&#(x?[0-9a-fA-F]+);")

/**
 * Convert the HTML the HN API returns for comment/story bodies into plain text: paragraph and
 * line-break tags become newlines, other tags are dropped, and HTML entities are decoded. Links
 * collapse to their visible text, which for HN is usually the URL itself.
 */
fun decodeHtml(html: String): String {
    if (html.isEmpty()) return ""
    var text = html
        .replace(Regex("(?i)<p>"), "\n\n")
        .replace(Regex("(?i)</p>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
    text = TAG_REGEX.replace(text, "")
    text = decodeEntities(text)
    text = MULTI_NEWLINE_REGEX.replace(text, "\n\n")
    return text.trim()
}

private fun decodeEntities(input: String): String {
    var text = input
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
    text = NUMERIC_ENTITY_REGEX.replace(text) { match ->
        val token = match.groupValues[1]
        val code = if (token.startsWith("x") || token.startsWith("X")) {
            token.drop(1).toIntOrNull(16)
        } else {
            token.toIntOrNull()
        }
        if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else match.value
    }
    return text
}

/** Display host for a story link — the host with any leading `www.` removed, or null if unparseable. */
fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val host = runCatching { URI(url).host }.getOrNull() ?: return null
    return host.removePrefix("www.").takeIf { it.isNotBlank() }
}

/**
 * Short relative time like "just now", "5m", "3h", "2d", "4mo", "1y", given an item's unix
 * [epochSeconds] and the current time [nowSeconds].
 */
fun relativeTime(epochSeconds: Long, nowSeconds: Long): String {
    val diff = (nowSeconds - epochSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "just now"
        diff < 3_600 -> "${diff / 60}m"
        diff < 86_400 -> "${diff / 3_600}h"
        diff < 2_592_000 -> "${diff / 86_400}d"
        diff < 31_536_000 -> "${diff / 2_592_000}mo"
        else -> "${diff / 31_536_000}y"
    }
}
