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
