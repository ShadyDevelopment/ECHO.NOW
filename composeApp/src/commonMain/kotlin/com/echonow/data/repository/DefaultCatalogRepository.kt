package com.echonow.data.repository

import com.echonow.data.remote.TmdbApi
import com.echonow.data.remote.streaming.StreamManager
import com.echonow.data.remote.streaming.StreamRequest
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.model.CastMember
import com.echonow.domain.model.VideoQuality
import com.echonow.domain.repository.CatalogRepository
import com.echonow.domain.repository.CatalogSearchPage

class DefaultCatalogRepository(
    private val tmdbApi: TmdbApi,
    private val streamManager: StreamManager,
    private val cache: CatalogCache,
) : CatalogRepository {
    private val seasonCountCache = mutableMapOf<String, Int?>()

    override suspend fun page(type: MediaType, page: Int, pageSize: Int): List<Media> {
        require(page >= 0) { "Page index must not be negative." }
        require(pageSize > 0) { "Page size must be positive." }
        val tmdbPage = tmdbApi.popular(type, page + 1)
        val result = tmdbPage.results.map { it.toMedia(type) }
        cache.put(result)
        return result
    }

    override suspend fun search(query: String, page: Int): CatalogSearchPage {
        val term = query.trim()
        require(page > 0) { "Search page must be positive." }
        if (term.isEmpty()) return CatalogSearchPage(emptyList(), 1, 1)
        val result = tmdbApi.search(term, page)
        val media = result.results.mapNotNull { title ->
            when (title.mediaType) {
                "movie" -> title.toMedia(MediaType.MOVIE)
                "tv" -> title.toMedia(MediaType.SERIES)
                else -> null
            }
        }
        cache.put(media)
        return CatalogSearchPage(media, result.page, result.totalPages)
    }

    override suspend fun find(id: String): Media? {
        val cached = cache.get(id)
        val needsDetails = cached == null || (cached.type == MediaType.SERIES && cached.seasons.isEmpty())
        if (!needsDetails) return cached
        return tmdbApi.find(id)?.also { cache.put(listOf(it)) } ?: cached
    }

    override suspend fun seasonCount(id: String): Int? {
        val cached = cache.get(id)?.seasons
            ?.asSequence()
            ?.filter { it.number > 0 }
            ?.count()
        if (cached != null && cached > 0) return cached
        if (seasonCountCache.containsKey(id)) return seasonCountCache[id]
        return tmdbApi.seasonCount(id).also { seasonCountCache[id] = it }
    }

    override suspend fun trailer(id: String): String? = tmdbApi.trailer(id)

    override suspend fun cast(id: String): List<CastMember> = tmdbApi.cast(id)

    override suspend fun streams(media: Media, season: Int?, episode: Int?): List<VideoQuality> {
        val tmdbId = media.id.substringAfter(':', media.id)
        val targetSeason = season ?: if (media.type == MediaType.SERIES) (media.seasons.firstOrNull()?.number ?: 1) else null
        val targetEpisode = episode ?: if (media.type == MediaType.SERIES) (media.seasons.firstOrNull()?.episodes?.firstOrNull()?.number ?: 1) else null
        val request = StreamRequest(
            tmdbId = tmdbId,
            imdbId = media.imdbId ?: cache.get(media.id)?.imdbId,
            title = media.title,
            releaseYear = media.year,
            season = targetSeason,
            episode = targetEpisode,
            type = media.type,
        )
        val results = streamManager.fetchAllStreams(request)
        val available = results.flatMap { result ->
            result.sources.map { source ->
                VideoQuality(
                    label = normalizeQuality(source.quality, source.url),
                    url = source.url,
                    format = source.format,
                    isM3u8 = source.isM3u8,
                    headers = result.headers,
                    providerName = result.providerName,
                    audioLanguage = source.audioLanguage,
                )
            }
        }
        return available.distinctBy { it.url }
            .sortedWith(
                compareBy<VideoQuality> { it.audioLanguage.preferenceRank() }
                    .thenByDescending { qualityRank(it.label) },
            )
    }

    private fun String?.preferenceRank(): Int {
        val sourceLanguage = normalizeLanguage(this)
        return when {
            sourceLanguage == null || sourceLanguage.isBlank() -> 1
            sourceLanguage == "en" -> 0
            else -> 2
        }
    }

    private fun normalizeLanguage(language: String?): String? {
        val value = language?.trim()?.lowercase()
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.takeIf(String::isNotBlank)
            ?: return null
        return when (value) {
            "original audio", "original language" -> "original"
            "english" -> "en"
            "spanish" -> "es"
            "french" -> "fr"
            "german" -> "de"
            "italian" -> "it"
            "japanese" -> "ja"
            "korean" -> "ko"
            "chinese" -> "zh"
            "mandarin" -> "zh"
            "hindi" -> "hi"
            "portuguese" -> "pt"
            "russian" -> "ru"
            "arabic" -> "ar"
            else -> value
        }
    }

    private fun normalizeQuality(raw: String, url: String): String {
        val text = "$raw ${url.substringBefore('?').substringAfterLast('/')}".lowercase()
        return when {
            "2160" in text || "4k" in text || "uhd" in text -> "2160p"
            "1440" in text || "2k" in text -> "1440p"
            "1080" in text || "fhd" in text -> "1080p"
            "720" in text || Regex("(^|[^a-z])hd([^a-z]|$)").containsMatchIn(text) -> "720p"
            "480" in text -> "480p"
            "360" in text -> "360p"
            else -> "Auto"
        }
    }

    private fun qualityRank(label: String): Int = label.removeSuffix("p").toIntOrNull() ?: 0
}
