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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shapeshed.kiosk.R
import kotlinx.coroutines.launch

/**
 * Adaptive list-detail layout: on a phone it's the stories list, tapping opens the article
 * full-screen (with back); on a tablet/foldable the list and article sit side by side. The
 * detail content key is the selected story id.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun KioskListDetail() {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()
    val selectedId = navigator.currentDestination?.contentKey

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                StoriesScreen(
                    selectedStoryId = selectedId,
                    onOpenStory = { id ->
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val storyId = navigator.currentDestination?.contentKey
                if (storyId != null) {
                    ArticleScreen(
                        storyId = storyId,
                        showBack = navigator.canNavigateBack(),
                        onBack = { scope.launch { navigator.navigateBack() } },
                    )
                } else {
                    EmptyDetail()
                }
            }
        },
    )
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
