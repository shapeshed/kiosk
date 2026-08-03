package com.shapeshed.kiosk.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

data class ReaderArticle(
    val title: String?,
    val source: String?,
    val blocks: List<ReaderBlock>,
    val ogImageUrl: String? = null,
)

sealed interface ReaderBlock {
    data class Heading(val level: Int, val text: List<ReaderInline>) : ReaderBlock
    data class Paragraph(val text: List<ReaderInline>) : ReaderBlock
    data class Quote(val blocks: List<ReaderBlock>) : ReaderBlock
    data class CodeBlock(val text: String) : ReaderBlock
    data class BulletedList(val items: List<List<ReaderInline>>) : ReaderBlock
    data class NumberedList(val items: List<List<ReaderInline>>) : ReaderBlock
    data class DefinitionList(val items: List<DefinitionItem>) : ReaderBlock
    data class Table(
        val rows: List<List<List<ReaderInline>>>,
        val caption: List<ReaderInline>? = null,
        val headerRows: Set<Int> = emptySet(),
    ) : ReaderBlock
    data class Image(val src: String, val alt: String?) : ReaderBlock
    data class Figure(val images: List<Image>, val caption: List<ReaderInline>?) : ReaderBlock
    data object Divider : ReaderBlock
}

data class DefinitionItem(
    val term: List<ReaderInline>,
    val descriptions: List<List<ReaderInline>>,
)

enum class ReaderScript { NONE, SUPERSCRIPT, SUBSCRIPT }

data class ReaderInline(
    val text: String,
    val href: String? = null,
    val strong: Boolean = false,
    val emphasis: Boolean = false,
    val code: Boolean = false,
    val script: ReaderScript = ReaderScript.NONE,
    val highlighted: Boolean = false,
    val underlined: Boolean = false,
    val deleted: Boolean = false,
)

fun parseReaderArticle(
    title: String?,
    source: String?,
    contentHtml: String,
    baseUrl: String?,
    ogImageUrl: String? = null,
): ReaderArticle {
    val document = Jsoup.parseBodyFragment(contentHtml, baseUrl.orEmpty())
    return ReaderArticle(
        title = title?.takeIf { it.isNotBlank() },
        source = source?.takeIf { it.isNotBlank() },
        blocks = document.body().children().flatMap(::parseBlock),
        ogImageUrl = ogImageUrl?.takeIf { it.isNotBlank() },
    )
}

private fun parseBlock(element: Element): List<ReaderBlock> =
    if (element.isHiddenFromReader()) {
        emptyList()
    } else {
        when (element.normalName()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> inlineText(element).toParagraphTextOrNull()?.let {
                listOf(ReaderBlock.Heading(element.normalName().drop(1).toInt(), it))
            }.orEmpty()
            "p" -> parseParagraph(element)
            "blockquote" -> {
                val blocks = element.children().flatMap(::parseBlock).ifEmpty {
                    inlineText(element).toParagraphTextOrNull()?.let { listOf(ReaderBlock.Paragraph(it)) }.orEmpty()
                }
                if (blocks.isEmpty()) emptyList() else listOf(ReaderBlock.Quote(blocks))
            }
            "pre" -> element.wholeText().trim().takeIf { it.isNotBlank() }?.let {
                listOf(ReaderBlock.CodeBlock(it))
            }.orEmpty()
            "ul" -> parseList(element)?.let { listOf(ReaderBlock.BulletedList(it)) }.orEmpty()
            "ol" -> parseList(element)?.let { listOf(ReaderBlock.NumberedList(it)) }.orEmpty()
            "dl" -> parseDefinitionList(element)?.let { listOf(ReaderBlock.DefinitionList(it)) }.orEmpty()
            "img" -> parseImage(element)?.let(::listOf).orEmpty()
            "figure" -> parseFigure(element)
            "hr" -> listOf(ReaderBlock.Divider)
            "table" -> parseTable(element)?.let(::listOf).orEmpty()
            else -> {
                val childBlocks = element.children().flatMap(::parseBlock)
                childBlocks.ifEmpty {
                    inlineText(element).toParagraphTextOrNull()?.let { listOf(ReaderBlock.Paragraph(it)) }.orEmpty()
                }
            }
        }
    }

private fun parseParagraph(element: Element): List<ReaderBlock> {
    val paragraph = inlineText(element).toParagraphTextOrNull()?.let { ReaderBlock.Paragraph(it) }
    val images = element.select("img, picture img")
        .filterNot { it.isHiddenFromReader() }
        .mapNotNull(::parseImage)
    return listOfNotNull(paragraph) + images
}

private fun parseFigure(element: Element): List<ReaderBlock> {
    val images = element.select("img, picture img")
        .filterNot { it.isHiddenFromReader() }
        .mapNotNull(::parseImage)
    val caption = element.selectFirst("figcaption")?.let(::inlineText)?.toParagraphTextOrNull()
    if (images.isEmpty()) return listOfNotNull(caption?.let { ReaderBlock.Paragraph(it) })
    return listOf(ReaderBlock.Figure(images = images, caption = caption))
}

