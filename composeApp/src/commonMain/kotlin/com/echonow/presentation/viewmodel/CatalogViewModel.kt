package com.echonow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.usecase.GetMedia
import com.echonow.domain.usecase.LoadCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogState(
    val items: List<Media> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val endReached: Boolean = false,
    val selectedGenre: String? = null,
)

class CatalogViewModel(
    private val loadCatalog: LoadCatalog,
    private val getMedia: GetMedia,
) : ViewModel() {
    private val _movies = MutableStateFlow(CatalogState())
    val movies = _movies.asStateFlow()
    private val _series = MutableStateFlow(CatalogState())
    val series = _series.asStateFlow()

    init {
        refresh(MediaType.MOVIE)
        refresh(MediaType.SERIES)
    }

    fun load(type: MediaType, next: Boolean = false, refreshing: Boolean = false) {
        val state = state(type)
        val current = state.value
        if (
            current.loadingMore ||
            (next && (current.endReached || current.loading)) ||
            (!next && current.loading && !refreshing)
        ) return
        val page = if (next) current.page + 1 else 0
        state.value = current.copy(
            loading = !next && current.items.isEmpty(),
            loadingMore = next,
            refreshing = refreshing,
            error = null,
        )
        viewModelScope.launch {
            runCatching { loadCatalog(type, page) }
                .onSuccess { items ->
                    state.value = state.value.copy(
                        items = if (next) state.value.items + items else items,
                        loading = false,
                        loadingMore = false,
                        refreshing = false,
                        page = page,
                        endReached = items.isEmpty(),
                    )
                }
                .onFailure { error ->
                    state.value = state.value.copy(
                        loading = false,
                        loadingMore = false,
                        refreshing = false,
                        error = error.message ?: "Unable to load content.",
                    )
                }
        }
    }

    fun refresh(type: MediaType) = load(type, refreshing = true)

    fun selectGenre(type: MediaType, genre: String?) {
        state(type).value = state(type).value.copy(selectedGenre = genre)
    }

    private fun state(type: MediaType) = if (type == MediaType.MOVIE) _movies else _series
}
