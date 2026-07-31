package com.shapeshed.kiosk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.Feed
import kotlinx.coroutines.launch

/**
 * Adaptive list-detail layout: on a phone it's the stories list, tapping opens the article
 * full-screen (with back); on a tablet/foldable the list and article sit side by side. The
 * detail content key is the selected story id.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KioskListDetail() {
    val app = LocalContext.current.applicationContext as KioskApp
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val selectedDestination = navigator.currentDestination?.contentKey?.toArticleDestinationOrNull()
    val selectedId = selectedDestination?.storyId
    val showListSelection = LocalConfiguration.current.screenWidthDp >= 600
    val storyIdsByFeed = remember { mutableStateMapOf<Feed, List<Long>>() }
    var activeFeed by rememberSaveable { mutableStateOf<Feed?>(null) }
    var articleOpenedFromSearch by rememberSaveable { mutableStateOf(false) }
    // Owned here, not by StoriesScreen, so opening/closing an article can collapse/expand it
    // synchronously around the pane transition (see openStory/closeArticle below) instead of
    // signalling across composables and hoping the two animations happen to line up — that
    // indirection was exactly what caused a visible flash of the plain list before search
    // snapped back open.
    val searchBarState = rememberContainedSearchBarState()
    // True from the moment a search result is tapped until the navigator actually reflects the new
    // article (selectedId != null). Search now collapses BEFORE navigating (see openStory below), so
    // there's a real window where isSearchExpanded has already gone false but articleOpen hasn't yet
    // gone true — StoriesScreen's clear-on-collapse effect can't tell that apart from a genuine "user
    // exited search" collapse without this. Reset as soon as the navigation lands, not left sticky
    // like articleOpenedFromSearch, so a later genuine exit-search action still clears normally.
    var openingArticleFromSearch by remember { mutableStateOf(false) }
    val activeStoryIds = activeFeed?.let { storyIdsByFeed[it] }.orEmpty()
    val activeStoryIndex = selectedId?.let(activeStoryIds::indexOf) ?: -1
    val previousStoryInListId = activeStoryIds.getOrNull(activeStoryIndex - 1)
    val nextStoryInListId = activeStoryIds.getOrNull(activeStoryIndex + 1)

    LaunchedEffect(selectedId) {
        if (selectedId != null) openingArticleFromSearch = false
    }

    fun openStory(feed: Feed?, id: Long, forceNativeReader: Boolean) {
        articleOpenedFromSearch = feed == null
        if (feed == null) openingArticleFromSearch = true
        feed?.let { activeFeed = it }
        scope.launch {
            app.settings.markViewed(id)
            navigator.navigateTo(
                ListDetailPaneScaffoldRole.Detail,
                ArticleDestination(storyId = id, forceNativeReader = forceNativeReader).toNavigationKey(),
            )
        }
        // Collapse concurrently, NOT awaited before navigating: unlike closeArticle (where
        // navigateBack() reveals the list pane, so expand must finish first — see below),
        // navigateTo() covers the list pane immediately, so the collapse animation plays out
        // entirely hidden behind the now-opening article. Awaiting it first — as this used to —
        // meant the list pane briefly became visible while search visibly collapsed, before the
        // article appeared on top of it.
        if (feed == null) scope.launch { searchBarState.animateToCollapsed() }
    }
    fun closeArticle() {
        scope.launch {
            // Expand search BEFORE navigating back so it's already showing by the time the list
            // pane is revealed, rather than the plain list flashing first and search snapping
            // open a moment later.
            if (articleOpenedFromSearch) searchBarState.animateToExpanded()
            navigator.navigateBack()
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                StoriesScreen(
                    selectedStoryId = selectedId?.takeIf { showListSelection },
                    articleOpen = selectedId != null,
                    openingArticleFromSearch = openingArticleFromSearch,
                    searchBarState = searchBarState,
                    onFeedStoriesChange = { feed, storyIds ->
                        storyIdsByFeed[feed] = storyIds
                    },
                    onOpenStory = ::openStory,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val contentKey = navigator.currentDestination?.contentKey
                val destination = contentKey?.toArticleDestinationOrNull()
                if (destination != null) {
                    // NavigableListDetailPaneScaffold handles the system back gesture itself by
                    // calling navigator.navigateBack() directly, which bypasses the onBack lambda
                    // below (that only runs from the visible back-arrow tap) — so a back swipe never
                    // restored search. This nested BackHandler takes priority over the scaffold's own
                    // handling and runs the same close-article logic for the gesture too.
                    BackHandler(enabled = navigator.canNavigateBack(), onBack = ::closeArticle)
                    ArticleScreen(
                        storyId = destination.storyId,
                        forceNativeReader = destination.forceNativeReader,
                        storyIds = activeStoryIds,
                        previousStoryId = previousStoryInListId,
                        nextStoryId = nextStoryInListId,
                        showBack = navigator.canNavigateBack(),
                        onBack = ::closeArticle,
                        // Swiping to a different story leaves the original search-result context —
                        // back from wherever you end up should return to the plain list, not
                        // re-expand search. Without this, articleOpenedFromSearch stays stuck true
                        // for the rest of the detail-pane session since it's only ever set once, in
                        // openStory, and swiping never routes back through there.
                        onOpenAdjacentStory = { articleOpenedFromSearch = false },
                    )
                } else {
                    EmptyDetail()
                }
            }
        },
    )
}

data class ArticleDestination(
    val storyId: Long,
    val forceNativeReader: Boolean = false,
)

private fun ArticleDestination.toNavigationKey(): String =
    if (forceNativeReader) "$storyId:native" else storyId.toString()

private fun String.toArticleDestinationOrNull(): ArticleDestination? {
    val storyId = substringBefore(':').toLongOrNull() ?: return null
    val forceNativeReader = substringAfter(':', missingDelimiterValue = "") == "native"
    return ArticleDestination(storyId = storyId, forceNativeReader = forceNativeReader)
}

@Composable
private fun EmptyDetail() {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.select_story),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