private fun parseImage(element: Element): ReaderBlock.Image? {
    val src = element.imageUrl() ?: return null
    return ReaderBlock.Image(
        src = src,
        alt = element.attr("alt").takeIf { it.isNotBlank() },
    )
}

private fun Element.imageUrl(): String? {
    val direct = firstResolvedUrl(
        "src",
        "data-src",
        "data-original",
        "data-lazy-src",
        "data-url",
    )
    if (direct != null) return direct

    val srcset = bestSrcsetUrl("srcset")
        ?: bestSrcsetUrl("data-srcset")
        ?: parent()
            ?.takeIf { it.normalName() == "picture" }
            ?.selectFirst("source[srcset], source[data-srcset]")
            ?.let { source -> source.bestSrcsetUrl("srcset") ?: source.bestSrcsetUrl("data-srcset") }
    return srcset
}

private fun Element.firstResolvedUrl(vararg attributes: String): String? =
    attributes.firstNotNullOfOrNull { attribute ->
        val absolute = absUrl(attribute)
        if (absolute.isNotBlank()) {
            absolute
        } else {
            attr(attribute).resolveAgainstOwnerDocument(this)
        }
    }

private fun Element.bestSrcsetUrl(attribute: String): String? =
    attr(attribute)
        .split(',')
        .mapNotNull { candidate ->
            candidate.trim()
                .substringBefore(' ')
                .takeIf { it.isNotBlank() }
                ?.resolveAgainstOwnerDocument(this)
        }
        .lastOrNull()

private fun String.resolveAgainstOwnerDocument(element: Element): String? {
    if (isBlank()) return null
    return if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        val baseUrl = element.ownerDocument()?.location().orEmpty()
        if (baseUrl.isBlank()) {
            this
        } else {
            runCatching { URI(baseUrl).resolve(this).toString() }.getOrDefault(this)
        }
    }
}

private fun parseList(element: Element): List<List<ReaderInline>>? {
    val items = element.children()
        .filter { it.normalName() == "li" && !it.isHiddenFromReader() }
        .mapNotNull { inlineText(it).toParagraphTextOrNull() }
    return items.takeIf { it.isNotEmpty() }
}

private fun parseDefinitionList(element: Element): List<DefinitionItem>? {
    val items = buildList {
        var term: List<ReaderInline>? = null
        val descriptions = mutableListOf<List<ReaderInline>>()
        fun flush() {
            val currentTerm = term
            if (currentTerm != null && descriptions.isNotEmpty()) {
                add(DefinitionItem(currentTerm, descriptions.toList()))
            }
            term = null
            descriptions.clear()
        }
        element.children().filterNot { it.isHiddenFromReader() }.forEach { child ->
            when (child.normalName()) {
                "dt" -> {
                    flush()
                    term = inlineText(child).toParagraphTextOrNull()
                }
                "dd" -> inlineText(child).toParagraphTextOrNull()?.let(descriptions::add)
            }
        }
        flush()
    }
    return items.takeIf { it.isNotEmpty() }
}

private fun parseTable(element: Element): ReaderBlock.Table? {
    val parsedRows = element.select("tr").mapNotNull { row ->
        val cells = row.children()
            .filter { it.normalName() == "th" || it.normalName() == "td" }
            .map(::inlineText)
        cells.takeIf { it.isNotEmpty() && it.any { cell -> cell.isNotEmpty() } }
            ?.let { it to row.children().any { cell -> cell.normalName() == "th" } }
    }
    val rows = parsedRows.map { it.first }
    val headerRows = parsedRows.mapIndexedNotNull { index, (_, hasHeaderCell) ->
        index.takeIf { hasHeaderCell }
    }.toSet()
    val caption = element.children()
        .firstOrNull { it.normalName() == "caption" && !it.isHiddenFromReader() }
        ?.let(::inlineText)
        ?.toParagraphTextOrNull()
    return rows.takeIf { it.isNotEmpty() }
        ?.let { ReaderBlock.Table(rows = it, caption = caption, headerRows = headerRows) }
}

private fun inlineText(element: Element): List<ReaderInline> =
    element.childNodes().flatMap { node -> parseInline(node) }.normalizeInlineWhitespace()

