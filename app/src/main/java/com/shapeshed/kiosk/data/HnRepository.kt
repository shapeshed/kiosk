package com.shapeshed.kiosk.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Turns the item-by-id HN API into the lists the UI needs. The API has no batch endpoint, so
 * fetching a feed or a comment thread is inherently N+1; calls are fanned out concurrently on
 * [Dispatchers.IO] and OkHttp's dispatcher bounds the real parallelism.
 */
class HnRepository(private val api: HnApi) {

    /** The given feed's current ids in rank order (up to ~500), for paged loading. */
    suspend fun storyIds(feed: Feed): List<Long> = withContext(Dispatchers.IO) { api.feedIds(feed) }

    /** Fetch the given story [ids] concurrently, preserving order; unreadable/removed ones drop out. */
    suspend fun stories(ids: List<Long>): List<Story> = withContext(Dispatchers.IO) {
        coroutineScope {
            ids.map { id -> async { api.itemJson(id)?.let(::parseStoryJson) } }.awaitAll()
        }.filterNotNull()
    }

    /** A single story by id, or null if missing. */
    suspend fun story(id: Long): Story? = withContext(Dispatchers.IO) {
        api.itemJson(id)?.let(::parseStoryJson)
    }

    /**
     * The story's comments as a flattened, depth-annotated thread. Fetches the tree breadth-first
     * (each level concurrently) up to [maxTotal] comments, then orders it depth-first for display.
     */
    suspend fun commentThread(story: Story, maxTotal: Int = MAX_COMMENTS): List<FlatComment> =
        withContext(Dispatchers.IO) {
            val byId = fetchComments(story.kids, maxTotal)
            flattenThread(story.kids, byId, maxTotal)
        }

    private suspend fun fetchComments(rootKids: List<Long>, maxTotal: Int): Map<Long, Comment> {
        val byId = HashMap<Long, Comment>()
        var frontier = rootKids
        while (frontier.isNotEmpty() && byId.size < maxTotal) {
            val level = coroutineScope {
                frontier.map { id -> async { api.itemJson(id)?.let(::parseCommentJson) } }.awaitAll()
            }.filterNotNull()
            val next = ArrayList<Long>()
            for (comment in level) {
                byId[comment.id] = comment
                next.addAll(comment.kids)
            }
            frontier = next
        }
        return byId
    }

    companion object {
        private const val MAX_COMMENTS = 200
    }
}
