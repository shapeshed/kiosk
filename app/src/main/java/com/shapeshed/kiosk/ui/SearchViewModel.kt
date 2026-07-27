package com.shapeshed.kiosk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.data.HnRepository
import com.shapeshed.kiosk.data.SearchFilter
import com.shapeshed.kiosk.data.SearchSort
import com.shapeshed.kiosk.data.SettingsStore
import com.shapeshed.kiosk.data.Story
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: HnRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val viewedIds: StateFlow<Set<Long>> =
        settings.viewedStoryIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _state = MutableStateFlow<UiState<List<Story>>>(UiState.Content(emptyList()))
    val state: StateFlow<UiState<List<Story>>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private var query = ""
    private var filter = SearchFilter.STORIES
    private var sort = SearchSort.RELEVANCE
    private var page = 0
    private var totalPages = 0
    private var searchJob: Job? = null

    fun markViewed(id: Long) {
        viewModelScope.launch { settings.markViewed(id) }
    }

    fun search(query: String, filter: SearchFilter, sort: SearchSort) {
        val normalized = query.trim()
        if (normalized == this.query && filter == this.filter && sort == this.sort) return
        this.query = normalized
        this.filter = filter
        this.sort = sort
        searchJob?.cancel()
        if (normalized.isBlank()) {
            page = 0
            totalPages = 0
            _loadingMore.value = false
            _state.value = UiState.Content(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadFirstPage()
        }
    }

    fun retry() {
        searchJob?.cancel()
        if (query.isBlank()) return
        searchJob = viewModelScope.launch { loadFirstPage() }
    }

    fun loadMore() {
        if (_loadingMore.value || query.isBlank() || page + 1 >= totalPages) return
        val current = (_state.value as? UiState.Content)?.data ?: return
        viewModelScope.launch {
            _loadingMore.value = true
            val nextPage = page + 1
            runCatching {
                repository.search(query, filter, sort, nextPage, PAGE_SIZE)
            }.onSuccess { result ->
                page = result.page
                totalPages = result.totalPages
                if (result.stories.isNotEmpty()) {
                    _state.value = UiState.Content(current + result.stories)
                }
            }
            _loadingMore.value = false
        }
    }

    private suspend fun loadFirstPage() {
        _state.value = UiState.Loading
        page = 0
        totalPages = 0
        _state.value = runCatching {
            repository.search(query, filter, sort, page = 0, pageSize = PAGE_SIZE)
        }.fold(
            onSuccess = { result ->
                page = result.page
                totalPages = result.totalPages
                UiState.Content(result.stories)
            },
            onFailure = { UiState.Error(it) },
        )
    }

    companion object {
        private const val PAGE_SIZE = 25
        private const val SEARCH_DEBOUNCE_MS = 300L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KioskApp
                SearchViewModel(app.repository, app.settings)
            }
        }
    }
}
