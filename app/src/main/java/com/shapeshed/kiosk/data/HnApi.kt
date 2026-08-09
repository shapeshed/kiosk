package com.shapeshed.kiosk.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Thin client over the official read-only Hacker News API, hosted on Firebase. No key, no auth —
 * a stable, long-lived dependency. All calls are blocking; run them off the main
 * thread (the repository does, on Dispatchers.IO).
 *
 * See https://github.com/HackerNews/API
 */
class HnApi(private val client: OkHttpClient) {

    /** Ordered list of the given feed's current item ids (the API returns up to ~500). Empty on failure. */
    fun feedIds(feed: Feed): List<Long> = get(feed.path)?.let(::parseIdArray) ?: emptyList()

    /** Raw JSON for a single item (story, comment, job, poll), or null if missing/unreadable. */
    fun itemJson(id: Long): String? = get("item/$id.json")

    fun searchJson(
        query: String,
        filter: SearchFilter,
        sort: SearchSort,
        page: Int,
        hitsPerPage: Int,
    ): String? {
        val endpoint = if (sort == SearchSort.DATE) "search_by_date" else "search"
        val url = ALGOLIA_BASE.toHttpUrl().newBuilder()
            .addPathSegment(endpoint)
            .addQueryParameter("query", query)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("hitsPerPage", hitsPerPage.toString())
            .apply {
                filter.tag?.let { addQueryParameter("tags", it) }
            }
            .build()
        return getUrl(url.toString())
    }

    private fun get(path: String): String? = getUrl(BASE + path)

    private fun getUrl(url: String): String? {
        val request = Request.Builder()
            .url(url)
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
        private const val ALGOLIA_BASE = "https://hn.algolia.com/api/v1/"
        private const val USER_AGENT = "Kiosk (Android)"
    }
}
