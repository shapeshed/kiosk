package com.shapeshed.kiosk.ui

import android.content.Context
import android.content.Intent
import java.io.File
import androidx.core.content.FileProvider
import androidx.core.net.toUri

/** Hand [url] off to the system browser, ignoring failures (e.g. no browser installed). */
fun openExternally(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Hand a local PDF file off to Android's default PDF-capable app. */
fun openPdfExternally(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
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
