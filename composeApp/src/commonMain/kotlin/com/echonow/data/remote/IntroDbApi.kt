package com.echonow.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class IntroDbApi(private val client: HttpClient) {
    suspend fun episodeTiming(
        tmdbId: Long,
        season: Int,
        episode: Int,
        durationMs: Long,
    ): EpisodeTiming? {
        val response = client.get("$INTRO_DB_API_URL/media") {
            parameter("tmdb_id", tmdbId)
            parameter("season", season)
            parameter("episode", episode)
            durationMs.takeIf { it > 0L }?.let { parameter("duration_ms", it) }
        }.body<IntroDbMediaResponse>()
        val intro = response.intro.firstOrNull { it.startMs != null || it.endMs != null }
        val creditsStartMs = response.credits.firstNotNullOfOrNull { it.startMs }

        return EpisodeTiming(
            introStartMs = intro?.startMs,
            introEndMs = intro?.endMs,
            creditsStartMs = creditsStartMs,
        ).takeIf { it.introStartMs != null || it.introEndMs != null || it.creditsStartMs != null }
    }

    private companion object {
        const val INTRO_DB_API_URL = "https://api.theintrodb.org/v3"
    }
}

data class EpisodeTiming(
    val introStartMs: Long?,
    val introEndMs: Long?,
    val creditsStartMs: Long?,
)

@Serializable
private data class IntroDbMediaResponse(
    val intro: List<IntroDbSegment> = emptyList(),
    val credits: List<IntroDbSegment> = emptyList(),
)

@Serializable
private data class IntroDbSegment(
    @SerialName("start_ms") val startMs: Long? = null,
    @SerialName("end_ms") val endMs: Long? = null,
)
