package com.shapeshed.kiosk.ui

import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shapeshed.kiosk.data.ReaderExtractionEntity
import com.shapeshed.kiosk.data.Story
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.abs

// Clone the page, run Readability on it, and hand back {t: title, c: contentHtml, x: textContent,
// og: social image url} — or "" on failure. The image is read from the live document (not the clone
// Readability consumes), so it's cheap: the page is already loaded, no extra network round trip.
private const val EXTRACT_JS =
    "(function(){try{" +
        "var og=document.querySelector(\"meta[property='og:image'],meta[name='twitter:image']\");" +
        "var oi=og&&og.content?new URL(og.content,document.baseURI).href:'';" +
        "var a=new Readability(document.cloneNode(true),{classesToPreserve:" +
        "['caption','emoji','hidden','invisible','sr-only','visually-hidden','visuallyhidden'," +
        "'wp-caption','wp-caption-text','wp-smiley']}).parse();" +
        "return a?JSON.stringify({t:a.title,c:a.content,x:a.textContent||'',og:oi}):\"\";" +
        "}catch(e){return \"\";}})();"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArticleWebView(
    url: String,
    pageReady: Boolean,
    pageBackground: Int,
    pageForeground: Int,
    pageMuted: Int,
    contentTopPad: Dp,
    loadingStory: Story? = null,
    onPageReady: () -> Unit,
    onScroll: (scrollY: Int) -> Unit,
    onExtracted: (ReaderExtraction) -> Unit,
    onExtractionFailed: () -> Unit,
) {
    val readabilityJs = rememberReadabilityScript()
    val holder = remember { WebViewHolder() }
    val latestOnScroll by rememberUpdatedState(onScroll)
    val chromeBackground = MaterialTheme.colorScheme.surface
    var webViewVisible by remember(url) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(chromeBackground)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPad)
                .background(Color(pageBackground)),
            factory = { ctx ->
                WebView(ctx).apply {
                    holder.destroyed = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    isSoundEffectsEnabled = false
                    isHapticFeedbackEnabled = false
                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        latestOnScroll(scrollY)
                    }
                    val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
                    var downX = 0f
                    var downY = 0f
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                view.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = abs(event.x - downX)
                                val dy = abs(event.y - downY)
                                if (dx > touchSlop || dy > touchSlop) {
                                    view.parent.requestDisallowInterceptTouchEvent(dy >= dx)
                                }
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL,
                            -> {
                                view.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    // Match the reader background so there's no white flash while (re)loading.
                    setBackgroundColor(pageBackground)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.loadsImagesAutomatically = false
                    settings.blockNetworkImage = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(false)
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, finishedUrl: String?) {
                            if (holder.destroyed || holder.webView !== view) return
                            view.evaluateJavascript(readabilityJs) {
                                if (holder.destroyed || holder.webView !== view) return@evaluateJavascript
                                onPageReady()
                                if (holder.destroyed || holder.webView !== view) return@evaluateJavascript
                                view.postVisualStateCallback(
                                    0L,
                                    object : WebView.VisualStateCallback() {
                                        override fun onComplete(requestId: Long) {
                                            if (holder.destroyed || holder.webView !== view) return
                                            webViewVisible = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                    holder.webView = this
                }
            },
            update = { webView ->
                if (holder.loadedKey != url) {
                    webViewVisible = false
                    webView.loadUrl(url)
                    holder.loadedKey = url
                }
            },
        )

        if (!webViewVisible) {
            if (loadingStory != null) {
                WebArticlePlaceholder(
                    story = loadingStory,
                    pageBackground = pageBackground,
                    pageForeground = pageForeground,
                    pageMuted = pageMuted,
                    topPad = contentTopPad,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(pageBackground))
                        .padding(top = contentTopPad),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }
        }
    }

    LaunchedEffect(pageReady) {
        if (pageReady) {
            val webView = holder.webView ?: return@LaunchedEffect
            webView.evaluateJavascript(EXTRACT_JS) { raw ->
                if (holder.destroyed || holder.webView !== webView) return@evaluateJavascript
                val parsed = parseExtraction(raw)
                if (parsed != null) onExtracted(parsed) else onExtractionFailed()
            }
        }
    }

    DisposableEffect(holder) {
        onDispose {
            holder.destroyed = true
            val webView = holder.webView
            holder.webView = null
            webView?.apply {
                // Clear focus before detaching: a focused View being removed triggers Android's
                // automatic focus reassignment, which re-enters Compose's measure/layout
                // synchronously and crashes when this runs while a pager is mid-measure.
                clearFocus()
                webViewClient = WebViewClient()
                webChromeClient = null
                onPause()
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
    }
}

private class WebViewHolder {
    var webView: WebView? = null
    var loadedKey: String? = null
    var destroyed = false
}

internal data class ReaderExtraction(
    val title: String?,
    val contentHtml: String,
    val textContent: String,
    val ogImageUrl: String?,
)

internal object ReaderExtractionCache {
    private const val MaxEntries = 20
    private val entries = object : LinkedHashMap<Long, CachedReaderExtraction>(MaxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, CachedReaderExtraction>?): Boolean =
            size > MaxEntries
    }
    var generation by mutableLongStateOf(0L)
        private set

    @Synchronized
    fun get(storyId: Long, url: String): ReaderExtraction? =
        entries[storyId]?.takeIf { it.url == url }?.extraction

    @Synchronized
    fun put(storyId: Long, url: String, extraction: ReaderExtraction) {
        entries[storyId] = CachedReaderExtraction(url = url, extraction = extraction)
        generation++
    }
}

/**
 * Which stories' extraction has failed. Only the outermost, chrome-showing ArticleScreen instance
 * ever runs the extraction WebView, but every recursive per-page instance needs to know when it
 * fails so it can stop waiting on the placeholder and fall back — so this is shared, like
 * [ReaderExtractionCache], rather than instance-local state.
 */
internal object ReaderExtractionFailures {
    private val failed = HashSet<Long>()
    var generation by mutableLongStateOf(0L)
        private set

    @Synchronized
    fun isFailed(storyId: Long): Boolean = failed.contains(storyId)

    @Synchronized
    fun markFailed(storyId: Long) {
        if (failed.add(storyId)) generation++
    }

    @Synchronized
    fun clear(storyId: Long) {
        if (failed.remove(storyId)) generation++
    }
}

private data class CachedReaderExtraction(
    val url: String,
    val extraction: ReaderExtraction,
)

internal fun ReaderExtractionEntity.toReaderExtraction(): ReaderExtraction =
    ReaderExtraction(
        title = title,
        contentHtml = contentHtml,
        textContent = textContent,
        ogImageUrl = ogImageUrl,
    )

@Composable
private fun rememberReadabilityScript(): String {
    val context = LocalContext.current
    return remember {
        context.assets.open("readability.js").bufferedReader().use { it.readText() }
    }
}

private fun parseExtraction(raw: String?): ReaderExtraction? {
    if (raw == null || raw == "null") return null
    val inner = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
    if (inner.isNullOrBlank()) return null
    val obj = runCatching { JSONObject(inner) }.getOrNull() ?: return null
    val content = obj.optString("c")
    if (content.isBlank()) return null
    return ReaderExtraction(
        title = obj.optString("t").takeIf { it.isNotBlank() },
        contentHtml = content,
        textContent = obj.optString("x"),
        ogImageUrl = obj.optString("og").takeIf { it.isNotBlank() },
    )
}
