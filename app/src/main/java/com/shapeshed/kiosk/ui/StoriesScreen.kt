package com.shapeshed.kiosk.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.Feed
import com.shapeshed.kiosk.data.Story
import com.shapeshed.kiosk.data.hostOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val FEEDS = Feed.entries

/**
 * Home screen: a left-aligned title bar that hides as you scroll (Google News-style), a scrollable
 * row of pill-shaped feed tabs bound to a [HorizontalPager] (swipe left/right or tap to change
 * feed), and the Gmail-style story list. The last-viewed feed is persisted and restored on launch;
 * each feed keeps its own paging state via a keyed [StoriesViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoriesScreen(
    selectedStoryId: Long?,
    onOpenStory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as KioskApp
    // Read the persisted feed once so the pager can start on the right page without a visible jump.
    val startFeed by produceState<Feed?>(initialValue = null) {
        value = app.settings.selectedFeed.first()
    }

    val feed = startFeed
    if (feed == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = FEEDS.indexOf(feed).coerceAtLeast(0),
    ) { FEEDS.size }
    val scope = rememberCoroutineScope()
    // The title bar slides away on scroll-down and returns on scroll-up; the tabs stay pinned.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Remember the feed the user settles on, so it reopens next launch.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            app.settings.setSelectedFeed(FEEDS[page])
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    scrollBehavior = scrollBehavior,
                )
                FeedTabs(
                    selectedIndex = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            key = { FEEDS[it].name },
            // Compose the neighbouring feed so it starts loading before the user swipes to it.
            beyondViewportPageCount = 1,
        ) { page ->
            FeedPane(
                feed = FEEDS[page],
                selectedStoryId = selectedStoryId,
                onOpenStory = onOpenStory,
            )
        }
    }
}

/**
 * M3 Primary tabs bound to the pager: Title Small labels with the primary pill bottom-indicator.
 * Scrollable so the active feed stays in view when the labels don't all fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Zero edge padding + the Tab's built-in 16dp text padding lands the first label at the
        // same 16dp start inset as the app-bar title, so "Top" aligns under "Kiosk".
        edgePadding = 0.dp,
    ) {
        FEEDS.forEachIndexed { index, feed ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        text = stringResource(feed.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
            )
        }
    }
}

/** One feed's story list, with its own [StoriesViewModel] keyed by feed so state survives swipes. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeedPane(
    feed: Feed,
    selectedStoryId: Long?,
    onOpenStory: (Long) -> Unit,
    viewModel: StoriesViewModel = viewModel(
        key = "feed-${feed.name}",
        factory = StoriesViewModel.factory(feed),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val viewedIds by viewModel.viewedIds.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error -> ErrorState(onRetry = viewModel::load)
            is UiState.Content -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                StoryList(
                    stories = current.data,
                    selectedStoryId = selectedStoryId,
                    viewedIds = viewedIds,
                    loadingMore = loadingMore,
                    onLoadMore = viewModel::loadMore,
                    onOpenStory = { id ->
                        viewModel.markViewed(id)
                        onOpenStory(id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryList(
    stories: List<Story>,
    selectedStoryId: Long?,
    viewedIds: Set<Long>,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenStory: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    // Load the next page once the last few rows come into view.
    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        itemsIndexed(stories, key = { _, story -> story.id }) { index, story ->
            // Gmail-style group: only the very first row is rounded, and only on top; the rest
            // are square and hairline-separated, so the list reads as one continuous block.
            StoryCard(
                story = story,
                selected = story.id == selectedStoryId,
                viewed = story.id in viewedIds,
                shape = if (index == 0) {
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                } else {
                    RectangleShape
                },
                onClick = { onOpenStory(story.id) },
            )
        }
        if (loadingMore) {
            item(key = "loading-more") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun StoryCard(
    story: Story,
    selected: Boolean,
    viewed: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { SourceAvatar(url = story.url, title = story.title) },
            headlineContent = {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.bodyLarge,
                    // Unread stories are bold, like Gmail; opened ones render normal weight.
                    fontWeight = if (viewed) FontWeight.Normal else FontWeight.Bold,
                    color = if (viewed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = hostOf(story.url)?.let { host ->
                {
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.couldnt_load))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@StringRes
private fun Feed.labelRes(): Int = when (this) {
    Feed.TOP -> R.string.feed_top
    Feed.NEW -> R.string.feed_new
    Feed.BEST -> R.string.feed_best
    Feed.ASK -> R.string.feed_ask
    Feed.SHOW -> R.string.feed_show
    Feed.JOBS -> R.string.feed_jobs
}
