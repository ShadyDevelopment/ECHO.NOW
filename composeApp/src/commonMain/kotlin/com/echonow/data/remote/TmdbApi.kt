package com.echonow.data.remote

import com.echonow.domain.model.Episode
import com.echonow.domain.model.CastMember
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.model.Season
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.request.header
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TMDB_API_URL = "https://api.themoviedb.org/3"
private const val TMDB_IMAGE_URL = "https://image.tmdb.org/t/p"

class TmdbApi(
    private val client: HttpClient,
    private val apiKey: String,
    private val readAccessToken: String,
) {
    suspend fun popular(type: MediaType, page: Int): TmdbPageResponse {
        requireConfigured()
        val path = if (type == MediaType.MOVIE) "movie/popular" else "tv/popular"
        return client.get("$TMDB_API_URL/$path") {
            authorize()
            parameter("page", page)
        }.body()
    }

    suspend fun search(query: String, page: Int): TmdbPageResponse {
        requireConfigured()
        return client.get("$TMDB_API_URL/search/multi") {
            authorize()
            parameter("query", query.trim())
            parameter("include_adult", false)
            parameter("page", page)
        }.body()
    }

    suspend fun find(id: String): Media? {
        requireConfigured()
        val (type, tmdbId) = parseId(id) ?: return null
        return if (type == MediaType.MOVIE) {
            client.get("$TMDB_API_URL/movie/$tmdbId") {
                authorize()
            }.body<TmdbMovieDetails>().toMedia()
        } else {
            val details = client.get("$TMDB_API_URL/tv/$tmdbId") {
                authorize()
            }.body<TmdbSeriesDetails>()
            val seasons = coroutineScope {
                details.seasons.filter { it.number > 0 && it.episodeCount > 0 }.map { season ->
                    async {
                        client.get("$TMDB_API_URL/tv/$tmdbId/season/${season.number}") {
                            authorize()
                        }.body<TmdbSeasonDetails>().toSeason(tmdbId, details.episodeRunTime.firstOrNull() ?: 0)
                    }
                }.map { it.await() }
            }
            details.toMedia().copy(seasons = seasons)
        }
    }

    suspend fun seasonCount(id: String): Int? {
        requireConfigured()
        val (type, tmdbId) = parseId(id) ?: return null
        if (type != MediaType.SERIES) return null
        return client.get("$TMDB_API_URL/tv/$tmdbId") {
            authorize()
        }.body<TmdbSeriesDetails>().seasons
            .asSequence()
            .filter { it.number > 0 && it.episodeCount > 0 }
            .count()
    }

    suspend fun trailer(id: String): String? {
        requireConfigured()
        val (type, tmdbId) = parseId(id) ?: return null
        val mediaPath = if (type == MediaType.MOVIE) "movie" else "tv"
        val videos = client.get("$TMDB_API_URL/$mediaPath/$tmdbId/videos") {
            authorize()
        }.body<TmdbVideosResponse>().results
        val youtubeVideos = videos.filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNotBlank() }
        val trailer = youtubeVideos.firstOrNull { it.type.equals("Trailer", ignoreCase = true) && it.official }
            ?: youtubeVideos.firstOrNull { it.type.equals("Trailer", ignoreCase = true) }
            ?: youtubeVideos.firstOrNull { it.type.equals("Teaser", ignoreCase = true) }
            ?: return null
        return "https://www.youtube.com/watch?v=${trailer.key}"
    }

    suspend fun cast(id: String): List<CastMember> {
        requireConfigured()
        val (type, tmdbId) = parseId(id) ?: return emptyList()
        val mediaPath = if (type == MediaType.MOVIE) "movie" else "tv"
        return client.get("$TMDB_API_URL/$mediaPath/$tmdbId/credits") {
            authorize()
        }.body<TmdbCreditsResponse>().cast.map { member ->
            CastMember(
                id = member.id,
                name = member.name,
                character = member.character,
                profileUrl = member.profilePath?.let { "$TMDB_IMAGE_URL/w185$it" },
            )
        }
    }

    private fun requireConfigured() {
        require(apiKey.isNotBlank() || readAccessToken.isNotBlank()) {
            "TMDB credentials are missing from the project-root config.cfg."
        }
    }

    private fun HttpRequestBuilder.authorize() {
        parameter("language", "en-US")
        if (readAccessToken.isNotBlank()) {
            header(HttpHeaders.Authorization, "Bearer $readAccessToken")
        } else {
            parameter("api_key", apiKey)
        }
    }
}

@Serializable
data class TmdbPageResponse(
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
    val results: List<TmdbTitle> = emptyList(),
)

@Serializable
data class TmdbVideosResponse(
    val results: List<TmdbVideo> = emptyList(),
)

@Serializable
data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember> = emptyList(),
)

