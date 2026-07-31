package com.shapeshed.kiosk.ui

import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderTheme
import com.shapeshed.kiosk.data.Story
import com.shapeshed.kiosk.data.hostOf
import com.shapeshed.kiosk.data.parseReaderArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    forceNativeReader: Boolean = false,
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
    val activeForceNativeReader = activeStoryId == storyId && forceNativeReader
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
    val pageBackground = android.graphics.Color.parseColor(palette.background)
    // Reader content sits below the overlay bar: pad the top by status-bar + app-bar height (CSS
    // px ≈ dp). The bar only hides after scrolling down, so by then this padding is off-screen.
    val density = LocalDensity.current
    // status-bar/notch height (dp) + app-bar (64dp) + a comfortable gap.
    val readerTopPad = (WindowInsets.statusBarsIgnoringVisibility.getTop(density) / density.density).toInt() + 88

    // Kiosk is reader-first: WebView exists only as a hidden Readability extraction tool. Failure is
    // shared (see ReaderExtractionFailures) since only the outermost instance ever runs it, but every
    // recursive per-page instance needs to see the failure to fall back instead of waiting forever.
    val readerFailureGeneration = ReaderExtractionFailures.generation
    val readerFailed = ReaderExtractionFailures.isFailed(activeStoryId)
    var selectedForceNativeReader by remember(activeStoryId, activeForceNativeReader) {
        mutableStateOf(activeForceNativeReader)
    }
    val pdfArticleUrl = remember(story?.url) { story?.url?.pdfUrlOrNull() }
    val isPdfArticle = pdfArticleUrl != null
    val forceExternalArticle = remember(story?.url) { story?.url?.requiresExternalApp() == true }
    val readerMode = !isPdfArticle && !forceExternalArticle && !readerFailed
    var extracted by remember(activeStoryId) { mutableStateOf<ReaderExtraction?>(null) }
    var pageReady by remember(activeStoryId) { mutableStateOf(false) }
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
    // Instagram-style: the bar shows on launch, then slides up off-screen (and the app goes
    // immersive) when scrolling down, and slides back on scrolling up. The bar OVERLAYS the
    // content (not the scaffold's top slot), so animating it never relayouts the WebView.
    var barVisible by remember(activeStoryId) { mutableStateOf(true) }
    // The reader hides its bar on scroll (immersive); the web view keeps the bar pinned so the
    // page can sit padded below it with no gap.
    val immersiveEligible = !isPdfArticle && !forceExternalArticle && !readerFailed
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
            showReadAloud = false
            currentReadAloudBlockIndex = null
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
    LaunchedEffect(forceExternalArticle) {
        if (forceExternalArticle) {
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
                forceExternalArticle -> ExternalArticleFallback(
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
                        // Extraction itself runs in a single sibling WebView outside this pager page
                        // (see below) — this branch only ever displays the placeholder while it waits.
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

        // Single extraction WebView for whichever story is active, kept OUTSIDE the story-to-story
        // pager's composition entirely (not per-page) — its AndroidView is never part of the pager's
        // beyond-bounds subcomposition, which is what raced with pager measurement and crashed
        // on-device. Swiping just retargets this same instance to the new URL (see ArticleWebView's
        // update block); every page (including this one) picks up the result via
        // ReaderExtractionCache, watched above. Only the outermost, chrome-showing ArticleScreen
        // instance ever has allowWebView = true — recursive per-page instances never extract.
        //
        // story.id == activeStoryId guards against a real race: right after activeStoryId changes
        // (swipe settles), story (from the newly-keyed ArticleViewModel) lags behind by a
        // recomposition or two and briefly still holds the PREVIOUS story's data. Without this
        // check, the WebView below would load that stale URL, then get redirected moments later when
        // story catches up — and the abandoned load's onPageFinished callback races with the new
        // navigation, making extraction run against a blank mid-navigation page and spuriously fail.
        if (
            allowWebView && readerMode && readyNativeReaderArticle == null &&
            story != null && story.url != null && story.id == activeStoryId
        ) {
            Box(Modifier.fillMaxSize()) {
                ArticleWebView(
                    url = story.url,
                    pageReady = pageReady,
                    pageBackground = android.graphics.Color.TRANSPARENT,
                    contentTopPad = 0.dp,
                    onPageReady = { pageReady = true },
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
                        if (extracted != null) return@ArticleWebView
                        ReaderExtractionFailures.markFailed(activeStoryId)
                        pendingSpeedReader = false
                        pendingReadAloud = false
                        Toast.makeText(context, R.string.reader_unavailable, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        // Preload the next story's extraction in a second, independent headless WebView — also kept
        // outside the pager, same as the active one above, so it can't hit the pager-measurement race
        // either. It fetches its own story data directly (not through ArticleViewModel/
        // collectAsStateWithLifecycle, which is what lagged behind activeStoryId above) since all it
        // needs is a URL, and nextPreload is remember(nextStoryId)-keyed so it resets cleanly if the
        // user swipes past before it finishes.
        val nextStoryId = effectiveNextStoryId
        var nextPreload by remember(nextStoryId) { mutableStateOf<Pair<Long, String>?>(null) }
        LaunchedEffect(nextStoryId) {
            val id = nextStoryId ?: return@LaunchedEffect
            val url = app.repository.story(id)?.url
            if (url != null) nextPreload = id to url
        }
        var nextPageReady by remember(nextStoryId) { mutableStateOf(false) }
        val nextPreloadUrl = nextPreload?.takeIf { it.first == nextStoryId }?.second
        val nextCacheGeneration = ReaderExtractionCache.generation
        val nextAlreadyExtracted = nextStoryId != null && nextPreloadUrl != null &&
            ReaderExtractionCache.get(nextStoryId, nextPreloadUrl) != null
        val nextFailureGeneration = ReaderExtractionFailures.generation
        val nextFailed = nextStoryId != null && ReaderExtractionFailures.isFailed(nextStoryId)
        val nextPdfUrl = remember(nextPreloadUrl) { nextPreloadUrl?.pdfUrlOrNull() }
        val nextRequiresExternal = remember(nextPreloadUrl) { nextPreloadUrl?.requiresExternalApp() == true }
        if (
            allowWebView && nextStoryId != null && nextPreloadUrl != null &&
            nextPdfUrl == null && !nextRequiresExternal && !nextFailed && !nextAlreadyExtracted
        ) {
            Box(Modifier.fillMaxSize()) {
                ArticleWebView(
                    url = nextPreloadUrl,
                    pageReady = nextPageReady,
                    pageBackground = android.graphics.Color.TRANSPARENT,
                    contentTopPad = 0.dp,
                    onPageReady = { nextPageReady = true },
                    onScroll = {},
                    onExtracted = { extraction ->
                        ReaderExtractionCache.put(nextStoryId, nextPreloadUrl, extraction)
                        scope.launch {
                            app.readerExtractions.put(
                                storyId = nextStoryId,
                                url = nextPreloadUrl,
                                title = extraction.title,
                                contentHtml = extraction.contentHtml,
                                textContent = extraction.textContent,
                            )
                        }
                    },
                    onExtractionFailed = { ReaderExtractionFailures.markFailed(nextStoryId) },
                )
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
                    forceNativeReader = selectedForceNativeReader,
                    storyIds = storyIds,
                    showChrome = false,
                    enableStoryPager = false,
                    // Recursive per-page instances never run their own extraction WebView — the
                    // outer instance's single sibling WebView (outside this pager entirely) handles
                    // it for whichever story is active. Any WebView living inside a pager page raced
                    // with the pager's beyond-bounds subcomposition and crashed on-device.
                    allowWebView = false,
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
                                        if (!isPdfArticle && !forceExternalArticle) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.appearance)) },
                                                trailingIcon = { AppearanceGlyph() },
                                                onClick = {
                                                    showReaderMenu = false
                                                    showAppearance = true
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.read_aloud)) },
                                                trailingIcon = { Icon(Icons.Filled.PlayArrow, null) },
                                                onClick = {
                                                    showReaderMenu = false
                                                    if (readAloudSegments.isNotEmpty()) {
                                                        readAloudAutoPlayKey += 1
                                                        showReadAloud = true
                                                        barVisible = true
                                                    } else {
                                                        pendingReadAloud = true
                                                        selectedForceNativeReader = true
                                                        ReaderExtractionFailures.clear(activeStoryId)
                                                    }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.speed_reader)) },
                                                trailingIcon = { Icon(Icons.AutoMirrored.Filled.Article, null) },
                                                onClick = {
                                                    showReaderMenu = false
                                                    if (speedReadWords.isNotEmpty()) {
                                                        showReadAloud = false
                                                        currentReadAloudBlockIndex = null
                                                        showSpeedReader = true
                                                        barVisible = false
                                                    } else {
                                                        showReadAloud = false
                                                        currentReadAloudBlockIndex = null
                                                        pendingSpeedReader = true
                                                        selectedForceNativeReader = true
                                                        ReaderExtractionFailures.clear(activeStoryId)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WebArticlePlaceholder(
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
