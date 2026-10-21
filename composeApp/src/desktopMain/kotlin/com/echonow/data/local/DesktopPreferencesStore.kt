package com.echonow.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.prefs.Preferences

private class DesktopPreferencesStore : KeyValueStore {
    private data class Entry(val default: String, val state: MutableStateFlow<String>)

    private val preferences = Preferences.userRoot().node("com/echonow")
    private val entries = mutableMapOf<String, Entry>()

    override fun observeString(key: String, default: String): Flow<String> =
        synchronized(entries) {
            entries.getOrPut(key) { Entry(default, MutableStateFlow(preferences.get(key, default))) }
                .state.asStateFlow()
        }

    override suspend fun putString(key: String, value: String) {
        val entry = entry(key, "")
        preferences.put(key, value)
        entry.state.value = value
    }

    override suspend fun remove(key: String) {
        val entry = entry(key, "")
        preferences.remove(key)
        entry.state.value = entry.default
    }

    private fun entry(key: String, default: String): Entry = synchronized(entries) {
        entries.getOrPut(key) { Entry(default, MutableStateFlow(preferences.get(key, default))) }
    }
}

actual fun createKeyValueStore(): KeyValueStore = DesktopPreferencesStore()
