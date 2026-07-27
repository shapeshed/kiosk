package com.shapeshed.kiosk.ui

import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.FlatComment
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderTheme
import com.shapeshed.kiosk.data.ReaderArticle
import com.shapeshed.kiosk.data.ReaderBlock
import com.shapeshed.kiosk.data.ReaderInline
import com.shapeshed.kiosk.data.Story
import com.shapeshed.kiosk.data.hostOf
import com.shapeshed.kiosk.data.parseReaderArticle
import com.shapeshed.kiosk.data.relativeTime
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

// Clone the page, run Readability on it, and hand back {t: title, c: contentHtml} — or "" on failure.
private const val EXTRACT_JS =
    "(function(){try{var a=new Readability(document.cloneNode(true)).parse();" +
        "return a?JSON.stringify({t:a.title,c:a.content}):\"\";}catch(e){return \"\";}})();"

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun ArticleScreen(
    storyId: Long,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    rendererOverride: ArticleRendererOverride? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as KioskApp
    val viewModel: ArticleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "article-$storyId",
        factory = remember(storyId) { ArticleViewModel.factory(app, storyId) },
    )
    val storyState by viewModel.story.collectAsStateWithLifecycle()
    val commentsState by viewModel.comments.collectAsStateWithLifecycle()
    val story = (storyState as? UiState.Content)?.data
    val darkTheme = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val readerTheme by app.settings.readerTheme.collectAsStateWithLifecycle(ReaderTheme.SYSTEM)
    val readerFont by app.settings.readerFont.collectAsStateWithLifecycle(ReaderFont.NEWSREADER)
    val palette = readerPaletteFor(readerTheme, darkTheme)
    val fontFaceCss = rememberReaderFontFaceCss(readerFont)
    // Matches the reader stylesheet background (see buildReaderHtml) so covers/flash are seamless.
    val pageBackground = android.graphics.Color.parseColor(palette.background)
    // Reader content sits below the overlay bar: pad the top by status-bar + app-bar height (CSS
    // px ≈ dp). The bar only hides after scrolling down, so by then this padding is off-screen.
    val density = LocalDensity.current
    // status-bar/notch height (dp) + app-bar (64dp) + a comfortable gap.
    val readerTopPad = (WindowInsets.statusBarsIgnoringVisibility.getTop(density) / density.density).toInt() + 88

    // Reader vs web is a persisted preference (applies to every article); readerFailed is a
    // per-article fallback to web when Readability can't extract this page.
    val readerModePref by app.settings.readerModeEnabled.collectAsStateWithLifecycle(true)
    var readerFailed by remember(storyId) { mutableStateOf(false) }
    val readerMode = rendererOverride != null || (readerModePref && !readerFailed)
    val forceWebReader = rendererOverride == ArticleRendererOverride.WEB_READER
    val forceNativeReader = rendererOverride == ArticleRendererOverride.NATIVE_READER
    var extracted by remember(storyId) { mutableStateOf<Pair<String?, String>?>(null) }
    var pageReady by remember(storyId) { mutableStateOf(false) }
    var readerShown by remember(storyId) { mutableStateOf(false) }
    var openedLinkUrl by remember(storyId) { mutableStateOf<String?>(null) }
    val nativeReaderListState = remember(storyId) { LazyListState() }
    var showAppearance by remember { mutableStateOf(false) }
    // Rebuilt whenever the extracted article OR the palette/font changes, so an appearance change
    // re-themes the article you're currently reading (not just the next one).
    val readerHtml = remember(extracted, palette, fontFaceCss, readerFont, story?.url, readerTopPad) {
        extracted?.let { (title, content) ->
            buildReaderHtml(
                title = title,
                source = hostOf(story?.url),
                contentHtml = content,
                palette = palette,
                fontFaceCss = fontFaceCss,
                readerFontFamily = readerFont.cssStack,
                topPadPx = readerTopPad,
            )
        }
    }
    val readerArticle = remember(extracted, story?.url) {
        extracted?.let { (title, content) ->
            parseReaderArticle(
                title = title,
                source = hostOf(story?.url),
                contentHtml = content,
                baseUrl = story?.url,
            )
        }
    }
    val nativeReaderReady = readerMode && !forceWebReader && readerArticle?.blocks?.isNotEmpty() == true
    val webReaderReady = readerMode && !forceNativeReader && readerHtml != null
    // Instagram-style: the bar shows on launch, then slides up off-screen (and the app goes
    // immersive) when scrolling down, and slides back on scrolling up. The bar OVERLAYS the
    // content (not the scaffold's top slot), so animating it never relayouts the WebView.
    var barVisible by remember(storyId) { mutableStateOf(true) }
    // The reader hides its bar on scroll (immersive); the web view keeps the bar pinned so the
    // page can sit padded below it with no gap.
    val effectiveBarVisible = !readerMode || barVisible
    // Previous WebView scroll position — tracked here because the View callback's oldScrollY is
    // unreliable for a WebView. Plain holder (no snapshot) so scroll events don't recompose.
    val lastScrollY = remember(storyId) { intArrayOf(0) }

    // Comments are summoned from the top bar, not always on screen — so the reader stays clean.
    var showComments by remember(storyId) { mutableStateOf(false) }
    // System bars follow the app bar: visible together, and hidden together (immersive) when the
    // user scrolls down — the native "immersive" API, tied to scroll direction like Instagram.
    val view = LocalView.current
    val insetsController = remember(view) {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(insetsController) {
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(effectiveBarVisible) {
        if (effectiveBarVisible) insetsController?.show(WindowInsetsCompat.Type.systemBars())
        else insetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }
    LaunchedEffect(nativeReaderReady) {
        if (nativeReaderReady) readerShown = true
    }
    BackHandler(enabled = openedLinkUrl != null) {
        openedLinkUrl = null
        lastScrollY[0] = nativeReaderListState.readerScrollKey()
        barVisible = true
    }
    // Load the thread in the background so opening comments is instant when summoned.
    LaunchedEffect(storyId) { viewModel.loadComments() }

    val linkedUrl = openedLinkUrl
    Box(modifier.fillMaxSize()) {
            when {
                storyState is UiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                story == null -> ErrorState(stringResource(R.string.couldnt_load), viewModel::loadStory)
                story.url == null -> TextPost(story)
                linkedUrl != null -> ArticleWebView(
                    url = linkedUrl,
                    readerMode = false,
                    readerHtml = null,
                    pageReady = true,
                    pageBackground = pageBackground,
                    contentTopPad = readerTopPad.dp,
                    onPageReady = {},
                    onReaderShownChange = {},
                    onScroll = { scrollY ->
                        val dy = scrollY - lastScrollY[0]
                        lastScrollY[0] = scrollY
                        if (dy > 12) barVisible = false
                        else if (dy < -12) barVisible = true
                    },
                    onExtracted = { _, _ -> },
                    onExtractionFailed = {},
                )
                else -> {
                    if (nativeReaderReady && readerArticle != null) {
                        NativeReaderArticle(
                            article = readerArticle,
                            palette = palette,
                            readerFont = readerFont,
                            listState = nativeReaderListState,
                            topPad = readerTopPad.dp,
                            onScroll = { scrollY ->
                                val dy = scrollY - lastScrollY[0]
                                lastScrollY[0] = scrollY
                                if (dy > 12) barVisible = false
                                else if (dy < -12) barVisible = true
                            },
                            onOpenLink = { url ->
                                openedLinkUrl = url
                                barVisible = true
                            },
                        )
                    } else {
                        ArticleWebView(
                            url = story.url,
                            readerMode = readerMode,
                            readerHtml = if (webReaderReady) readerHtml else null,
                            pageReady = pageReady,
                            pageBackground = pageBackground,
                            contentTopPad = if (readerMode) 0.dp else readerTopPad.dp,
                            onPageReady = { pageReady = true },
                            onReaderShownChange = { readerShown = it },
                            onScroll = { scrollY ->
                                val dy = scrollY - lastScrollY[0]
                                lastScrollY[0] = scrollY
                                if (dy > 12) barVisible = false      // scrolling down hides the bar
                                else if (dy < -12) barVisible = true // scrolling up reveals it
                            },
                            onExtracted = { title, content -> extracted = title to content },
                            onExtractionFailed = {
                                readerFailed = true
                                Toast.makeText(context, R.string.reader_unavailable, Toast.LENGTH_SHORT).show()
                            },
                        )
                        // Cover the raw page until the native reader model is ready, so reader mode
                        // still opens without a flash of web chrome.
                        if (readerMode && !readerShown) {
                            Box(
                                Modifier.fillMaxSize().background(Color(pageBackground)),
                                contentAlignment = Alignment.Center,
                            ) { LoadingIndicator() }
                        }
                    }
                }
            }

            // Overlay app bar (not the scaffold top slot): reveals on scroll-up, hides on
            // scroll-down, and never relayouts the WebView underneath.
            AnimatedVisibility(
                visible = effectiveBarVisible,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
            ) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        if (showBack || openedLinkUrl != null) {
                            IconButton(onClick = {
                                if (openedLinkUrl != null) {
                                    openedLinkUrl = null
                                    lastScrollY[0] = nativeReaderListState.readerScrollKey()
                                    barVisible = true
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        if (openedLinkUrl == null) {
                            IconButton(onClick = { showAppearance = true }) {
                                Icon(Icons.Filled.FormatSize, stringResource(R.string.appearance))
                            }
                            if ((story?.descendants ?: 0) > 0) {
                                IconButton(onClick = { showComments = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Comment,
                                        stringResource(R.string.comments),
                                    )
                                }
                            }
                            if (story?.url != null && rendererOverride == null) {
                                IconButton(onClick = {
                                    val next = !readerModePref
                                    readerFailed = false
                                    scope.launch { app.settings.setReaderModeEnabled(next) }
                                }) {
                                    if (readerMode) {
                                        Icon(Icons.Filled.Public, stringResource(R.string.web_view))
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Article, stringResource(R.string.reader_view))
                                    }
                                }
                                IconButton(onClick = { openExternally(context, story.url) }) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.open_in_browser))
                                }
                            }
                        }
                    },
                    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                )
            }

            // Summoned from the top bar: a modal sheet over the article, dismissed to return
            // straight to distraction-free reading. The reader itself carries no comments chrome.
            if (showComments) {
                CommentsModalSheet(
                    commentCount = story?.descendants ?: 0,
                    state = commentsState,
                    onDismiss = { showComments = false },
                )
            }
    }

    if (showAppearance) {
        AppearanceSheet(
            current = readerTheme,
            currentFont = readerFont,
            onSelectTheme = { scope.launch { app.settings.setReaderTheme(it) } },
            onSelectFont = { scope.launch { app.settings.setReaderFont(it) } },
            onDismiss = { showAppearance = false },
        )
    }
}

