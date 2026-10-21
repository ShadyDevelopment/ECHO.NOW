package com.echonow.di

import com.echonow.data.local.KeyValueStore
import com.echonow.data.local.LocalWatchProgressRepository
import com.echonow.data.local.LocalProfileRepository
import com.echonow.data.local.SettingsFavoritesRepository
import com.echonow.data.local.SettingsPreferencesRepository
import com.echonow.data.local.createKeyValueStore
import com.echonow.data.remote.TmdbApi
import com.echonow.data.remote.IntroDbApi
import com.echonow.data.remote.SubtitlesApi
import com.echonow.data.repository.DefaultCatalogRepository
import com.echonow.data.repository.CatalogCache
import com.echonow.domain.repository.CatalogRepository
import com.echonow.domain.repository.FavoritesRepository
import com.echonow.domain.repository.PreferencesRepository
import com.echonow.domain.repository.ProfileRepository
import com.echonow.domain.repository.WatchProgressRepository
import com.echonow.domain.usecase.GetFavorites
import com.echonow.domain.usecase.GetMedia
import com.echonow.domain.usecase.GetSeasonCount
import com.echonow.domain.usecase.LoadCatalog
import com.echonow.domain.usecase.ObservePreferences
import com.echonow.domain.usecase.SearchCatalog
import com.echonow.presentation.viewmodel.CatalogViewModel
import com.echonow.presentation.viewmodel.DetailViewModel
import com.echonow.presentation.viewmodel.FavoritesViewModel
import com.echonow.presentation.viewmodel.SearchViewModel
import com.echonow.presentation.viewmodel.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModule(tmdbApiKey: String, tmdbReadAccessToken: String, subdlApiKey: String): Module = module {
    single<KeyValueStore> { createKeyValueStore() }
    single<ProfileRepository> { LocalProfileRepository(get()) }
    single {
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single { TmdbApi(get(), tmdbApiKey, tmdbReadAccessToken) }
    single { IntroDbApi(get()) }
    single { SubtitlesApi(get(), subdlApiKey) }
    single { CatalogCache() }
    single<CatalogRepository> { DefaultCatalogRepository(get(), get(), get()) }
    single<FavoritesRepository> { SettingsFavoritesRepository(get(), get()) }
    single<PreferencesRepository> { SettingsPreferencesRepository(get(), get()) }
    single<WatchProgressRepository> { LocalWatchProgressRepository(get(), get()) }

    factory { LoadCatalog(get()) }
    factory { SearchCatalog(get()) }
    factory { GetMedia(get()) }
    factory { GetSeasonCount(get()) }
    factory { GetFavorites(get(), get()) }
    factory { ObservePreferences(get()) }

    factory { CatalogViewModel(get(), get()) }
    factory { DetailViewModel(get(), get(), get(), get()) }
    factory { SearchViewModel(get()) }
    factory { FavoritesViewModel(get()) }
    factory { SettingsViewModel(get()) }
}
