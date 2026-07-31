package com.shapeshed.kiosk.ui

internal fun String.pdfUrlOrNull(): String? {
    val uri = runCatching { java.net.URI(this) }.getOrNull() ?: return null
    val path = uri.path.orEmpty()
    if (!path.substringAfterLast('/').endsWith(".pdf", ignoreCase = true)) return null

    if (uri.host.equals("github.com", ignoreCase = true)) {
        val segments = path.trim('/').split('/')
        val blobIndex = segments.indexOf("blob")
        if (blobIndex == 2 && segments.size > 4) {
            val owner = segments[0]
            val repo = segments[1]
            val ref = segments[3]
            val filePath = segments.drop(4).joinToString("/")
            return "https://raw.githubusercontent.com/$owner/$repo/$ref/$filePath"
        }
    }

    return this
}

internal fun String.requiresExternalApp(): Boolean {
    val host = runCatching { java.net.URI(this).host.orEmpty() }.getOrDefault("")
        .removePrefix("www.")
        .removePrefix("mobile.")
        .removePrefix("m.")
    return host == "twitter.com" ||
        host == "x.com" ||
        host == "youtube.com" ||
        host == "youtu.be"
}
