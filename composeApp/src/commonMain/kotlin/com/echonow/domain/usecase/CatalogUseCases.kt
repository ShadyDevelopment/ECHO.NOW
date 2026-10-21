package com.echonow.domain.usecase

import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.repository.CatalogRepository
import com.echonow.domain.repository.FavoritesRepository
import com.echonow.domain.repository.PreferencesRepository
import com.echonow.domain.repository.UserPreferences
import com.echonow.domain.repository.WatchProgress
import kotlinx.coroutines.flow.Flow

class LoadCatalog(private val repository: CatalogRepository) {
    suspend operator fun invoke(type: MediaType, page: Int) = repository.page(type, page)
}

class SearchCatalog(private val repository: CatalogRepository) {
    suspend operator fun invoke(query: String, page: Int = 1) = repository.search(query, page)
}

class GetMedia(private val repository: CatalogRepository) {
    suspend operator fun invoke(id: String): Media? = repository.find(id)
}

class GetSeasonCount(private val repository: CatalogRepository) {
    suspend operator fun invoke(id: String): Int? = repository.seasonCount(id)
}

class GetFavorites(private val favorites: FavoritesRepository, private val catalog: CatalogRepository) {
    fun observeIds() = favorites.observe()
    suspend fun resolve(ids: Set<String>): List<Media> = ids.mapNotNull { catalog.find(it) }
    suspend fun toggle(id: String) = favorites.toggle(id)
    suspend fun clear() = favorites.clear()
}

class ObservePreferences(private val repository: PreferencesRepository) {
    fun invoke(): Flow<UserPreferences> = repository.observe()
    suspend fun update(preferences: UserPreferences) = repository.update(preferences)
    suspend fun reset() = repository.reset()
}

class CalculateWatchPercentage {
    operator fun invoke(progress: WatchProgress): Float = progress.watchedPercentage
}
