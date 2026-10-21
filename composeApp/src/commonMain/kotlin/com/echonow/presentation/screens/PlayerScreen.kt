package com.echonow.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.echonow.presentation.components.EchoButton as Button
import com.echonow.presentation.components.EchoOutlinedButton as OutlinedButton
import com.echonow.presentation.components.EchoTextButton as TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.ceil
import echo_now.composeapp.generated.resources.Res
import echo_now.composeapp.generated.resources.echo_tv_mark
import org.jetbrains.compose.resources.painterResource
import com.echonow.data.remote.SubtitleOption
import com.echonow.data.remote.SubtitlesApi
import com.echonow.domain.repository.PreferencesRepository
import com.echonow.domain.repository.WatchProgress
import com.echonow.domain.repository.WatchProgressRepository
import com.echonow.domain.model.VideoQuality
import com.echonow.domain.model.Episode
import com.echonow.presentation.components.LocalEchoImageLoader
import coil3.compose.AsyncImage
import com.echonow.platform.PlatformVideoSurface
import com.echonow.platform.observeOnlinePairedDevices
import com.echonow.platform.castPlayback
import com.echonow.platform.DialogBackdropEffect
import com.echonow.presentation.components.DevicePairingDialog
import com.echonow.platform.SubtitleFilePickerButton
import com.echonow.platform.PlatformBackHandler
import com.echonow.platform.setPlatformFullscreen
import com.echonow.presentation.viewmodel.DetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private data class SubtitleCue(val startMillis: Long, val endMillis: Long, val text: String)

@Composable
private fun StreamResolvingMark() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Image(
        painter = painterResource(Res.drawable.echo_tv_mark),
        contentDescription = "Loading video stream",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        alpha = alpha,
        modifier = Modifier.size(72.dp),
    )
}

private const val UP_NEXT_COUNTDOWN_SECONDS = 10

