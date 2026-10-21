package com.echonow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echonow.domain.model.Media
import com.echonow.domain.usecase.GetFavorites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(private val favorites: GetFavorites) : ViewModel() {
    private val _items = MutableStateFlow<List<Media>>(emptyList())
    val items = _items.asStateFlow()
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids = _ids.asStateFlow()

    init {
        viewModelScope.launch {
            favorites.observeIds().collect { ids ->
                _ids.value = ids
                _items.value = favorites.resolve(ids)
            }
        }
    }

    fun toggle(id: String) = viewModelScope.launch { favorites.toggle(id) }
    fun clear() = viewModelScope.launch { favorites.clear() }
}
