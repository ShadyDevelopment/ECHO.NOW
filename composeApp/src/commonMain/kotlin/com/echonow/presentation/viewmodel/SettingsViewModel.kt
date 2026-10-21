package com.echonow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echonow.domain.repository.UserPreferences
import com.echonow.domain.usecase.ObservePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferences: ObservePreferences) : ViewModel() {
    private val _state = MutableStateFlow(UserPreferences())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { preferences.invoke().collect { _state.value = it } }
    }

    fun update(value: UserPreferences) = viewModelScope.launch { preferences.update(value) }
    fun reset() = viewModelScope.launch { preferences.reset() }
}
