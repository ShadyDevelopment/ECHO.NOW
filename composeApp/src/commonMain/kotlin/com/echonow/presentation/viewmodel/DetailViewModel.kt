package com.echonow.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echonow.domain.model.Media
import com.echonow.domain.model.CastMember
import com.echonow.domain.model.VideoQuality
import com.echonow.data.remote.IntroDbApi
import com.echonow.domain.repository.CatalogRepository
import com.echonow.domain.usecase.GetFavorites
import com.echonow.domain.usecase.GetMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailState(
    val media: Media? = null,
    val streams: List<VideoQuality> = emptyList(),
    val streamsLoading: Boolean = false,
    val streamsError: String? = null,
    val trailerUrl: String? = null,
    val trailerLoading: Boolean = false,
    val trailerError: String? = null,
    val cast: List<CastMember> = emptyList(),
    val castLoading: Boolean = false,
    val castError: String? = null,
    val favorite: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

class DetailViewModel(
    private val getMedia: GetMedia,
    private val catalog: CatalogRepository,
    private val favorites: GetFavorites,
    private val introDbApi: IntroDbApi,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state = _state.asStateFlow()
    private var activeId: String? = null
    private var activeSeason: Int? = null
    private var activeEpisode: Int? = null
    private var streamRequestId = 0
    private var loadedCastId: String? = null
    private val requestedEpisodeTimings = mutableSetOf<String>()

    fun loadEpisodeTimings(seasonNumber: Int, episodeNumber: Int, playbackDurationMs: Long = 0L) {
        val media = _state.value.media ?: return
        if (media.type != com.echonow.domain.model.MediaType.SERIES) return
        val season = media.seasons.firstOrNull { it.number == seasonNumber } ?: return
        val episode = season.episodes.firstOrNull { it.number == episodeNumber } ?: return
        val tmdbId = media.id.substringAfter(':', "").toLongOrNull() ?: return
        val durationMs = playbackDurationMs.takeIf { it > 0L }
            ?: episode.durationMinutes.toLong() * 60_000L
        if (durationMs <= 0L) return
        val requestKey = "${media.id}:$seasonNumber:$episodeNumber:$durationMs"
        if (!requestedEpisodeTimings.add(requestKey)) return

        viewModelScope.launch {
            runCatching {
                runCatching {
                    introDbApi.episodeTiming(tmdbId, seasonNumber, episodeNumber, durationMs)
                }.getOrNull() ?: introDbApi.episodeTiming(tmdbId, seasonNumber, episodeNumber, 0L)
            }
                .onSuccess { timing ->
                    if (timing != null && _state.value.media?.id == media.id) {
                        val latestMedia = _state.value.media ?: return@onSuccess
                        val updatedMedia = latestMedia.copy(
                            seasons = latestMedia.seasons.map { currentSeason ->
                                if (currentSeason.number != seasonNumber) currentSeason else currentSeason.copy(
                                    episodes = currentSeason.episodes.map { currentEpisode ->
                                        if (currentEpisode.number != episodeNumber) currentEpisode else currentEpisode.copy(
                                            introStartMs = timing.introStartMs,
                                            introEndMs = timing.introEndMs,
                                            creditsStartMs = timing.creditsStartMs,
                                        )
                                    },
                                )
                            },
                        )
                        _state.value = _state.value.copy(media = updatedMedia)
                    }
                }
                .onFailure {
                    requestedEpisodeTimings.remove(requestKey)
                }
        }
    }

    fun load(id: String) {
        if (activeId == id && _state.value.media != null) return
        activeId = id
        viewModelScope.launch {
            _state.value = DetailState(loading = true)
            runCatching { getMedia(id) }
                .onSuccess { media ->
                    if (media == null) {
                        _state.value = DetailState(loading = false, error = "This title is not available.")
                    } else {
                        _state.value = DetailState(media = media, loading = false)
                        favorites.observeIds().collect { ids ->
                            _state.value = _state.value.copy(favorite = media.id in ids)
                        }
                    }
                }
                .onFailure { _state.value = DetailState(loading = false, error = it.message ?: "Unable to load title.") }
        }
    }

    fun loadStreams(season: Int? = null, episode: Int? = null) {
        val media = _state.value.media ?: return
        activeSeason = season
        activeEpisode = episode
        val requestId = ++streamRequestId
        _state.value = _state.value.copy(streamsLoading = true, streamsError = null)
        viewModelScope.launch {
            runCatching { catalog.streams(media, season, episode) }
                .onSuccess { streams ->
                    if (requestId == streamRequestId && _state.value.media?.id == media.id) {
                        _state.value = _state.value.copy(
                            streams = streams,
                            streamsLoading = false,
                            streamsError = if (streams.isEmpty()) "Configured providers did not return a playable source for this selection." else null,
                        )
                    }
                }
                .onFailure { error ->
                    if (requestId == streamRequestId && _state.value.media?.id == media.id) {
                        _state.value = _state.value.copy(
                            streamsLoading = false,
                            streamsError = error.message ?: "Unable to resolve configured playback sources.",
                        )
                    }
                }
        }
    }

    fun loadTrailer(id: String) {
        if (_state.value.media?.id != id || _state.value.trailerLoading) return
        _state.value = _state.value.copy(trailerLoading = true, trailerError = null)
        viewModelScope.launch {
            runCatching { catalog.trailer(id) }
                .onSuccess { url ->
                    if (_state.value.media?.id == id) {
                        _state.value = _state.value.copy(
                            trailerUrl = url,
                            trailerLoading = false,
                            trailerError = if (url == null) "No trailer is available for this title." else null,
                        )
                    }
                }
                .onFailure { error ->
                    if (_state.value.media?.id == id) {
                        _state.value = _state.value.copy(
                            trailerLoading = false,
                            trailerError = error.message ?: "Unable to load the trailer.",
                        )
                    }
                }
        }
    }

    fun loadCast(id: String) {
        if (_state.value.media?.id != id || loadedCastId == id || _state.value.castLoading) return
        loadedCastId = id
        _state.value = _state.value.copy(castLoading = true, castError = null)
        viewModelScope.launch {
            runCatching { catalog.cast(id) }
                .onSuccess { cast ->
                    if (_state.value.media?.id == id) {
                        _state.value = _state.value.copy(cast = cast, castLoading = false)
                    }
                }
                .onFailure { error ->
                    if (_state.value.media?.id == id) {
                        loadedCastId = null
                        _state.value = _state.value.copy(
                            castLoading = false,
                            castError = error.message ?: "Unable to load cast.",
                        )
                    }
                }
        }
    }

    fun toggleFavorite() {
        val id = _state.value.media?.id ?: return
        viewModelScope.launch { favorites.toggle(id) }
    }

    fun playSelection() = loadStreams(activeSeason, activeEpisode)
}
