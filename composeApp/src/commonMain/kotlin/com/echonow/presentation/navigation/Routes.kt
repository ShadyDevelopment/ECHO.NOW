package com.echonow.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object MoviesRoute

@Serializable
data object SeriesRoute

@Serializable
data object SearchRoute

@Serializable
data object FavoritesRoute

@Serializable
data object SettingsRoute

@Serializable
data class DetailsRoute(val id: String)

@Serializable
data class PlayerRoute(
    val id: String,
    val season: Int = 0,
    val episode: Int = 0,
    val qualityIndex: Int = 0,
    val resumePositionMillis: Long = 0L,
)
