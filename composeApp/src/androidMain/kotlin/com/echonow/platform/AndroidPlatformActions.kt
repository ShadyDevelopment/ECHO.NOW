package com.echonow.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import android.view.WindowManager
import android.view.View
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.echonow.domain.repository.SubtitleStyle
import com.echonow.domain.repository.PairedDevice
import com.echonow.domain.repository.PlaybackRequest
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import kotlinx.coroutines.delay
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerSurface
import com.shuyu.gsyvideoplayer.compose.native_.rememberGSYPlayerController
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerEvent
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.subtitle.GSYSubtitleStyle
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.exo2.ExoMediaSourceInterceptListener
import tv.danmaku.ijk.media.exo2.ExoSourceManager
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import java.io.File
import kotlinx.coroutines.flow.StateFlow

private object EchoPlaybackSource {
    @Volatile
    var format: String = ""

    @Volatile
    var headers: Map<String, String> = emptyMap()
}

private object EchoPlaybackCache {
    @Volatile
    private var instance: SimpleCache? = null

    fun get(): SimpleCache = instance ?: synchronized(this) {
        instance ?: SimpleCache(
            File(AndroidPlatformContext.context.cacheDir, "echo-now-media3-cache"),
            LeastRecentlyUsedCacheEvictor(512L * 1024L * 1024L),
            StandaloneDatabaseProvider(AndroidPlatformContext.context),
        ).also { instance = it }
    }
}

private fun hidePlayerBottomProgressBar(player: StandardGSYVideoPlayer) {
    player.setBottomProgressBarDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    val progressBarId = player.resources.getIdentifier(
        "bottom_progressbar",
        "id",
        player.context.packageName,
    )
    if (progressBarId != 0) {
        player.post {
            player.findViewById<View>(progressBarId)?.apply {
                visibility = View.GONE
                alpha = 0f
            }
        }
    }
}

