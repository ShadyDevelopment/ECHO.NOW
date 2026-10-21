package com.echonow.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroDbApiTest {
    @Test
    fun tvEpisodeLookupUsesSeriesIdAndEpisodeCoordinates() = runBlocking {
        var requestUrl: String? = null
        val client = HttpClient(MockEngine { request ->
            requestUrl = request.url.toString()
            respond(
                content = """{"tmdb_id":1396,"type":"tv","season":1,"episode":3,"intro":[{"start_ms":182558,"end_ms":199404}],"credits":[{"start_ms":2841000,"end_ms":null}]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val timing = try {
            IntroDbApi(client).episodeTiming(
                tmdbId = 1396,
                season = 1,
                episode = 3,
                durationMs = 2_600_000,
            )
        } finally {
            client.close()
        }

        val url = io.ktor.http.Url(requireNotNull(requestUrl))
        assertEquals(URLProtocol.HTTPS, url.protocol)
        assertEquals("api.theintrodb.org", url.host)
        assertEquals("1396", url.parameters["tmdb_id"])
        assertEquals("1", url.parameters["season"])
        assertEquals("3", url.parameters["episode"])
        assertEquals("2600000", url.parameters["duration_ms"])
        assertEquals(EpisodeTiming(182558, 199404, 2841000), timing)
    }
}