@Composable
fun PlayerScreen(
    id: String,
    season: Int,
    episode: Int,
    qualityIndex: Int,
    resumePositionMillis: Long = 0L,
    onNextEpisode: (season: Int, episode: Int) -> Unit,
    onBack: () -> Unit,
) {
    val detail: DetailViewModel = koinViewModel()
    val state by detail.state.collectAsState()
    val preferences: PreferencesRepository = koinInject()
    val subtitlesApi: SubtitlesApi = koinInject()
    val progressRepository: WatchProgressRepository = koinInject()
    val scope = rememberCoroutineScope()
    var userPreferences by remember { mutableStateOf(com.echonow.domain.repository.UserPreferences()) }
    var playing by remember { mutableStateOf(true) }
    var subtitlesEnabled by remember { mutableStateOf(false) }
    var subtitleControlsOpen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsEpoch by remember { mutableIntStateOf(0) }
    var playbackError by remember(id, season, episode) { mutableStateOf<String?>(null) }
    var videoReady by remember(id, season, episode) { mutableStateOf(false) }
    var seekRequest by remember(id, season, episode) { mutableLongStateOf(resumePositionMillis) }
    var selectedQuality by remember(id, season, episode) { mutableIntStateOf(qualityIndex) }
    var failedStreamUrls by remember(id, season, episode) { mutableStateOf(emptySet<String>()) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var screenRatioRequest by remember(id) { mutableIntStateOf(0) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showUpNext by remember(id, season, episode) { mutableStateOf(false) }
    var upNextDismissed by remember(id, season, episode) { mutableStateOf(false) }
    var actionButtonFocused by remember { mutableStateOf(false) }
    var subtitleSliderFocused by remember { mutableStateOf(false) }
    var subtitleCues by remember { mutableStateOf(emptyList<SubtitleCue>()) }
    var subtitleOptions by remember { mutableStateOf(emptyList<SubtitleOption>()) }
    var subtitleStatus by remember { mutableStateOf<String?>(null) }
    var subtitleQuerying by remember { mutableStateOf(false) }
    var castMenuExpanded by remember { mutableStateOf(false) }
    var pairingDialogOpen by remember { mutableStateOf(false) }
    var castStatus by remember { mutableStateOf<String?>(null) }
    val onlineDevices by observeOnlinePairedDevices().collectAsState()
    val playerFocus = remember { FocusRequester() }
    val skipIntroFocus = remember(id, season, episode) { FocusRequester() }
    val skipCreditsFocus = remember(id, season, episode) { FocusRequester() }
    val upNextPlayFocus = remember(id, season, episode) { FocusRequester() }
    val upNextCancelFocus = remember(id, season, episode) { FocusRequester() }
    val latestMedia by rememberUpdatedState(state.media)
    val latestPosition by rememberUpdatedState(position)
    val latestDuration by rememberUpdatedState(duration)
    val latestSeason by rememberUpdatedState(season)
    val latestEpisode by rememberUpdatedState(episode)
    var advancedToNext by remember(id, season, episode) { mutableStateOf(false) }
    var autoPlayNextEpisode by remember(id) { mutableStateOf(true) }

    val currentEpisode = state.media?.seasons
        ?.firstOrNull { it.number == season }
        ?.episodes
        ?.firstOrNull { it.number == episode }
    val introStart = currentEpisode?.introStartMs ?: 0L
    val introEnd = currentEpisode?.introEndMs
    val episodePlaylist = state.media?.seasons.orEmpty().flatMap { currentSeason ->
        currentSeason.episodes.map { currentSeason.number to it }
    }
    val currentEpisodeIndex = episodePlaylist.indexOfFirst { it.first == season && it.second.number == episode }
    val nextEpisode = episodePlaylist.getOrNull(currentEpisodeIndex + 1)
    val remainingPlaybackMs = (duration - position).coerceAtLeast(0L)
    val endingWindowStart = (duration - UP_NEXT_COUNTDOWN_SECONDS * 1_000L).coerceAtLeast(0L)
    val creditsStart = currentEpisode?.creditsStartMs?.takeIf { it in 0 until duration }
    val upNextTriggerPosition = (maxOf(endingWindowStart, creditsStart ?: endingWindowStart) - 15_000L)
        .coerceAtLeast(0L)
    val upNextSeconds = ceil(
        ((upNextTriggerPosition + UP_NEXT_COUNTDOWN_SECONDS * 1_000L - position).coerceAtLeast(0L)) / 1_000.0,
    ).toInt().coerceIn(0, UP_NEXT_COUNTDOWN_SECONDS)
    val upNextEligible = state.media?.type == com.echonow.domain.model.MediaType.SERIES &&
        duration > 0L &&
        nextEpisode != null &&
        position >= upNextTriggerPosition
    val skipCreditsVisible = creditsStart != null && position >= creditsStart && !showUpNext
    val skipIntroVisible = introEnd?.let { end ->
        val skipWindowEnd = minOf(end, introStart + 10_000L)
        !showUpNext && introStart <= end && position >= introStart && position < skipWindowEnd
    } ?: false

    fun playNextEpisode() {
        if (advancedToNext) return
        val next = nextEpisode ?: return
        advancedToNext = true
        showUpNext = false
        onNextEpisode(next.first, next.second.number)
    }

    fun showUpNextPrompt() {
        if (!upNextEligible || upNextDismissed || showUpNext) return
        showUpNext = true
    }

    fun advanceToNextEpisode() {
        showUpNextPrompt()
    }

    LaunchedEffect(Unit) { setPlatformFullscreen(true) }
    PlatformBackHandler(onBack)
    LaunchedEffect(Unit) { playerFocus.requestFocus() }
    DisposableEffect(Unit) {
        onDispose {
            setPlatformFullscreen(false)
            val media = latestMedia
            val total = latestDuration
            val current = latestPosition
            if (media != null && total > 0L && current > 0L) {
                scope.launch {
                    progressRepository.update(
                        WatchProgress(
                            mediaId = media.id,
                            title = media.title,
                            posterUrl = media.posterUrl,
                            type = media.type,
                            positionMillis = current,
                            durationMillis = total,
                            season = latestSeason,
                            episode = latestEpisode,
                            episodeTitle = media.seasons
                                .firstOrNull { it.number == latestSeason }
                                ?.episodes
                                ?.firstOrNull { it.number == latestEpisode }
                                ?.title
                                .orEmpty(),
                        ),
                    )
                }
            }
        }
    }
    LaunchedEffect(id) {
        position = 0L
        duration = 0L
        detail.load(id)
        userPreferences = preferences.observe().first()
    }
    var autoSubtitlesAttempted by remember(id, season, episode) { mutableStateOf(false) }

    LaunchedEffect(state.media?.id, season, episode) {
        if (state.media != null) detail.loadStreams(season.takeIf { it > 0 }, episode.takeIf { it > 0 })
    }
    LaunchedEffect(state.media?.id, season, episode, currentEpisode?.tmdbId, duration) {
        if (state.media != null) detail.loadEpisodeTimings(season, episode, duration)
    }

    LaunchedEffect(state.media?.id, season, episode, userPreferences.autoApplySubtitles) {
        val currentMedia = state.media
        if (currentMedia != null && userPreferences.autoApplySubtitles && !autoSubtitlesAttempted && subtitleCues.isEmpty()) {
            autoSubtitlesAttempted = true
            scope.launch {
                runCatching {
                    val results = subtitlesApi.search(
                        media = currentMedia,
                        language = userPreferences.subtitleLanguage,
                        season = season.takeIf { it > 0 },
                        episode = episode.takeIf { it > 0 },
                        openSubtitlesApiKey = userPreferences.openSubtitlesApiKey,
                        subdlApiKey = userPreferences.subdlApiKey,
                    )
                    subtitleOptions = results
                    val targetOption = results.firstOrNull { it.provider == SubtitlesApi.ECHO_SUBS }
                        ?: results.firstOrNull()
                    if (targetOption != null) {
                        subtitleStatus = "Auto-downloading subtitle: ${targetOption.title}…"
                        val text = subtitlesApi.download(
                            option = targetOption,
                            openSubtitlesApiKey = userPreferences.openSubtitlesApiKey,
                            subdlApiKey = userPreferences.subdlApiKey,
                        )
                        val cues = parseSubtitleDocument(text)
                        if (cues.isNotEmpty()) {
                            subtitleCues = cues
                            subtitlesEnabled = true
                            subtitleStatus = "ECHOSubs: Applied ${targetOption.title} (${cues.size} cues)"
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(controlsEpoch, playing, playbackError, videoReady) {
        if (playing && playbackError == null && videoReady) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(showUpNext) {
        if (showUpNext) upNextPlayFocus.requestFocus()
    }
    LaunchedEffect(showUpNext, upNextSeconds, autoPlayNextEpisode, nextEpisode) {
        if (showUpNext && upNextSeconds == 0 && autoPlayNextEpisode && nextEpisode != null) {
            playNextEpisode()
        }
    }
    LaunchedEffect(upNextEligible, upNextDismissed) {
        showUpNext = upNextEligible && !upNextDismissed
    }
    LaunchedEffect(id, season, episode, resumePositionMillis) {
        while (true) {
            delay(5_000)
            val media = latestMedia ?: continue
            val total = latestDuration
            if (media.id == id && total > 0L && latestPosition > 0L) {
                progressRepository.update(
                    WatchProgress(
                        mediaId = media.id,
                        title = media.title,
                        posterUrl = media.posterUrl,
                        type = media.type,
                        positionMillis = latestPosition,
                        durationMillis = total,
                        season = season,
                        episode = episode,
                        episodeTitle = media.seasons
                            .firstOrNull { it.number == season }
                            ?.episodes
                            ?.firstOrNull { it.number == episode }
                            ?.title
                            .orEmpty(),
                    ),
                )
            }
        }
    }
    var skipActionAutoFocused by remember(id, season, episode) { mutableStateOf(false) }
    LaunchedEffect(skipIntroVisible, skipCreditsVisible, controlsVisible) {
        val skipFocus = when {
            skipIntroVisible -> skipIntroFocus
            skipCreditsVisible -> skipCreditsFocus
            else -> null
        }
        if (skipFocus != null && !controlsVisible && !skipActionAutoFocused) {
            skipFocus.requestFocus()
            skipActionAutoFocused = true
        } else if (skipFocus == null) {
            skipActionAutoFocused = false
        }
    }

    val selectedStream = state.streams.getOrNull(selectedQuality)
    val url = selectedStream?.url ?: state.media?.qualities?.getOrNull(selectedQuality)?.url
    LaunchedEffect(selectedStream?.url) {
        playbackError = null
        videoReady = false
        controlsVisible = true
    }
    val activeCue = if (subtitlesEnabled) {
        val subtitlePosition = position + userPreferences.subtitleOffsetMillis
        subtitleCues.firstOrNull { subtitlePosition in it.startMillis..it.endMillis }
    } else {
        null
    }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .focusRequester(playerFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if ((actionButtonFocused || subtitleSliderFocused) && event.key !in setOf(Key.Escape, Key.Back)) {
                    return@onPreviewKeyEvent false
                }
                controlsVisible = true
                controlsEpoch++
                when (event.key) {
                    Key.DirectionLeft -> { seekRequest -= 10_000; true }
                    Key.DirectionRight -> { seekRequest += 10_000; true }
                    Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter -> { playing = !playing; true }
                    Key.Escape, Key.Back -> { onBack(); true }
                    else -> false
                }
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            PlatformVideoSurface(
                streamUrl = url,
                format = selectedStream?.format ?: "hls",
                isM3u8 = selectedStream?.isM3u8 ?: true,
                headers = selectedStream?.headers ?: emptyMap(),
                playing = playing,
                embeddedSubtitlesEnabled = subtitlesEnabled && subtitleCues.isEmpty(),
                subtitleStyle = userPreferences.subtitleStyle,
                subtitleOffsetMillis = userPreferences.subtitleOffsetMillis,
                subtitleVerticalPositionPercent = userPreferences.subtitleVerticalPositionPercent.coerceIn(0, 25),
                seekRequestMillis = seekRequest,
                screenRatioRequest = screenRatioRequest,
                onComplete = ::advanceToNextEpisode,
                onProgress = { current, total ->
                    position = current.coerceAtLeast(0)
                    duration = total.coerceAtLeast(0)
                    if (total > 0L) videoReady = true
                },
                onError = { message ->
                    controlsVisible = true
                    videoReady = false
                    controlsEpoch++
                    val failedUrl = selectedStream?.url
                    val updatedFailures = failedStreamUrls + listOfNotNull(failedUrl)
                    failedStreamUrls = updatedFailures
                    val fallbackIndex = state.streams.indexOfFirst { it.url !in updatedFailures }
                    if (fallbackIndex >= 0 && fallbackIndex != selectedQuality) {
                        selectedQuality = fallbackIndex
                        playbackError = null
                    } else {
                        val provider = selectedStream?.providerName?.takeIf(String::isNotBlank)
                        playbackError = buildString {
                            append(message)
                            if (provider != null) append(" Source: $provider.")
                            append(" All available streams failed. Check that the provider returns a playable video URL and required access headers.")
                        }
                    }
                },
            )
        } else {
            val streamError = state.streamsError ?: state.error
            if (streamError != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(streamError, color = Color.White, textAlign = TextAlign.Center)
                    if (state.streamsError != null) {
                        OutlinedButton(
                            onClick = { detail.loadStreams(season.takeIf { it > 0 }, episode.takeIf { it > 0 }) },
                            modifier = Modifier.onFocusChanged { actionButtonFocused = it.isFocused },
                        ) { Text("Retry") }
                    }
                }
            } else {
                StreamResolvingMark()
            }
        }
        if (url != null) {
            Box(
                Modifier.fillMaxSize().pointerInput(url, controlsVisible) {
                    detectTapGestures {
                        controlsVisible = !controlsVisible
                        controlsEpoch++
                    }
                },
            )
        }
        playbackError?.let { message ->
            Text(
                message,
                modifier = Modifier.align(Alignment.Center)
                    .background(Color.Black.copy(alpha = .78f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        activeCue?.let { cue ->
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        bottom = (if (controlsVisible) 160.dp else 48.dp) +
                            maxHeight * (userPreferences.subtitleVerticalPositionPercent.coerceIn(0, 25) / 100f),
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cue.text,
                    modifier = Modifier.then(
                        if (userPreferences.subtitleStyle.boxEnabled) {
                            Modifier.border(1.dp, Color(userPreferences.subtitleStyle.border), RoundedCornerShape(6.dp))
                                .background(
                                    Color(userPreferences.subtitleStyle.background).copy(
                                        alpha = (100 - userPreferences.subtitleStyle.boxTransparency.coerceIn(0, 100)) / 100f,
                                    ),
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        } else Modifier,
                    ),
                    color = Color(userPreferences.subtitleStyle.text),
                    fontSize = userPreferences.subtitleStyle.sizeSp.coerceIn(14, 48).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = when (userPreferences.subtitleStyle.fontFamily) {
                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                        "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                    },
                    textAlign = TextAlign.Center,
                    lineHeight = (userPreferences.subtitleStyle.sizeSp.coerceIn(14, 48) * 1.25f).sp,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.9f),
                            offset = Offset(0f, 2f),
                            blurRadius = 5f,
                        ),
                    ),
                )
            }
        }
        if (skipIntroVisible) {
            OutlinedButton(
                onClick = {
                    seekRequest += (introEnd - position).coerceAtLeast(0L)
                    controlsEpoch++
                },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = if (controlsVisible) 150.dp else 24.dp)
                    .focusRequester(skipIntroFocus)
                    .onFocusChanged {
                        actionButtonFocused = it.isFocused || showUpNext
                    },
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skip Intro")
            }
        }
        if (skipCreditsVisible) {
            OutlinedButton(
                onClick = {
                    if (nextEpisode != null) {
                        playNextEpisode()
                    } else {
                        seekRequest += remainingPlaybackMs
                        controlsEpoch++
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = if (controlsVisible) 150.dp else 24.dp)
                    .focusRequester(skipCreditsFocus)
                    .onFocusChanged { actionButtonFocused = it.isFocused || showUpNext },
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skip Credits")
            }
        }
        if (controlsVisible) {
            Column(
                Modifier.fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = .72f), Color.Transparent, Color.Black.copy(alpha = .82f)),
                        ),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.fillMaxWidth().height(56.dp)) {
                    Text(
                        state.media?.let { media ->
                            if (media.type == com.echonow.domain.model.MediaType.SERIES && season > 0 && episode > 0) {
                                "${media.title} - S$season:E$episode"
                            } else {
                                media.title
                            }
                        }.orEmpty(),
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = { castMenuExpanded = true }) {
                            Icon(Icons.Rounded.Cast, contentDescription = "Cast to paired ECHO NOW device", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = castMenuExpanded,
                            onDismissRequest = { castMenuExpanded = false },
                        ) {
                            if (onlineDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No paired devices online") },
                                    enabled = false,
                                    onClick = {},
                                )
                            } else {
                                onlineDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(device.name) },
                                        onClick = {
                                            castMenuExpanded = false
                                            scope.launch {
                                                castStatus = null
                                                runCatching {
                                                    castPlayback(device.id, id, season, episode)
                                                }.onFailure {
                                                    castStatus = it.message ?: "Could not open playback on ${device.name}."
                                                }.onSuccess {
                                                    onBack()
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                            if (onlineDevices.isNotEmpty()) Divider()
                            DropdownMenuItem(
                                text = { Text("Pair with code") },
                                onClick = {
                                    castMenuExpanded = false
                                    pairingDialogOpen = true
                                },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth()) {
                        if (duration > 0L) {
                            Canvas(Modifier.fillMaxWidth().height(48.dp)) {
                                subtitleCues.forEach { cue ->
                                    val fraction = (cue.startMillis.toFloat() / duration).coerceIn(0f, 1f)
                                    val x = size.width * fraction
                                    drawLine(
                                        color = Color.White.copy(alpha = .75f),
                                        start = androidx.compose.ui.geometry.Offset(x, size.height * .46f),
                                        end = androidx.compose.ui.geometry.Offset(x, size.height * .54f),
                                        strokeWidth = 2.dp.toPx(),
                                    )
                                }
                            }
                        }
                        Slider(
                            value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                            onValueChange = { target -> seekRequest += target.toLong() - position },
                            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val transport = @androidx.compose.runtime.Composable { modifier: Modifier ->
                            PlayerTransportControls(
                                modifier = modifier,
                                playing = playing,
                                onRewind = { seekRequest -= 10_000; controlsEpoch++ },
                                onTogglePlayback = { playing = !playing; controlsEpoch++ },
                                onForward = { seekRequest += 10_000; controlsEpoch++ },
                            )
                        }
                        val options = @androidx.compose.runtime.Composable { modifier: Modifier, arrangement: Arrangement.Horizontal ->
                            PlayerUtilityControls(
                                modifier = modifier,
                                horizontalArrangement = arrangement,
                                subtitlesEnabled = subtitlesEnabled,
                                onSubtitles = { subtitleControlsOpen = true; controlsEpoch++ },
                                qualityLabel = selectedStream?.label ?: "Auto",
                                qualities = state.streams,
                                qualityMenuExpanded = showQualityMenu,
                                onQualityMenuChange = { showQualityMenu = it },
                                onQualitySelected = {
                                    selectedQuality = it
                                    showQualityMenu = false
                                    controlsEpoch++
                                },
                                showAutoPlayNext = season > 0 && episode > 0,
                                autoPlayNext = autoPlayNextEpisode,
                                onToggleAutoPlayNext = { autoPlayNextEpisode = !autoPlayNextEpisode },
                                onAspectRatio = { screenRatioRequest++; controlsEpoch++ },
                            )
                        }
                        if (maxWidth < 600.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("${formatTime(position)} / ${formatTime(duration)}", color = Color.White)
                                    transport(Modifier)
                                }
                                options(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly)
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.weight(1f)) {
                                    Text("${formatTime(position)} / ${formatTime(duration)}", color = Color.White)
                                }
                                transport(Modifier.weight(1f))
                                options(Modifier.weight(1f), Arrangement.End)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpNext && nextEpisode != null) {
        UpNextOverlay(
            seriesTitle = state.media?.title.orEmpty(),
            thumbnailUrl = state.media?.backdropUrl.orEmpty().ifBlank { state.media?.posterUrl.orEmpty() },
            nextSeason = nextEpisode.first,
            nextEpisode = nextEpisode.second,
            secondsRemaining = upNextSeconds,
            autoPlayEnabled = autoPlayNextEpisode,
            playFocusRequester = upNextPlayFocus,
            cancelFocusRequester = upNextCancelFocus,
            onActionFocusChanged = { actionButtonFocused = it },
            onPlayNow = ::playNextEpisode,
            onCancel = {
                showUpNext = false
                upNextDismissed = true
                actionButtonFocused = false
            },
        )
    }
    if (pairingDialogOpen) {
        DevicePairingDialog(onDismiss = { pairingDialogOpen = false })
    }
    castStatus?.let { message ->
        AlertDialog(
            onDismissRequest = { castStatus = null },
            containerColor = Color(0xFF15151C),
            title = { Text("Cast failed") },
            text = {
                DialogBackdropEffect()
                Text(message)
            },
            confirmButton = { TextButton(onClick = { castStatus = null }) { Text("OK") } },
        )
    }

    if (subtitleControlsOpen) {
        AlertDialog(
            onDismissRequest = { subtitleControlsOpen = false },
            containerColor = Color(0xFF15151C),
            title = { Text("Subtitle controls") },
            text = {
                DialogBackdropEffect()
                Column(
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        subtitleStatus ?: "Default provider: ECHOSubs API (${userPreferences.subtitleLanguage}).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        if (subtitleOptions.isEmpty()) "Subtitle tracks" else "${subtitleOptions.size} subtitle tracks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !subtitleQuerying,
                            onClick = {
                                val media = state.media ?: return@Button
                                scope.launch {
                                    subtitleQuerying = true
                                    subtitleStatus = null
                                    runCatching {
                                        subtitlesApi.search(
                                            media,
                                            userPreferences.subtitleLanguage,
                                            season.takeIf { it > 0 },
                                            episode.takeIf { it > 0 },
                                            userPreferences.openSubtitlesApiKey,
                                            userPreferences.subdlApiKey,
                                        )
                                    }.onSuccess {
                                        subtitleOptions = it
                                        subtitleStatus = if (it.isEmpty()) "No matching subtitles were found." else "Choose a subtitle to download."
                                    }.onFailure {
                                        subtitleStatus = it.message ?: "Subtitle search failed."
                                    }
                                    subtitleQuerying = false
                                }
                            },
                        ) { Text(if (subtitleQuerying) "Searching…" else "Search subtitles") }
                        SubtitleFilePickerButton(
                            onSelected = { name, contents ->
                                val parsed = parseSubtitleDocument(contents)
                                if (parsed.isEmpty()) {
                                    subtitleStatus = "$name did not contain readable SRT or VTT cues."
                                } else {
                                    subtitleCues = parsed
                                    subtitlesEnabled = true
                                    subtitleStatus = "Loaded $name (${parsed.size} cues)."
                                }
                            },
                            onError = { subtitleStatus = it },
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                            subtitlesEnabled = !subtitlesEnabled
                            controlsEpoch++
                        }) { Text(if (subtitlesEnabled) "Turn subtitles off" else "Turn subtitles on") }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                            val updated = userPreferences.copy(subtitleOffsetMillis = userPreferences.subtitleOffsetMillis - 250)
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        }) { Text("−250 ms") }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                            val updated = userPreferences.copy(subtitleOffsetMillis = userPreferences.subtitleOffsetMillis + 250)
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        }) { Text("+250 ms") }
                    }
                    Text("Offset: ${userPreferences.subtitleOffsetMillis} ms")
                    Text("Vertical position: ${userPreferences.subtitleVerticalPositionPercent.coerceIn(0, 25)}%")
                    Slider(
                        modifier = Modifier.onFocusChanged { subtitleSliderFocused = it.isFocused },
                        value = userPreferences.subtitleVerticalPositionPercent.coerceIn(0, 25).toFloat(),
                        onValueChange = { position ->
                            val updated = userPreferences.copy(subtitleVerticalPositionPercent = position.toInt())
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        },
                        valueRange = 0f..25f,
                    )
                    Slider(
                        value = userPreferences.subtitleStyle.sizeSp.toFloat(),
                        onValueChange = { size ->
                            val updated = userPreferences.copy(
                                subtitleStyle = userPreferences.subtitleStyle.copy(sizeSp = size.toInt()),
                            )
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        },
                        valueRange = 14f..36f,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val updated = userPreferences.copy(
                                subtitleStyle = userPreferences.subtitleStyle.copy(
                                    background = 0xCC000000,
                                    text = 0xFFFFFFFF,
                                ),
                            )
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Dark subtitle background") }
                        OutlinedButton(onClick = {
                            val updated = userPreferences.copy(
                                subtitleStyle = userPreferences.subtitleStyle.copy(
                                    background = 0xCCFFFFFF,
                                    text = 0xFF000000,
                                ),
                            )
                            userPreferences = updated
                            scope.launch { preferences.update(updated) }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Light subtitle background") }
                    }
                    OutlinedButton(onClick = {
                        val updated = userPreferences.copy(
                            subtitleStyle = userPreferences.subtitleStyle.copy(border = 0xFFFFFFFF),
                        )
                        userPreferences = updated
                        scope.launch { preferences.update(updated) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("High-contrast subtitle border") }
                    subtitleOptions.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    subtitleStatus = "Downloading ${option.title}…"
                                    runCatching {
                                        subtitlesApi.download(
                                            option,
                                            userPreferences.openSubtitlesApiKey,
                                            userPreferences.subdlApiKey,
                                        )
                                    }
                                        .onSuccess { text ->
                                            val cues = parseSubtitleDocument(text)
                                            if (cues.isEmpty()) {
                                                subtitleStatus = "Downloaded subtitle has no supported SRT/VTT cues."
                                            } else if (userPreferences.autoApplySubtitles) {
                                                subtitleCues = cues
                                                subtitlesEnabled = true
                                                subtitleStatus = "Applied ${option.title}."
                                            } else {
                                                subtitleCues = cues
                                                subtitleStatus = "Downloaded ${option.title}. Turn subtitles on to apply it."
                                            }
                                        }.onFailure { subtitleStatus = it.message ?: "Subtitle download failed." }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${option.language}  ·  ${option.title}  ·  ${option.provider}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { subtitleControlsOpen = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun PlayerTransportControls(
    modifier: Modifier = Modifier,
    playing: Boolean,
    onRewind: () -> Unit,
    onTogglePlayback: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onRewind) {
            Icon(Icons.Rounded.Replay10, "Back 10 seconds", tint = Color.White)
        }
        IconButton(onClick = onTogglePlayback) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playing) "Pause" else "Play",
                tint = Color.White,
            )
        }
        IconButton(onClick = onForward) {
            Icon(Icons.Rounded.Forward10, "Forward 10 seconds", tint = Color.White)
        }
    }
}

@Composable
private fun UpNextOverlay(
    seriesTitle: String,
    thumbnailUrl: String,
    nextSeason: Int,
    nextEpisode: Episode,
    secondsRemaining: Int,
    autoPlayEnabled: Boolean,
    playFocusRequester: FocusRequester,
    cancelFocusRequester: FocusRequester,
    onActionFocusChanged: (Boolean) -> Unit,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            Modifier.widthIn(max = 320.dp)
                .padding(end = 20.dp, bottom = 20.dp)
                .background(Color(0xF017171D), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                imageLoader = LocalEchoImageLoader.current,
                contentDescription = "Next episode thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(.72f)
                    .height(82.dp)
                    .background(Color(0xFF292930), RoundedCornerShape(8.dp)),
            )
            Text(
                "Up Next: S$nextSeason:E${nextEpisode.number}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                "$seriesTitle${nextEpisode.title.takeIf(String::isNotBlank)?.let { " - $it" }.orEmpty()}",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            LinearProgressIndicator(
                progress = secondsRemaining / UP_NEXT_COUNTDOWN_SECONDS.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.18f),
            )
            Text(
                if (autoPlayEnabled) "Playing in $secondsRemaining seconds"
                else "Auto-play is off",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPlayNow,
                    modifier = Modifier.weight(1f)
                        .focusRequester(playFocusRequester)
                        .onFocusChanged { onActionFocusChanged(it.isFocused) },
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play Now")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                        .focusRequester(cancelFocusRequester)
                        .onFocusChanged { onActionFocusChanged(it.isFocused) },
                ) {
                    Text("Cancel")
                }
            }

        }
    }
}

@Composable
private fun PlayerUtilityControls(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    subtitlesEnabled: Boolean,
    onSubtitles: () -> Unit,
    qualityLabel: String,
    qualities: List<VideoQuality>,
    qualityMenuExpanded: Boolean,
    onQualityMenuChange: (Boolean) -> Unit,
    onQualitySelected: (Int) -> Unit,
    showAutoPlayNext: Boolean,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: () -> Unit,
    onAspectRatio: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSubtitles) {
            Icon(
                Icons.Rounded.Subtitles,
                "Subtitle controls",
                tint = if (subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
        Box {
            IconButton(onClick = { onQualityMenuChange(true) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.HighQuality, contentDescription = "Video quality", tint = Color.White)
                    Text(qualityLabel, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            DropdownMenu(
                expanded = qualityMenuExpanded,
                onDismissRequest = { onQualityMenuChange(false) },
            ) {
                qualities.forEachIndexed { index, quality ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (quality.providerName.isBlank()) quality.label
                                else "${quality.label} · ${quality.providerName}",
                            )
                        },
                        onClick = { onQualitySelected(index) },
                    )
                }
            }
        }
        if (showAutoPlayNext) {
            IconButton(onClick = onToggleAutoPlayNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = if (autoPlayNext) "Auto-play next episode on" else "Auto-play next episode off",
                    tint = if (autoPlayNext) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
        IconButton(onClick = onAspectRatio) {
            Icon(Icons.Rounded.AspectRatio, contentDescription = "Adjust screen ratio", tint = Color.White)
        }
    }
}

private fun parseSubtitleDocument(document: String): List<SubtitleCue> {
    val normalized = document.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
    return normalized.split(Regex("\n\\s*\n"))
        .mapNotNull { block ->
            val lines = block.lines().map(String::trim).filter(String::isNotEmpty)
            val timingIndex = lines.indexOfFirst { "-->" in it }
            if (timingIndex < 0) return@mapNotNull null
            val timing = lines[timingIndex].split("-->")
            if (timing.size != 2) return@mapNotNull null
            val start = parseSubtitleTime(timing[0].trim().substringBefore(' '))
            val end = parseSubtitleTime(timing[1].trim().substringBefore(' '))
            if (start == null || end == null || end <= start) return@mapNotNull null
            val text = lines.drop(timingIndex + 1).joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
            text.takeIf(String::isNotBlank)?.let { SubtitleCue(start, end, it) }
        }.sortedBy(SubtitleCue::startMillis)
}

private fun parseSubtitleTime(value: String): Long? {
    val time = value.replace(',', '.').split(':')
    if (time.size !in 2..3) return null
    val secondsParts = time.last().split('.')
    val seconds = secondsParts.firstOrNull()?.toLongOrNull() ?: return null
    val fraction = secondsParts.getOrNull(1)?.take(3)?.padEnd(3, '0')?.toLongOrNull() ?: 0L
    val minutes = time.getOrNull(time.size - 2)?.toLongOrNull() ?: return null
    val hours = if (time.size == 3) time.first().toLongOrNull() ?: return null else 0L
    return ((hours * 3600 + minutes * 60 + seconds) * 1000) + fraction
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val remainder = seconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}"
    else "$minutes:${remainder.toString().padStart(2, '0')}"
}
