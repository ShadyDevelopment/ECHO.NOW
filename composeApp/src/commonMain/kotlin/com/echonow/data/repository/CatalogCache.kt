package com.echonow.data.repository

import com.echonow.domain.model.Media

class CatalogCache {
    private val values = mutableMapOf<String, Media>()

    fun get(id: String): Media? = values[id]

    fun put(items: List<Media>) {
        items.forEach { values[it.id] = it }
    }

    fun clear() = values.clear()
}
