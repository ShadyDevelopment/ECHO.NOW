package com.echonow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.echonow.platform.AndroidPlatformContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.echoNowDataStore: DataStore<Preferences> by preferencesDataStore(name = "echo-now")

private class DataStoreKeyValueStore(private val dataStore: DataStore<Preferences>) : KeyValueStore {
    override fun observeString(key: String, default: String): Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    override suspend fun putString(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { preferences -> preferences[preferenceKey] = value }
    }

    override suspend fun remove(key: String) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { preferences -> preferences.remove(preferenceKey) }
    }
}

actual fun createKeyValueStore(): KeyValueStore =
    DataStoreKeyValueStore(AndroidPlatformContext.context.echoNowDataStore)