@Serializable
data class TmdbCastMember(
    val id: Int,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class TmdbVideo(
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)

@Serializable
data class TmdbTitle(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
) {
    fun toMedia(type: MediaType): Media = Media(
        id = "${type.idPrefix}:$id",
        title = title ?: name.orEmpty(),
        type = type,
        year = ((if (type == MediaType.MOVIE) releaseDate else firstAirDate)
            ?.take(4)?.toIntOrNull() ?: 0),
        rating = voteAverage,
        runtimeMinutes = 0,
        genres = genreIds.mapNotNull { genreNames[it] },
        synopsis = overview,
        posterUrl = posterPath?.let { "$TMDB_IMAGE_URL/w500$it" }.orEmpty(),
        backdropUrl = backdropPath?.let { "$TMDB_IMAGE_URL/w1280$it" }.orEmpty(),
        originalLanguage = originalLanguage,
    )
}

@Serializable
data class TmdbMovieDetails(
    val id: Long,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
) {
    fun toMedia() = Media(
        id = "${MediaType.MOVIE.idPrefix}:$id",
        title = title,
        type = MediaType.MOVIE,
        year = releaseDate?.take(4)?.toIntOrNull() ?: 0,
        rating = voteAverage,
        runtimeMinutes = runtime ?: 0,
        genres = genres.map(TmdbGenre::name),
        synopsis = overview,
        posterUrl = posterPath?.let { "$TMDB_IMAGE_URL/w500$it" }.orEmpty(),
        backdropUrl = backdropPath?.let { "$TMDB_IMAGE_URL/w1280$it" }.orEmpty(),
        imdbId = imdbId,
        originalLanguage = originalLanguage,
    )
}

@Serializable
data class TmdbSeriesDetails(
    val id: Long,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    val seasons: List<TmdbSeasonSummary> = emptyList(),
) {
    fun toMedia() = Media(
        id = "${MediaType.SERIES.idPrefix}:$id",
        title = name,
        type = MediaType.SERIES,
        year = firstAirDate?.take(4)?.toIntOrNull() ?: 0,
        rating = voteAverage,
        runtimeMinutes = episodeRunTime.firstOrNull() ?: 0,
        genres = genres.map(TmdbGenre::name),
        synopsis = overview,
        posterUrl = posterPath?.let { "$TMDB_IMAGE_URL/w500$it" }.orEmpty(),
        backdropUrl = backdropPath?.let { "$TMDB_IMAGE_URL/w1280$it" }.orEmpty(),
        originalLanguage = originalLanguage,
        seasons = seasons.filter { it.number > 0 && it.episodeCount > 0 }
            .map { summary -> Season(number = summary.number, episodes = emptyList()) },
    )
}

@Serializable
data class TmdbGenre(val id: Int = 0, val name: String = "")

@Serializable
data class TmdbSeasonSummary(
    @SerialName("season_number") val number: Int = 0,
    @SerialName("episode_count") val episodeCount: Int = 0,
)

@Serializable
data class TmdbSeasonDetails(
    @SerialName("season_number") val number: Int = 0,
    val episodes: List<TmdbEpisode> = emptyList(),
) {
    fun toSeason(seriesId: Long, defaultRuntime: Int) = Season(
        number = number,
        episodes = episodes.map { episode ->
            Episode(
                id = "$seriesId:$number:${episode.number}",
                title = episode.name,
                number = episode.number,
                durationMinutes = episode.runtime ?: defaultRuntime,
                overview = episode.overview,
                stillUrl = episode.stillPath?.let { "$TMDB_IMAGE_URL/w500$it" },
                tmdbId = episode.id,
            )
        },
    )
}

@Serializable
data class TmdbEpisode(
    val id: Long? = null,
    @SerialName("episode_number") val number: Int,
    val name: String,
    val runtime: Int? = null,
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
)

private val MediaType.idPrefix: String
    get() = if (this == MediaType.MOVIE) "movie" else "series"

private fun parseId(id: String): Pair<MediaType, Long>? {
    val (prefix, value) = id.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
    val type = when (prefix) {
        "movie" -> MediaType.MOVIE
        "series" -> MediaType.SERIES
        else -> return null
    }
    return type to (value.toLongOrNull() ?: return null)
}

private val genreNames = mapOf(
    12 to "Adventure", 14 to "Fantasy", 16 to "Animation", 18 to "Drama",
    27 to "Horror", 28 to "Action", 35 to "Comedy", 36 to "History",
    37 to "Western", 53 to "Thriller", 80 to "Crime", 99 to "Documentary",
    878 to "Sci-Fi", 9648 to "Mystery", 10402 to "Music", 10749 to "Romance",
    10751 to "Family", 10752 to "War", 10759 to "Action & Adventure",
    10762 to "Kids", 10763 to "News", 10764 to "Reality", 10765 to "Sci-Fi & Fantasy",
    10766 to "Soap", 10767 to "Talk", 10768 to "War & Politics", 10770 to "TV Movie",
)
