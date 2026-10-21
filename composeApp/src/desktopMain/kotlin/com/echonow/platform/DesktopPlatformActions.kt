package com.echonow.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import kotlinx.coroutines.delay
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import com.echonow.domain.repository.SubtitleStyle
import com.echonow.domain.repository.PairedDevice
import com.echonow.domain.repository.PlaybackRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun dispatchDownload(url: String, target: DownloadTarget) {
    require(url.startsWith("https://") || url.startsWith("http://")) { "Download URL must use HTTP or HTTPS." }
    java.awt.Desktop.getDesktop().browse(java.net.URI(url))
}

actual fun configuredTmdbApiKey(): String = DesktopBuildConfiguration.tmdbApiKey
actual fun configuredTmdbReadAccessToken(): String = DesktopBuildConfiguration.tmdbReadAccessToken
actual fun configuredSubdlApiKey(): String = DesktopBuildConfiguration.subdlApiKey
actual fun configuredAppName(): String = DesktopBuildConfiguration.appName
actual fun configuredAppVersion(): String = DesktopBuildConfiguration.appVersion
actual fun configuredAppWeb(): String = DesktopBuildConfiguration.appWeb

actual fun readPlatformDeviceInfo(): PlatformDeviceInfo {
    val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.let { enumeration ->
        buildList {
            while (enumeration.hasMoreElements()) add(enumeration.nextElement())
        }
    }.orEmpty()
    val activeInterfaces = interfaces.filter { networkInterface ->
        try {
            networkInterface.isUp && !networkInterface.isLoopback
        } catch (_: java.net.SocketException) {
            false
        }
    }
    val addresses = activeInterfaces.flatMap { networkInterface ->
        val addressEnumeration = networkInterface.inetAddresses
        buildList {
            while (addressEnumeration.hasMoreElements()) {
                val address = addressEnumeration.nextElement()
                if (!address.isLoopbackAddress && !address.isLinkLocalAddress) {
                    address.hostAddress?.substringBefore('%')?.let(::add)
                }
            }
        }
    }.distinct().joinToString(", ").ifBlank { "Unavailable" }
    val macAddresses = activeInterfaces.mapNotNull { networkInterface ->
        try {
            networkInterface.hardwareAddress?.joinToString(":") { byte ->
                (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
            }
        } catch (_: java.net.SocketException) {
            null
        }
    }.distinct().joinToString(", ").ifBlank { "Unavailable" }
    val network = activeInterfaces.map { it.displayName }.distinct()
        .joinToString(", ").ifBlank { "Not connected" }
    val osName = System.getProperty("os.name").orEmpty().ifBlank { "Desktop" }
    val osVersion = System.getProperty("os.version").orEmpty()
    return PlatformDeviceInfo(
        model = "Desktop computer",
        operatingSystem = listOf(osName, osVersion).filter(String::isNotBlank).joinToString(" "),
        architecture = System.getProperty("os.arch").orEmpty().ifBlank { "Unavailable" },
        network = network,
        ipAddresses = addresses,
        macAddresses = macAddresses,
    )
}

actual fun isDesktopPlatform(): Boolean = true

actual fun isAndroidTvPlatform(): Boolean = false

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) = Unit

actual fun setPlatformFullscreen(enabled: Boolean) {
    val device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    if (enabled) {
        device.fullScreenWindow = java.awt.Window.getWindows().firstOrNull { it.isShowing }
    } else {
        device.fullScreenWindow = null
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
actual fun SubtitleFilePickerButton(
    onSelected: (name: String, contents: String) -> Unit,
    onError: (message: String) -> Unit,
) {
    OutlinedButton(onClick = {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Choose subtitle file"
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "Subtitle files (*.srt, *.vtt)",
                "srt", "vtt",
            )
        }
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            runCatching { file.readText() }
                .onSuccess { onSelected(file.name, it) }
                .onFailure { onError(it.message ?: "Could not read the selected subtitle file.") }
        }
    }) { Text("Upload subtitle") }
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
    val component = remember { EmbeddedMediaPlayerComponent() }
    DisposableEffect(component) {
        onDispose { component.release() }
    }
    LaunchedEffect(streamUrl) {
        val lastPosition = component.mediaPlayer().status().time()
        component.mediaPlayer().media().play(streamUrl)
        if (lastPosition > 0) {
            delay(300)
            component.mediaPlayer().controls().setTime(lastPosition)
        }
    }
    LaunchedEffect(playing) {
        val controls = component.mediaPlayer().controls()
        if (playing) controls.play() else controls.pause()
    }
    LaunchedEffect(screenRatioRequest) {
        val aspectRatios = listOf(null, "16:9", "4:3", "1:1")
        component.mediaPlayer().video().setAspectRatio(aspectRatios[screenRatioRequest % aspectRatios.size])
    }
    val latestComplete by androidx.compose.runtime.rememberUpdatedState(onComplete)
    var completionSent by remember(streamUrl) { mutableStateOf(false) }
    LaunchedEffect(embeddedSubtitlesEnabled) {
        component.mediaPlayer().subpictures().setTrack(if (embeddedSubtitlesEnabled) 0 else -1)
    }
    val previous = remember(component) { longArrayOf(0L) }
    LaunchedEffect(seekRequestMillis) {
        val delta = seekRequestMillis - previous[0]
        if (delta != 0L) {
            val player = component.mediaPlayer()
            player.controls().setTime((player.status().time() + delta).coerceAtLeast(0))
            previous[0] = seekRequestMillis
        }
    }
    LaunchedEffect(component, streamUrl) {
        while (true) {
            val status = component.mediaPlayer().status()
            onProgress(status.time(), status.length().coerceAtLeast(0))
            if (!completionSent && status.length() > 0 && status.time() >= status.length() - 500) {
                completionSent = true
                latestComplete()
            }
            delay(500)
        }
    }
    SwingPanel(factory = { component }, modifier = Modifier.fillMaxSize())
}

actual fun observePairedDevices(): StateFlow<List<PairedDevice>> = MutableStateFlow(emptyList())
actual fun observeOnlinePairedDevices(): StateFlow<List<PairedDevice>> = MutableStateFlow(emptyList())
actual fun observeIncomingPlayback(): Flow<PlaybackRequest> = kotlinx.coroutines.flow.emptyFlow()
actual fun setDevicePairingMode(code: String?, deviceName: String) = Unit
actual fun updateLocalDeviceName(deviceName: String) = Unit
actual suspend fun pairDeviceWithCode(code: String): String? = "ECHO NOW device pairing is available on Android devices on the same local network."
actual fun unpairDevice(deviceId: String) = Unit
actual suspend fun castPlayback(deviceId: String, mediaId: String, season: Int, episode: Int) {
    error("ECHO NOW device casting is available on Android devices on the same local network.")
}

@Composable
actual fun PlatformTrailerPlayer(videoId: String, onClose: () -> Unit) {
    var playbackError by remember(videoId) { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Watch trailer") },
        text = {
            DialogBackdropEffect()
            Text(playbackError ?: "Open this trailer in your default browser.")
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    runCatching {
                        java.awt.Desktop.getDesktop().browse(
                            java.net.URI("https://www.youtube.com/watch?v=$videoId"),
                        )
                    }.onFailure {
                        playbackError = it.message ?: "Could not open the trailer in your browser."
                    }
                },
            ) { Text("Open trailer") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

@Composable
actual fun DialogBackdropEffect() = Unit

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
