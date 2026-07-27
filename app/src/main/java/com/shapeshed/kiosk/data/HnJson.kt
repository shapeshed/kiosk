package com.shapeshed.kiosk.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure parsing and thread-shaping helpers for the Hacker News Firebase API. Kept free of Android
 * and network APIs so they can be unit-tested directly with org.json.
 */

/** Parse a `topstories.json`-style array of item ids. Returns empty on malformed input. */
fun parseIdArray(body: String): List<Long> =
    runCatching {
        val array = JSONArray(body)
        (0 until array.length()).map { array.getLong(it) }
    }.getOrDefault(emptyList())

/** Parse an item into a [Story], or null if it isn't a usable story (missing id/title, deleted). */
fun parseStory(o: JSONObject): Story? {
    if (o.optBoolean("deleted") || o.optBoolean("dead")) return null
    val id = o.optLong("id", 0L).takeIf { it != 0L } ?: return null
    val title = o.optString("title").takeIf { it.isNotBlank() } ?: return null
    return Story(
        id = id,
        title = title,
        url = o.optString("url").takeIf { it.isNotBlank() },
        by = o.optString("by"),
        score = o.optInt("score"),
        descendants = o.optInt("descendants"),
        time = o.optLong("time"),
        kids = o.optJSONArray("kids").toLongList(),
        text = o.optString("text").takeIf { it.isNotBlank() }?.let(::decodeHtml),
    )
}

/** Parse an item into a [Comment], or null if it has no id. Deleted/dead flags are preserved. */
fun parseComment(o: JSONObject): Comment? {
    val id = o.optLong("id", 0L).takeIf { it != 0L } ?: return null
    return Comment(
        id = id,
        by = o.optString("by"),
        time = o.optLong("time"),
        text = o.optString("text").let(::decodeHtml),
        kids = o.optJSONArray("kids").toLongList(),
        deleted = o.optBoolean("deleted"),
        dead = o.optBoolean("dead"),
    )
}

fun parseStoryJson(body: String): Story? =
    runCatching { parseStory(JSONObject(body)) }.getOrNull()

fun parseCommentJson(body: String): Comment? =
    runCatching { parseComment(JSONObject(body)) }.getOrNull()

fun parseSearchPageJson(body: String): SearchPage? =
    runCatching {
        val root = JSONObject(body)
        val hits = root.optJSONArray("hits") ?: JSONArray()
        SearchPage(
            stories = (0 until hits.length()).mapNotNull { index ->
                parseSearchHit(hits.getJSONObject(index))
            },
            page = root.optInt("page"),
            totalPages = root.optInt("nbPages"),
        )
    }.getOrNull()

private fun parseSearchHit(o: JSONObject): Story? {
    val id = o.optLong("objectID", 0L).takeIf { it != 0L } ?: return null
    val title = o.optString("title")
        .ifBlank { o.optString("story_title") }
        .takeIf { it.isNotBlank() }
        ?: return null
    return Story(
        id = id,
        title = title,
        url = o.optString("url").takeIf { it.isNotBlank() },
        by = o.optString("author"),
        score = o.optInt("points"),
        descendants = o.optInt("num_comments"),
        time = o.optLong("created_at_i"),
        kids = emptyList(),
        text = o.optString("story_text").takeIf { it.isNotBlank() }?.let(::decodeHtml),
    )
}

/**
 * Flatten a comment tree depth-first into the order HN displays it — each comment immediately
 * followed by its replies, indented one level deeper. Comments missing from [byId] (unfetched or
 * unreadable) and deleted/dead ones are skipped along with their subtrees. Bounded by [maxTotal]
 * and [maxDepth] so a huge thread can't produce an unbounded list.
 */
fun flattenThread(
    rootKids: List<Long>,
    byId: Map<Long, Comment>,
    maxTotal: Int = 200,
    maxDepth: Int = 10,
): List<FlatComment> {
    val out = ArrayList<FlatComment>()
    fun walk(ids: List<Long>, depth: Int) {
        if (depth > maxDepth) return
        for (id in ids) {
            if (out.size >= maxTotal) return
            val comment = byId[id] ?: continue
            if (comment.deleted || comment.dead) continue
            out.add(FlatComment(comment, depth))
            walk(comment.kids, depth + 1)
        }
    }
    walk(rootKids, 0)
    return out
}

private fun JSONArray?.toLongList(): List<Long> {
    if (this == null) return emptyList()
    return (0 until length()).map { getLong(it) }
}
