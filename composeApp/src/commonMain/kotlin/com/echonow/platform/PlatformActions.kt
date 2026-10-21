package com.echonow.platform

import androidx.compose.runtime.Composable
import com.echonow.domain.repository.SubtitleStyle
import com.echonow.domain.repository.PairedDevice
import com.echonow.domain.repository.PlaybackRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class DownloadTarget { BROWSER, ADM, VLC }

data class PlatformDeviceInfo(
    val model: String,
    val operatingSystem: String,
    val architecture: String,
    val network: String,
    val ipAddresses: String,
    val macAddresses: String,
)

expect fun dispatchDownload(url: String, target: DownloadTarget)

expect fun readPlatformDeviceInfo(): PlatformDeviceInfo

expect fun configuredTmdbApiKey(): String
expect fun configuredTmdbReadAccessToken(): String
expect fun configuredSubdlApiKey(): String
expect fun configuredAppName(): String
expect fun configuredAppVersion(): String
expect fun configuredAppWeb(): String

expect fun isDesktopPlatform(): Boolean

expect fun isAndroidTvPlatform(): Boolean

expect fun setPlatformFullscreen(enabled: Boolean)

@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)

expect fun currentTimeMillis(): Long

expect fun formatConfiguredDateTime(
    epochMillis: Long,
    timeZoneId: String,
    languageTag: String,
    pattern: String,
    manualClockValue: String,
): String

@Composable
expect fun SubtitleFilePickerButton(
    onSelected: (name: String, contents: String) -> Unit,
    onError: (message: String) -> Unit,
)

@Composable
expect fun PlatformVideoSurface(
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
)

expect fun observePairedDevices(): StateFlow<List<PairedDevice>>
expect fun observeOnlinePairedDevices(): StateFlow<List<PairedDevice>>
expect fun observeIncomingPlayback(): Flow<PlaybackRequest>
expect fun setDevicePairingMode(code: String?, deviceName: String)
expect fun updateLocalDeviceName(deviceName: String)
expect suspend fun pairDeviceWithCode(code: String): String?
expect fun unpairDevice(deviceId: String)
expect suspend fun castPlayback(deviceId: String, mediaId: String, season: Int, episode: Int)

@Composable
expect fun PlatformTrailerPlayer(videoId: String, onClose: () -> Unit)

@Composable
expect fun DialogBackdropEffect()

expect fun extractSubtitleArchive(bytes: ByteArray): String

expect fun computeSha256(input: String): String
