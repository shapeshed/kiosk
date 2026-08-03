package com.shapeshed.kiosk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderDocumentTest {

    @Test
    fun parseReaderArticleKeepsBlockOrder() {
        val article = parseReaderArticle(
            title = "Title",
            source = "example.com",
            contentHtml = """
                <h2>Intro</h2>
                <p>Hello <strong>native</strong> reader.</p>
                <blockquote><p>Quoted text</p></blockquote>
                <ul><li>One</li><li>Two</li></ul>
                <pre><code>val x = 1</code></pre>
            """.trimIndent(),
            baseUrl = "https://example.com/story/",
        )

        assertEquals("Title", article.title)
        assertEquals("example.com", article.source)
        assertEquals(5, article.blocks.size)
        assertTrue(article.blocks[0] is ReaderBlock.Heading)
        assertTrue(article.blocks[1] is ReaderBlock.Paragraph)
        assertTrue(article.blocks[2] is ReaderBlock.Quote)
        assertTrue(article.blocks[3] is ReaderBlock.BulletedList)
        assertTrue(article.blocks[4] is ReaderBlock.CodeBlock)
    }

    @Test
    fun parseReaderArticlePreservesInlineLinksAndEmphasis() {
        val paragraph = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """<p>Read <a href="/next"><em>this</em></a> <strong>now</strong>.</p>""",
            baseUrl = "https://example.com/posts/current",
        ).blocks.single() as ReaderBlock.Paragraph

        assertEquals("Read this now.", paragraph.text.joinToString("") { it.text })
        val link = paragraph.text.single { it.text == "this" }
        assertEquals("https://example.com/next", link.href)
        assertTrue(link.emphasis)
        assertTrue(paragraph.text.single { it.text == "now" }.strong)
    }

    @Test
    fun parseReaderArticlePreservesStyledWeightAndItalic() {
        val paragraph = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """
                <p><span style="font-weight: 700">Bold</span>
                <span class="font-semibold">semibold</span>
                <span style="font-style: italic">italic</span>.</p>
            """.trimIndent(),
            baseUrl = "https://example.com/posts/current",
        ).blocks.single() as ReaderBlock.Paragraph

        assertTrue(paragraph.text.single { it.text == "Bold" }.strong)
        assertTrue(paragraph.text.single { it.text == "semibold" }.strong)
        assertTrue(paragraph.text.single { it.text == "italic" }.emphasis)
    }

    @Test
    fun parseReaderArticleResolvesImages() {
        val figure = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """<figure><img src="image.png" alt="Diagram"><figcaption>Figure 1</figcaption></figure>""",
            baseUrl = "https://example.com/articles/",
        ).blocks.first() as ReaderBlock.Figure

        val image = figure.images.single()
        assertEquals("https://example.com/articles/image.png", image.src)
        assertEquals("Diagram", image.alt)
        assertEquals("Figure 1", figure.caption?.joinToString("") { it.text })
    }

    @Test
    fun parseReaderArticleKeepsImagesInsideParagraphs() {
        val blocks = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """<p><img src="/hero.jpg" alt="Hero"></p><p>Body text.</p>""",
            baseUrl = "https://example.com/story/",
        ).blocks

        val image = blocks.first() as ReaderBlock.Image
        assertEquals("https://example.com/hero.jpg", image.src)
        assertEquals("Hero", image.alt)
        assertEquals("Body text.", (blocks.last() as ReaderBlock.Paragraph).text.joinToString("") { it.text })
    }

    @Test
    fun parseReaderArticleKeepsAdjacentParagraphsSeparate() {
        val blocks = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """<p>First paragraph.</p><p>Second paragraph.</p>""",
            baseUrl = "https://example.com/story/",
        ).blocks

        assertEquals(2, blocks.size)
        assertEquals("First paragraph.", (blocks[0] as ReaderBlock.Paragraph).text.joinToString("") { it.text })
        assertEquals("Second paragraph.", (blocks[1] as ReaderBlock.Paragraph).text.joinToString("") { it.text })
    }

    @Test
    fun parseReaderArticlePreservesTableRowsAndCells() {
        val table = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """
                <table>
                    <thead><tr><th>Language</th><th>Score</th></tr></thead>
                    <tbody><tr><td>Kotlin</td><td>10</td></tr></tbody>
                </table>
            """.trimIndent(),
            baseUrl = "https://example.com/story/",
        ).blocks.single() as ReaderBlock.Table

        assertEquals(2, table.rows.size)
        assertEquals(listOf("Language", "Score"), table.rows[0].map { cell -> cell.joinToString("") { it.text } })
        assertEquals(listOf("Kotlin", "10"), table.rows[1].map { cell -> cell.joinToString("") { it.text } })
    }

    @Test
    fun parseReaderArticlePreservesDefinitionListsAndTableCaptions() {
        val blocks = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """
                <dl><dt>Term</dt><dd>Definition</dd></dl>
                <table><caption>Results</caption><tr><th>Key</th><td>Value</td></tr></table>
            """.trimIndent(),
            baseUrl = "https://example.com/story",
        ).blocks

        val definitions = blocks[0] as ReaderBlock.DefinitionList
        assertEquals("Term", definitions.items.single().term.joinToString("") { it.text })
        assertEquals("Definition", definitions.items.single().descriptions.single().joinToString("") { it.text })
        val table = blocks[1] as ReaderBlock.Table
        assertEquals("Results", table.caption?.joinToString("") { it.text })
    }

    @Test
    fun parseReaderArticlePreservesInlineSemanticText() {
        val paragraph = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = "<p>x<sup>2</sup> <mark>important</mark> <u>underlined</u> <del>removed</del></p>",
            baseUrl = "https://example.com/story",
        ).blocks.single() as ReaderBlock.Paragraph

        assertEquals(ReaderScript.SUPERSCRIPT, paragraph.text.single { it.text == "2" }.script)
        assertTrue(paragraph.text.single { it.text == "important" }.highlighted)
        assertTrue(paragraph.text.single { it.text == "underlined" }.underlined)
        assertTrue(paragraph.text.single { it.text == "removed" }.deleted)
    }

    @Test
    fun parseReaderArticleSkipsBrowserHiddenMetadata() {
        val blocks = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """
                <p>Visible article text.</p>
                <p hidden>Published yesterday</p>
                <div aria-hidden="true">Modified today</div>
                <ul style="display: none"><li>tag-one</li><li>tag-two</li></ul>
                <p class="screen-reader-text">Hidden label</p>
                <p class="invisible">Invisible label</p>
                <p class="visuallyhidden">Visually hidden label</p>
            """.trimIndent(),
            baseUrl = "https://example.com/story/",
        ).blocks

        assertEquals(1, blocks.size)
        assertEquals("Visible article text.", (blocks.single() as ReaderBlock.Paragraph).text.joinToString("") { it.text })
    }

    @Test
    fun parseReaderArticleResolvesLazyAndSrcsetImages() {
        val blocks = parseReaderArticle(
            title = null,
            source = null,
            contentHtml = """
                <figure>
                    <picture>
                        <source srcset="small.webp 480w, /large.webp 960w">
                        <img data-lazy-src="fallback.jpg" alt="Lazy image">
                    </picture>
                </figure>
                <p><img srcset="thumb.jpg 320w, full.jpg 1280w" alt="Srcset image"></p>
            """.trimIndent(),
            baseUrl = "https://example.com/articles/current",
        ).blocks

        assertEquals("https://example.com/articles/fallback.jpg", (blocks[0] as ReaderBlock.Figure).images.single().src)
        assertEquals("https://example.com/articles/full.jpg", (blocks[1] as ReaderBlock.Image).src)
    }
}
