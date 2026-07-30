package com.shapeshed.kiosk.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.shapeshed.kiosk.MainActivity
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.FlatComment
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderTheme
import com.shapeshed.kiosk.data.ReaderArticle
import com.shapeshed.kiosk.data.ReaderBlock
import com.shapeshed.kiosk.data.ReaderExtractionEntity
import com.shapeshed.kiosk.data.ReaderInline
import com.shapeshed.kiosk.data.Story
import com.shapeshed.kiosk.data.hostOf
import com.shapeshed.kiosk.data.parseReaderArticle
import com.shapeshed.kiosk.data.relativeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.io.File
import kotlin.math.abs
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

// Clone the page, run Readability on it, and hand back {t: title, c: contentHtml} — or "" on failure.
private const val EXTRACT_JS =
    "(function(){try{var a=new Readability(document.cloneNode(true),{classesToPreserve:" +
        "['caption','emoji','hidden','invisible','sr-only','visually-hidden','visuallyhidden'," +
        "'wp-caption','wp-caption-text','wp-smiley']}).parse();" +
        "return a?JSON.stringify({t:a.title,c:a.content,x:a.textContent||''}):\"\";}catch(e){return \"\";}})();"

private const val ReaderTopGap = 24

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
    storyIds: List<Long> = emptyList(),
    previousStoryId: Long? = null,
    nextStoryId: Long? = null,
    onOpenAdjacentStory: (Long) -> Unit = {},
    showChrome: Boolean = true,
    enableStoryPager: Boolean = true,
    allowWebView: Boolean = true,
    onArticleScroll: (Int) -> Unit = {},
    externalReadAloudBlockIndex: Int? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as KioskApp
    var activeStoryId by rememberSaveable(storyId) { mutableLongStateOf(storyId) }
    val activeStoryIndex = storyIds.indexOf(activeStoryId)
    val effectivePreviousStoryId = if (activeStoryIndex >= 0) {
        storyIds.getOrNull(activeStoryIndex - 1)
    } else {
        previousStoryId
    }
    val effectiveNextStoryId = if (activeStoryIndex >= 0) {
        storyIds.getOrNull(activeStoryIndex + 1)
    } else {
        nextStoryId
    }
    val activeRendererOverride = if (activeStoryId == storyId) rendererOverride else null
    val viewModel: ArticleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "article-$activeStoryId",
        factory = remember(activeStoryId) { ArticleViewModel.factory(app, activeStoryId) },
    )
    val storyState by viewModel.story.collectAsStateWithLifecycle()
    val commentsState by viewModel.comments.collectAsStateWithLifecycle()
    val story = (storyState as? UiState.Content)?.data
    val storyUrl = story?.url
    val darkTheme = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val readerTheme by app.settings.readerTheme.collectAsStateWithLifecycle(ReaderTheme.SYSTEM)
    val readerFont by app.settings.readerFont.collectAsStateWithLifecycle(ReaderFont.NEWSREADER)
    val readAloudSpeechRate by app.settings.readAloudSpeechRate.collectAsStateWithLifecycle(1f)
    val readAloudVoiceName by app.settings.readAloudVoiceName.collectAsStateWithLifecycle(null)
    val speedReaderWordsPerMinute by app.settings.speedReaderWordsPerMinute.collectAsStateWithLifecycle(350)
    val speedReaderTheme by app.settings.speedReaderTheme.collectAsStateWithLifecycle(ReaderTheme.DARK)
    val speedReaderFont by app.settings.speedReaderFont.collectAsStateWithLifecycle(ReaderFont.ATKINSON)
    val palette = readerPaletteFor(readerTheme, darkTheme)
    val fontFaceCss = rememberReaderFontFaceCss(readerFont)
    // Matches the reader stylesheet background (see buildReaderHtml) so covers/flash are seamless.
    val pageBackground = android.graphics.Color.parseColor(palette.background)
    val webPageBackground = MaterialTheme.colorScheme.surface.toArgb()
    // Reader content sits below the overlay bar: pad the top by status-bar + app-bar height (CSS
    // px ≈ dp). The bar only hides after scrolling down, so by then this padding is off-screen.
    val density = LocalDensity.current
    // status-bar/notch height (dp) + app-bar (64dp) + a comfortable gap.
    val readerTopPad = (WindowInsets.statusBarsIgnoringVisibility.getTop(density) / density.density).toInt() + 88
    val webViewTopPad = (readerTopPad - ReaderTopGap).coerceAtLeast(0)

    // Kiosk is reader-first: WebView exists only as a hidden Readability extraction tool.
    var readerFailed by remember(activeStoryId) { mutableStateOf(false) }
    var selectedRendererOverride by remember(activeStoryId, activeRendererOverride) {
        mutableStateOf(activeRendererOverride)
    }
    val pdfArticleUrl = remember(story?.url) { story?.url?.renderablePdfUrlOrNull() }
    val isPdfArticle = pdfArticleUrl != null
    val forceWebArticle = remember(story?.url) { story?.url?.requiresWebView() == true }
    val readerMode = !isPdfArticle && !forceWebArticle && !readerFailed
    val forceNativeReader = selectedRendererOverride == ArticleRendererOverride.NATIVE_READER
    var extracted by remember(activeStoryId) { mutableStateOf<ReaderExtraction?>(null) }
    var pageReady by remember(activeStoryId) { mutableStateOf(false) }
    var readerShown by remember(activeStoryId) { mutableStateOf(false) }
    var openedLinkUrl by remember(activeStoryId) { mutableStateOf<String?>(null) }
    val nativeReaderListState = remember(activeStoryId) { LazyListState() }
    var showAppearance by remember { mutableStateOf(false) }
    var showReaderMenu by remember(activeStoryId) { mutableStateOf(false) }
    var showSpeedReader by rememberSaveable(activeStoryId) { mutableStateOf(false) }
    var pendingSpeedReader by remember(activeStoryId) { mutableStateOf(false) }
    var showReadAloud by rememberSaveable { mutableStateOf(false) }
    var readAloudAutoPlayKey by rememberSaveable { mutableLongStateOf(0L) }
    var pendingReadAloud by remember(activeStoryId) { mutableStateOf(false) }
    var currentReadAloudBlockIndex by remember(activeStoryId) { mutableStateOf<Int?>(null) }
    val readerCacheGeneration = ReaderExtractionCache.generation
    LaunchedEffect(storyUrl, readerCacheGeneration) {
        val memoryCached = storyUrl?.let { ReaderExtractionCache.get(activeStoryId, it) }
        val diskCached = if (memoryCached == null) {
            app.readerExtractions.get(activeStoryId)?.takeIf { it.url == storyUrl }?.toReaderExtraction()
        } else {
            null
        }
        val cached = memoryCached ?: diskCached
        if (cached != null) {
            if (diskCached != null) storyUrl?.let { ReaderExtractionCache.put(activeStoryId, it, diskCached) }
            extracted = cached
            pageReady = true
            readerShown = true
        }
    }
    // Rebuilt whenever the extracted article OR the palette/font changes, so an appearance change
    // re-themes the article you're currently reading (not just the next one).
    val readerHtml = remember(extracted, palette, fontFaceCss, readerFont, story?.url, readerTopPad) {
        extracted?.let { extraction ->
            buildReaderHtml(
                title = extraction.title,
                source = hostOf(story?.url),
                contentHtml = extraction.contentHtml,
                palette = palette,
                fontFaceCss = fontFaceCss,
                readerFontFamily = readerFont.cssStack,
                topPadPx = readerTopPad,
            )
        }
    }
    val readerArticle = remember(extracted, story?.url) {
        extracted?.let { extraction ->
            parseReaderArticle(
                title = extraction.title,
                source = hostOf(story?.url),
                contentHtml = extraction.contentHtml,
                baseUrl = story?.url,
            )
        }
    }
    val speedReadWords = remember(extracted) { extracted?.textContent?.speedReadWords().orEmpty() }
    val readAloudSegments = remember(readerArticle) { readerArticle?.readAloudSegments().orEmpty() }
    val readyNativeReaderArticle = if (readerMode && readerArticle?.blocks?.isNotEmpty() == true) {
        readerArticle
    } else {
        null
    }
    val nativeReaderReady = readyNativeReaderArticle != null
    // Instagram-style: the bar shows on launch, then slides up off-screen (and the app goes
    // immersive) when scrolling down, and slides back on scrolling up. The bar OVERLAYS the
    // content (not the scaffold's top slot), so animating it never relayouts the WebView.
    var barVisible by remember(activeStoryId) { mutableStateOf(true) }
    // The reader hides its bar on scroll (immersive); the web view keeps the bar pinned so the
    // page can sit padded below it with no gap.
    val immersiveEligible = !isPdfArticle && !forceWebArticle && !readerFailed
    val effectiveBarVisible = showChrome && !showSpeedReader && (!immersiveEligible || barVisible)
    // Previous WebView scroll position — tracked here because the View callback's oldScrollY is
    // unreliable for a WebView. Plain holder (no snapshot) so scroll events don't recompose.
    val lastScrollY = remember(activeStoryId) { intArrayOf(0) }

    // Comments are summoned from the top bar, not always on screen — so the reader stays clean.
    var showComments by remember(activeStoryId) { mutableStateOf(false) }
    // System bars follow the app bar: visible together, and hidden together (immersive) when the
    // user scrolls down — the native "immersive" API, tied to scroll direction like Instagram.
    val view = LocalView.current
    val activity = LocalActivity.current
    val insetsController = remember(view, activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(insetsController, showChrome) {
        if (showChrome) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (showChrome) insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(effectiveBarVisible, showChrome) {
        if (showChrome) {
            if (effectiveBarVisible) insetsController?.show(WindowInsetsCompat.Type.systemBars())
            else insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(nativeReaderReady) {
        if (nativeReaderReady) readerShown = true
    }
    LaunchedEffect(showChrome, activeStoryId, story?.id, story?.descendants, story?.kids) {
        val currentStory = story ?: return@LaunchedEffect
        if (!showChrome) return@LaunchedEffect
        val hasComments = currentStory.descendants > 0 || currentStory.kids.isNotEmpty()
        if (!hasComments) return@LaunchedEffect
        delay(750)
        viewModel.loadComments()
    }
    LaunchedEffect(pendingSpeedReader, speedReadWords) {
        if (pendingSpeedReader && speedReadWords.isNotEmpty()) {
            pendingSpeedReader = false
            showSpeedReader = true
            barVisible = false
        }
    }
    LaunchedEffect(pendingReadAloud, readAloudSegments) {
        if (pendingReadAloud && readAloudSegments.isNotEmpty()) {
            pendingReadAloud = false
            readAloudAutoPlayKey += 1
            showReadAloud = true
            barVisible = true
        }
    }
    LaunchedEffect(activeStoryId) {
        if (showReadAloud) {
            readAloudAutoPlayKey += 1
            currentReadAloudBlockIndex = null
        }
    }
    LaunchedEffect(forceWebArticle) {
        if (forceWebArticle) {
            pendingSpeedReader = false
            showSpeedReader = false
            pendingReadAloud = false
            showReadAloud = false
            currentReadAloudBlockIndex = null
        }
    }
    BackHandler(enabled = showChrome && showSpeedReader) {
        showSpeedReader = false
        barVisible = true
    }
    BackHandler(enabled = showChrome && showReadAloud) {
        showReadAloud = false
        currentReadAloudBlockIndex = null
        barVisible = true
        if (showBack) onBack()
    }
    BackHandler(enabled = showChrome && openedLinkUrl != null) {
        openedLinkUrl = null
        lastScrollY[0] = nativeReaderListState.readerScrollKey()
        barVisible = true
    }
    val linkedUrl = openedLinkUrl
    Box(modifier.fillMaxSize()) {
        val articleContent: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize().background(Color(pageBackground))) {
                when {
                storyState is UiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                story == null -> ErrorState(stringResource(R.string.couldnt_load), viewModel::loadStory)
                story.url == null -> TextPost(
                    story = story,
                    palette = palette,
                    readerFont = readerFont,
                    listState = nativeReaderListState,
                    topPad = readerTopPad.dp,
                    onScroll = { scrollY ->
                        val dy = scrollY - lastScrollY[0]
                        lastScrollY[0] = scrollY
                        if (dy > 12) barVisible = false
                        else if (dy < -12) barVisible = true
                        onArticleScroll(scrollY)
                    },
                )
                pdfArticleUrl != null -> ExternalArticleFallback(
                    story = story,
                    message = stringResource(R.string.pdf_external_summary),
                    buttonLabel = stringResource(R.string.open_in_pdf_reader),
                    topPad = readerTopPad.dp,
                    onOpenExternally = { openExternally(context, pdfArticleUrl) },
                )
                linkedUrl != null -> ExternalArticleFallback(
                    story = story.copy(url = linkedUrl),
                    message = stringResource(R.string.open_link_external_summary),
                    buttonLabel = stringResource(R.string.open_in_browser),
                    topPad = readerTopPad.dp,
                    onOpenExternally = { openExternally(context, linkedUrl) },
                )
                forceWebArticle -> ExternalArticleFallback(
                    story = story,
                    message = stringResource(R.string.external_app_summary),
                    buttonLabel = stringResource(R.string.open_in_browser),
                    topPad = readerTopPad.dp,
                    onOpenExternally = { openExternally(context, story.url) },
                )
                else -> {
                    if (readyNativeReaderArticle != null) {
                        NativeReaderArticle(
                            article = readyNativeReaderArticle,
                            palette = palette,
                            readerFont = readerFont,
                            listState = nativeReaderListState,
                            topPad = readerTopPad.dp,
                            activeReadAloudBlockIndex = externalReadAloudBlockIndex ?: currentReadAloudBlockIndex,
                            onScroll = { scrollY ->
                                val dy = scrollY - lastScrollY[0]
                                lastScrollY[0] = scrollY
                                if (dy > 12) barVisible = false
                                else if (dy < -12) barVisible = true
                                onArticleScroll(scrollY)
                            },
                            onOpenLink = { url ->
                                openExternally(context, url)
                            },
                        )
                    } else {
                        if (allowWebView && readerMode) {
                            Box(Modifier.size(1.dp).alpha(0f)) {
                                ArticleWebView(
                                    url = story.url,
                                    readerMode = true,
                                    readerHtml = null,
                                    pageReady = pageReady,
                                    pageBackground = android.graphics.Color.TRANSPARENT,
                                    contentTopPad = 0.dp,
                                    onPageReady = { pageReady = true },
                                    onReaderShownChange = {},
                                    onScroll = {},
                                    onExtracted = { extraction ->
                                        ReaderExtractionCache.put(activeStoryId, storyUrl, extraction)
                                        extracted = extraction
                                        scope.launch {
                                            app.readerExtractions.put(
                                                storyId = activeStoryId,
                                                url = storyUrl,
                                                title = extraction.title,
                                                contentHtml = extraction.contentHtml,
                                                textContent = extraction.textContent,
                                            )
                                        }
                                    },
                                    onExtractionFailed = {
                                        if (!showChrome) {
                                            return@ArticleWebView
                                        }
                                        if (extracted != null) return@ArticleWebView
                                        readerFailed = true
                                        pendingSpeedReader = false
                                        pendingReadAloud = false
                                        Toast.makeText(context, R.string.reader_unavailable, Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }
                        if (readerMode) {
                            WebArticlePlaceholder(
                                story = story,
                                pageBackground = pageBackground,
                                topPad = readerTopPad.dp,
                            )
                        } else {
                            ExternalArticleFallback(
                                story = story,
                                message = stringResource(R.string.reader_unavailable_external_summary),
                                buttonLabel = stringResource(R.string.open_in_browser),
                                topPad = readerTopPad.dp,
                                onOpenExternally = { openExternally(context, story.url) },
                            )
                        }
                    }
                }
            }
            }
        }

        if (
            showChrome &&
            enableStoryPager &&
            linkedUrl == null &&
            !showSpeedReader &&
            storyIds.isNotEmpty() &&
            activeStoryIndex >= 0 &&
            (effectivePreviousStoryId != null || effectiveNextStoryId != null)
        ) {
            val pagerState = rememberPagerState(initialPage = activeStoryIndex) { storyIds.size }
            var aligningPagerToStory by remember { mutableStateOf(false) }

            LaunchedEffect(storyId, activeStoryIndex) {
                aligningPagerToStory = true
                if (activeStoryIndex >= 0 && pagerState.currentPage != activeStoryIndex) {
                    pagerState.scrollToPage(activeStoryIndex)
                }
                aligningPagerToStory = false
            }
            LaunchedEffect(pagerState, storyIds, activeStoryId, activeStoryIndex) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    if (aligningPagerToStory) return@collect
                    val adjacentStoryId = storyIds.getOrNull(page)
                    if (adjacentStoryId != null && adjacentStoryId != activeStoryId) {
                        activeStoryId = adjacentStoryId
                        scope.launch { app.settings.markViewed(adjacentStoryId) }
                        onOpenAdjacentStory(adjacentStoryId)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                key = { storyIds[it] },
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            ) { page ->
                val articleStoryId = storyIds[page]
                ArticleScreen(
                    storyId = articleStoryId,
                    showBack = false,
                    onBack = {},
                    modifier = Modifier.fillMaxSize(),
                    rendererOverride = selectedRendererOverride,
                    storyIds = storyIds,
                    showChrome = false,
                    enableStoryPager = false,
                    allowWebView = articleStoryId == activeStoryId,
                    externalReadAloudBlockIndex = if (articleStoryId == activeStoryId) {
                        currentReadAloudBlockIndex
                    } else {
                        null
                    },
                    onArticleScroll = { scrollY ->
                        val dy = scrollY - lastScrollY[0]
                        lastScrollY[0] = scrollY
                        if (dy > 12) barVisible = false
                        else if (dy < -12) barVisible = true
                    },
                )
            }
        } else {
            articleContent()
        }

            // Overlay app bar (not the scaffold top slot): reveals on scroll-up, hides on
            // scroll-down, and never relayouts the WebView underneath.
            if (showChrome) AnimatedVisibility(
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
                            val commentCount = story?.descendants ?: 0
                            val hasComments = commentCount > 0 || story?.kids?.isNotEmpty() == true
                            if (hasComments) {
                                IconButton(onClick = {
                                    viewModel.loadComments()
                                    showComments = true
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Comment,
                                        stringResource(R.string.comments),
                                    )
                                }
                            }
                            if (story?.url != null) {
                                Box {
                                    IconButton(onClick = { showReaderMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, stringResource(R.string.reader_tools))
                                    }
                                    DropdownMenu(
                                        expanded = showReaderMenu,
                                        onDismissRequest = { showReaderMenu = false },
                                        shape = RoundedCornerShape(28.dp),
                                    ) {
                                        if (!isPdfArticle && !forceWebArticle) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.appearance)) },
                                                trailingIcon = { AppearanceGlyph() },
                                                onClick = {
                                                    showReaderMenu = false
                                                    showAppearance = true
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.speed_reader)) },
                                                onClick = {
                                                    showReaderMenu = false
                                                    if (speedReadWords.isNotEmpty()) {
                                                        showSpeedReader = true
                                                        barVisible = false
                                                    } else {
                                                        pendingSpeedReader = true
                                                        selectedRendererOverride = ArticleRendererOverride.NATIVE_READER
                                                        readerFailed = false
                                                        readerShown = false
                                                    }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.read_aloud)) },
                                                onClick = {
                                                    showReaderMenu = false
                                                    if (readAloudSegments.isNotEmpty()) {
                                                        readAloudAutoPlayKey += 1
                                                        showReadAloud = true
                                                        barVisible = true
                                                    } else {
                                                        pendingReadAloud = true
                                                        selectedRendererOverride = ArticleRendererOverride.NATIVE_READER
                                                        readerFailed = false
                                                        readerShown = false
                                                    }
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.share)) },
                                            trailingIcon = { Icon(Icons.Filled.Share, null) },
                                            onClick = {
                                                showReaderMenu = false
                                                shareArticle(context, story.title, story.url)
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(
                                                        if (isPdfArticle) {
                                                            R.string.open_in_pdf_reader
                                                        } else {
                                                            R.string.open_in_browser
                                                        },
                                                    ),
                                                )
                                            },
                                            trailingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                                            onClick = {
                                                showReaderMenu = false
                                                if (pdfArticleUrl != null) {
                                                    scope.launch {
                                                        val file = withContext(Dispatchers.IO) {
                                                            runCatching {
                                                                app.okHttpClient.downloadPdf(
                                                                    pdfArticleUrl,
                                                                    File(app.cacheDir, "pdf"),
                                                                )
                                                            }.getOrNull()
                                                        }
                                                        if (file != null) {
                                                            openPdfExternally(context, file)
                                                        } else {
                                                            openExternally(context, story.url)
                                                        }
                                                    }
                                                } else {
                                                    openExternally(context, story.url)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                )
            }

            if (showChrome && showReadAloud) {
                ReadAloudControls(
                    title = readerArticle?.title ?: story?.title,
                    source = readerArticle?.source ?: hostOf(story?.url),
                    imageUrl = readerArticle?.firstImageUrlOrNull(),
                    segments = readAloudSegments,
                    autoPlayKey = readAloudAutoPlayKey,
                    canSkipToPreviousArticle = effectivePreviousStoryId != null,
                    canSkipToNextArticle = effectiveNextStoryId != null,
                    speechRate = readAloudSpeechRate,
                    selectedVoiceName = readAloudVoiceName,
                    onSpeechRateChange = { rate ->
                        scope.launch { app.settings.setReadAloudSpeechRate(rate) }
                    },
                    onVoiceNameChange = { voiceName ->
                        scope.launch { app.settings.setReadAloudVoiceName(voiceName) }
                    },
                    onCurrentBlockChange = { currentReadAloudBlockIndex = it },
                    onSkipToPreviousArticle = {
                        effectivePreviousStoryId?.let { adjacentStoryId ->
                            activeStoryId = adjacentStoryId
                            scope.launch { app.settings.markViewed(adjacentStoryId) }
                            onOpenAdjacentStory(adjacentStoryId)
                        }
                    },
                    onSkipToNextArticle = {
                        effectiveNextStoryId?.let { adjacentStoryId ->
                            activeStoryId = adjacentStoryId
                            scope.launch { app.settings.markViewed(adjacentStoryId) }
                            onOpenAdjacentStory(adjacentStoryId)
                        }
                    },
                    onDismiss = {
                        showReadAloud = false
                        currentReadAloudBlockIndex = null
                        barVisible = true
                    },
                    backgroundColor = Color(pageBackground),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }

            if (showChrome && showSpeedReader) {
                SpeedReaderOverlay(
                    words = speedReadWords,
                    palette = readerPaletteFor(speedReaderTheme, darkTheme),
                    readerFont = speedReaderFont,
                    wordsPerMinute = speedReaderWordsPerMinute,
                    currentTheme = speedReaderTheme,
                    onWordsPerMinuteChange = { wpm ->
                        scope.launch { app.settings.setSpeedReaderWordsPerMinute(wpm) }
                    },
                    onSelectTheme = { theme ->
                        scope.launch { app.settings.setSpeedReaderTheme(theme) }
                    },
                    onSelectFont = { font ->
                        scope.launch { app.settings.setSpeedReaderFont(font) }
                    },
                    onDismiss = {
                        showSpeedReader = false
                        barVisible = true
                    },
                )
            }

            // Summoned from the top bar: a modal sheet over the article, dismissed to return
            // straight to distraction-free reading. The reader itself carries no comments chrome.
            if (showChrome && showComments) {
                CommentsModalSheet(
                    commentCount = story?.descendants ?: 0,
                    state = commentsState,
                    onDismiss = { showComments = false },
                )
            }
    }

    if (showChrome && showAppearance) {
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
fun ReaderExtractionPreloader(
    stories: List<Story>,
    modifier: Modifier = Modifier,
    limit: Int = 10,
) {
    val context = LocalContext.current
    val app = context.applicationContext as KioskApp
    val scope = rememberCoroutineScope()
    val candidates = remember(stories, limit) {
        stories.take(limit).filter { story ->
            val url = story.url ?: return@filter false
            !url.requiresWebView() && url.renderablePdfUrlOrNull() == null
        }
    }
    val candidateKey = remember(candidates) {
        candidates.joinToString(separator = "|") { "${it.id}:${it.url}" }
    }
    var queue by remember { mutableStateOf<List<Story>>(emptyList()) }
    var current by remember { mutableStateOf<Story?>(null) }
    var pageReady by remember(current?.id) { mutableStateOf(false) }

    LaunchedEffect(candidateKey) {
        queue = candidates.filter { story ->
            val url = story.url ?: return@filter false
            ReaderExtractionCache.get(story.id, url) == null &&
                app.readerExtractions.get(story.id)?.url != url
        }
        current = null
    }

    LaunchedEffect(queue, current) {
        if (current == null && queue.isNotEmpty()) {
            current = queue.first()
            queue = queue.drop(1)
        }
    }

    val story = current ?: return
    val url = story.url ?: return
    Box(modifier.size(1.dp).alpha(0f)) {
        ArticleWebView(
            url = url,
            readerMode = true,
            readerHtml = null,
            pageReady = pageReady,
            pageBackground = android.graphics.Color.TRANSPARENT,
            contentTopPad = 0.dp,
            onPageReady = { pageReady = true },
            onReaderShownChange = {},
            onScroll = {},
            onExtracted = { extraction ->
                ReaderExtractionCache.put(story.id, url, extraction)
                current = null
                scope.launch {
                    app.readerExtractions.put(
                        storyId = story.id,
                        url = url,
                        title = extraction.title,
                        contentHtml = extraction.contentHtml,
                        textContent = extraction.textContent,
                    )
                }
            },
            onExtractionFailed = {
                current = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WebArticlePlaceholder(
    story: Story,
    pageBackground: Int,
    topPad: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val host = hostOf(story.url) ?: story.url.orEmpty()
    Box(
        modifier
            .fillMaxSize()
            .background(Color(pageBackground))
            .padding(top = topPad),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SourceAvatar(url = story.url, title = story.title, size = 56.dp)
            Text(
                text = story.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (host.isNotBlank()) {
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LoadingIndicator(Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ExternalArticleFallback(
    story: Story,
    message: String,
    buttonLabel: String,
    topPad: androidx.compose.ui.unit.Dp,
    onOpenExternally: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = hostOf(story.url) ?: story.url.orEmpty()
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(top = topPad)
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SourceAvatar(url = story.url, title = story.title, size = 56.dp)
        Text(
            text = story.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (host.isNotBlank()) {
            Text(
                text = host,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onOpenExternally,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(buttonLabel)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArticleWebView(
    url: String,
    readerMode: Boolean,
    readerHtml: String?,
    pageReady: Boolean,
    pageBackground: Int,
    contentTopPad: androidx.compose.ui.unit.Dp,
    loadingStory: Story? = null,
    onPageReady: () -> Unit,
    onReaderShownChange: (Boolean) -> Unit,
    onScroll: (scrollY: Int) -> Unit,
    onExtracted: (ReaderExtraction) -> Unit,
    onExtractionFailed: () -> Unit,
) {
    val readabilityJs = rememberReadabilityScript()
    val holder = remember { WebViewHolder() }
    val latestOnScroll by androidx.compose.runtime.rememberUpdatedState(onScroll)
    val chromeBackground = MaterialTheme.colorScheme.surface
    val contentKey = readerHtml.takeIf { readerMode } ?: url
    var webViewVisible by remember(contentKey) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(chromeBackground)) {
        androidx.compose.ui.viewinterop.AndroidView(
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
                            if (holder.showingReader) {
                                view.postVisualStateCallback(
                                    0L,
                                    object : WebView.VisualStateCallback() {
                                        override fun onComplete(requestId: Long) {
                                            if (holder.destroyed || holder.webView !== view) return
                                            webViewVisible = true
                                            onReaderShownChange(true)
                                        }
                                    },
                                )
                            } else {
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
                                                onReaderShownChange(false)
                                            }
                                        },
                                    )
                                }
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
                            webViewVisible = false
                            webView.loadDataWithBaseURL(url, readerHtml, "text/html", "utf-8", null)
                            holder.loadedKey = readerHtml
                            holder.showingReader = true
                        }
                    }
                    // web mode, or reader requested but not yet extracted: load the page either to
                    // show it or to run Readability on its DOM.
                    else -> {
                        if (holder.loadedKey != url) {
                            webViewVisible = false
                            webView.loadUrl(url)
                            holder.loadedKey = url
                            holder.showingReader = false
                        }
                    }
                }
            },
        )

        if (!webViewVisible) {
            if (loadingStory != null) {
                WebArticlePlaceholder(
                    story = loadingStory,
                    pageBackground = pageBackground,
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

    LaunchedEffect(readerMode, readerHtml, pageReady) {
        if (readerMode && readerHtml == null && pageReady) {
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
            holder.webView?.apply {
                webViewClient = WebViewClient()
                webChromeClient = null
                onPause()
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            holder.webView = null
        }
    }
}

private class WebViewHolder {
    var webView: WebView? = null
    var loadedKey: String? = null
    var showingReader = false
    var destroyed = false
}

private data class ReaderExtraction(
    val title: String?,
    val contentHtml: String,
    val textContent: String,
)

private object ReaderExtractionCache {
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

private data class CachedReaderExtraction(
    val url: String,
    val extraction: ReaderExtraction,
)

private fun ReaderExtractionEntity.toReaderExtraction(): ReaderExtraction =
    ReaderExtraction(
        title = title,
        contentHtml = contentHtml,
        textContent = textContent,
    )

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
    )
}

@Composable
private fun NativeReaderArticle(
    article: ReaderArticle,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    listState: LazyListState,
    topPad: androidx.compose.ui.unit.Dp,
    activeReadAloudBlockIndex: Int?,
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
    val latestOnScroll by androidx.compose.runtime.rememberUpdatedState(onScroll)
    var expandedImage by remember(article) { mutableStateOf<ReaderImagePreview?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.readerScrollKey() }
            .collect { latestOnScroll(it) }
    }
    LaunchedEffect(activeReadAloudBlockIndex) {
        activeReadAloudBlockIndex?.let { blockIndex ->
            listState.animateScrollToItem(index = blockIndex + 1, scrollOffset = 48)
        }
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
            itemsIndexed(article.blocks, key = { index, _ -> "block-$index" }) { index, block ->
                val active = activeReadAloudBlockIndex == index
                ReaderMeasure(
                    modifier = if (active) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    } else {
                        Modifier
                    },
                ) {
                    ReaderBlockView(
                        block = block,
                        foreground = foreground,
                        muted = muted,
                        link = link,
                        rule = rule,
                        codeBg = codeBg,
                        readerFontFamily = readerFontFamily,
                        onOpenLink = onOpenLink,
                        onOpenImage = { src, alt -> expandedImage = ReaderImagePreview(src, alt) },
                    )
                }
            }
        }
    }
    expandedImage?.let { image ->
        ZoomableImageOverlay(
            image = image,
            background = background,
            onDismiss = { expandedImage = null },
        )
    }
}

private data class ReaderImagePreview(
    val src: String,
    val alt: String?,
)

private fun LazyListState.readerScrollKey(): Int =
    firstVisibleItemIndex * 100_000 + firstVisibleItemScrollOffset

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReadAloudControls(
    title: String?,
    source: String?,
    imageUrl: String?,
    segments: List<ReadAloudSegment>,
    autoPlayKey: Long,
    canSkipToPreviousArticle: Boolean,
    canSkipToNextArticle: Boolean,
    speechRate: Float,
    selectedVoiceName: String?,
    onSpeechRateChange: (Float) -> Unit,
    onVoiceNameChange: (String?) -> Unit,
    onCurrentBlockChange: (Int?) -> Unit,
    onSkipToPreviousArticle: () -> Unit,
    onSkipToNextArticle: () -> Unit,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val app = appContext as? KioskApp
    val activity = LocalActivity.current
    val window = activity?.window
    val shouldRead = remember { AtomicBoolean(false) }
    val hasAudioFocus = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val audioManager = remember(appContext) { appContext.getSystemService(AudioManager::class.java) }
    val readAloudAudioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPlaying by rememberSaveable(segments) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(segments) { mutableIntStateOf(0) }
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    var draftSpeechRate by rememberSaveable(speechRate) {
        mutableFloatStateOf(speechRate.nearestReadAloudSpeechRate())
    }
    var segmentStartElapsedMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var voiceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaSession by remember { mutableStateOf<MediaSession?>(null) }
    var mediaArtwork by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    val latestSpeechRate by androidx.compose.runtime.rememberUpdatedState(speechRate)
    val latestSelectedVoiceName by androidx.compose.runtime.rememberUpdatedState(selectedVoiceName)
    val latestAvailableVoices by androidx.compose.runtime.rememberUpdatedState(availableVoices)
    val audioFocusRequest = remember(audioManager) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(readAloudAudioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { focusChange ->
                if (
                    focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    mainHandler.post {
                        shouldRead.set(false)
                        tts?.stop()
                        isPlaying = false
                        onCurrentBlockChange(null)
                        hasAudioFocus.set(false)
                    }
                }
            }
            .build()
    }

    fun requestReadAloudAudioFocus(): Boolean {
        if (hasAudioFocus.get()) return true
        val granted = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus.set(granted)
        return granted
    }

    fun releaseReadAloudAudioFocus() {
        if (hasAudioFocus.getAndSet(false)) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(appContext, segments) {
        ready = false
        error = null
        isPlaying = false
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(appContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    val languageStatus = engine.setLanguage(Locale.getDefault())
                    if (
                        languageStatus == TextToSpeech.LANG_MISSING_DATA ||
                        languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        error = appContext.getString(R.string.read_aloud_language_unavailable)
                    } else {
                        engine.setAudioAttributes(readAloudAudioAttributes)
                        val voices = engine.readAloudVoices()
                        availableVoices = voices
                        latestSelectedVoiceName?.let { voiceName ->
                            voices.firstOrNull { it.name == voiceName }?.let(engine::setVoice)
                        }
                        engine.setSpeechRate(latestSpeechRate)
                        ready = true
                    }
                } else {
                    error = appContext.getString(R.string.read_aloud_unavailable)
                }
            }
        }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val index = utteranceId?.readAloudIndexOrNull() ?: return
                    mainHandler.post {
                        currentIndex = index
                        segmentStartElapsedMillis = SystemClock.elapsedRealtime()
                        onCurrentBlockChange(segments.getOrNull(index)?.blockIndex)
                        isPlaying = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val nextIndex = (utteranceId?.readAloudIndexOrNull() ?: return) + 1
                    mainHandler.post {
                        if (shouldRead.get() && nextIndex < segments.size) {
                            currentIndex = nextIndex
                            onCurrentBlockChange(segments[nextIndex].blockIndex)
                            engine.speakReadAloudSegment(
                                text = segments[nextIndex].text,
                                index = nextIndex,
                                speechRate = latestSpeechRate,
                                voice = latestSelectedVoiceName?.let { voiceName ->
                                    latestAvailableVoices.firstOrNull { it.name == voiceName }
                                },
                            )
                        } else {
                            shouldRead.set(false)
                            isPlaying = false
                            onCurrentBlockChange(null)
                            releaseReadAloudAudioFocus()
                        }
                    }
                }

                @Deprecated("Required by framework callback; onError(String, Int) handles modern engines.")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        shouldRead.set(false)
                        isPlaying = false
                        onCurrentBlockChange(null)
                        releaseReadAloudAudioFocus()
                        error = appContext.getString(R.string.read_aloud_unavailable)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post {
                        shouldRead.set(false)
                        isPlaying = false
                        onCurrentBlockChange(null)
                        releaseReadAloudAudioFocus()
                        error = appContext.getString(R.string.read_aloud_unavailable)
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (interrupted) {
                        mainHandler.post {
                            isPlaying = false
                            releaseReadAloudAudioFocus()
                        }
                    }
                }
            },
        )
        tts = engine

        onDispose {
            shouldRead.set(false)
            engine.stop()
            engine.shutdown()
            releaseReadAloudAudioFocus()
            onCurrentBlockChange(null)
            tts = null
        }
    }

    LaunchedEffect(autoPlayKey, ready, segments) {
        val engine = tts
        if (autoPlayKey > 0L && ready && segments.isNotEmpty() && engine != null) {
            engine.stop()
            if (requestReadAloudAudioFocus()) {
                shouldRead.set(true)
                currentIndex = 0
                segmentStartElapsedMillis = SystemClock.elapsedRealtime()
                onCurrentBlockChange(segments[0].blockIndex)
                engine.speakReadAloudSegment(
                    text = segments[0].text,
                    index = 0,
                    speechRate = speechRate,
                    voice = selectedVoiceName?.let { voiceName -> availableVoices.firstOrNull { it.name == voiceName } },
                )
            } else {
                shouldRead.set(false)
                isPlaying = false
                error = appContext.getString(R.string.read_aloud_unavailable)
            }
        }
    }

    LaunchedEffect(ready, speechRate, selectedVoiceName, availableVoices) {
        val engine = tts ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        if (selectedVoiceName == null) {
            engine.setLanguage(Locale.getDefault())
        } else {
            availableVoices.firstOrNull { it.name == selectedVoiceName }?.let(engine::setVoice)
        }
        engine.setSpeechRate(speechRate)
    }

    val selectedVoice = selectedVoiceName?.let { voiceName ->
        availableVoices.firstOrNull { it.name == voiceName }
    }
    val selectedVoiceLabel = selectedVoice?.readAloudLabel() ?: stringResource(R.string.system_voice)
    val displayTitle = title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.read_aloud)
    val displaySource = source?.takeIf { it.isNotBlank() } ?: when {
        error != null -> error.orEmpty()
        segments.isEmpty() -> stringResource(R.string.read_aloud_empty)
        !ready -> stringResource(R.string.read_aloud_starting)
        else -> stringResource(
            R.string.read_aloud_progress,
            (currentIndex + 1).coerceAtMost(segments.size),
            segments.size,
        )
    }
    fun speakSegment(engine: TextToSpeech, index: Int) {
        if (segments.isEmpty()) return
        if (!requestReadAloudAudioFocus()) {
            shouldRead.set(false)
            isPlaying = false
            error = appContext.getString(R.string.read_aloud_unavailable)
            return
        }
        val safeIndex = index.coerceIn(0, segments.lastIndex)
        currentIndex = safeIndex
        segmentStartElapsedMillis = SystemClock.elapsedRealtime()
        shouldRead.set(true)
        onCurrentBlockChange(segments[safeIndex].blockIndex)
        engine.speakReadAloudSegment(
            text = segments[safeIndex].text,
            index = safeIndex,
            speechRate = draftSpeechRate,
            voice = selectedVoice,
        )
    }
    fun speakCurrentSegment(engine: TextToSpeech) {
        speakSegment(engine, currentIndex)
    }
    fun moveToSegment(index: Int) {
        if (segments.isEmpty()) return
        val safeIndex = index.coerceIn(0, segments.lastIndex)
        currentIndex = safeIndex
        onCurrentBlockChange(segments[safeIndex].blockIndex)
        val engine = tts
        if (engine != null && isPlaying) {
            speakSegment(engine, safeIndex)
        }
    }
    fun togglePlayback() {
        val engine = tts ?: return
        if (isPlaying) {
            shouldRead.set(false)
            engine.stop()
            isPlaying = false
            releaseReadAloudAudioFocus()
        } else {
            speakCurrentSegment(engine)
        }
    }
    fun playPlayback() {
        if (!isPlaying) {
            tts?.let(::speakCurrentSegment)
        }
    }
    fun pausePlayback() {
        if (isPlaying) {
            shouldRead.set(false)
            tts?.stop()
            isPlaying = false
            releaseReadAloudAudioFocus()
        }
    }
    val latestPlayPlayback by androidx.compose.runtime.rememberUpdatedState(::playPlayback)
    val latestPausePlayback by androidx.compose.runtime.rememberUpdatedState(::pausePlayback)
    val latestSkipToPreviousArticle by androidx.compose.runtime.rememberUpdatedState(onSkipToPreviousArticle)
    val latestSkipToNextArticle by androidx.compose.runtime.rememberUpdatedState(onSkipToNextArticle)
    val latestSeekToParagraph by androidx.compose.runtime.rememberUpdatedState { position: Long ->
        moveToSegment(segments.segmentIndexForPosition(position))
    }
    val latestDismiss by androidx.compose.runtime.rememberUpdatedState {
        shouldRead.set(false)
        tts?.stop()
        releaseReadAloudAudioFocus()
        onDismiss()
    }

    LaunchedEffect(app, imageUrl) {
        mediaArtwork = null
        val client = app?.okHttpClient ?: return@LaunchedEffect
        val url = imageUrl ?: return@LaunchedEffect
        mediaArtwork = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body.byteStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.scaledForMediaMetadata()
                    }
                }
            }.getOrNull()
        }
    }

    DisposableEffect(appContext) {
        val launchIntent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val session = MediaSession(appContext, "KioskReadAloud").apply {
            setSessionActivity(contentIntent)
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() {
                        latestPlayPlayback()
                    }

                    override fun onPause() {
                        latestPausePlayback()
                    }

                    override fun onStop() {
                        latestDismiss()
                    }

                    override fun onSkipToPrevious() {
                        latestSkipToPreviousArticle()
                    }

                    override fun onSkipToNext() {
                        latestSkipToNextArticle()
                    }

                    override fun onSeekTo(pos: Long) {
                        latestSeekToParagraph(pos)
                    }
                },
            )
            isActive = true
        }
        mediaSession = session
        onDispose {
            session.isActive = false
            session.release()
            mediaSession = null
            appContext.readAloudNotificationManager().cancel(ReadAloudNotificationId)
        }
    }

    LaunchedEffect(
        appContext,
        mediaSession,
        displayTitle,
        displaySource,
        mediaArtwork,
        isPlaying,
        ready,
        error,
    ) {
        val session = mediaSession ?: return@LaunchedEffect
        appContext.notifyReadAloudMediaNotification(
            appContext.buildReadAloudNotification(
                mediaSession = session,
                title = displayTitle,
                source = displaySource,
                artwork = mediaArtwork,
                isPlaying = isPlaying,
            ),
        )
    }

    LaunchedEffect(mediaSession, displayTitle, displaySource, mediaArtwork, segments) {
        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, displayTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, displaySource)
                .putLong(
                    MediaMetadata.METADATA_KEY_DURATION,
                    segments.estimatedDurationMillis(),
                )
                .apply {
                    mediaArtwork?.let { artwork ->
                        putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
                        putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                    }
                }
                .build(),
        )
    }

    LaunchedEffect(mediaSession, isPlaying, ready, error, currentIndex, segments, segmentStartElapsedMillis) {
        val actions = (
            PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SEEK_TO
            ) or
            (if (canSkipToPreviousArticle) PlaybackState.ACTION_SKIP_TO_PREVIOUS else 0L) or
            (if (canSkipToNextArticle) PlaybackState.ACTION_SKIP_TO_NEXT else 0L)
        val segmentBasePosition = segments.positionForSegment(currentIndex)
        val elapsedInSegment = if (isPlaying) {
            SystemClock.elapsedRealtime() - segmentStartElapsedMillis
        } else {
            0L
        }
        val progressPosition = (segmentBasePosition + elapsedInSegment).coerceIn(
            0L,
            segments.estimatedDurationMillis(),
        )
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    when {
                        !ready || error != null -> PlaybackState.STATE_NONE
                        isPlaying -> PlaybackState.STATE_PLAYING
                        else -> PlaybackState.STATE_PAUSED
                    },
                    progressPosition,
                    if (isPlaying) 1f else 0f,
                )
                .build(),
        )
    }

    val miniPlayerDismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { distance -> distance * 0.35f },
    )
    val miniPlayerProgress = if (segments.isEmpty()) {
        0f
    } else {
        ((currentIndex + 1).coerceAtMost(segments.size).toFloat() / segments.size.toFloat())
            .coerceIn(0f, 1f)
    }
    val animatedPlayerProgress by animateFloatAsState(
        targetValue = miniPlayerProgress,
        animationSpec = tween(500),
        label = "readAloudProgress",
    )
    SwipeToDismissBox(
        state = miniPlayerDismissState,
        backgroundContent = { Box(Modifier.fillMaxSize().background(backgroundColor)) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        onDismiss = {
            latestDismiss()
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { animatedPlayerProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showPlayerSheet = true }
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = displaySource,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex > 0,
                        onClick = { moveToSegment(currentIndex - 1) },
                    ) {
                        Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous_paragraph))
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable(enabled = ready && segments.isNotEmpty() && error == null) {
                                togglePlayback()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex < segments.lastIndex,
                        onClick = { moveToSegment(currentIndex + 1) },
                    ) {
                        Icon(Icons.Filled.SkipNext, stringResource(R.string.next_paragraph))
                    }
                }
            }
        }
    }

    if (showPlayerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(104.dp),
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayTitle.firstOrNull()?.uppercase().orEmpty(),
                                    style = MaterialTheme.typography.displaySmall,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = displaySource,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (ready && error == null && segments.isNotEmpty()) {
                    val playerProgressModifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(WavyProgressIndicatorDefaults.LinearContainerHeight)
                    LinearWavyProgressIndicator(
                        progress = { animatedPlayerProgress },
                        amplitude = { if (isPlaying) 1f else 0f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = playerProgressModifier,
                    )
                    Text(
                        text = stringResource(
                            R.string.read_aloud_progress,
                            (currentIndex + 1).coerceAtMost(segments.size),
                            segments.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        text = segments.getOrNull(currentIndex)?.text.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text(
                        text = displaySource,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex > 0,
                        onClick = { moveToSegment(currentIndex - 1) },
                    ) {
                        Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous_paragraph))
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable(enabled = ready && segments.isNotEmpty() && error == null) { togglePlayback() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex < segments.lastIndex,
                        onClick = { moveToSegment(currentIndex + 1) },
                    ) {
                        Icon(Icons.Filled.SkipNext, stringResource(R.string.next_paragraph))
                    }
                }

                Text(
                    text = stringResource(R.string.read_aloud_speed, draftSpeechRate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Slider(
                    value = draftSpeechRate,
                    onValueChange = { value -> draftSpeechRate = value.nearestReadAloudSpeechRate() },
                    onValueChangeFinished = {
                        onSpeechRateChange(draftSpeechRate)
                        val engine = tts
                        if (engine != null) {
                            engine.setSpeechRate(draftSpeechRate)
                            if (isPlaying) speakCurrentSegment(engine)
                        }
                    },
                    valueRange = ReadAloudSpeechRates.first()..ReadAloudSpeechRates.last(),
                    steps = ReadAloudSpeechRates.size - 2,
                )

                Box {
                    TextButton(
                        enabled = ready && segments.isNotEmpty() && error == null,
                        onClick = { voiceMenuExpanded = true },
                    ) {
                        Text(stringResource(R.string.read_aloud_voice, selectedVoiceLabel))
                    }
                    DropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false },
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.system_voice)) },
                            onClick = {
                                voiceMenuExpanded = false
                                onVoiceNameChange(null)
                                val engine = tts
                                if (engine != null) {
                                    engine.setLanguage(Locale.getDefault())
                                    engine.setSpeechRate(draftSpeechRate)
                                    if (isPlaying) speakCurrentSegment(engine)
                                }
                            },
                        )
                        availableVoices.take(8).forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.readAloudLabel()) },
                                onClick = {
                                    voiceMenuExpanded = false
                                    onVoiceNameChange(voice.name)
                                    val engine = tts
                                    if (engine != null) {
                                        engine.setVoice(voice)
                                        engine.setSpeechRate(draftSpeechRate)
                                        if (isPlaying) speakCurrentSegment(engine)
                                    }
                                },
                            )
                        }
                        if (availableVoices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.read_aloud_no_voices)) },
                                onClick = { voiceMenuExpanded = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TextToSpeech.speakReadAloudSegment(
    text: String,
    index: Int,
    speechRate: Float,
    voice: Voice?,
) {
    voice?.let(::setVoice)
    setSpeechRate(speechRate)
    speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), readAloudUtteranceId(index))
}

