package com.shapeshed.kiosk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.data.FlatComment
import com.shapeshed.kiosk.data.HnRepository
import com.shapeshed.kiosk.data.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs a single story's article screen: the story itself, and its comments (loaded on demand). */
class ArticleViewModel(
    private val repository: HnRepository,
    private val storyId: Long,
) : ViewModel() {

    private val _story = MutableStateFlow<UiState<Story>>(UiState.Loading)
    val story: StateFlow<UiState<Story>> = _story.asStateFlow()

    private val _comments = MutableStateFlow<UiState<List<FlatComment>>>(UiState.Loading)
    val comments: StateFlow<UiState<List<FlatComment>>> = _comments.asStateFlow()

    private var commentsRequested = false

    init {
        loadStory()
    }

    fun loadStory() {
        viewModelScope.launch {
            _story.value = UiState.Loading
            _story.value = runCatching { repository.story(storyId) ?: error("story $storyId not found") }
                .fold({ UiState.Content(it) }, { UiState.Error(it) })
        }
    }

    /** Fetch the comment thread the first time the sheet is opened; a no-op afterwards. */
    fun loadComments() {
        if (commentsRequested) return
        commentsRequested = true
        viewModelScope.launch {
            _comments.value = runCatching {
                val story = (_story.value as? UiState.Content)?.data
                    ?: repository.story(storyId)
                    ?: error("story $storyId not found")
                repository.commentThread(story)
            }.fold({ UiState.Content(it) }, { UiState.Error(it) })
        }
    }

    companion object {
        fun factory(app: KioskApp, storyId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArticleViewModel(app.repository, storyId) }
        }
    }
}
