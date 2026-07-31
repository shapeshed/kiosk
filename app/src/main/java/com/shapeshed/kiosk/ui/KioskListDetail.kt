package com.shapeshed.kiosk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    var restoreSearchRequest by rememberSaveable { mutableIntStateOf(0) }
    val activeStoryIds = activeFeed?.let { storyIdsByFeed[it] }.orEmpty()
    val activeStoryIndex = selectedId?.let(activeStoryIds::indexOf) ?: -1
    val previousStoryInListId = activeStoryIds.getOrNull(activeStoryIndex - 1)
    val nextStoryInListId = activeStoryIds.getOrNull(activeStoryIndex + 1)
    fun openStory(feed: Feed?, id: Long, forceNativeReader: Boolean) {
        articleOpenedFromSearch = feed == null
        feed?.let { activeFeed = it }
        scope.launch {
            app.settings.markViewed(id)
            navigator.navigateTo(
                ListDetailPaneScaffoldRole.Detail,
                ArticleDestination(storyId = id, forceNativeReader = forceNativeReader).toNavigationKey(),
            )
        }
    }
    fun closeArticle() {
        scope.launch {
            val shouldRestoreSearch = articleOpenedFromSearch
            navigator.navigateBack()
            if (shouldRestoreSearch) restoreSearchRequest += 1
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                StoriesScreen(
                    selectedStoryId = selectedId?.takeIf { showListSelection },
                    articleOpen = selectedId != null,
                    restoreSearchRequest = restoreSearchRequest,
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
