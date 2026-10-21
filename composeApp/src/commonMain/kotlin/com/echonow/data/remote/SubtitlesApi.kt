package com.echonow.data.remote

import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.platform.computeSha256
import com.echonow.platform.extractSubtitleArchive
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SubtitleOption(
    val id: String,
    val title: String,
    val language: String,
    val format: String,
    val provider: String,
    val downloadUrl: String,
    val fileId: Long? = null,
)

class SubtitlesApi(
    private val client: HttpClient,
    private val configuredSubdlApiKey: String,
) {
    private var cachedCookie: String = ""

    suspend fun search(
        media: Media,
        language: String,
        season: Int?,
        episode: Int?,
        openSubtitlesApiKey: String,
        subdlApiKey: String = "",
    ): List<SubtitleOption> {
        val echoSubs = runCatching {
            searchEchoSubs(media, language, season, episode)
        }.getOrDefault(emptyList())

        val subdlKey = subdlApiKey.ifBlank { configuredSubdlApiKey }.trim()
        val subdlOptions = if (subdlKey.isNotEmpty()) {
            runCatching {
                searchSubdl(media, language, season, episode, subdlKey)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val openSubOptions = if (openSubtitlesApiKey.isNotBlank()) {
            runCatching {
                searchOpenSubtitles(media, language, season, episode, openSubtitlesApiKey.trim())
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return (echoSubs + subdlOptions + openSubOptions).distinctBy { it.provider to it.id }
    }

    suspend fun download(option: SubtitleOption, openSubtitlesApiKey: String, subdlApiKey: String = ""): String {
        if (option.provider == ECHO_SUBS) {
            return downloadEchoSubs(option)
        }

        val url = if (option.provider == OPEN_SUBTITLES && option.fileId != null) {
            val response = client.post("$OPEN_SUBTITLES_URL/download") {
                header("Api-Key", openSubtitlesApiKey)
                header(HttpHeaders.UserAgent, OPEN_SUBTITLES_USER_AGENT)
                contentType(ContentType.Application.Json)
                setBody(OpenSubtitlesDownloadRequest(option.fileId))
            }.body<OpenSubtitlesDownloadResponse>()
            response.link
        } else {
            option.downloadUrl
        }
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Subtitle service returned an invalid download URL."
        }
        val response = client.get(url) {
            val key = subdlApiKey.ifBlank { configuredSubdlApiKey }
            if (option.provider == SUBDL && key.isNotBlank()) {
                header("x-api-key", key)
            }
        }
        val bytes = response.body<ByteArray>()
        val text = extractSubtitleArchive(bytes)
        require(text.isNotBlank()) {
            "Subtitle download was empty or returned an unsupported archive."
        }
        return text
    }

    suspend fun testEchoSubsConnection(language: String): Boolean {
        val lan = normalizeOpenSubtitlesLanguage(language)
        val url = "$OPENSUBTITLES_BASE_URL/en/search2/sublanguageid-$lan/searchonlymovies-on/moviename-inception"
        val html = fetchHtmlWithAnubis(url)
        require(html.isNotBlank() && (html.contains("idmovie-") || html.contains("id=\"name"))) {
            "ECHOSubs API could not connect to OpenSubtitles."
        }
        return true
    }

    suspend fun testOpenSubtitlesConnection(apiKey: String) {
        require(apiKey.isNotBlank()) { "Enter an OpenSubtitles API key first." }
        val formats = client.get("$OPEN_SUBTITLES_URL/infos/formats") {
            header("Api-Key", apiKey.trim())
            header(HttpHeaders.UserAgent, OPEN_SUBTITLES_USER_AGENT)
        }.body<OpenSubtitlesFormatsResponse>()
        require(formats.data.isNotEmpty()) { "OpenSubtitles returned no supported formats." }
    }

    suspend fun testSubdlConnection(apiKey: String) {
        require(apiKey.isNotBlank()) { "Enter a SubDL API key first." }
        val response = client.get("$SUBDL_URL/subtitles") {
            parameter("api_key", apiKey.trim())
            parameter("film_name", "ECHO NOW connection test")
            parameter("languages", "EN")
        }.body<SubdlResponse>()
        require(response.status) { response.error ?: "SubDL rejected the API key." }
    }

    private suspend fun searchEchoSubs(
        media: Media,
        language: String,
        season: Int?,
        episode: Int?,
    ): List<SubtitleOption> {
        val lan = normalizeOpenSubtitlesLanguage(language)
        val cleanImdb = media.imdbId?.removePrefix("tt")?.trim()

        if (media.type == MediaType.SERIES) {
            val s = season ?: 1
            val e = episode ?: 1

            // 1. Try exact IMDB search if available
            if (!cleanImdb.isNullOrEmpty()) {
                val imdbUrl = "$OPENSUBTITLES_BASE_URL/en/search/sublanguageid-$lan/idimdb-$cleanImdb/season-$s/episode-$e"
                val html = runCatching { fetchHtmlWithAnubis(imdbUrl) }.getOrNull()
                if (!html.isNullOrBlank()) {
                    val subs = parseSubtitlesFromHtml(html, lan)
                    if (subs.isNotEmpty()) return subs
                }
            }

            // 2. Fallback to Series name search
            val query = media.title.encodeURLParameter()
            val nameUrl = "$OPENSUBTITLES_BASE_URL/en/search/sublanguageid-$lan/searchonlytvseries-on/moviename-$query/season-$s/episode-$e"
            val html = runCatching { fetchHtmlWithAnubis(nameUrl) }.getOrNull()
            if (!html.isNullOrBlank()) {
                val subs = parseSubtitlesFromHtml(html, lan)
                if (subs.isNotEmpty()) return subs
            }
            return emptyList()
        }

        // Movie search
        // 1. Try exact IMDB search if available
        if (!cleanImdb.isNullOrEmpty()) {
            val imdbUrl = "$OPENSUBTITLES_BASE_URL/en/search/sublanguageid-$lan/idimdb-$cleanImdb"
            val html = runCatching { fetchHtmlWithAnubis(imdbUrl) }.getOrNull()
            if (!html.isNullOrBlank()) {
                val subs = parseSubtitlesFromHtml(html, lan)
                if (subs.isNotEmpty()) return subs
            }
        }

        // 2. Movie Name search
        val query = media.title.encodeURLParameter()
        val nameUrl = "$OPENSUBTITLES_BASE_URL/en/search2/sublanguageid-$lan/searchonlymovies-on/moviename-$query"
        val html = runCatching { fetchHtmlWithAnubis(nameUrl) }.getOrNull()
        if (!html.isNullOrBlank()) {
            // First check if this page directly lists subtitles
            val directSubs = parseSubtitlesFromHtml(html, lan)
            if (directSubs.isNotEmpty()) return directSubs

            // If it returns a list of matching movies, fetch the first movie's subtitles page
            val movieIds = parseMovieIdsFromSearch(html)
            for (movieId in movieIds.take(2)) {
                val moviePageUrl = "$OPENSUBTITLES_BASE_URL/en/search/sublanguageid-$lan/idmovie-$movieId"
                val movieHtml = runCatching { fetchHtmlWithAnubis(moviePageUrl) }.getOrNull()
                if (!movieHtml.isNullOrBlank()) {
                    val subs = parseSubtitlesFromHtml(movieHtml, lan)
                    if (subs.isNotEmpty()) return subs
                }
            }
        }

        return emptyList()
    }

    private suspend fun downloadEchoSubs(option: SubtitleOption): String {
        // 1. Try direct download from dl.opensubtitles.org (fastest, no bot challenges)
        val candidateUrls = listOf(
            "$OPENSUBTITLES_DL_URL/en/download/sub/${option.id}",
            "$OPENSUBTITLES_DL_URL/en/download/file/${option.id}",
            "$OPENSUBTITLES_BASE_URL/en/subtitleserve/sub/${option.id}",
        )

        for (candidateUrl in candidateUrls) {
            val result = runCatching {
                val bytes = if (candidateUrl.contains("dl.opensubtitles.org")) {
                    client.get(candidateUrl) {
                        header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
                        header("Referer", "$OPENSUBTITLES_BASE_URL/")
                    }.body<ByteArray>()
                } else {
                    fetchBytesWithAnubis(candidateUrl)
                }
                extractSubtitleArchive(bytes)
            }.getOrNull()

            if (!result.isNullOrBlank() && (result.contains("-->") || result.contains("WEBVTT"))) {
                return result
            }
        }

        throw IllegalStateException("ECHOSubs API could not download valid subtitles for ${option.title}")
    }

    private suspend fun fetchHtmlWithAnubis(url: String): String {
        val bytes = fetchBytesWithAnubis(url)
        return bytes.decodeToString()
    }

    private suspend fun fetchBytesWithAnubis(url: String): ByteArray {
        val initialResponse = client.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (cachedCookie.isNotEmpty()) {
                header("Cookie", cachedCookie)
            }
        }
        val initialBytes = initialResponse.body<ByteArray>()
        val initialText = runCatching { initialBytes.decodeToString() }.getOrDefault("")

        if (!initialText.contains("anubis_challenge")) {
            return initialBytes
        }

        // Solve Anubis Proof-of-Work challenge
        val challengeJsonStr = initialText.substringAfter("id=\"anubis_challenge\"")
            .substringAfter(">")
            .substringBefore("</script>")
        val randomData = challengeJsonStr.substringAfter("\"randomData\":\"").substringBefore("\"")
        val challengeId = challengeJsonStr.substringAfter("\"id\":\"").substringBefore("\"")
        val diffStr = challengeJsonStr.substringAfter("\"difficulty\":").substringBefore("}").substringBefore(",")
        val difficulty = diffStr.trim().toIntOrNull() ?: 4

        val prefix = "0".repeat(difficulty)
        var nonce = 0L
        var hash = ""
        while (true) {
            val h = computeSha256(randomData + nonce)
            if (h.startsWith(prefix)) {
                hash = h
                break
            }
            nonce++
        }

        val setCookies = initialResponse.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        val initialCookieHeader = setCookies.joinToString("; ") { it.substringBefore(";").trim() }

        val passUrl = "$OPENSUBTITLES_BASE_URL/.within.website/x/cmd/anubis/api/pass-challenge?id=$challengeId&response=$hash&nonce=$nonce&redir=${url.encodeURLParameter()}&elapsedTime=500"
        val passResponse = client.get(passUrl) {
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
            if (initialCookieHeader.isNotEmpty()) {
                header("Cookie", initialCookieHeader)
            }
        }
        val passSetCookies = passResponse.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        val combinedCookies = (setCookies + passSetCookies)
            .map { it.substringBefore(";").trim() }
            .filter { it.contains("=") }
            .distinctBy { it.substringBefore("=") }
            .joinToString("; ")

        if (combinedCookies.isNotEmpty()) {
            cachedCookie = combinedCookies
        }

        val finalResponse = client.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_USER_AGENT)
            if (cachedCookie.isNotEmpty()) {
                header("Cookie", cachedCookie)
            }
        }
        return finalResponse.body<ByteArray>()
    }

    private fun parseSubtitlesFromHtml(html: String, lan: String): List<SubtitleOption> {
        val results = mutableListOf<SubtitleOption>()
        val rowRegex = Regex("""<tr[^>]*id="name([0-9]+)"[^>]*>(.*?)</tr>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val matches = rowRegex.findAll(html)
        for (match in matches) {
            val id = match.groupValues[1]
            val rowHtml = match.groupValues[2]

            val td1 = rowHtml.substringAfter("<td").substringBefore("</td>")
            val spanTitleMatch = Regex("""<span[^>]*title="([^"]+)"""", RegexOption.IGNORE_CASE).find(td1)
            val strongTitleMatch = Regex("""<strong[^>]*>.*?<a[^>]*>([^<]+)</a>.*?</strong>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(td1)
            val rawTitle = (spanTitleMatch?.groupValues?.get(1) ?: strongTitleMatch?.groupValues?.get(1) ?: "Subtitle $id")
                .replace(Regex("""Watch online|Download Subtitles Searcher""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            val langMatch = Regex("""<a[^>]*title="([^"]+)"[^>]*href="[^"]*sublanguageid-[^"]*"""", RegexOption.IGNORE_CASE).find(rowHtml)
            val language = langMatch?.groupValues?.get(1)?.trim() ?: getLanguageDisplayName(lan)

            val formatMatch = Regex("""<span class="p">([a-zA-Z0-9]+)</span>""", RegexOption.IGNORE_CASE).find(rowHtml)
            val format = formatMatch?.groupValues?.get(1)?.lowercase() ?: "srt"

            if (format in SUPPORTED_FORMATS || format.isBlank() || format == "sub" || format == "txt") {
                results.add(
                    SubtitleOption(
                        id = id,
                        title = rawTitle,
                        language = language,
                        format = if (format in SUPPORTED_FORMATS) format else "srt",
                        provider = ECHO_SUBS,
                        downloadUrl = "$OPENSUBTITLES_DL_URL/en/download/sub/$id",
                        fileId = id.toLongOrNull(),
                    )
                )
            }
        }
        return results
    }

    private fun parseMovieIdsFromSearch(html: String): List<String> {
        val idMovieRegex = Regex("""/idmovie-([0-9]+)""")
        return idMovieRegex.findAll(html).map { it.groupValues[1] }.distinct().toList()
    }

    private fun normalizeOpenSubtitlesLanguage(lang: String): String {
        val code = lang.trim().lowercase()
        if (code.length == 3) return code
        return when (code) {
            "en" -> "eng"
            "es" -> "spa"
            "fr" -> "fre"
            "de" -> "ger"
            "it" -> "ita"
            "pt" -> "por"
            "ru" -> "rus"
            "ja" -> "jpn"
            "ko" -> "kor"
            "zh" -> "chi"
            "ar" -> "ara"
            "hi" -> "hin"
            "tr" -> "tur"
            "pl" -> "pol"
            "nl" -> "dut"
            "sv" -> "swe"
            "da" -> "dan"
            "fi" -> "fin"
            "no" -> "nor"
            "cs" -> "cze"
            "el" -> "ell"
            "ro" -> "rum"
            "hu" -> "hun"
            "uk" -> "ukr"
            "id" -> "ind"
            "vi" -> "vie"
            "th" -> "tha"
            "he" -> "heb"
            "fa" -> "per"
            "hr" -> "hrv"
            "sr" -> "scc"
            "bg" -> "bul"
            "sk" -> "slo"
            "sl" -> "slv"
            "et" -> "est"
            "lv" -> "lav"
            "lt" -> "lit"
            "ms" -> "may"
            "bn" -> "ben"
            "ur" -> "urd"
            "all", "" -> "all"
            else -> "eng"
        }
    }

    private fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase()) {
            "eng", "en" -> "English"
            "spa", "es" -> "Spanish"
            "fre", "fra", "fr" -> "French"
            "ger", "deu", "de" -> "German"
            "ita", "it" -> "Italian"
            "por", "pt" -> "Portuguese"
            "pob" -> "Portuguese (BR)"
            "rus", "ru" -> "Russian"
            "jpn", "ja" -> "Japanese"
            "kor", "ko" -> "Korean"
            "chi", "zho", "zh" -> "Chinese"
            "zht" -> "Chinese (Trad)"
            "ara", "ar" -> "Arabic"
            "hin", "hi" -> "Hindi"
            "tur", "tr" -> "Turkish"
            "pol", "pl" -> "Polish"
            "dut", "nld", "nl" -> "Dutch"
            "swe", "sv" -> "Swedish"
            "dan", "da" -> "Danish"
            "fin", "fi" -> "Finnish"
            "nor", "no" -> "Norwegian"
            "cze", "ces", "cs" -> "Czech"
            "ell", "el" -> "Greek"
            "rum", "ron", "ro" -> "Romanian"
            "hun", "hu" -> "Hungarian"
            "ukr", "uk" -> "Ukrainian"
            "ind", "id" -> "Indonesian"
            "vie", "vi" -> "Vietnamese"
            "tha", "th" -> "Thai"
            "heb", "he" -> "Hebrew"
            "hrv", "hr" -> "Croatian"
            "scc", "srp", "sr" -> "Serbian"
            "bul", "bg" -> "Bulgarian"
            "slo", "slk", "sk" -> "Slovak"
            "slv", "sl" -> "Slovenian"
            else -> code.uppercase()
        }
    }

    private suspend fun searchSubdl(
        media: Media,
        language: String,
        season: Int?,
        episode: Int?,
        apiKey: String,
    ): List<SubtitleOption> {
        val tmdbId = media.id.substringAfter(':', media.id)
        val response = client.get("$SUBDL_URL/subtitles") {
            parameter("api_key", apiKey)
            parameter("tmdb_id", tmdbId)
            parameter("film_name", media.title)
            parameter("type", if (media.type == MediaType.MOVIE) "movie" else "tv")
            parameter("year", media.year.takeIf { it > 0 })
            parameter("languages", language.uppercase())
            parameter("subs_per_page", 30)
            parameter("unpack", 1)
            parameter("client", "custom_integration")
            if (media.type == MediaType.SERIES) {
                season?.let { parameter("season_number", it) }
                episode?.let { parameter("episode_number", it) }
            }
        }.body<SubdlResponse>()
        require(response.status) { response.error ?: "SubDL subtitle search failed." }
        return response.subtitles.flatMap { subtitle ->
            subtitle.unpackFiles.orEmpty()
                .filter { file ->
                    (season == null || file.season == null || file.season == season) &&
                        (episode == null || file.episode == null || file.episode == episode) &&
                        file.format.lowercase() in SUPPORTED_FORMATS
                }
                .mapNotNull { file ->
                    val downloadUrl = absoluteSubdlUrl(file.url)
                    downloadUrl?.let {
                        SubtitleOption(
                            id = file.fileId,
                            title = file.releaseName.ifBlank { file.name },
                            language = file.language.ifBlank { language },
                            format = file.format,
                            provider = SUBDL,
                            downloadUrl = it,
                        )
                    }
                }
        }
    }

    private suspend fun searchOpenSubtitles(
        media: Media,
        language: String,
        season: Int?,
        episode: Int?,
        apiKey: String,
    ): List<SubtitleOption> {
        val response = client.get("$OPEN_SUBTITLES_URL/subtitles") {
            header("Api-Key", apiKey)
            header(HttpHeaders.UserAgent, OPEN_SUBTITLES_USER_AGENT)
            parameter("languages", language.lowercase())
            parameter("type", if (media.type == MediaType.MOVIE) "movie" else "episode")
            media.imdbId?.let { parameter("imdb_id", it) }
                ?: parameter("query", media.title)
            if (media.type == MediaType.SERIES) {
                season?.let { parameter("season_number", it) }
                episode?.let { parameter("episode_number", it) }
            }
        }.body<OpenSubtitlesSearchResponse>()
        return response.data.flatMap { result ->
            result.attributes.files.filter { file ->
                file.fileName.substringAfterLast('.', "srt").lowercase() in SUPPORTED_FORMATS
            }.map { file ->
                SubtitleOption(
                    id = "${result.id}:${file.fileId}",
                    title = result.attributes.release ?: file.fileName,
                    language = result.attributes.language,
                    format = file.fileName.substringAfterLast('.', "srt"),
                    provider = OPEN_SUBTITLES,
                    downloadUrl = "",
                    fileId = file.fileId,
                )
            }
        }
    }

    private fun absoluteSubdlUrl(url: String): String? = when {
        url.startsWith("https://") || url.startsWith("http://") -> url
        url.startsWith("/") -> "$SUBDL_DOWNLOAD_URL$url"
        else -> null
    }

    companion object {
        const val ECHO_SUBS = "ECHOSubs API"
        const val SUBDL = "SubDL"
        const val OPEN_SUBTITLES = "OpenSubtitles"
        private const val OPENSUBTITLES_BASE_URL = "https://www.opensubtitles.org"
        private const val OPENSUBTITLES_DL_URL = "https://dl.opensubtitles.org"
        private const val SUBDL_URL = "https://api.subdl.com/api/v1"
        private const val SUBDL_DOWNLOAD_URL = "https://dl.subdl.com"
        private const val OPEN_SUBTITLES_URL = "https://api.opensubtitles.com/api/v1"
        private const val OPEN_SUBTITLES_USER_AGENT = "ECHO NOW v1.0"
        private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val SUPPORTED_FORMATS = setOf("srt", "vtt")
    }
}

@Serializable
private data class SubdlResponse(
    val status: Boolean = false,
    val error: String? = null,
    val subtitles: List<SubdlSubtitle> = emptyList(),
)

@Serializable
private data class SubdlSubtitle(
    val url: String = "",
    @SerialName("release_name") val releaseName: String = "",
    val name: String = "",
    val language: String = "",
    val format: String = "",
    @SerialName("unpack_files") val unpackFiles: List<SubdlSubtitleFile>? = null,
)

@Serializable
private data class SubdlSubtitleFile(
    @SerialName("file_n_id") val fileId: String = "",
    val name: String = "",
    @SerialName("release_name") val releaseName: String = "",
    val language: String = "",
    val format: String = "",
    val url: String = "",
    val season: Int? = null,
    val episode: Int? = null,
)

@Serializable
private data class OpenSubtitlesSearchResponse(val data: List<OpenSubtitlesResult> = emptyList())

@Serializable
private data class OpenSubtitlesResult(
    val id: String,
    val attributes: OpenSubtitlesAttributes,
)

@Serializable
private data class OpenSubtitlesAttributes(
    val language: String = "",
    val release: String? = null,
    val files: List<OpenSubtitlesFile> = emptyList(),
)

@Serializable
private data class OpenSubtitlesFile(
    @SerialName("file_id") val fileId: Long,
    @SerialName("file_name") val fileName: String,
)

@Serializable
private data class OpenSubtitlesDownloadRequest(@SerialName("file_id") val fileId: Long)

@Serializable
private data class OpenSubtitlesDownloadResponse(val link: String)

@Serializable
private data class OpenSubtitlesFormatsResponse(val data: List<String> = emptyList())