private fun TextToSpeech.readAloudVoices(): List<Voice> =
    voices
        ?.filter { voice -> !voice.isNetworkConnectionRequired && voice.locale.language == Locale.getDefault().language }
        ?.sortedWith(compareBy<Voice> { it.locale.displayLanguage }.thenBy { it.name })
        .orEmpty()

private fun Context.buildReadAloudNotification(
    mediaSession: MediaSession,
    title: String,
    source: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
): Notification {
    ensureReadAloudNotificationChannel()
    val launchIntent = Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val contentIntent = PendingIntent.getActivity(
        this,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return Notification.Builder(this, ReadAloudNotificationChannelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(source)
        .setSubText(getString(R.string.read_aloud))
        .setContentIntent(contentIntent)
        .setCategory(Notification.CATEGORY_TRANSPORT)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setOngoing(isPlaying)
        .setDefaults(0)
        .apply {
            artwork?.let(::setLargeIcon)
        }
        .setStyle(
            Notification.MediaStyle()
                .setMediaSession(mediaSession.sessionToken),
        )
        .build()
}

private fun Context.ensureReadAloudNotificationChannel() {
    val manager = readAloudNotificationManager()
    if (manager.getNotificationChannel(ReadAloudNotificationChannelId) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            ReadAloudNotificationChannelId,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        },
    )
}

private fun Context.readAloudNotificationManager(): NotificationManager =
    getSystemService(NotificationManager::class.java)

@SuppressLint("NotificationPermission")
private fun Context.notifyReadAloudMediaNotification(notification: Notification) {
    // Android 13+ exempts media-session notifications from POST_NOTIFICATIONS. Lint cannot infer
    // that this notification is MediaStyle and linked to a live MediaSession.
    readAloudNotificationManager().notify(ReadAloudNotificationId, notification)
}

private fun Bitmap.scaledForMediaMetadata(maxSize: Int = 512): Bitmap {
    val longestSide = width.coerceAtLeast(height)
    if (longestSide <= maxSize) return this
    val scale = maxSize.toFloat() / longestSide.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private const val ReadAloudNotificationChannelId = "read_aloud"
private const val ReadAloudNotificationId = 9001
private const val ReadAloudBaseWordsPerMinute = 180f
private const val ReadAloudMinimumSegmentMillis = 900L
private val ReadAloudSpeechRates = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun Float.nearestReadAloudSpeechRate(): Float =
    ReadAloudSpeechRates.minBy { kotlin.math.abs(it - this) }

private fun Voice.readAloudLabel(): String =
    locale.getDisplayName(Locale.getDefault()).ifBlank { name }

private fun readAloudUtteranceId(index: Int): String = "read-aloud-$index"

private fun String.readAloudIndexOrNull(): Int? =
    removePrefix("read-aloud-").toIntOrNull()

private data class ReadAloudSegment(
    val text: String,
    val blockIndex: Int,
) {
    val estimatedDurationMillis: Long =
        ((text.readAloudWordCount().toFloat() / ReadAloudBaseWordsPerMinute) * 60_000f)
            .toLong()
            .coerceAtLeast(ReadAloudMinimumSegmentMillis)
}

private fun String.readAloudWordCount(): Int =
    trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }
        .coerceAtLeast(1)

private fun ReaderArticle.readAloudSegments(): List<ReadAloudSegment> =
    blocks.flatMapIndexed { blockIndex, block ->
        block.readAloudParagraphs()
            .flatMap { paragraph -> paragraph.chunkForTextToSpeech() }
            .map { text -> ReadAloudSegment(text = text, blockIndex = blockIndex) }
    }

private fun List<ReadAloudSegment>.estimatedDurationMillis(): Long =
    sumOf { it.estimatedDurationMillis }.coerceAtLeast(ReadAloudMinimumSegmentMillis)

private fun List<ReadAloudSegment>.positionForSegment(index: Int): Long =
    take(index.coerceIn(0, size)).sumOf { it.estimatedDurationMillis }

private fun List<ReadAloudSegment>.segmentIndexForPosition(positionMillis: Long): Int {
    if (isEmpty()) return 0
    var elapsed = 0L
    forEachIndexed { index, segment ->
        elapsed += segment.estimatedDurationMillis
        if (positionMillis < elapsed) return index
    }
    return lastIndex
}

private fun ReaderArticle.firstImageUrlOrNull(): String? =
    blocks.firstNotNullOfOrNull { block ->
        when (block) {
            is ReaderBlock.Image -> block.src
            is ReaderBlock.Figure -> block.images.firstOrNull()?.src
            else -> null
        }
    }

private fun ReaderBlock.readAloudParagraphs(): List<String> =
    when (this) {
        is ReaderBlock.Heading -> listOf(text.plainText())
        is ReaderBlock.Paragraph -> listOf(text.plainText())
        is ReaderBlock.Quote -> blocks.flatMap { it.readAloudParagraphs() }
        is ReaderBlock.CodeBlock -> emptyList()
        is ReaderBlock.BulletedList -> items.map { it.plainText() }
        is ReaderBlock.NumberedList -> items.map { it.plainText() }
        is ReaderBlock.Image -> emptyList()
        is ReaderBlock.Figure -> listOfNotNull(caption?.plainText())
        ReaderBlock.Divider -> emptyList()
    }

private fun List<ReaderInline>.plainText(): String =
    joinToString(separator = "") { it.text }.replace(Regex("\\s+"), " ").trim()

private fun String.chunkForTextToSpeech(): List<String> {
    val maxLength = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3_500)
    if (length <= maxLength) return listOf(this)
    return chunked(maxLength).map { it.trim() }.filter { it.isNotEmpty() }
}