private object EchoExoMediaSourceInterceptor : ExoMediaSourceInterceptListener {
    override fun getMediaSource(
        dataSource: String,
        preview: Boolean,
        cacheEnable: Boolean,
        isLooping: Boolean,
        cacheDir: File?,
    ): MediaSource? {
        val normalizedFormat = EchoPlaybackSource.format.lowercase()
        val mimeType = when (normalizedFormat) {
            "hls", "m3u8" -> MimeTypes.APPLICATION_M3U8
            "dash", "mpd" -> MimeTypes.APPLICATION_MPD
            else -> return null
        }
        val headers = EchoPlaybackSource.headers
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(headers["User-Agent"] ?: "ECHO NOW")
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
        val networkFactory: DataSource.Factory = DefaultDataSource.Factory(
            AndroidPlatformContext.context,
            httpFactory,
        )
        val sourceFactory: DataSource.Factory = CacheDataSource.Factory()
            .setCache(EchoPlaybackCache.get())
            .setUpstreamDataSourceFactory(networkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val mediaItem = MediaItem.Builder()
            .setUri(dataSource)
            .setMimeType(mimeType)
            .build()
        return when (normalizedFormat) {
            "hls", "m3u8" -> HlsMediaSource.Factory(sourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
            else -> DashMediaSource.Factory(DefaultDashChunkSource.Factory(sourceFactory), sourceFactory)
                .createMediaSource(mediaItem)
        }
    }

    override fun getHttpDataSourceFactory(
        userAgent: String?,
        listener: androidx.media3.datasource.TransferListener?,
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int,
        mapHeadData: Map<String, String>?,
        allowCrossProtocolRedirects: Boolean,
    ): DataSource.Factory? = null

    override fun cacheWriteDataSinkFactory(cachePath: String?, url: String?): androidx.media3.datasource.DataSink.Factory? = null
}

private fun withSubtitleAlpha(color: Long, opacityPercent: Int): Int {
    val alpha = (opacityPercent.coerceIn(0, 100) * 255 / 100) shl 24
    return (color.toInt() and 0x00FFFFFF) or alpha
}

object AndroidPlatformContext {
    lateinit var context: Context
    lateinit var activity: Activity
    var tmdbApiKey: String = ""
    var tmdbReadAccessToken: String = ""
    var subdlApiKey: String = ""
    var appName: String = "ECHO NOW"
    var appVersion: String = "1.0.0"
    var appWeb: String = ""
    var previousPlayerOrientation: Int? = null

    fun initialize(
        activity: Activity,
        tmdbApiKey: String,
        tmdbReadAccessToken: String,
        subdlApiKey: String,
        appName: String,
        appVersion: String,
        appWeb: String,
    ) {
        this.activity = activity
        this.context = activity.applicationContext
        this.tmdbApiKey = tmdbApiKey
        this.tmdbReadAccessToken = tmdbReadAccessToken
        this.subdlApiKey = subdlApiKey
        this.appName = appName
        this.appVersion = appVersion
        this.appWeb = appWeb
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        ExoSourceManager.setExoMediaSourceInterceptListener(EchoExoMediaSourceInterceptor)
        AndroidDevicePairing.initialize(activity.applicationContext, AndroidPlatformContext.appName)
    }
}

actual fun dispatchDownload(url: String, target: DownloadTarget) {
    require(url.startsWith("https://") || url.startsWith("http://")) { "Download URL must use HTTP or HTTPS." }
    val packageName = when (target) {
        DownloadTarget.BROWSER -> null
        DownloadTarget.ADM -> "com.dv.adm"
        DownloadTarget.VLC -> "org.videolan.vlc"
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        if (packageName != null && AndroidPlatformContext.context.packageManager.getLaunchIntentForPackage(packageName) != null) {
            setPackage(packageName)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidPlatformContext.context.startActivity(intent)
}

actual fun configuredTmdbApiKey(): String = AndroidPlatformContext.tmdbApiKey
actual fun configuredTmdbReadAccessToken(): String = AndroidPlatformContext.tmdbReadAccessToken
actual fun configuredSubdlApiKey(): String = AndroidPlatformContext.subdlApiKey
actual fun configuredAppName(): String = AndroidPlatformContext.appName
actual fun configuredAppVersion(): String = AndroidPlatformContext.appVersion
actual fun configuredAppWeb(): String = AndroidPlatformContext.appWeb

actual fun readPlatformDeviceInfo(): PlatformDeviceInfo {
    val context = AndroidPlatformContext.context
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val network = connectivityManager?.activeNetwork
    val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
    val linkProperties = network?.let(connectivityManager::getLinkProperties)
    val networkType = when {
        capabilities == null -> "Not connected"
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        else -> "Connected"
    }
    val ipAddresses = linkProperties?.linkAddresses.orEmpty()
        .mapNotNull { linkAddress ->
            val address = linkAddress.address
            if (address.isLoopbackAddress || address.isLinkLocalAddress) null
            else address.hostAddress?.substringBefore('%')
        }
        .distinct()
        .joinToString(", ")
        .ifBlank { "Unavailable" }
    val manufacturer = Build.MANUFACTURER.takeIf(String::isNotBlank)
    val model = Build.MODEL.takeIf(String::isNotBlank)
    return PlatformDeviceInfo(
        model = listOfNotNull(manufacturer, model).distinct().joinToString(" ").ifBlank { "Android device" },
        operatingSystem = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        architecture = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "Unavailable" },
        network = networkType,
        ipAddresses = ipAddresses,
        macAddresses = "Unavailable (restricted by Android)",
    )
}

actual fun isDesktopPlatform(): Boolean = false

actual fun observePairedDevices(): StateFlow<List<PairedDevice>> = AndroidDevicePairing.pairedDevices()
actual fun observeOnlinePairedDevices(): StateFlow<List<PairedDevice>> = AndroidDevicePairing.onlinePairedDevices()
actual fun observeIncomingPlayback(): kotlinx.coroutines.flow.Flow<PlaybackRequest> = AndroidDevicePairing.incomingPlayback()
actual fun setDevicePairingMode(code: String?, deviceName: String) = AndroidDevicePairing.setPairingMode(code, deviceName)
actual fun updateLocalDeviceName(deviceName: String) = AndroidDevicePairing.updateName(deviceName)
actual suspend fun pairDeviceWithCode(code: String): String? = AndroidDevicePairing.pairWithCode(code)
actual fun unpairDevice(deviceId: String) = AndroidDevicePairing.unpair(deviceId)
actual suspend fun castPlayback(deviceId: String, mediaId: String, season: Int, episode: Int) =
    AndroidDevicePairing.cast(deviceId, mediaId, season, episode)

actual fun isAndroidTvPlatform(): Boolean =
    AndroidPlatformContext.context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}

fun hideSystemBarsAfterDelay(delayMillis: Long = 3_000L) {
    val window = AndroidPlatformContext.activity.window
    val decorView = window.decorView
    decorView.postDelayed({
        if (decorView.isAttachedToWindow) {
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }, delayMillis)
}

actual fun setPlatformFullscreen(enabled: Boolean) {
    val window = AndroidPlatformContext.activity.window
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enabled) {
        if (AndroidPlatformContext.previousPlayerOrientation == null) {
            AndroidPlatformContext.previousPlayerOrientation =
                AndroidPlatformContext.activity.requestedOrientation
        }
        AndroidPlatformContext.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        AndroidPlatformContext.previousPlayerOrientation?.let { previousOrientation ->
            AndroidPlatformContext.activity.requestedOrientation = previousOrientation
            AndroidPlatformContext.previousPlayerOrientation = null
        }
        hideSystemBarsAfterDelay()
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatConfiguredDateTime(
    epochMillis: Long,
    timeZoneId: String,
    languageTag: String,
    pattern: String,
    manualClockValue: String,
): String = runCatching {
    val zone = if (timeZoneId == "AUTO") java.time.ZoneId.systemDefault() else java.time.ZoneId.of(timeZoneId)
    val locale = if (languageTag == "SYSTEM") java.util.Locale.getDefault()
    else java.util.Locale.forLanguageTag(languageTag)
    val instant = if (manualClockValue.isBlank()) {
        java.time.Instant.ofEpochMilli(epochMillis)
    } else {
        java.time.LocalDateTime.parse(
            manualClockValue,
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale),
        ).atZone(zone).toInstant()
    }
    java.time.format.DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(instant)
}.getOrElse { "Invalid date/time format" }

@Composable
actual fun DialogBackdropEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val previousDimAmount = window?.attributes?.dimAmount
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.78f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = 36 }
            }
        }
        onDispose {
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.attributes = window.attributes.apply { blurBehindRadius = 0 }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                previousDimAmount?.let(window::setDimAmount)
            }
        }
    }
}

