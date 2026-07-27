package com.shapeshed.kiosk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.data.Feed
import com.shapeshed.kiosk.data.HnRepository
import com.shapeshed.kiosk.data.SettingsStore
import com.shapeshed.kiosk.data.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoriesViewModel(
    private val feed: Feed,
    private val repository: HnRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    /** Ids of stories already opened — read rows render un-bold. */
    val viewedIds: StateFlow<Set<Long>> =
        settings.viewedStoryIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun markViewed(id: Long) {
        viewModelScope.launch { settings.markViewed(id) }
    }

    private val _state = MutableStateFlow<UiState<List<Story>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Story>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    // Full ranked id list; we page through it as the user scrolls.
    private var ids: List<Long> = emptyList()
    private var loaded = 0

    /** True once every id has been paged in. */
    val endReached: Boolean get() = loaded >= ids.size

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (_state.value !is UiState.Content) _state.value = UiState.Loading
            _state.value = runCatching {
                ids = repository.storyIds(feed)
                loaded = 0
                nextPage()
            }.fold({ UiState.Content(it) }, { UiState.Error(it) })
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            runCatching {
                ids = repository.storyIds(feed)
                loaded = 0
                nextPage()
            }.onSuccess { _state.value = UiState.Content(it) }
            _refreshing.value = false
        }
    }

    /** Append the next page; a no-op while already loading, mid-refresh, or at the end. */
    fun loadMore() {
        if (_loadingMore.value || _refreshing.value || endReached) return
        val current = (_state.value as? UiState.Content)?.data ?: return
        viewModelScope.launch {
            _loadingMore.value = true
            val more = runCatching { nextPage() }.getOrDefault(emptyList())
            if (more.isNotEmpty()) _state.value = UiState.Content(current + more)
            _loadingMore.value = false
        }
    }

    /** Fetch the next [PAGE_SIZE] ids and advance the cursor. */
    private suspend fun nextPage(): List<Story> {
        if (loaded >= ids.size) return emptyList()
        val end = (loaded + PAGE_SIZE).coerceAtMost(ids.size)
        val page = repository.stories(ids.subList(loaded, end))
        loaded = end
        return page
    }

    companion object {
        private const val PAGE_SIZE = 25

        /** A factory bound to one [feed]; each pager page keys its own instance so feeds page independently. */
        fun factory(feed: Feed): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KioskApp
                StoriesViewModel(feed, app.repository, app.settings)
            }
        }
    }
}
