package com.shapeshed.kiosk.ui

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val selectedDestination = navigator.currentDestination?.contentKey?.toArticleDestinationOrNull()
    val selectedId = selectedDestination?.storyId
    val settingsSelected = navigator.currentDestination?.contentKey == SettingsDestinationKey
    val showListSelection = LocalConfiguration.current.screenWidthDp >= 600
    val storyIdsByFeed = remember { mutableStateMapOf<Feed, List<Long>>() }
    var activeFeed by rememberSaveable { mutableStateOf<Feed?>(null) }
    val activeStoryIds = activeFeed?.let { storyIdsByFeed[it] }.orEmpty()
    val activeStoryIndex = selectedId?.let(activeStoryIds::indexOf) ?: -1
    val previousStoryInListId = activeStoryIds.getOrNull(activeStoryIndex - 1)
    val nextStoryInListId = activeStoryIds.getOrNull(activeStoryIndex + 1)

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                StoriesScreen(
                    selectedStoryId = selectedId?.takeIf { showListSelection },
                    settingsSelected = settingsSelected,
                    onFeedStoriesChange = { feed, storyIds ->
                        storyIdsByFeed[feed] = storyIds
                    },
                    onOpenStory = { feed, id, renderer ->
                        activeFeed = feed
                        scope.launch {
                            navigator.navigateTo(
                                ListDetailPaneScaffoldRole.Detail,
                                ArticleDestination(storyId = id, renderer = renderer).toNavigationKey(),
                            )
                        }
                    },
                    onOpenSettings = {
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, SettingsDestinationKey)
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val contentKey = navigator.currentDestination?.contentKey
                when {
                    contentKey == SettingsDestinationKey -> SettingsScreen(
                        showBack = navigator.canNavigateBack(),
                        onBack = { scope.launch { navigator.navigateBack() } },
                    )
                    else -> {
                        val destination = contentKey?.toArticleDestinationOrNull()
                        if (destination != null) {
                            ArticleScreen(
                                storyId = destination.storyId,
                                rendererOverride = destination.renderer,
                                storyIds = activeStoryIds,
                                previousStoryId = previousStoryInListId,
                                nextStoryId = nextStoryInListId,
                                onOpenAdjacentStory = {},
                                showBack = navigator.canNavigateBack(),
                                onBack = { scope.launch { navigator.navigateBack() } },
                            )
                        } else {
                            EmptyDetail()
                        }
                    }
                }
            }
        },
    )
}

data class ArticleDestination(
    val storyId: Long,
    val renderer: ArticleRendererOverride? = null,
)

enum class ArticleRendererOverride {
    WEB_PAGE,
    WEB_READER,
    NATIVE_READER,
}

private const val SettingsDestinationKey = "settings"

private fun ArticleDestination.toNavigationKey(): String =
    when (renderer) {
        ArticleRendererOverride.WEB_PAGE -> "$storyId:web-page"
        ArticleRendererOverride.WEB_READER -> "$storyId:web"
        ArticleRendererOverride.NATIVE_READER -> "$storyId:native"
        null -> storyId.toString()
    }

private fun String.toArticleDestinationOrNull(): ArticleDestination? {
    val storyId = substringBefore(':').toLongOrNull() ?: return null
    val renderer = when (substringAfter(':', missingDelimiterValue = "")) {
        "web-page" -> ArticleRendererOverride.WEB_PAGE
        "web" -> ArticleRendererOverride.WEB_READER
        "native" -> ArticleRendererOverride.NATIVE_READER
        else -> null
    }
    return ArticleDestination(storyId = storyId, renderer = renderer)
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