private const val SpeedReaderInitialPauseMillis = 300L
private const val SpeedReaderMinWordsPerMinute = 150
private const val SpeedReaderMaxWordsPerMinute = 800
private const val SpeedReaderWordsPerMinuteStep = 25

@Composable
private fun SpeedReaderOverlay(
    words: List<String>,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    wordsPerMinute: Int,
    currentTheme: ReaderTheme,
    onWordsPerMinuteChange: (Int) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val readerFontFamily = readerFont.fontFamily
    var wordIndex by rememberSaveable(words) { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val latestWordsPerMinute by androidx.compose.runtime.rememberUpdatedState(
        wordsPerMinute.coerceIn(SpeedReaderMinWordsPerMinute, SpeedReaderMaxWordsPerMinute),
    )
    val progress = if (words.isEmpty()) {
        0f
    } else {
        ((wordIndex + 1).coerceAtMost(words.size).toFloat() / words.size.toFloat()).coerceIn(0f, 1f)
    }
    val window = LocalActivity.current?.window

    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(words) {
        wordIndex = 0
        delay(SpeedReaderInitialPauseMillis)
        while (wordIndex < words.lastIndex) {
            delay(60_000L / latestWordsPerMinute.coerceAtLeast(1))
            wordIndex += 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.speed_reader_settings),
                    tint = muted,
                )
            }
        }
        val currentWord = words.getOrNull(wordIndex).orEmpty()
        Text(
            text = currentWord,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = readerFontFamily,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50.dp)),
            color = foreground.copy(alpha = 0.58f),
            trackColor = muted.copy(alpha = 0.24f),
        )
    }

    if (showSettings) {
        SpeedReaderSettingsSheet(
            wordsPerMinute = wordsPerMinute,
            currentTheme = currentTheme,
            currentFont = readerFont,
            onWordsPerMinuteChange = onWordsPerMinuteChange,
            onSelectTheme = onSelectTheme,
            onSelectFont = onSelectFont,
            onDismiss = { showSettings = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedReaderSettingsSheet(
    wordsPerMinute: Int,
    currentTheme: ReaderTheme,
    currentFont: ReaderFont,
    onWordsPerMinuteChange: (Int) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember(wordsPerMinute) { mutableFloatStateOf(wordsPerMinute.toFloat()) }
    val snappedWordsPerMinute = sliderValue
        .roundToNearest(SpeedReaderWordsPerMinuteStep)
        .toInt()
        .coerceIn(SpeedReaderMinWordsPerMinute, SpeedReaderMaxWordsPerMinute)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.speed_reader_settings),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.speed_reader_speed_value, snappedWordsPerMinute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                    onWordsPerMinuteChange(value.roundToNearest(SpeedReaderWordsPerMinuteStep).toInt())
                },
                valueRange = SpeedReaderMinWordsPerMinute.toFloat()..SpeedReaderMaxWordsPerMinute.toFloat(),
                steps = ((SpeedReaderMaxWordsPerMinute - SpeedReaderMinWordsPerMinute) / SpeedReaderWordsPerMinuteStep) - 1,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.speed_reader_slow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.speed_reader_fast),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.speed_reader_colors),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(ReaderTheme.SYSTEM, ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK)
                    .forEach { theme ->
                        ThemeSwatch(
                            fill = swatchBrush(theme),
                            label = readerThemeLabel(theme),
                            selected = currentTheme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTheme(theme) },
                        )
                    }
            }
            Text(
                text = stringResource(R.string.reader_font),
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

private fun Float.roundToNearest(step: Int): Float =
    (kotlin.math.round(this / step.toFloat()) * step).coerceIn(
        SpeedReaderMinWordsPerMinute.toFloat(),
        SpeedReaderMaxWordsPerMinute.toFloat(),
    )

@Composable
private fun AppearanceGlyph(modifier: Modifier = Modifier) {
    Text(
        text = "Aa",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private fun String.speedReadWords(): List<String> =
    this
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

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
    onOpenImage: (src: String, alt: String?) -> Unit,
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
                        onOpenImage = onOpenImage,
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
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenImage(block.src, block.alt) },
        )
        is ReaderBlock.Figure -> ReaderFigure(block, muted, link, codeBg, readerFontFamily, onOpenLink, onOpenImage)
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
    onOpenImage: (src: String, alt: String?) -> Unit,
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
                    .heightIn(min = 1.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenImage(image.src, image.alt) },
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
private fun ZoomableImageOverlay(
    image: ReaderImagePreview,
    background: Color,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scale = remember(image.src) { Animatable(1f) }
    var offsetX by remember(image.src) { mutableFloatStateOf(0f) }
    var offsetY by remember(image.src) { mutableFloatStateOf(0f) }

    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(background.copy(alpha = 0.98f))
            .pointerInput(image.src) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale.value * zoom).coerceIn(1f, 5f)
                    scope.launch { scale.snapTo(nextScale) }
                    if (nextScale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
            .pointerInput(image.src) {
                detectTapGestures(
                    onDoubleTap = {
                        offsetX = 0f
                        offsetY = 0f
                        scope.launch {
                            scale.animateTo(
                                targetValue = if (scale.value > 1f) 1f else 2.5f,
                                animationSpec = tween(180),
                            )
                        }
                    },
                    onTap = { onDismiss() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = image.src,
            contentDescription = image.alt,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offsetX
                    translationY = offsetY
                },
        )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PdfArticle(
    url: String,
    title: String,
    topPad: androidx.compose.ui.unit.Dp,
    onOpenExternally: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as KioskApp
    val pdfState by produceState<UiState<File>>(initialValue = UiState.Loading, url) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.okHttpClient.downloadPdf(url, File(app.cacheDir, "pdf")) }
                .fold(
                    onSuccess = { UiState.Content(it) },
                    onFailure = { UiState.Error(it) },
                )
        }
    }

    when (val state = pdfState) {
        UiState.Loading -> Box(Modifier.fillMaxSize().padding(top = topPad), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        is UiState.Error -> PdfError(title = title, topPad = topPad, onOpenExternally = onOpenExternally)
        is UiState.Content -> PdfDocument(
            file = state.data,
            title = title,
            topPad = topPad,
            onOpenExternally = onOpenExternally,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PdfDocument(
    file: File,
    title: String,
    topPad: androidx.compose.ui.unit.Dp,
    onOpenExternally: () -> Unit,
) {
    val pageCountState by produceState<UiState<Int>>(initialValue = UiState.Loading, file) {
        value = withContext(Dispatchers.IO) {
            runCatching { file.pdfPageCount() }
                .fold(
                    onSuccess = { UiState.Content(it) },
                    onFailure = { UiState.Error(it) },
                )
        }
    }

    when (val state = pageCountState) {
        UiState.Loading -> Box(Modifier.fillMaxSize().padding(top = topPad), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        is UiState.Error -> PdfError(title = title, topPad = topPad, onOpenExternally = onOpenExternally)
        is UiState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPad + 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.data, key = { page -> "pdf-page-$page" }) { page ->
                PdfPage(file = file, pageIndex = page)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PdfPage(
    file: File,
    pageIndex: Int,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val pageState by produceState<UiState<Bitmap>>(initialValue = UiState.Loading, file, pageIndex, widthPx) {
            value = withContext(Dispatchers.IO) {
                runCatching { file.renderPdfPage(pageIndex, widthPx) }
                    .fold(
                        onSuccess = { UiState.Content(it) },
                        onFailure = { UiState.Error(it) },
                    )
            }
        }
        when (val state = pageState) {
            UiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
            is UiState.Error -> Text(
                text = stringResource(R.string.pdf_page_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            is UiState.Content -> Image(
                bitmap = state.data.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PdfError(
    title: String,
    topPad: androidx.compose.ui.unit.Dp,
    onOpenExternally: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPad)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.pdf_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = onOpenExternally,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.open_pdf))
        }
    }
}

private fun String.renderablePdfUrlOrNull(): String? {
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

private fun String.requiresWebView(): Boolean {
    val host = runCatching { java.net.URI(this).host.orEmpty() }.getOrDefault("")
        .removePrefix("www.")
        .removePrefix("mobile.")
        .removePrefix("m.")
    return host == "twitter.com" ||
        host == "x.com" ||
        host == "youtube.com" ||
        host == "youtu.be"
}

private fun OkHttpClient.downloadPdf(url: String, directory: File): File {
    directory.mkdirs()
    val target = File(directory, "${url.hashCode().toUInt()}.pdf")
    if (target.exists() && target.hasPdfMagicHeader()) return target
    if (target.exists()) target.delete()
    newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) error("Could not download PDF: ${response.code}")
        val body = response.body
        val contentType = body.contentType()
        val mimeLooksPdf = contentType?.type == "application" && contentType.subtype.equals("pdf", ignoreCase = true)
        target.outputStream().use { output ->
            BufferedInputStream(body.byteStream()).use { input ->
                val header = ByteArray(PdfMagicHeader.size)
                val bytesRead = input.read(header)
                val bodyLooksPdf = bytesRead == PdfMagicHeader.size && header.contentEquals(PdfMagicHeader)
                if (!mimeLooksPdf && !bodyLooksPdf) error("Response is not a PDF")
                output.write(header, 0, bytesRead.coerceAtLeast(0))
                input.copyTo(output)
            }
        }
    }
    return target
}

private val PdfMagicHeader = "%PDF-".encodeToByteArray()

private fun File.hasPdfMagicHeader(): Boolean =
    runCatching {
        inputStream().use { input ->
            val header = ByteArray(PdfMagicHeader.size)
            input.read(header) == PdfMagicHeader.size && header.contentEquals(PdfMagicHeader)
        }
    }.getOrDefault(false)

private fun File.pdfPageCount(): Int =
    ParcelFileDescriptor.open(this, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }

private fun File.renderPdfPage(pageIndex: Int, widthPx: Int): Bitmap =
    ParcelFileDescriptor.open(this, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(pageIndex).use { page ->
                val heightPx = (widthPx.toFloat() * page.height.toFloat() / page.width.toFloat())
                    .toInt()
                    .coerceAtLeast(1)
                Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }

@Composable
private fun TextPost(
    story: Story,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    listState: LazyListState,
    topPad: androidx.compose.ui.unit.Dp,
    onScroll: (Int) -> Unit,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val readerFontFamily = readerFont.fontFamily
    val latestOnScroll by androidx.compose.runtime.rememberUpdatedState(onScroll)
    LaunchedEffect(listState) {
        snapshotFlow { listState.readerScrollKey() }
            .collect { latestOnScroll(it) }
    }

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(background),
            contentPadding = PaddingValues(start = 24.dp, top = topPad, end = 24.dp, bottom = 72.dp),
        ) {
            item(key = "text-post") {
                ReaderMeasure(modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = readerFontFamily,
                            fontSize = 34.sp,
                            lineHeight = 41.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = foreground,
                    )
                    Text(
                        text = story.by,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = readerFontFamily,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        color = muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    story.text?.takeIf { it.isNotBlank() }?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = readerFontFamily,
                                fontSize = 20.sp,
                                lineHeight = 34.sp,
                            ),
                            color = foreground,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
            }
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
                listOf(ReaderTheme.SYSTEM, ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK)
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
    ReaderTheme.SYSTEM -> Brush.horizontalGradient(
        0f to Color(0xFFFDFDFB),
        0.5f to Color(0xFFFDFDFB),
        0.5f to Color(0xFF16161A),
        1f to Color(0xFF16161A),
    )
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
