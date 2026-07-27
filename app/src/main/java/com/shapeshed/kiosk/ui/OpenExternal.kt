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
