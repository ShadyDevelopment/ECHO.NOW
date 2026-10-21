package com.echonow.data.remote.streaming

import com.echonow.domain.model.MediaType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// ----------------------------------------------------------------------
// Models
// ----------------------------------------------------------------------

@Serializable
data class StreamRequest(
    val tmdbId: String,
    val imdbId: String? = null,
    val title: String? = null,
    val releaseYear: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val type: MediaType
)

@Serializable
data class StreamSource(
    val url: String,
    val quality: String = "Auto",
    val format: String = "hls", // hls, mp4
    val isM3u8: Boolean = true,
    val audioLanguage: String? = null,
)

@Serializable
data class SubtitleTrack(
    val url: String,
    val language: String,
    val label: String? = null
)

@Serializable
data class StreamResult(
    val providerName: String,
    val sources: List<StreamSource>,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

interface StreamProvider {
    val name: String
    suspend fun extractStreams(request: StreamRequest): StreamResult?
}

// ----------------------------------------------------------------------
// HTTP Client
// ----------------------------------------------------------------------

fun createStreamingHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 6000
            connectTimeoutMillis = 4000
            socketTimeoutMillis = 6000
        }
        defaultRequest {
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }
    }
}

// ----------------------------------------------------------------------
// Utilities
// ----------------------------------------------------------------------

@OptIn(ExperimentalEncodingApi::class)
object CryptoUtils {
    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length
        val result = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            result[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return result
    }

    fun bytesToHex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append("0123456789abcdef"[v ushr 4])
            append("0123456789abcdef"[v and 0x0F])
        }
    }

    fun base64Encode(bytes: ByteArray): String {
        return Base64.Default.encode(bytes)
    }

    fun base64Encode(str: String): String {
        return Base64.Default.encode(str.encodeToByteArray())
    }

    fun base64Decode(str: String): ByteArray {
        val clean = str.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        val padded = when (clean.length % 4) {
            2 -> "$clean=="
            3 -> "$clean="
            else -> clean
        }
        return Base64.Default.decode(padded)
    }

    fun base64UrlDecode(str: String): ByteArray {
        val std = str.replace('-', '+').replace('_', '/')
        return base64Decode(std)
    }

    fun base64UrlEncodeNoPadding(bytes: ByteArray): String {
        return Base64.UrlSafe.encode(bytes).trimEnd('=')
    }
}

object ProxyUnwrapper {
    private val knownThirdPartyProxies: Map<String, List<Regex>> = mapOf(
        "https://hls1.vid1.site" to listOf(Regex("""/proxy/(.+)$""")),
        "https://madplay.site" to listOf(Regex("""/api/[^/]+/proxy\?url=(.+)$""")),
        "https://streams.smashystream.top" to listOf(Regex("""/proxy/m3u8/(.+?)/[^/]+$""")),
        "*" to listOf(
            Regex("""^https://[^/]+\.workers\.dev/((?:https?://|https?%3A%2F%2F).+)$"""),
            Regex("""^https://[^/]+\.workers\.dev/((?:https?://)?[^/]+/file2/.+)$"""),
            Regex("""^https://.+?\.workers\.dev/((?:https?://).+)$"""),
            Regex("""/proxy/(.+)$"""),
            Regex("""/(?:m3u8|mp4)-proxy\?url=(.+?)(?:&|$)"""),
            Regex("""/api/[^/]+/proxy\?url=(.+)$"""),
            Regex("""/proxy\?.*url=([^&]+)"""),
            Regex("""/stream/proxy/(.+)$"""),
            Regex("""^https://[^/]+/((?:https?://)?[a-zA-Z0-9.-]+/file2/.+)$"""),
            Regex("""^https://[^/]+\.workers\.dev/(?:m3u8|mp4)-proxy\?url=(.+?)(?:&|$)""")
        )
    )

    fun unwrapUrl(url: String): String {
        val origin = try {
            val parsed = Url(url)
            "${parsed.protocol.name}://${parsed.host}"
        } catch (_: Exception) { null }

        val patterns = (knownThirdPartyProxies[origin] ?: emptyList()) + (knownThirdPartyProxies["*"] ?: emptyList())

        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                var decoded = match.groupValues[1]
                var previous: String
                do {
                    previous = decoded
                    decoded = decoded.decodeURLQueryComponent()
                } while (decoded != previous)

                return decoded
            }
        }
        return url
    }
}

// ----------------------------------------------------------------------
// 1. VidSrc Provider (providers/vidsrc/vidsrc.ts)
// ----------------------------------------------------------------------

class VidSrcProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidSrc"
    private val baseUrl = "https://vsembed.ru"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val pageUrl = if (request.type == MediaType.MOVIE) {
            "$baseUrl/embed/movie?tmdb=${request.tmdbId}"
        } else {
            "$baseUrl/embed/tv?tmdb=${request.tmdbId}&season=${request.season ?: 1}&episode=${request.episode ?: 1}"
        }

        val html = client.get(pageUrl) {
            header("Referer", baseUrl)
        }.bodyAsText()

        val iframeRegex = Regex("""<iframe[^>]*\s+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val secondSrc = iframeRegex.find(html)?.groupValues?.get(1) ?: return null
        val secondUrl = if (secondSrc.startsWith("//")) "https:$secondSrc" else secondSrc

        val secondHtml = client.get(secondUrl) {
            header("Referer", pageUrl)
        }.bodyAsText()

        val thirdRegex = Regex("""src:\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        val relSrc = thirdRegex.find(secondHtml)?.groupValues?.get(1) ?: return null

        val thirdUrl = if (relSrc.startsWith("http")) relSrc else {
            val baseParsed = Url(secondUrl)
            "${baseParsed.protocol.name}://${baseParsed.host}$relSrc"
        }

        val thirdHtml = client.get(thirdUrl) {
            header("Referer", secondUrl)
        }.bodyAsText()

        val fileField = Regex("""file\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(thirdHtml)?.groupValues?.get(1) ?: return null

        val replaced = fileField
            .replace("{v1}", "neonhorizonworkshops.com")
            .replace("{v2}", "wanderlynest.com")
            .replace("{v3}", "orchidpixelgardens.com")
            .replace("{v4}", "cloudnestra.com")

        val urls = replaced.split(Regex("""\s+or\s+""", RegexOption.IGNORE_CASE))
            .filter { it.isNotBlank() && !it.contains("{") && !it.contains("}") }

        if (urls.isEmpty()) null
        else StreamResult(
            providerName = name,
            sources = urls.map { StreamSource(url = it, quality = "Auto", format = "hls", isM3u8 = true) },
            headers = mapOf(
                "Referer" to "https://cloudnestra.com/",
                "Origin" to "https://cloudnestra.com"
            )
        )
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 2. VixSrc Provider (providers/vixsrc/vixsrc.ts)
// ----------------------------------------------------------------------

@Serializable
private data class VixSrcSublinkResponse(val src: String? = null)

class VixSrcProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VixSrc"
    private val baseUrl = "https://vixsrc.to"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val pageUrl = if (request.type == MediaType.MOVIE) {
            "$baseUrl/api/movie/${request.tmdbId}"
        } else {
            "$baseUrl/api/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
        }

        val sublink: VixSrcSublinkResponse = client.get(pageUrl) {
            header("Referer", baseUrl)
            header("Origin", baseUrl)
        }.body()

        val suburl = sublink.src ?: return null
        val fullSubUrl = if (suburl.startsWith("http")) suburl else "$baseUrl$suburl"

        val html = client.get(fullSubUrl) {
            header("Referer", pageUrl)
        }.bodyAsText()

        val token = Regex("""token["']\s*:\s*["']([^"']+)""").find(html)?.groupValues?.get(1) ?: return null
        val expires = Regex("""expires["']\s*:\s*["']([^"']+)""").find(html)?.groupValues?.get(1) ?: return null
        val playlist = Regex("""url\s*:\s*["']([^"']+)""").find(html)?.groupValues?.get(1) ?: return null

        val sep = if (playlist.contains("?")) "&" else "?"
        val masterUrl = "$playlist${sep}token=$token&expires=$expires&h=1"

        StreamResult(
            providerName = name,
            sources = listOf(StreamSource(url = masterUrl, quality = "1080", format = "hls", isM3u8 = true)),
            headers = mapOf("Referer" to pageUrl)
        )
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 3. CineSu Provider (providers/cinesu/cinesu.ts)
// ----------------------------------------------------------------------

class CineSuProvider(private val client: HttpClient) : StreamProvider {
    override val name = "CineSu"
    private val baseUrl = "https://cine.su"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val streamUrl = if (request.type == MediaType.MOVIE) {
            "$baseUrl/v1/stream/master/movie/${request.tmdbId}.m3u8"
        } else {
            "$baseUrl/v1/stream/master/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}.m3u8"
        }

        val response = client.get(streamUrl) {
            header("Referer", "$baseUrl/en/watch")
            header("Origin", baseUrl)
        }

        if (response.status == HttpStatusCode.OK) {
            StreamResult(
                providerName = name,
                sources = listOf(StreamSource(url = streamUrl, quality = "1080", format = "hls", isM3u8 = true)),
                headers = mapOf("Referer" to "$baseUrl/en/watch", "Origin" to baseUrl)
            )
        } else null
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 4. Icefy Provider (providers/icefy/icefy.ts)
// ----------------------------------------------------------------------

@Serializable
private data class IcefyResponse(val stream: String? = null)

class IcefyProvider(private val client: HttpClient) : StreamProvider {
    override val name = "Icefy"
    private val baseUrl = "https://streams.icefy.top"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val url = if (request.type == MediaType.MOVIE) {
            "$baseUrl/movie/${request.tmdbId}"
        } else {
            "$baseUrl/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
        }

        val resp: IcefyResponse = client.get(url) {
            header("Referer", baseUrl)
            header("Origin", baseUrl)
        }.body()

        val streamUrl = resp.stream ?: return null
        StreamResult(
            providerName = name,
            sources = listOf(StreamSource(url = streamUrl, quality = "1080", format = "hls", isM3u8 = true)),
            headers = mapOf("Referer" to baseUrl, "Origin" to baseUrl)
        )
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 5. Popr Provider (providers/popr/popr.ts)
// ----------------------------------------------------------------------

@Serializable
private data class PoprVidnestStream(val url: String? = null, val quality: String? = null)

@Serializable
private data class PoprVidnestSub(val url: String? = null, val lang: String? = null)

@Serializable
private data class PoprVidnestResult(
    val streams: List<PoprVidnestStream>? = null,
    val subtitles: List<PoprVidnestSub>? = null
)

@Serializable
private data class PoprResponse(val results: List<PoprVidnestResult>? = null)

class PoprProvider(private val client: HttpClient) : StreamProvider {
    override val name = "Popr"
    private val baseUrl = "https://popr.ink"
    private val servers = listOf("default", "catflix", "hexa", "Gama", "Liligoon", "Sigma", "Prime", "Alfa", "Lamda", "ynx_vidsrc")

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        for (server in servers) {
            try {
                val url = if (request.type == MediaType.MOVIE) {
                    "$baseUrl/api/vidnest?id=${request.tmdbId}&type=movie" + if (server != "default") "&server=$server" else ""
                } else {
                    "$baseUrl/api/vidnest?id=${request.tmdbId}&type=tv&server=$server&season=${request.season ?: 1}&episode=${request.episode ?: 1}"
                }

                val resp: PoprResponse = client.get(url) {
                    header("Referer", "$baseUrl/")
                }.body()

                val firstRes = resp.results?.firstOrNull() ?: continue
                val stream = firstRes.streams?.firstOrNull()?.url ?: continue

                val subs = firstRes.subtitles?.mapNotNull { sub ->
                    sub.url?.let { SubtitleTrack(url = it, language = sub.lang ?: "eng", label = sub.lang) }
                } ?: emptyList()

                return@coroutineScope StreamResult(
                    providerName = name,
                    sources = listOf(StreamSource(url = stream, quality = "Auto", isM3u8 = stream.contains(".m3u8"))),
                    subtitles = subs,
                    headers = mapOf("Referer" to "$baseUrl/")
                )
            } catch (_: Exception) { continue }
        }
        null
    }
}

// ----------------------------------------------------------------------
// 6. VidApi Provider (providers/vidapi/vidapi.ts)
// ----------------------------------------------------------------------

@Serializable
private data class VidApiData(
    @SerialName("stream_urls") val streamUrls: List<String>? = null,
    @SerialName("file_name") val fileName: String? = null
)

@Serializable
private data class VidApiSub(val lang: String = "", val url: String = "")

@Serializable
private data class VidApiResponse(
    @SerialName("status_code") val statusCode: String = "",
    val data: VidApiData? = null,
    @SerialName("default_subs") val defaultSubs: List<VidApiSub>? = null
)

class VidApiProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidApi"
    private val apiUrl = "https://streamdata.vaplayer.ru/api.php"
    private val iframeUrl = "https://brightpathsignals.com"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val typeStr = if (request.type == MediaType.MOVIE) "movie" else "tv"
        val response: VidApiResponse = client.get(apiUrl) {
            url {
                parameters.append("tmdb", request.tmdbId)
                parameters.append("type", typeStr)
                if (request.type == MediaType.SERIES) {
                    parameters.append("season", (request.season ?: 1).toString())
                    parameters.append("episode", (request.episode ?: 1).toString())
                }
            }
            header("Referer", "$iframeUrl/")
            header("Origin", iframeUrl)
        }.body()

        if (response.statusCode != "200" || response.data?.streamUrls.isNullOrEmpty()) null
        else {
            val sources = response.data?.streamUrls?.map { url ->
                val isMp4 = url.contains(".mp4") || url.contains(".mkv")
                StreamSource(url = url, quality = "Auto", format = if (isMp4) "mp4" else "hls", isM3u8 = !isMp4)
            } ?: emptyList()

            val subs = response.defaultSubs?.map { SubtitleTrack(url = it.url, language = it.lang, label = it.lang) } ?: emptyList()

            StreamResult(
                providerName = name,
                sources = sources,
                subtitles = subs,
                headers = mapOf("Referer" to "$iframeUrl/", "Origin" to iframeUrl)
            )
        }
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 7. FShare Provider (providers/fshare/fshare.ts)
// ----------------------------------------------------------------------

@Serializable
private data class FshareSourceItem(val src: String = "", val label: String? = null, val type: String = "video/mp4")

@Serializable
private data class FshareFileData(val sources: List<FshareSourceItem>? = null, val backups: List<FshareSourceItem>? = null)

@Serializable
private data class FshareData(val file: FshareFileData? = null)

@Serializable
private data class FshareResponse(val status: String = "", val data: FshareData? = null)

class FShareProvider(private val client: HttpClient) : StreamProvider {
    override val name = "FShare"
    private val baseUrl = "https://fsharetv.cc"
    private val trailer = "Png81APqcxU"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        if (request.type != MediaType.MOVIE) return null
        val imdbId = request.imdbId ?: return null
        val moviePage = client.get("$baseUrl/movie/$imdbId") {
            header("Referer", baseUrl)
        }.bodyAsText()

        val watchPath = Regex("""href="(/w/[^"]+)"""").find(moviePage)?.groupValues?.get(1) ?: return null
        val watchPage = client.get("$baseUrl$watchPath") {
            header("Referer", baseUrl)
        }.bodyAsText()

        val patterns = listOf(
            Regex("""Movie\.setSource\(["']([^"']+)["']"""),
            Regex("""setSource\(["']([^"']+)["']"""),
            Regex(""""source_id"\s*:\s*["']([^"']+)["']"""),
            Regex("""source_id\s*=\s*["']([^"']+)["']"""),
            Regex(""""file_id"\s*:\s*["']([^"']+)["']""")
        )

        val sourceId = patterns.firstNotNullOfOrNull { it.find(watchPage)?.groupValues?.get(1) } ?: return null

        val apiResp: FshareResponse = client.get("$baseUrl/api/file/$sourceId/source?trailer=$trailer&type=watch") {
            header("Referer", "$baseUrl/")
            header("X-Requested-With", "XMLHttpRequest")
        }.body()

        val items = (apiResp.data?.file?.sources ?: emptyList()) + (apiResp.data?.file?.backups ?: emptyList())
        val sources = items.filter { it.src.isNotBlank() }.map { s ->
            val fullUrl = if (s.src.startsWith("http")) s.src else "$baseUrl${s.src}"
            StreamSource(url = fullUrl, quality = s.label ?: "Auto", format = "mp4", isM3u8 = false)
        }

        if (sources.isEmpty()) null
        else StreamResult(providerName = name, sources = sources, headers = mapOf("Referer" to baseUrl))
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 8. VidNest Provider (providers/vidnest/vidnest.ts & decrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class VidnestEncryptedResponse(val encrypted: Boolean = false, val data: String = "")

class VidNestProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidNest"
    private val apiBaseUrl = "https://new.vidnest.fun"
    private val baseUrl = "https://vidnest.fun"
    private val servers = listOf(
        "moviebox" to "",
        "allmovies" to "",
        "catflix" to "",
        "purstream" to "",
        "hollymoviehd" to "",
        "lamda" to "",
        "flixhq" to "",
        "vidlink" to "",
        "onehd" to "?server=upcloud",
        "klikxxi" to ""
    )

    private val alphabet = "RB0fpH8ZEyVLkv7c2i6MAJ5u3IKFDxlS1NTsnGaqmXYdUrtzjwObCgQP94hoeW+/="
    private val reverseMap = IntArray(256) { 64 }.also { map ->
        for (i in alphabet.indices) {
            map[alphabet[i].code] = i
        }
    }

    private fun decodeCustomBase64(input: String): String {
        var padded = input.trim()
        val mod = padded.length % 4
        if (mod != 0) padded += "=".repeat(4 - mod)

        val bytes = ArrayList<Byte>()
        var i = 0
        while (i < padded.length) {
            val c0 = reverseMap[padded[i].code]
            val c1 = reverseMap[padded[i + 1].code]
            val c2 = if (padded[i + 2] == '=') 64 else reverseMap[padded[i + 2].code]
            val c3 = if (padded[i + 3] == '=') 64 else reverseMap[padded[i + 3].code]

            bytes.add(((c0 shl 2) or (c1 shr 4)).toByte())
            if (c2 != 64) bytes.add((((c1 and 0x0F) shl 4) or (c2 shr 2)).toByte())
            if (c3 != 64) bytes.add((((c2 and 0x03) shl 6) or c3).toByte())
            i += 4
        }
        return bytes.toByteArray().decodeToString()
    }

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        for ((server, query) in servers) {
            try {
                val url = if (request.type == MediaType.MOVIE) {
                    "$apiBaseUrl/$server/movie/${request.tmdbId}$query"
                } else {
                    "$apiBaseUrl/$server/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}$query"
                }

                val encResp: VidnestEncryptedResponse = client.get(url) {
                    header("Referer", "$baseUrl/")
                    header("Origin", baseUrl)
                }.body()

                val decodedJson = decodeCustomBase64(encResp.data)
                val jsonTree = Json.parseToJsonElement(decodedJson).jsonObject

                val foundUrls = mutableListOf<String>()

                // 1. Top-level url string (onehd)
                val directUrl = jsonTree["url"]?.jsonPrimitive?.contentOrNull
                if (!directUrl.isNullOrBlank() && directUrl.startsWith("http")) {
                    foundUrls.add(directUrl)
                }

                // 2. Vidlink format (data.stream.playlist)
                val vidlinkUrl = jsonTree["data"]?.jsonObject?.get("stream")?.jsonObject?.get("playlist")?.jsonPrimitive?.contentOrNull
                if (!vidlinkUrl.isNullOrBlank() && vidlinkUrl.startsWith("http")) {
                    foundUrls.add(vidlinkUrl)
                }

                // 3. Arrays: sources, streams, url
                val sourcesArray = jsonTree["sources"]?.jsonArray
                    ?: jsonTree["streams"]?.jsonArray
                    ?: jsonTree["url"]?.jsonArray

                sourcesArray?.forEach { elem ->
                    val link = elem.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                        ?: elem.jsonObject["file"]?.jsonPrimitive?.contentOrNull
                        ?: elem.jsonObject["link"]?.jsonPrimitive?.contentOrNull
                    if (!link.isNullOrBlank() && link.startsWith("http")) foundUrls.add(link)
                }

                if (foundUrls.isNotEmpty()) {
                    return@coroutineScope StreamResult(
                        providerName = name,
                        sources = foundUrls.map { StreamSource(url = it, quality = "Auto", isM3u8 = it.contains(".m3u8")) },
                        headers = mapOf("Referer" to "$baseUrl/")
                    )
                }
            } catch (_: Exception) { continue }
        }
        null
    }
}

// ----------------------------------------------------------------------
// 9. AnyEmbed Provider (providers/anyembed/anyembed.ts)
// ----------------------------------------------------------------------

@Serializable
private data class AnyEmbedSession(val token: String? = null)

@Serializable
private data class AnyEmbedStreamItem(val url: String = "", val quality: String? = null)

@Serializable
private data class AnyEmbedSourceItem(val streams: List<AnyEmbedStreamItem>? = null)

@Serializable
private data class AnyEmbedResponse(val success: Boolean = false, val sources: List<AnyEmbedSourceItem>? = null)

class AnyEmbedProvider(private val client: HttpClient) : StreamProvider {
    override val name = "AnyEmbed"
    private val baseUrl = "https://api.anyembed.xyz"
    private val frontendUrl = "https://anyembed.xyz"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val sessionResp: AnyEmbedSession = client.get("$baseUrl/api/v1/session") {
            header("Referer", frontendUrl)
            header("Origin", frontendUrl)
        }.body()

        val token = sessionResp.token ?: return null

        val response: AnyEmbedResponse = client.get("$baseUrl/api/v1/stream/${request.tmdbId}") {
            header("Referer", frontendUrl)
            header("Origin", frontendUrl)
            header("x-session-token", token)
        }.body()

        val streams = response.sources?.flatMap { it.streams ?: emptyList() } ?: emptyList()
        if (streams.isEmpty()) null
        else StreamResult(
            providerName = name,
            sources = streams.map { StreamSource(url = it.url, quality = it.quality ?: "Auto", isM3u8 = it.url.contains(".m3u8")) }
        )
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 10. Videasy Provider (providers/videasy/videasy.ts & decryptor.ts)
// ----------------------------------------------------------------------

@Serializable
private data class DecVideasyRequest(val text: String, val id: String)

@Serializable
private data class DecSource(val url: String = "", val quality: String? = null)

@Serializable
private data class DecSubtitle(val url: String = "", val lang: String? = null)

@Serializable
private data class DecResult(val sources: List<DecSource>? = null, val subtitles: List<DecSubtitle>? = null)

@Serializable
private data class DecResponse(val status: Int = 0, val result: DecResult? = null)

class VidEasyProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidEasy"
    private val endpoints = listOf(
        "https://api.videasy.net/mb-flix/sources-with-title",
        "https://api2.videasy.net/cuevana/sources-with-title",
        "https://api.videasy.net/1movies/sources-with-title",
        "https://api.videasy.net/superflix/sources-with-title",
        "https://api.videasy.net/lamovie/sources-with-title",
        "https://api.videasy.net/cdn/sources-with-title"
    )

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        for (endpoint in endpoints) {
            try {
                val blob = client.get(endpoint) {
                    url {
                        parameters.append("title", request.title ?: "")
                        parameters.append("mediaType", if (request.type == MediaType.MOVIE) "movie" else "tv")
                        parameters.append("tmdbId", request.tmdbId)
                        parameters.append("imdbId", request.imdbId ?: "")
                        parameters.append("episodeId", (request.episode ?: 1).toString())
                        parameters.append("seasonId", (request.season ?: 1).toString())
                        if (request.type == MediaType.MOVIE && request.releaseYear != null && request.releaseYear > 0) {
                            parameters.append("year", request.releaseYear.toString())
                        }
                    }
                    header("Referer", "https://player.videasy.net/")
                    header("Origin", "https://player.videasy.net")
                }.bodyAsText()

                if (blob.length < 10) continue

                val dec: DecResponse = client.post("https://enc-dec.app/api/dec-videasy") {
                    contentType(ContentType.Application.Json)
                    setBody(DecVideasyRequest(text = blob, id = request.tmdbId))
                }.body()

                val sources = dec.result?.sources?.map {
                    StreamSource(url = it.url, quality = it.quality ?: "Auto", isM3u8 = it.url.contains(".m3u8"))
                } ?: emptyList()

                val subs = dec.result?.subtitles?.map {
                    SubtitleTrack(url = it.url, language = it.lang ?: "eng", label = it.lang)
                } ?: emptyList()

                if (sources.isNotEmpty()) {
                    return@coroutineScope StreamResult(providerName = name, sources = sources, subtitles = subs)
                }
            } catch (_: Exception) { continue }
        }
        null
    }
}

// ----------------------------------------------------------------------
// 11. FMovies4U Provider (providers/fmovies4u/fmovies4u.ts & decrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class FMoviesUrlItem(val link: String = "", val quality: String? = null, val type: String? = null)

@Serializable
private data class FMoviesTrackItem(val file: String = "", val label: String? = null)

@Serializable
private data class FMoviesSourceGroup(val url: List<FMoviesUrlItem>? = null, val tracks: List<FMoviesTrackItem>? = null)

@Serializable
private data class FMoviesResponse(val success: Boolean = false, val sources: List<FMoviesSourceGroup>? = null)

class FMovies4UProvider(private val client: HttpClient) : StreamProvider {
    override val name = "FMovies4U"
    private val baseUrl = "https://fmovies4u.com"
    private val key = "Fm0v1es4U_2026_SecureKey_DoNotShare_v2_Protected".encodeToByteArray()

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val path = if (request.type == MediaType.MOVIE) {
            "/api/movie/${request.tmdbId}"
        } else {
            "/api/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
        }

        val referer = if (request.type == MediaType.MOVIE) {
            "$baseUrl/embed/movie/${request.tmdbId}?autoPlay=false"
        } else {
            "$baseUrl/embed/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}?autoPlay=false"
        }

        val encryptedText = client.get("$baseUrl$path") {
            header("Referer", referer)
        }.bodyAsText()

        // OpenSSL / CryptoJS AES-CBC format: Salted__ (8 bytes) + 8 bytes salt + ciphertext
        val rawBytes = CryptoUtils.base64Decode(encryptedText)
        val decryptedJson = if (rawBytes.size > 16 && rawBytes.take(8).toByteArray().decodeToString() == "Salted__") {
            val salt = rawBytes.sliceArray(8 until 16)
            val cipher = rawBytes.sliceArray(16 until rawBytes.size)
            // EVP_BytesToKey equivalent for OpenSSL MD5
            val keyIv = evpBytesToKey(key, salt, 32, 16)
            val plain = StreamingCrypto.aesDecryptCbc(cipher, keyIv.first, keyIv.second)
            plain.decodeToString()
        } else {
            // direct AES-CBC with key padded
            val paddedKey = ByteArray(32)
            System.arraycopy(key, 0, paddedKey, 0, minOf(key.size, 32))
            val plain = StreamingCrypto.aesDecryptCbc(rawBytes, paddedKey, ByteArray(16))
            plain.decodeToString()
        }

        val parsed: FMoviesResponse = Json { ignoreUnknownKeys = true }.decodeFromString(decryptedJson)
        val sources = parsed.sources?.flatMap { group ->
            group.url?.map {
                StreamSource(url = it.link, quality = it.quality ?: "Auto", isM3u8 = it.link.contains(".m3u8"))
            } ?: emptyList()
        } ?: emptyList()

        if (sources.isEmpty()) null
        else StreamResult(providerName = name, sources = sources)
    } catch (_: Exception) { null }

    private fun evpBytesToKey(password: ByteArray, salt: ByteArray, keyLen: Int, ivLen: Int): Pair<ByteArray, ByteArray> {
        val keyIv = ByteArray(keyLen + ivLen)
        var lastDigest = ByteArray(0)
        var offset = 0
        while (offset < keyIv.size) {
            val data = lastDigest + password + salt
            lastDigest = StreamingCrypto.md5(data)
            val count = minOf(lastDigest.size, keyIv.size - offset)
            System.arraycopy(lastDigest, 0, keyIv, offset, count)
            offset += count
        }
        val key = keyIv.sliceArray(0 until keyLen)
        val iv = keyIv.sliceArray(keyLen until keyLen + ivLen)
        return Pair(key, iv)
    }
}

// ----------------------------------------------------------------------
// 12. VidRock Provider (providers/vidrock/vidrock.ts & encrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class VidrockStreamItem(val url: String = "", val language: String? = null)

@Serializable
private data class VidrockSubItem(val label: String = "", val file: String = "")

class VidRockProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidRock"
    private val baseUrl = "https://vidrock.net/"
    private val subBaseUrl = "https://sub.vdrk.site"
    private val passphrase = "x7k9mPqT2rWvY8zA5bC3nF6hJ2lK4mN9"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val itemId = if (request.type == MediaType.SERIES) {
            "${request.tmdbId}_${request.season ?: 1}_${request.episode ?: 1}"
        } else {
            request.tmdbId
        }

        val keyBytes = passphrase.encodeToByteArray()
        val ivBytes = passphrase.substring(0, 16).encodeToByteArray()
        val encrypted = StreamingCrypto.aesEncryptCbc(itemId.encodeToByteArray(), keyBytes, ivBytes)
        val urlSafeEncrypted = CryptoUtils.base64UrlEncodeNoPadding(encrypted)

        val typeStr = if (request.type == MediaType.MOVIE) "movie" else "tv"
        val streamsJson = client.get("${baseUrl}api/$typeStr/$urlSafeEncrypted") {
            header("Referer", baseUrl)
            header("Origin", baseUrl)
        }.bodyAsText()

        val jsonTree = Json.parseToJsonElement(streamsJson).jsonObject
        val sources = mutableListOf<StreamSource>()
        for (item in jsonTree.values) {
            val itemJson = item.jsonObject
            val streamUrl = itemJson["url"]?.jsonPrimitive?.contentOrNull ?: continue
            val audioLanguage = itemJson["language"]?.jsonPrimitive?.contentOrNull
            if (streamUrl.contains("hls2.vdrk.site")) {
                try {
                    val secondJson = client.get(streamUrl) {
                        header("Referer", baseUrl)
                    }.bodyAsText()
                    val cdnArray = Json.parseToJsonElement(secondJson).jsonArray
                    cdnArray.forEach { cdnElem ->
                        val rawUrl = cdnElem.jsonObject["url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                        val finalUrl = if (rawUrl.startsWith("https://proxy.vidrock.store/")) {
                            rawUrl.removePrefix("https://proxy.vidrock.store/").removePrefix("/").decodeURLQueryComponent()
                        } else rawUrl
                        val res = cdnElem.jsonObject["resolution"]?.jsonPrimitive?.contentOrNull ?: "1080"
                        sources.add(
                            StreamSource(
                                url = finalUrl,
                                quality = "${res}p",
                                format = if (finalUrl.contains(".mp4")) "mp4" else "hls",
                                isM3u8 = !finalUrl.contains(".mp4"),
                                audioLanguage = cdnElem.jsonObject["language"]?.jsonPrimitive?.contentOrNull ?: audioLanguage,
                            ),
                        )
                    }
                } catch (_: Exception) {}
            } else {
                sources.add(
                    StreamSource(
                        url = streamUrl,
                        quality = "1080",
                        isM3u8 = streamUrl.contains(".m3u8"),
                        audioLanguage = audioLanguage,
                    ),
                )
            }
        }

        val subs = try {
            val subUrl = if (request.type == MediaType.SERIES) {
                "$subBaseUrl/v2/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
            } else {
                "$subBaseUrl/v2/movie/${request.tmdbId}"
            }
            val subList: List<VidrockSubItem> = client.get(subUrl) {
                header("Referer", baseUrl)
            }.body()
            subList.map { SubtitleTrack(url = it.file, language = it.label, label = it.label) }
        } catch (_: Exception) { emptyList() }

        if (sources.isEmpty()) null
        else StreamResult(providerName = name, sources = sources, subtitles = subs)
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 13. StreamMafia Provider (providers/streammafia/streammafia.ts & decrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class MafiaSessionResponse(val token: String? = null)

@Serializable
private data class MafiaEncryptedPayload(val iv: String = "", val tag: String = "", val data: String = "")

@Serializable
private data class MafiaDownloadItem(val url: String? = null, val quality: String? = null)

@Serializable
private data class MafiaStreamData(
    val hls_streaming: String? = null,
    val download: List<MafiaDownloadItem>? = null
)

@Serializable
private data class MafiaApiResponse(val stream: MafiaStreamData? = null)

class StreamMafiaProvider(private val client: HttpClient) : StreamProvider {
    override val name = "StreamMafia"
    private val baseUrl = "https://player.nhdapi.com"
    private val secret = "Z9#rL!v2K*5qP&7mXw"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val sessRes = client.post("$baseUrl/api/session") {
            header("Referer", "$baseUrl/")
            header("Origin", baseUrl)
        }
        val setCookie = sessRes.headers["Set-Cookie"] ?: ""
        val cookie = if (setCookie.isNotBlank()) {
            setCookie.split(';').firstOrNull() ?: ""
        } else {
            "vid_session=" + CryptoUtils.base64Encode("""{"id":"${request.tmdbId}","iat":${System.currentTimeMillis() / 1000}}""")
        }

        val tokenResp: MafiaSessionResponse = client.get("$baseUrl/api/token") {
            header("Referer", "$baseUrl/")
            header("Origin", baseUrl)
            if (cookie.isNotBlank()) header("Cookie", cookie)
        }.body()
        val token = tokenResp.token ?: ""

        val pageUrl = if (request.type == MediaType.MOVIE) {
            "$baseUrl/api/movie/?id=${request.tmdbId}"
        } else {
            "$baseUrl/api/?tv=${request.tmdbId}&season=${request.season ?: 1}&episode=${request.episode ?: 1}"
        }

        val encPayload: MafiaEncryptedPayload = client.get(pageUrl) {
            header("Referer", "$baseUrl/")
            header("Origin", baseUrl)
            header("x-api-token", token)
            header("x-content-id", request.tmdbId)
            if (cookie.isNotBlank()) header("Cookie", cookie)
        }.body()

        val iv = CryptoUtils.base64Decode(encPayload.iv)
        val tag = CryptoUtils.base64Decode(encPayload.tag)
        val ciphertext = CryptoUtils.base64Decode(encPayload.data)
        val cipherWithTag = ciphertext + tag

        val key = StreamingCrypto.sha256(secret.encodeToByteArray())
        val plainBytes = StreamingCrypto.aesDecryptGcm(cipherWithTag, key, iv)
        val api: MafiaApiResponse = Json { ignoreUnknownKeys = true }.decodeFromString(plainBytes.decodeToString())

        val sources = mutableListOf<StreamSource>()
        api.stream?.hls_streaming?.let {
            sources.add(StreamSource(url = it, quality = "Auto", format = "hls", isM3u8 = true))
        }
        api.stream?.download?.forEach { dl ->
            if (!dl.url.isNullOrBlank()) {
                sources.add(StreamSource(url = dl.url, quality = dl.quality ?: "Auto", format = "mp4", isM3u8 = dl.url.contains(".m3u8")))
            }
        }

        if (sources.isEmpty()) null
        else StreamResult(providerName = name, sources = sources)
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 14. Peachify Provider (providers/peachify/peachify.ts & decrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class PeachifySourceItem(
    val url: String? = null,
    val src: String? = null,
    val file: String? = null,
    val stream: String? = null,
    val streamUrl: String? = null,
    val playbackUrl: String? = null,
    val quality: String? = null,
    val type: String? = null
) {
    val finalUrl: String? get() = url ?: src ?: file ?: stream ?: streamUrl ?: playbackUrl
}

@Serializable
private data class PeachifyApiResponse(
    val isEncrypted: Boolean = false,
    val data: String? = null,
    val sources: List<PeachifySourceItem>? = null
)

class PeachifyProvider(private val client: HttpClient) : StreamProvider {
    override val name = "Peachify"
    private val keyB64 = "YThmMmExYjVlOWM0NzA4MTRmNmIyYzNhNWQ4ZTdmOWMxYTJiM2M0ZDVlM2Y3YThiOGNhZDFlMmQwYTRkNWM1Yg=="
    private val servers = listOf(
        "https://uwu.eat-peach.sbs/moviebox",
        "https://usa.eat-peach.sbs/holly",
        "https://usa.eat-peach.sbs/air",
        "https://usa.eat-peach.sbs/multi",
        "https://uwu.eat-peach.sbs/net",
        "https://uwu.eat-peach.sbs/bmb"
    )

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        val keyHex = CryptoUtils.base64Decode(keyB64).decodeToString()
        val keyBytes = CryptoUtils.hexToBytes(keyHex)

        for (server in servers) {
            try {
                val url = if (request.type == MediaType.MOVIE) {
                    "$server/movie/${request.tmdbId}"
                } else {
                    "$server/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
                }

                val resp: PeachifyApiResponse = client.get(url) {
                    header("Referer", "https://peachify.top/")
                    header("Origin", "https://peachify.top")
                }.body()

                val sources = if (resp.isEncrypted && !resp.data.isNullOrBlank()) {
                    val parts = resp.data.split('.')
                    if (parts.size == 3) {
                        val iv = CryptoUtils.base64UrlDecode(parts[0])
                        val ct = CryptoUtils.base64UrlDecode(parts[1])
                        val tag = CryptoUtils.base64UrlDecode(parts[2])
                        val decrypted = StreamingCrypto.aesDecryptGcm(ct + tag, keyBytes, iv)
                        val decResp: PeachifyApiResponse = Json { ignoreUnknownKeys = true }.decodeFromString(decrypted.decodeToString())
                        decResp.sources ?: emptyList()
                    } else emptyList()
                } else resp.sources ?: emptyList()

                val validSources = sources.mapNotNull { item ->
                    item.finalUrl?.let { StreamSource(url = it, quality = item.quality ?: "Auto", isM3u8 = it.contains(".m3u8")) }
                }

                if (validSources.isNotEmpty()) {
                    return@coroutineScope StreamResult(
                        providerName = name,
                        sources = validSources
                    )
                }
            } catch (_: Exception) { continue }
        }
        null
    }
}

// ----------------------------------------------------------------------
// 15. VidZee Provider (providers/vidzee/vidzee.ts & decrypt.ts)
// ----------------------------------------------------------------------

@Serializable
private data class VidZeeLinkItem(val link: String = "")

@Serializable
private data class VidZeeTrackItem(val url: String = "", val lang: String = "")

@Serializable
private data class VidZeeServerResponse(val url: List<VidZeeLinkItem> = emptyList(), val tracks: List<VidZeeTrackItem> = emptyList())

class VidZeeProvider(private val client: HttpClient) : StreamProvider {
    override val name = "VidZee"
    private val coreUrl = "https://core.vidzee.wtf"
    private val playerUrl = "https://player.vidzee.wtf"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        try {
            val keyBlob = client.get("$coreUrl/api-key") {
                header("Referer", playerUrl)
                header("Origin", playerUrl)
            }.bodyAsText()

            val rawBytes = CryptoUtils.base64Decode(keyBlob)
            if (rawBytes.size <= 28) return@coroutineScope null

            val iv = rawBytes.sliceArray(0 until 12)
            val tag = rawBytes.sliceArray(12 until 28)
            val cipher = rawBytes.sliceArray(28 until rawBytes.size)

            val deriveKeySecret = StreamingCrypto.sha256("4f2a9c7d1e8b3a6f0d5c2e9a7b1f4d8c".encodeToByteArray())
            val derivedDecrypted = StreamingCrypto.aesDecryptGcm(cipher + tag, deriveKeySecret, iv)
            val aesKeyStr = derivedDecrypted.decodeToString()

            val aesKeyBytes = ByteArray(32)
            val strBytes = aesKeyStr.encodeToByteArray()
            System.arraycopy(strBytes, 0, aesKeyBytes, 0, minOf(strBytes.size, 32))

            for (sr in 0..6) {
                try {
                    val serverUrl = if (request.type == MediaType.MOVIE) {
                        "$playerUrl/api/server?id=${request.tmdbId}&sr=$sr"
                    } else {
                        "$playerUrl/api/server?id=${request.tmdbId}&sr=$sr&ss=${request.season ?: 1}&ep=${request.episode ?: 1}"
                    }

                    val sResp: VidZeeServerResponse = client.get(serverUrl) {
                        header("Referer", playerUrl)
                        header("Origin", playerUrl)
                    }.body()

                    val links = sResp.url.mapNotNull { item ->
                        try {
                            val decoded = CryptoUtils.base64Decode(item.link).decodeToString()
                            val parts = decoded.split(':')
                            if (parts.size == 2) {
                                val streamIv = CryptoUtils.base64Decode(parts[0])
                                val streamCipher = CryptoUtils.base64Decode(parts[1])
                                val plain = StreamingCrypto.aesDecryptCbc(streamCipher, aesKeyBytes, streamIv)
                                plain.decodeToString()
                            } else null
                        } catch (_: Exception) { null }
                    }.filter { it.startsWith("http") }

                    if (links.isNotEmpty()) {
                        return@coroutineScope StreamResult(
                            providerName = name,
                            sources = links.map { StreamSource(url = it, quality = "Auto", isM3u8 = it.contains(".m3u8")) },
                            subtitles = sResp.tracks.map { SubtitleTrack(url = it.url, language = it.lang, label = it.lang) }
                        )
                    }
                } catch (_: Exception) { continue }
            }
        } catch (_: Exception) { null }
        null
    }
}

// ----------------------------------------------------------------------
// 16. 02MovieDownloader Provider (providers/02moviedownloader/02moviedownloader.ts)
// ----------------------------------------------------------------------

@Serializable
private data class Movie02Token(val token: String? = null)

@Serializable
private data class Movie02DownloadItem(val url: String = "", val resolution: String? = null)

@Serializable
private data class Movie02DataInner(val downloads: List<Movie02DownloadItem>? = null)

@Serializable
private data class Movie02DownloadData(val data: Movie02DataInner? = null)

@Serializable
private data class Movie02PayloadData(val downloadData: Movie02DownloadData? = null)

@Serializable
private data class Movie02Response(val data: Movie02PayloadData? = null)

class MovieDownloader02Provider(private val client: HttpClient) : StreamProvider {
    override val name = "02MovieDownloader"
    private val baseUrl = "https://02moviedownloader.site"

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = try {
        val tokenResp: Movie02Token = client.post("$baseUrl/api/verify-robot") {
            header("Origin", baseUrl)
            header("Referer", "$baseUrl/")
        }.body()

        val token = tokenResp.token ?: return null

        val path = if (request.type == MediaType.MOVIE) {
            "/api/download/movie/${request.tmdbId}"
        } else {
            "/api/download/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
        }

        val respText = client.get("$baseUrl$path") {
            header("x-session-token", token)
            header("Referer", "$baseUrl$path")
        }.bodyAsText()

        val decryptedJson = if (respText.contains("\"encrypted\":true")) {
            val tree = Json.parseToJsonElement(respText).jsonObject
            val bundle = tree["data"]?.jsonPrimitive?.content ?: ""
            val parts = bundle.split(':')
            val iv = CryptoUtils.base64Decode(parts[0])
            val cipher = CryptoUtils.base64Decode(parts[1])
            val key = StreamingCrypto.sha256(token.encodeToByteArray())
            StreamingCrypto.aesDecryptCbc(cipher, key, iv).decodeToString()
        } else respText

        val parsed: Movie02Response = Json { ignoreUnknownKeys = true }.decodeFromString(decryptedJson)
        val downloads = parsed.data?.downloadData?.data?.downloads ?: emptyList()

        val sources = downloads.map {
            StreamSource(url = it.url, quality = it.resolution ?: "1080", format = "mp4", isM3u8 = false)
        }

        if (sources.isEmpty()) null
        else StreamResult(providerName = name, sources = sources)
    } catch (_: Exception) { null }
}

// ----------------------------------------------------------------------
// 17. Tulnex Provider (providers/tulnex/tulnex.ts)
// ----------------------------------------------------------------------

@Serializable
private data class TulnexResponse(val payload: String? = null)

class TulnexProvider(private val client: HttpClient) : StreamProvider {
    override val name = "Tulnex"
    private val baseUrl = "https://api.tulnex.com"
    private val servers = listOf(
        "onion", "vidzee", "icefy", "tik", "vaplayer",
        "vidfast-alpha", "uniquestream", "vidfast-mega", "vidfast-vrapid",
        "allmovies", "vidlink", "vidfast-vedge", "vidfast-vfast", "moviebox"
    )

    private fun extractTulnexUrl(obj: JsonObject): String? {
        val direct = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: obj["stream"]?.jsonPrimitive?.contentOrNull
            ?: obj["playlist"]?.jsonPrimitive?.contentOrNull
            ?: obj["streamUrl"]?.jsonPrimitive?.contentOrNull
            ?: obj["stream_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["streaming_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["video_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["m3u8"]?.jsonPrimitive?.contentOrNull
        if (!direct.isNullOrBlank() && direct.startsWith("http")) return direct

        // sources array
        obj["sources"]?.jsonArray?.forEach { elem ->
            val u = elem.jsonObject["url"]?.jsonPrimitive?.contentOrNull
            if (!u.isNullOrBlank() && u.startsWith("http")) return u
        }

        // links array
        obj["links"]?.jsonArray?.forEach { elem ->
            val u = elem.jsonObject["url"]?.jsonPrimitive?.contentOrNull
            if (!u.isNullOrBlank() && u.startsWith("http")) return u
        }

        // data.stream.playlist or data.url
        val dataObj = obj["data"]?.jsonObject
        val dataStream = dataObj?.get("stream")?.jsonObject?.get("playlist")?.jsonPrimitive?.contentOrNull
        if (!dataStream.isNullOrBlank() && dataStream.startsWith("http")) return dataStream

        val dataUrl = dataObj?.get("url")?.jsonPrimitive?.contentOrNull
        if (!dataUrl.isNullOrBlank() && dataUrl.startsWith("http")) return dataUrl

        // streams array
        obj["streams"]?.jsonArray?.forEach { elem ->
            val u = elem.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                ?: elem.jsonObject["link"]?.jsonPrimitive?.contentOrNull
            if (!u.isNullOrBlank() && u.startsWith("http")) return u
        }

        return null
    }

    override suspend fun extractStreams(request: StreamRequest): StreamResult? = coroutineScope {
        for (server in servers) {
            try {
                val url = if (request.type == MediaType.MOVIE) {
                    "$baseUrl/$server/movie/${request.tmdbId}"
                } else {
                    "$baseUrl/$server/tv/${request.tmdbId}/${request.season ?: 1}/${request.episode ?: 1}"
                }

                val resp: TulnexResponse = client.get(url).body()
                val payload = resp.payload ?: continue

                // L4 HMAC separation: receivedHmac|payloadStr
                val sep = payload.indexOf('|')
                if (sep != -1) {
                    val rawPayloadB64 = payload.substring(sep + 1)
                    val l4out = CryptoUtils.base64Decode(rawPayloadB64).decodeToString()

                    // L3 AES-CBC decrypt
                    val l3parts = l4out.split('.')
                    if (l3parts.size == 3) {
                        val iv = CryptoUtils.base64Decode(l3parts[0])
                        val salt = CryptoUtils.base64Decode(l3parts[1])
                        val cipher = CryptoUtils.base64Decode(l3parts[2])
                        val l3KeyPass = CryptoUtils.base64Decode("U24wMHBEMGcjTDNfQUVTX1MzY3VyM0szeUAyMDI2JHNleA==")
                        val keyBytes = StreamingCrypto.pbkdf2(l3KeyPass, salt, 100000, 32, "SHA-512")
                        val l3Decrypted = StreamingCrypto.aesDecryptCbc(cipher, keyBytes, iv).decodeToString()

                        // L2 binary decode
                        val binDecoded = CryptoUtils.base64Decode(l3Decrypted).decodeToString()
                            .split(' ')
                            .mapNotNull { it.toIntOrNull(2)?.toChar() }
                            .joinToString("")

                        // L1 XOR decrypt
                        val l1Key = CryptoUtils.base64Decode("U24wMHBEMGcjTDFfWDBSX000c3QzckszeSEyMDI2c2V4")
                        val l1Salt = CryptoUtils.base64Decode("eEs5IW1SMkBwTDUjblE4c2V4")
                        val xorKey = StreamingCrypto.pbkdf2(l1Key, l1Salt, 50000, 32, "SHA-256")
                        val hexBytes = CryptoUtils.hexToBytes(binDecoded)
                        val outBytes = ByteArray(hexBytes.size) { i -> (hexBytes[i].toInt() xor xorKey[i % 32].toInt()).toByte() }
                        val jsonStr = outBytes.decodeToString()

                        val parsedTree = Json.parseToJsonElement(jsonStr).jsonObject
                        val streamUrl = extractTulnexUrl(parsedTree)

                        if (!streamUrl.isNullOrBlank()) {
                            return@coroutineScope StreamResult(
                                providerName = name,
                                sources = listOf(StreamSource(url = streamUrl, quality = "Auto", isM3u8 = streamUrl.contains(".m3u8")))
                            )
                        }
                    }
                }
            } catch (_: Exception) { continue }
        }
        null
    }
}

// ----------------------------------------------------------------------
// Aggregator / Manager
// ----------------------------------------------------------------------

class StreamManager(private val providers: List<StreamProvider>) {

    private fun StreamResult.unwrapped(): StreamResult = copy(
        sources = sources.map { source -> source.copy(url = ProxyUnwrapper.unwrapUrl(source.url)) }
    )

    suspend fun fetchAllStreams(request: StreamRequest): List<StreamResult> = coroutineScope {
        providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(6000) {
                        provider.extractStreams(request)?.unwrapped()
                    }
                } catch (e: Exception) {
                    println("Error in provider ${provider.name}: ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull().filter { it.sources.isNotEmpty() }
    }

    suspend fun fetchFirstAvailableStream(request: StreamRequest): StreamResult? = coroutineScope {
        val deferred = CompletableDeferred<StreamResult?>()
        val jobs = providers.map { provider ->
            launch {
                try {
                    val result = withTimeoutOrNull(6000) {
                        provider.extractStreams(request)
                    }
                    if (result != null && result.sources.isNotEmpty()) {
                        deferred.complete(result.unwrapped())
                    }
                } catch (e: Exception) {
                    println("Error in provider ${provider.name}: ${e.message}")
                }
            }
        }
        launch {
            jobs.joinAll()
            deferred.complete(null)
        }
        val winner = deferred.await()
        jobs.forEach { it.cancel() }
        winner
    }
}

// ----------------------------------------------------------------------
// Koin Module Setup
// ----------------------------------------------------------------------

val StreamingHttpClient = named("streamingHttpClient")

val streamingModule = module {
    single(StreamingHttpClient) { createStreamingHttpClient() }

    single { VidSrcProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VixSrcProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { FMovies4UProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VidZeeProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { AnyEmbedProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { CineSuProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { FShareProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { IcefyProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { PeachifyProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { PoprProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { StreamMafiaProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { TulnexProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VidApiProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VidEasyProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VidNestProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { VidRockProvider(get(StreamingHttpClient)) } bind StreamProvider::class
    single { MovieDownloader02Provider(get(StreamingHttpClient)) } bind StreamProvider::class

    single { StreamManager(getAll<StreamProvider>()) }
}
