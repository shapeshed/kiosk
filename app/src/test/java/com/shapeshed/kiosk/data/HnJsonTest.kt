package com.shapeshed.kiosk.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HnJsonTest {

    @Test
    fun parseIdArrayReadsLongs() {
        assertEquals(listOf(1L, 2L, 3L), parseIdArray("[1, 2, 3]"))
    }

    @Test
    fun parseIdArrayReturnsEmptyOnGarbage() {
        assertEquals(emptyList<Long>(), parseIdArray("not json"))
    }

    @Test
    fun parseStoryReadsFields() {
        val story = parseStory(
            JSONObject(
                """
                {"id":1,"type":"story","by":"pg","title":"Hello","url":"https://example.com",
                 "score":42,"descendants":7,"time":1700000000,"kids":[10,11]}
                """.trimIndent(),
            ),
        )
        requireNotNull(story)
        assertEquals(1L, story.id)
        assertEquals("Hello", story.title)
        assertEquals("https://example.com", story.url)
        assertEquals(42, story.score)
        assertEquals(7, story.descendants)
        assertEquals(listOf(10L, 11L), story.kids)
    }

    @Test
    fun parseStoryReturnsNullForDeletedOrTitleless() {
        assertNull(parseStory(JSONObject("""{"id":1,"deleted":true}""")))
        assertNull(parseStory(JSONObject("""{"id":1,"type":"story"}""")))
    }

    @Test
    fun textPostHasNullUrlAndDecodedText() {
        val story = parseStory(
            JSONObject("""{"id":2,"title":"Ask HN","text":"a &#x27;b&#x27;","time":1}"""),
        )
        requireNotNull(story)
        assertNull(story.url)
        assertEquals("a 'b'", story.text)
    }

    @Test
    fun parseCommentPreservesFlags() {
        val comment = parseComment(
            JSONObject("""{"id":9,"by":"alice","time":123,"text":"&gt; hi","kids":[1],"dead":true}"""),
        )
        requireNotNull(comment)
        assertEquals(9L, comment.id)
        assertEquals("alice", comment.by)
        assertEquals("> hi", comment.text)
        assertEquals(listOf(1L), comment.kids)
        assertTrue(comment.dead)
        assertFalse(comment.deleted)
    }

    @Test
    fun flattenThreadOrdersDepthFirstWithDepths() {
        val byId = mapOf(
            comment(1, kids = listOf(2, 3)),
            comment(2),
            comment(3),
            comment(4),
        )
        val flat = flattenThread(rootKids = listOf(1, 4), byId = byId)
        assertEquals(listOf(1L to 0, 2L to 1, 3L to 1, 4L to 0), flat.map { it.comment.id to it.depth })
    }

    @Test
    fun flattenThreadHonoursMaxTotal() {
        val byId = mapOf(comment(1, kids = listOf(2, 3)), comment(2), comment(3))
        val flat = flattenThread(listOf(1), byId, maxTotal = 2)
        assertEquals(listOf(1L, 2L), flat.map { it.comment.id })
    }

    @Test
    fun flattenThreadHonoursMaxDepth() {
        val byId = mapOf(comment(1, kids = listOf(2)), comment(2))
        val flat = flattenThread(listOf(1), byId, maxDepth = 0)
        assertEquals(listOf(1L), flat.map { it.comment.id })
    }

    @Test
    fun flattenThreadSkipsMissingAndDeleted() {
        val byId = mapOf(
            comment(1),
            comment(2, deleted = true),
            // id 3 intentionally absent from the map
        )
        val flat = flattenThread(listOf(1, 2, 3), byId)
        assertEquals(listOf(1L), flat.map { it.comment.id })
    }

    private fun comment(
        id: Long,
        kids: List<Long> = emptyList(),
        deleted: Boolean = false,
    ): Pair<Long, Comment> =
        id to Comment(id, by = "u$id", time = 0, text = "c$id", kids = kids, deleted = deleted, dead = false)
}
