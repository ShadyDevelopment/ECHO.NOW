package com.echonow.data.local

import kotlinx.coroutines.flow.Flow

interface KeyValueStore {
    fun observeString(key: String, default: String): Flow<String>
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
}

expect fun createKeyValueStore(): KeyValueStore