private fun parseInline(
    node: Node,
    href: String? = null,
    strong: Boolean = false,
    emphasis: Boolean = false,
    code: Boolean = false,
    script: ReaderScript = ReaderScript.NONE,
    highlighted: Boolean = false,
    underlined: Boolean = false,
    deleted: Boolean = false,
): List<ReaderInline> =
    when (node) {
        is TextNode -> listOf(
            ReaderInline(
                text = node.text(),
                href = href,
                strong = strong,
                emphasis = emphasis,
                code = code,
                script = script,
                highlighted = highlighted,
                underlined = underlined,
                deleted = deleted,
            ),
        )
        is Element -> {
            if (node.isHiddenFromReader()) {
                emptyList()
            } else {
                val styledStrong = strong || node.hasStrongTextStyle()
                val styledEmphasis = emphasis || node.hasEmphasisTextStyle()
                when (node.normalName()) {
                    "br" -> listOf(ReaderInline("\n", href, styledStrong, styledEmphasis, code))
                    "a" -> node.childNodes().flatMap {
                        parseInline(
                            node = it,
                            href = node.absUrl("href")
                                .ifBlank { node.attr("href") }
                                .takeIf { url -> url.isNotBlank() },
                            strong = styledStrong,
                            emphasis = styledEmphasis,
                            code = code,
                        )
                    }
                    "strong", "b" -> node.childNodes().flatMap {
                        parseInline(it, href, strong = true, styledEmphasis, code)
                    }
                    "em", "i" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, emphasis = true, code)
                    }
                    "code" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code = true)
                    }
                    "sup" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code, ReaderScript.SUPERSCRIPT, highlighted, underlined, deleted)
                    }
                    "sub" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code, ReaderScript.SUBSCRIPT, highlighted, underlined, deleted)
                    }
                    "mark" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code, script, highlighted = true, underlined, deleted)
                    }
                    "u", "ins" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code, script, highlighted, underlined = true, deleted)
                    }
                    "s", "strike", "del" -> node.childNodes().flatMap {
                        parseInline(it, href, styledStrong, styledEmphasis, code, script, highlighted, underlined, deleted = true)
                    }
                    "script", "style", "noscript" -> emptyList()
                    else -> node.childNodes().flatMap { parseInline(it, href, styledStrong, styledEmphasis, code) }
                }
            }
        }
        else -> emptyList()
    }

private fun Element.hasStrongTextStyle(): Boolean {
    val style = attr("style")
    if (Regex("""font-weight\s*:\s*(bold|bolder|[6-9]00)""", RegexOption.IGNORE_CASE).containsMatchIn(style)) {
        return true
    }
    return classNames().any { className ->
        val name = className.lowercase()
        name == "bold" || name == "strong" || name == "font-bold" || name == "font-semibold"
    }
}

private fun Element.hasEmphasisTextStyle(): Boolean {
    val style = attr("style")
    if (Regex("""font-style\s*:\s*italic""", RegexOption.IGNORE_CASE).containsMatchIn(style)) {
        return true
    }
    return classNames().any { className ->
        val name = className.lowercase()
        name == "italic" || name == "emphasis" || name == "font-italic"
    }
}

private fun Element.isHiddenFromReader(): Boolean {
    return generateSequence(this) { it.parent() }.any { it.hasHiddenReaderMarker() }
}

private fun Element.hasHiddenReaderMarker(): Boolean {
    if (hasAttr("hidden")) return true
    if (attr("aria-hidden").equals("true", ignoreCase = true)) return true

    val style = attr("style").lowercase()
    if (
        style.contains("display:none") ||
        style.contains("display: none") ||
        style.contains("visibility:hidden") ||
        style.contains("visibility: hidden")
    ) {
        return true
    }

    return classNames().any { className ->
        val name = className.lowercase()
        name == "hidden" ||
            name == "invisible" ||
            name == "sr-only" ||
            name == "visually-hidden" ||
            name == "visuallyhidden" ||
            name == "screen-reader-text"
    }
}

private fun List<ReaderInline>.normalizeInlineWhitespace(): List<ReaderInline> {
    val output = ArrayList<ReaderInline>()
    for (inline in this) {
        val text = inline.text.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        if (text.isEmpty()) continue
        val previous = output.lastOrNull()
        if (
            previous != null &&
            previous.href == inline.href &&
            previous.strong == inline.strong &&
            previous.emphasis == inline.emphasis &&
            previous.code == inline.code &&
            previous.script == inline.script &&
            previous.highlighted == inline.highlighted &&
            previous.underlined == inline.underlined &&
            previous.deleted == inline.deleted
        ) {
            output[output.lastIndex] = previous.copy(text = previous.text + text)
        } else {
            output += inline.copy(text = text)
        }
    }
    return output
}

private fun List<ReaderInline>.toParagraphTextOrNull(): List<ReaderInline>? {
    val text = joinToString(separator = "") { it.text }.trim()
    if (text.isBlank()) return null
    val leading = Regex("^\\s+")
    val trailing = Regex("\\s+$")
    return mapIndexed { index, inline ->
        when (index) {
            0 -> inline.copy(text = leading.replace(inline.text, ""))
            lastIndex -> inline.copy(text = trailing.replace(inline.text, ""))
            else -> inline
        }
    }.filter { it.text.isNotEmpty() }
}
