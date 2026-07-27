package com.shapeshed.kiosk.data

/** A Hacker News story (link post or Ask/Show self-post). */
data class Story(
    val id: Long,
    val title: String,
    val url: String?,          // article link; null for text posts (Ask HN, etc.)
    val by: String,
    val score: Int,
    val descendants: Int,      // comment count reported by the API
    val time: Long,            // unix seconds
    val kids: List<Long>,      // top-level comment ids, in display order
    val text: String?,         // decoded body for self-posts
)

/** A single comment. [text] is already decoded from the API's HTML to plain text. */
data class Comment(
    val id: Long,
    val by: String,
    val time: Long,
    val text: String,
    val kids: List<Long>,
    val deleted: Boolean,
    val dead: Boolean,
)

/** A comment plus its indentation depth in the flattened thread (0 = top level). */
data class FlatComment(
    val comment: Comment,
    val depth: Int,
)

data class SearchPage(
    val stories: List<Story>,
    val page: Int,
    val totalPages: Int,
)

enum class SearchSort {
    RELEVANCE,
    DATE,
}

enum class SearchFilter(val tag: String?) {
    ALL("(story,job)"),
    STORIES("story"),
    ASK("ask_hn"),
    SHOW("show_hn"),
    JOBS("job"),
}
