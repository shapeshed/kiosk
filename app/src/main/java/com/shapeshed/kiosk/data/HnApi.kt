package com.shapeshed.kiosk.data

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Thin client over the official read-only Hacker News API, hosted on Firebase. No key, no auth —
 * a deliberately boring, long-lived dependency. All calls are blocking; run them off the main
 * thread (the repository does, on Dispatchers.IO).
 *
 * See https://github.com/HackerNews/API
 */
class HnApi(private val client: OkHttpClient) {

    /** Ordered list of the given feed's current item ids (the API returns up to ~500). Empty on failure. */
    fun feedIds(feed: Feed): List<Long> = get(feed.path)?.let(::parseIdArray) ?: emptyList()

    /** Raw JSON for a single item (story, comment, job, poll), or null if missing/unreadable. */
    fun itemJson(id: Long): String? = get("item/$id.json")

    private fun get(path: String): String? {
        val request = Request.Builder()
            .url(BASE + path)
            .header("User-Agent", USER_AGENT)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body.string() else null
            }
        }.getOrNull()
    }

    companion object {
        private const val BASE = "https://hacker-news.firebaseio.com/v0/"
        private const val USER_AGENT = "Kiosk (Android)"
    }
}