@Composable
actual fun SubtitleFilePickerButton(
    onSelected: (name: String, contents: String) -> Unit,
    onError: (message: String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val contents = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected subtitle file.")
                onSelected(uri.lastPathSegment?.substringAfterLast('/') ?: "subtitle.srt", contents)
            }.onFailure { onError(it.message ?: "Could not read the selected subtitle file.") }
        }
    }
    OutlinedButton(onClick = { launcher.launch(arrayOf("text/*", "application/octet-stream")) }) {
        Text("Upload subtitle")
    }
}


@Composable
actual fun PlatformVideoSurface(
    streamUrl: String,
    format: String,
    isM3u8: Boolean,
    headers: Map<String, String>,
    playing: Boolean,
    embeddedSubtitlesEnabled: Boolean,
    subtitleStyle: SubtitleStyle,
    subtitleOffsetMillis: Long,
    subtitleVerticalPositionPercent: Int,
    seekRequestMillis: Long,
    screenRatioRequest: Int,
    onComplete: () -> Unit,
    onProgress: (positionMillis: Long, durationMillis: Long) -> Unit,
    onError: (message: String) -> Unit,
) {
    val cachePath = remember {
        File(AndroidPlatformContext.context.cacheDir, "echo-now-player-cache").apply {
            check(isDirectory || mkdirs()) { "Could not create the video cache directory." }
        }
    }
    val controller = rememberGSYPlayerController(autoPauseResume = true)
    val snapshot by controller.snapshot
    var lastKnownPosition by remember(controller) { mutableLongStateOf(0L) }
    val latestComplete by rememberUpdatedState(onComplete)
    val latestError by rememberUpdatedState(onError)
    val playbackHeaders = remember(headers) {
        buildMap {
            put(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            )
            putAll(headers)
        }
    }
    LaunchedEffect(snapshot.currentPosition) {
        if (snapshot.currentPosition > 0L) lastKnownPosition = snapshot.currentPosition
    }
    LaunchedEffect(streamUrl, format, headers) {
        EchoPlaybackSource.format = format
        EchoPlaybackSource.headers = playbackHeaders
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        controller.setCachePath(cachePath)
        controller.setShowPauseCover(true)
        controller.setReleaseWhenLossAudio(false)
        controller.setSeekOnStart(lastKnownPosition.takeIf { it > 0L } ?: seekRequestMillis)
        val overrideExtension = when {
            format.equals("dash", ignoreCase = true) -> "mpd"
            format.equals("mp4", ignoreCase = true) -> "mp4"
            streamUrl.substringBefore('?').endsWith(".mp4", ignoreCase = true) -> "mp4"
            isM3u8 -> "m3u8"
            streamUrl.substringBefore('?').endsWith(".m3u8", ignoreCase = true) -> "m3u8"
            else -> ""
        }
        val optionBuilder = com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder()
            .setUrl(streamUrl)
            .setMapHeadData(playbackHeaders)
            .setCacheWithPlay(false)
            .setVideoTitle("ECHO NOW")
            .setOverrideExtension(overrideExtension)
        controller.setUp(optionBuilder, true)
        controller.setHeaders(playbackHeaders)
        controller.setOverrideExtension(overrideExtension.takeIf(String::isNotBlank))
        controller.withHost(::hidePlayerBottomProgressBar)
    }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is GSYPlayerEvent.AutoComplete -> latestComplete()
                is GSYPlayerEvent.Prepared -> {
                    controller.withHost(::hidePlayerBottomProgressBar)
                    preferEnglishAudioTrack()
                }
                is GSYPlayerEvent.Error -> latestError("This stream failed to load (player error ${event.what}/${event.extra}).")
                else -> Unit
            }
        }
    }
    LaunchedEffect(playing) {
        if (playing) controller.resume() else controller.pause()
    }
    LaunchedEffect(streamUrl, screenRatioRequest) {
        val aspectModes = listOf(
            GSYVideoType.SCREEN_TYPE_DEFAULT,
            GSYVideoType.SCREEN_TYPE_16_9,
            GSYVideoType.SCREEN_TYPE_4_3,
            GSYVideoType.SCREEN_TYPE_FULL,
        )
        GSYVideoType.setShowType(aspectModes[screenRatioRequest % aspectModes.size])
        controller.changeTextureViewShowType()
    }
    LaunchedEffect(embeddedSubtitlesEnabled, subtitleStyle, subtitleOffsetMillis, subtitleVerticalPositionPercent) {
        controller.withHost { player ->
            player.setSubtitleEnabled(embeddedSubtitlesEnabled)
            player.setSubtitleOffsetMs(subtitleOffsetMillis)
            player.setSubtitleStyle(
                GSYSubtitleStyle.Builder()
                    .setTextColor(subtitleStyle.text.toInt())
                    .setTextSizeSp(subtitleStyle.sizeSp.toFloat())
                    .setBackgroundColor(
                        if (subtitleStyle.boxEnabled) {
                            withSubtitleAlpha(subtitleStyle.background, 100 - subtitleStyle.boxTransparency)
                        } else 0,
                    )
                    .setShadow(
                        if (subtitleStyle.boxEnabled) subtitleStyle.border.toInt() else 0,
                        2f,
                        0f,
                        0f,
                    )
                    .setBottomMarginDp(65 + subtitleVerticalPositionPercent.coerceIn(0, 25) * 4)
                    .build(),
            )
        }

    }
    applyGsySeek(controller, seekRequestMillis)
    LaunchedEffect(snapshot.currentPosition, snapshot.duration) {
        onProgress(snapshot.currentPosition, snapshot.duration)
    }
    Box(Modifier.fillMaxSize()) {
        GSYPlayerSurface(controller, Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp)
                .background(androidx.compose.ui.graphics.Color.Black),
        )
    }
}

