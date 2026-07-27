package com.shapeshed.kiosk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shapeshed.kiosk.data.hostOf

/**
 * Circular avatar for a story's source, mirroring Aerial's station logos: the site's favicon on a
 * tonal plate, falling back to the source's initial letter when there's no favicon or it fails to
 * load. Favicons are fetched first-party from the site itself (no third-party icon service).
 */
@Composable
fun SourceAvatar(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val host = hostOf(url)
    val faviconUrl = remember(host) { host?.let { "https://$it/favicon.ico" } }
    var failed by remember(faviconUrl) { mutableStateOf(false) }
    val showFavicon = faviconUrl != null && !failed

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (showFavicon) {
            AsyncImage(
                model = faviconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = avatarInitial(host ?: title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun avatarInitial(source: String): String =
    source.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
