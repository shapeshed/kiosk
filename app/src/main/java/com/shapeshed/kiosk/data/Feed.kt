package com.shapeshed.kiosk.data

/**
 * The ranked story lists the HN API exposes. Each maps to one Firebase endpoint returning an
 * ordered array of item ids. The [name] is what we persist as the user's last-viewed feed.
 *
 * See https://github.com/HackerNews/API
 */
enum class Feed(val path: String) {
    TOP("topstories.json"),
    NEW("newstories.json"),
    BEST("beststories.json"),
    ASK("askstories.json"),
    SHOW("showstories.json"),
    JOBS("jobstories.json"),
}
