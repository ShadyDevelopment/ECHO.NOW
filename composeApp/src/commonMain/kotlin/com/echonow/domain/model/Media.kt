package com.echonow.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType { MOVIE, SERIES }

data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val durationMinutes: Int,
    val overview: String = "",
    val stillUrl: String? = null,
    val introStartMs: Long? = null,
    val introEndMs: Long? = null,
    val creditsStartMs: Long? = null,
    val tmdbId: Long? = null,
)

data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

data class VideoQuality(
    val label: String,
    val url: String,
    val format: String = "hls",
    val isM3u8: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val providerName: String = "",
    val audioLanguage: String? = null,
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String?,
)

data class Media(
    val id: String,
    val title: String,
    val type: MediaType,
    val year: Int,
    val rating: Double,
    val runtimeMinutes: Int,
    val genres: List<String>,
    val synopsis: String,
    val posterUrl: String,
    val backdropUrl: String,
    val qualities: List<VideoQuality> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val imdbId: String? = null,
    val originalLanguage: String? = null,
)
