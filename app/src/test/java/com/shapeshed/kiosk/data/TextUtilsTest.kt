package com.shapeshed.kiosk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextUtilsTest {

    @Test
    fun decodeHtmlDecodesEntities() {
        assertEquals("'", decodeHtml("&#x27;"))
        assertEquals("> reply", decodeHtml("&gt; reply"))
        assertEquals("a & b", decodeHtml("a &amp; b"))
        assertEquals("\"quote\"", decodeHtml("&quot;quote&quot;"))
    }

    @Test
    fun decodeHtmlTurnsParagraphsAndBreaksIntoNewlines() {
        assertEquals("one\n\ntwo", decodeHtml("<p>one</p><p>two</p>"))
        assertEquals("a\nb", decodeHtml("a<br>b"))
    }

    @Test
    fun decodeHtmlStripsTagsLeavingLinkText() {
        assertEquals(
            "https://x.com",
            decodeHtml("""<a href="https://x.com" rel="nofollow">https://x.com</a>"""),
        )
    }

    @Test
    fun decodeHtmlHandlesEmpty() {
        assertEquals("", decodeHtml(""))
    }

    @Test
    fun hostOfStripsWww() {
        assertEquals("example.com", hostOf("https://www.example.com/path?q=1"))
        assertEquals("news.ycombinator.com", hostOf("http://news.ycombinator.com"))
    }

    @Test
    fun hostOfReturnsNullForMissingOrInvalid() {
        assertNull(hostOf(null))
        assertNull(hostOf(""))
        assertNull(hostOf("not a url"))
    }

    @Test
    fun relativeTimeBuckets() {
        val now = 1_000_000_000L
        assertEquals("just now", relativeTime(now, now))
        assertEquals("just now", relativeTime(now - 30, now))
        assertEquals("2m", relativeTime(now - 120, now))
        assertEquals("2h", relativeTime(now - 7_200, now))
        assertEquals("2d", relativeTime(now - 172_800, now))
        assertEquals("2mo", relativeTime(now - 5_184_000, now))
        assertEquals("2y", relativeTime(now - 63_072_000, now))
    }
}
