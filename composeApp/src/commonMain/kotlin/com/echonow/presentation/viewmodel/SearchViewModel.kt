package com.echonow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echonow.domain.model.Media
import com.echonow.domain.usecase.SearchCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<Media> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val page: Int = 0,
    val totalPages: Int = 0,
) {
    val hasMore: Boolean get() = page in 1 until totalPages
}

class SearchViewModel(private val searchCatalog: SearchCatalog) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestId = 0

    fun queryChanged(query: String) {
        requestId += 1
        val activeRequest = requestId
        searchJob?.cancel()
        loadMoreJob?.cancel()
        if (query.isBlank()) {
            _state.value = SearchState(query = query)
            return
        }
        _state.value = SearchState(query = query, loading = true)
        searchJob = viewModelScope.launch {
            delay(500)
            try {
                val page = searchCatalog(query, 1)
                if (activeRequest != requestId) return@launch
                _state.value = SearchState(
                    query = query,
                    results = page.results,
                    searched = true,
                    page = page.page,
                    totalPages = page.totalPages,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (activeRequest != requestId) return@launch
                _state.value = SearchState(
                    query = query,
                    error = error.message ?: "Search failed.",
                    searched = true,
                )
            }
        }
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore || current.query.isBlank()) return
        val activeRequest = requestId
        val nextPage = current.page + 1
        _state.value = current.copy(loadingMore = true, error = null)
        loadMoreJob = viewModelScope.launch {
            try {
                val page = searchCatalog(current.query, nextPage)
                if (activeRequest != requestId) return@launch
                val latest = _state.value
                _state.value = latest.copy(
                    results = (latest.results + page.results).distinctBy { "${it.type}:${it.id}" },
                    loadingMore = false,
                    page = page.page,
                    totalPages = page.totalPages,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (activeRequest != requestId) return@launch
                _state.value = _state.value.copy(
                    loadingMore = false,
                    error = error.message ?: "More results could not be loaded.",
                )
            }
        }
    }

    fun clear() = queryChanged("")
}