/**
 * The comment thread as a modal sheet, summoned from the article's top bar. Opens large (skips the
 * partial state) so tapping "comments" lands you straight in the thread; drag down to dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentsModalSheet(
    commentCount: Int,
    state: UiState<List<FlatComment>>,
    onDismiss: () -> Unit,
) {
    val nowSeconds = remember { System.currentTimeMillis() / 1000 }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Bound the height so the LazyColumn has a fixed frame to scroll within.
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            Text(
                text = pluralStringResource(R.plurals.comments_count, commentCount, commentCount),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                when (state) {
                    is UiState.Loading -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                    is UiState.Error -> item(key = "error") {
                        Text(stringResource(R.string.couldnt_load_comments), Modifier.padding(16.dp))
                    }
                    is UiState.Content -> {
                        if (state.data.isEmpty()) {
                            item(key = "empty") { Text(stringResource(R.string.no_comments), Modifier.padding(16.dp)) }
                        } else {
                            items(state.data, key = { it.comment.id }) { flat ->
                                CommentRow(flat, nowSeconds)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleWebView(
    url: String,
    readerMode: Boolean,
    readerHtml: String?,
    pageReady: Boolean,
    pageBackground: Int,
    contentTopPad: androidx.compose.ui.unit.Dp,
    onPageReady: () -> Unit,
    onReaderShownChange: (Boolean) -> Unit,
    onScroll: (scrollY: Int) -> Unit,
    onExtracted: (title: String?, content: String) -> Unit,
    onExtractionFailed: () -> Unit,
) {
    val readabilityJs = rememberReadabilityScript()
    val holder = remember { WebViewHolder() }

    androidx.compose.ui.viewinterop.AndroidView(
        // Web mode: offset the whole WebView below the pinned bar (a WebView ignores its own top
        // padding for content layout). Reader mode uses CSS padding instead, so this is 0.
        modifier = Modifier.fillMaxSize().padding(top = contentTopPad),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    onScroll(scrollY)
                }
                // Match the reader background so there's no white flash while (re)loading.
                setBackgroundColor(pageBackground)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        if (holder.showingReader) {
                            // Lift the cover only once the reader HTML is actually ready to
                            // draw — postVisualStateCallback fires on the first paintable frame,
                            // avoiding a flash of the underlying raw page.
                            view.postVisualStateCallback(
                                0L,
                                object : WebView.VisualStateCallback() {
                                    override fun onComplete(requestId: Long) = onReaderShownChange(true)
                                },
                            )
                        } else {
                            // The raw page finished: make Readability available and extract.
                            view.evaluateJavascript(readabilityJs, null)
                            onPageReady()
                            onReaderShownChange(false)
                        }
                    }
                }
                holder.webView = this
            }
        },
        update = { webView ->
            when {
                readerMode && readerHtml != null -> {
                    // Key on the HTML itself so a theme/appearance change (new HTML) reloads.
                    if (holder.loadedKey != readerHtml) {
                        webView.loadDataWithBaseURL(url, readerHtml, "text/html", "utf-8", null)
                        holder.loadedKey = readerHtml
                        holder.showingReader = true
                    }
                }
                // web mode, or reader requested but not yet extracted: load the page either to
                // show it or to run Readability on its DOM.
                else -> {
                    if (holder.loadedKey != url) {
                        webView.loadUrl(url)
                        holder.loadedKey = url
                        holder.showingReader = false
                    }
                }
            }
        },
    )

    LaunchedEffect(readerMode, readerHtml, pageReady) {
        if (readerMode && readerHtml == null && pageReady) {
            val webView = holder.webView ?: return@LaunchedEffect
            webView.evaluateJavascript(EXTRACT_JS) { raw ->
                val parsed = parseExtraction(raw)
                if (parsed != null) onExtracted(parsed.first, parsed.second) else onExtractionFailed()
            }
        }
    }
}

private class WebViewHolder {
    var webView: WebView? = null
    var loadedKey: String? = null
    var showingReader = false
}

@Composable
private fun rememberReadabilityScript(): String {
    val context = LocalContext.current
    return remember {
        context.assets.open("readability.js").bufferedReader().use { it.readText() }
    }
}

// Bundled OFL reader fonts as base64 @font-face rules, so WebView reader and native reader
// typography stay comparable offline.
@Composable
private fun rememberReaderFontFaceCss(readerFont: ReaderFont): String {
    val context = LocalContext.current
    return remember(context, readerFont) {
        when (readerFont) {
            ReaderFont.SYSTEM_SANS -> ""
            ReaderFont.NEWSREADER -> buildAssetFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    AssetReaderFont("fonts/newsreader-400.woff2", weight = 400),
                    AssetReaderFont("fonts/newsreader-700.woff2", weight = 700),
                ),
                readBytes = { path -> context.assets.open(path).use { it.readBytes() } },
            )
            ReaderFont.LITERATA -> buildRawResourceFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    RawReaderFont(R.font.literata, weight = 400),
                    RawReaderFont(R.font.literata, weight = 700),
                    RawReaderFont(R.font.literata_italic, weight = 400, style = "italic"),
                    RawReaderFont(R.font.literata_italic, weight = 700, style = "italic"),
                ),
                readBytes = { resId -> context.resources.openRawResource(resId).use { it.readBytes() } },
            )
            ReaderFont.ATKINSON -> buildRawResourceFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    RawReaderFont(R.font.atkinson_hyperlegible_regular, weight = 400),
                    RawReaderFont(R.font.atkinson_hyperlegible_bold, weight = 700),
                    RawReaderFont(R.font.atkinson_hyperlegible_italic, weight = 400, style = "italic"),
                    RawReaderFont(R.font.atkinson_hyperlegible_bold_italic, weight = 700, style = "italic"),
                ),
                readBytes = { resId -> context.resources.openRawResource(resId).use { it.readBytes() } },
            )
        }
    }
}

private data class AssetReaderFont(
    val path: String,
    val weight: Int,
    val style: String = "normal",
)

private data class RawReaderFont(
    val resId: Int,
    val weight: Int,
    val style: String = "normal",
)

private fun buildAssetFontFaceCss(
    family: String,
    fonts: List<AssetReaderFont>,
    readBytes: (String) -> ByteArray,
): String = fonts.joinToString(separator = "") { font ->
    fontFaceCss(
        family = family,
        weight = font.weight,
        style = font.style,
        format = "woff2",
        bytes = readBytes(font.path),
    )
}

private fun buildRawResourceFontFaceCss(
    family: String,
    fonts: List<RawReaderFont>,
    readBytes: (Int) -> ByteArray,
): String = fonts.joinToString(separator = "") { font ->
    fontFaceCss(
        family = family,
        weight = font.weight,
        style = font.style,
        format = "truetype",
        bytes = readBytes(font.resId),
    )
}

private fun fontFaceCss(
    family: String,
    weight: Int,
    style: String,
    format: String,
    bytes: ByteArray,
): String {
    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    return "@font-face{font-family:'$family';font-style:$style;font-weight:$weight;" +
        "src:url(data:font/$format;base64,$base64) format('$format');font-display:swap;}"
}

private fun parseExtraction(raw: String?): Pair<String?, String>? {
    if (raw == null || raw == "null") return null
    val inner = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
    if (inner.isNullOrBlank()) return null
    val obj = runCatching { JSONObject(inner) }.getOrNull() ?: return null
    val content = obj.optString("c")
    if (content.isBlank()) return null
    return obj.optString("t").takeIf { it.isNotBlank() } to content
}

@Composable
private fun NativeReaderArticle(
    article: ReaderArticle,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    listState: LazyListState,
    topPad: androidx.compose.ui.unit.Dp,
    onScroll: (Int) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val link = remember(palette) { Color(android.graphics.Color.parseColor(palette.link)) }
    val rule = remember(palette) { Color(android.graphics.Color.parseColor(palette.rule)) }
    val codeBg = remember(palette) { Color(android.graphics.Color.parseColor(palette.codeBg)) }
    val readerFontFamily = readerFont.fontFamily

    LaunchedEffect(listState) {
        snapshotFlow { listState.readerScrollKey() }
            .collect(onScroll)
    }

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().background(background),
            contentPadding = PaddingValues(start = 24.dp, top = topPad, end = 24.dp, bottom = 72.dp),
        ) {
            item(key = "header") {
                ReaderMeasure(modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)) {
                    article.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = readerFontFamily,
                                fontSize = 34.sp,
                                lineHeight = 41.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = foreground,
                        )
                    }
                    article.source?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = readerFontFamily,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                            ),
                            color = muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            itemsIndexed(article.blocks, key = { index, _ -> "block-$index" }) { _, block ->
                ReaderMeasure {
                    ReaderBlockView(
                        block = block,
                        foreground = foreground,
                        muted = muted,
                        link = link,
                        rule = rule,
                        codeBg = codeBg,
                        readerFontFamily = readerFontFamily,
                        onOpenLink = onOpenLink,
                    )
                }
            }
        }
    }
}

private fun LazyListState.readerScrollKey(): Int =
    firstVisibleItemIndex * 100_000 + firstVisibleItemScrollOffset

@Composable
private fun ReaderMeasure(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().widthIn(max = 760.dp),
    ) {
        content()
    }
}

@Composable
private fun ReaderBlockView(
    block: ReaderBlock,
    foreground: Color,
    muted: Color,
    link: Color,
    rule: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
) {
    when (block) {
        is ReaderBlock.Heading -> ReaderText(
            text = readerAnnotatedString(block.text, foreground, link, codeBg, readerFontFamily),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 44.sp)
                2 -> MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp)
                else -> MaterialTheme.typography.titleMedium.copy(fontSize = 23.sp, lineHeight = 30.sp)
            }.copy(fontFamily = readerFontFamily, fontWeight = FontWeight.Bold),
            color = foreground,
            modifier = Modifier.padding(top = 32.dp, bottom = 12.dp),
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Paragraph -> ReaderText(
            text = readerAnnotatedString(block.text, foreground, link, codeBg, readerFontFamily),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = readerFontFamily,
                fontSize = 20.sp,
                lineHeight = 34.sp,
            ),
            color = foreground,
            modifier = Modifier.padding(bottom = 22.dp),
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Quote -> Row(
            Modifier
                .padding(top = 2.dp, bottom = 24.dp)
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(rule),
            )
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                block.blocks.forEach {
                    ReaderBlockView(
                        block = it,
                        foreground = muted,
                        muted = muted,
                        link = link,
                        rule = rule,
                        codeBg = codeBg,
                        readerFontFamily = readerFontFamily,
                        onOpenLink = onOpenLink,
                    )
                }
            }
        }
        is ReaderBlock.CodeBlock -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp,
            ),
            color = foreground,
            modifier = Modifier
                .padding(bottom = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(codeBg)
                .padding(12.dp),
        )
        is ReaderBlock.BulletedList -> ReaderList(
            items = block.items,
            ordered = false,
            foreground = foreground,
            link = link,
            codeBg = codeBg,
            readerFontFamily = readerFontFamily,
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.NumberedList -> ReaderList(
            items = block.items,
            ordered = true,
            foreground = foreground,
            link = link,
            codeBg = codeBg,
            readerFontFamily = readerFontFamily,
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Image -> AsyncImage(
            model = block.src,
            contentDescription = block.alt,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .padding(bottom = 28.dp)
                .fillMaxWidth(),
        )
        is ReaderBlock.Figure -> ReaderFigure(block, muted, link, codeBg, readerFontFamily, onOpenLink)
        ReaderBlock.Divider -> HorizontalDivider(
            color = rule,
            modifier = Modifier.padding(top = 10.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun ReaderFigure(
    figure: ReaderBlock.Figure,
    muted: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp, bottom = 28.dp)) {
        figure.images.forEachIndexed { index, image ->
            AsyncImage(
                model = image.src,
                contentDescription = image.alt,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(bottom = if (index == figure.images.lastIndex) 0.dp else 12.dp)
                    .fillMaxWidth()
                    .heightIn(min = 1.dp),
            )
        }
        figure.caption?.let { caption ->
            ReaderText(
                text = readerAnnotatedString(caption, muted, link, codeBg, readerFontFamily),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = readerFontFamily,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                ),
                color = muted,
                modifier = Modifier.padding(top = 8.dp),
                onOpenLink = onOpenLink,
            )
        }
    }
}

@Composable
private fun ReaderText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val linkModifier = modifier.pointerInput(text) {
        detectTapGestures { position ->
            val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
            val url = text.getStringAnnotations(tag = ReaderUrlAnnotation, start = offset, end = offset)
                .firstOrNull()
                ?.item
                ?: return@detectTapGestures
            onOpenLink(url)
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        modifier = linkModifier,
        onTextLayout = { layoutResult = it },
    )
}

private val NewsreaderFontFamily = FontFamily(
    Font(
        R.font.newsreader,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.newsreader,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.newsreader,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.italic(1f)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.italic(1f)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.italic(1f)),
    ),
)

private val LiterataFontFamily = FontFamily(
    Font(
        R.font.literata,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.literata,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.literata,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.italic(1f)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.italic(1f)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.italic(1f)),
    ),
)

private val AtkinsonHyperlegibleFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, weight = FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, weight = FontWeight.Bold),
    Font(R.font.atkinson_hyperlegible_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

private val ReaderFont.fontFamily: FontFamily
    get() = when (this) {
        ReaderFont.NEWSREADER -> NewsreaderFontFamily
        ReaderFont.LITERATA -> LiterataFontFamily
        ReaderFont.ATKINSON -> AtkinsonHyperlegibleFontFamily
        ReaderFont.SYSTEM_SANS -> FontFamily.SansSerif
    }

private val ReaderFont.cssFamily: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON -> "Atkinson Hyperlegible"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }

private val ReaderFont.cssStack: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "'Newsreader'"
        ReaderFont.LITERATA -> "'Literata'"
        ReaderFont.ATKINSON -> "'Atkinson Hyperlegible'"
        ReaderFont.SYSTEM_SANS -> "-apple-system, BlinkMacSystemFont, 'Roboto', 'Noto Sans', sans-serif"
    }

@Composable
private fun ReaderList(
    items: List<List<ReaderInline>>,
    ordered: Boolean,
    foreground: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (ordered) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = readerFontFamily, fontSize = 20.sp),
                    color = foreground,
                )
                ReaderText(
                    text = readerAnnotatedString(item, foreground, link, codeBg, readerFontFamily),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = readerFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                    ),
                    color = foreground,
                    modifier = Modifier.weight(1f),
                    onOpenLink = onOpenLink,
                )
            }
        }
    }
}

private fun readerAnnotatedString(
    inlines: List<ReaderInline>,
    foreground: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
): AnnotatedString = buildAnnotatedString {
    inlines.forEach { inline ->
        val style = SpanStyle(
            color = if (inline.href != null) link else foreground,
            fontFamily = if (inline.code) FontFamily.Monospace else readerFontFamily,
            fontSize = if (inline.code) 17.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
            fontWeight = if (inline.strong) FontWeight.Bold else null,
            fontStyle = if (inline.emphasis) FontStyle.Italic else null,
            background = Color.Unspecified,
            textDecoration = TextDecoration.None,
        )
        if (inline.href == null) {
            withStyle(style) { append(inline.text) }
        } else {
            pushStringAnnotation(tag = ReaderUrlAnnotation, annotation = inline.href)
            withStyle(style) { append(inline.text) }
            pop()
        }
    }
}

private const val ReaderUrlAnnotation = "reader-url"

@Composable
private fun TextPost(story: Story) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(story.title, style = MaterialTheme.typography.headlineSmall)
        if (!story.text.isNullOrBlank()) {
            Text(
                story.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun CommentRow(flat: FlatComment, nowSeconds: Long) {
    val indent = (flat.depth.coerceAtMost(6) * 12).dp
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp + indent, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = "${flat.comment.by}  ·  ${relativeTime(flat.comment.time, nowSeconds)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = flat.comment.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Instapaper-style swipe-up appearance panel: theme swatches and reader font choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSheet(
    current: ReaderTheme,
    currentFont: ReaderFont,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // System isn't offered explicitly: it's the default until a choice is made, then
                // the chosen theme sticks.
                listOf(ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK)
                    .forEach { theme ->
                        ThemeSwatch(
                            fill = swatchBrush(theme),
                            label = readerThemeLabel(theme),
                            selected = current == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTheme(theme) },
                        )
                    }
            }
            Text(
                text = "Font",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderFont.entries.forEach { font ->
                    FontChoiceRow(
                        font = font,
                        selected = currentFont == font,
                        onClick = { onSelectFont(font) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FontChoiceRow(
    font: ReaderFont,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = font.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = font.fontFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private val ReaderFont.label: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON -> "Atkinson Hyperlegible"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }

@Composable
private fun ThemeSwatch(
    fill: Brush,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(fill)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    )
}

private fun swatchBrush(theme: ReaderTheme): Brush = when (theme) {
    ReaderTheme.LIGHT -> SolidColor(Color(0xFFFDFDFB))
    ReaderTheme.SEPIA -> SolidColor(Color(0xFFF4ECD8))
    ReaderTheme.DARK -> SolidColor(Color(0xFF16161A))
    ReaderTheme.SYSTEM -> Brush.horizontalGradient(listOf(Color(0xFFFDFDFB), Color(0xFF16161A)))
}

@Composable
private fun readerThemeLabel(theme: ReaderTheme): String = stringResource(
    when (theme) {
        ReaderTheme.SYSTEM -> R.string.theme_system
        ReaderTheme.LIGHT -> R.string.theme_light
        ReaderTheme.DARK -> R.string.theme_dark
        ReaderTheme.SEPIA -> R.string.theme_sepia
    },
)

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
