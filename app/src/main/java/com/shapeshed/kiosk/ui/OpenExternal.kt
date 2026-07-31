package com.shapeshed.kiosk.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Hand [url] off to the system browser, ignoring failures (e.g. no browser installed). */
fun openExternally(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Share an article URL through Android's system share sheet. */
fun shareArticle(context: Context, title: String, url: String) {
    runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, url)
        context.startActivity(
            Intent.createChooser(shareIntent, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