private fun preferEnglishAudioTrack() {
    val mediaPlayer = GSYVideoManager.instance().player?.mediaPlayer as? IjkExo2MediaPlayer ?: return
    val selector = mediaPlayer.trackSelector ?: return
    selector.setParameters(
        selector.parameters.buildUpon()
            .setPreferredAudioLanguages("en", "eng")
            .build(),
    )
}

@Composable
private fun applyGsySeek(controller: com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerController, seekRequestMillis: Long) {
    val lastRequest = androidx.compose.runtime.remember(controller) { longArrayOf(0L) }
    LaunchedEffect(seekRequestMillis) {
        val delta = seekRequestMillis - lastRequest[0]
        if (delta != 0L) {
            controller.seekRelative(delta)
            lastRequest[0] = seekRequestMillis
        }
    }
}

actual fun computeSha256(input: String): String {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

actual fun extractSubtitleArchive(bytes: ByteArray): String {
    if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
        java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes)).use { gzip ->
            return gzip.bufferedReader(Charsets.UTF_8).readText()
        }
    }
    if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            var fallback: String? = null
            while (entry != null) {
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".srt") || name.endsWith(".vtt"))) {
                    return zis.bufferedReader(Charsets.UTF_8).readText()
                } else if (!entry.isDirectory && (name.endsWith(".sub") || name.endsWith(".txt"))) {
                    if (fallback == null) fallback = zis.bufferedReader(Charsets.UTF_8).readText()
                }
                entry = zis.nextEntry
            }
            if (fallback != null) return fallback
        }
    }
    return bytes.decodeToString()
}
