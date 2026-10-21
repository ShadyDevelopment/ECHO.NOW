package com.echonow.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.echonow.presentation.components.EchoButton as Button
import com.echonow.presentation.components.EchoOutlinedButton as OutlinedButton
import com.echonow.presentation.components.EchoTextButton as TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.echonow.domain.model.MediaType
import com.echonow.presentation.components.ErrorPanel
import com.echonow.presentation.components.LocalEchoImageLoader
import com.echonow.presentation.components.MediaMetadataPill
import com.echonow.theme.LocalButtonCustomization
import com.echonow.presentation.viewmodel.DetailViewModel
import com.echonow.platform.PlatformTrailerPlayer
import com.echonow.platform.DialogBackdropEffect
import com.echonow.domain.repository.WatchProgress
import com.echonow.domain.repository.WatchProgressRepository
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetailsScreen(
    id: String,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, qualityIndex: Int, resumePositionMillis: Long) -> Unit,
) {
    val viewModel: DetailViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    var seasonIndex by remember(id) { mutableIntStateOf(0) }
    var episodeIndex by remember(id) { mutableIntStateOf(0) }
    var showSeasons by remember { mutableStateOf(false) }
    var pendingEpisode by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showingTrailer by remember(id) { mutableStateOf(false) }
    val progressRepository: WatchProgressRepository = koinInject()
    val watchProgress by progressRepository.observe().collectAsState(emptyList())
    val castRowState = rememberLazyListState()
    var visibleCastCount by remember(id) { mutableIntStateOf(8) }

    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state.media?.id, seasonIndex, episodeIndex) {
        val media = state.media ?: return@LaunchedEffect
        if (media.type == MediaType.SERIES) {
            val s = media.seasons.getOrNull(seasonIndex)?.number ?: 1
            val e = media.seasons.getOrNull(seasonIndex)?.episodes?.getOrNull(episodeIndex)?.number ?: 1
            viewModel.loadStreams(s, e)
        } else {
            viewModel.loadStreams()
        }
    }
    LaunchedEffect(state.media?.id) {
        state.media?.id?.let(viewModel::loadTrailer)
    }
    LaunchedEffect(state.media?.id) {
        val media = state.media ?: return@LaunchedEffect
        val firstRegularSeason = media.seasons.indexOfFirst { it.number > 0 }
        if (firstRegularSeason >= 0) seasonIndex = firstRegularSeason
    }

    when {
        state.loading -> androidx.compose.material3.CircularProgressIndicator(Modifier.padding(36.dp))
        state.error != null && state.media == null -> ErrorPanel(state.error!!) { viewModel.load(id) }
        state.media == null -> ErrorPanel("This title could not be found.", onBack)
        else -> {
            val media = state.media!!
            val buttonShape = LocalButtonCustomization.current.shape
            val progressOutlineColor = MaterialTheme.colorScheme.primary
            val movieProgress = watchProgress.firstOrNull { it.mediaId == media.id && it.type == MediaType.MOVIE }
            val latestSeriesProgress = watchProgress.firstOrNull { it.mediaId == media.id && it.type == MediaType.SERIES }
            val seriesResumePosition = latestSeriesProgress?.positionMillis ?: 0L
            val progressPercent = { progress: WatchProgress ->
                (progress.positionMillis.toFloat() / progress.durationMillis.coerceAtLeast(1)).coerceIn(0f, 1f)
            }
            val launchEpisode: (Int, Int) -> Unit = { selectedSeason, selectedEpisode ->
                val progress = watchProgress.firstOrNull {
                    it.mediaId == media.id && it.type == MediaType.SERIES &&
                        it.season == selectedSeason && it.episode == selectedEpisode
                }
                if (progress != null && progress.watchedPercentage < WatchProgress.SERIES_CONTINUE_WATCHING_CUTOFF) {
                    pendingEpisode = selectedSeason to selectedEpisode
                } else {
                    onPlay(selectedSeason, selectedEpisode, 0, 0L)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp),
            ) {
                item {
                    Box(Modifier.fillMaxWidth().height(400.dp)) {
                        AsyncImage(
                            model = media.backdropUrl,
                            imageLoader = LocalEchoImageLoader.current,
                            contentDescription = "${media.title} backdrop",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF0D0F12).copy(alpha = .4f),
                                        Color(0xFF0D0F12),
                                    ),
                                ),
                            ),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF0D0F12).copy(alpha = .92f),
                                        Color(0xFF0D0F12).copy(alpha = .48f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                        )
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(
                            Modifier.align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                media.title,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (media.year > 0) MediaMetadataPill(media.year.toString())
                                if (media.rating > 0) MediaMetadataPill("★ ${media.rating}")
                                media.genres.take(3).forEach { genre -> MediaMetadataPill(genre) }
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 28.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(media.synopsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (media.type == MediaType.MOVIE) {
                                val moviePercent = movieProgress?.let(progressPercent) ?: 0f
                                if (movieProgress == null || moviePercent > .99f) {
                                    Button(onClick = { onPlay(0, 0, 0, 0L) }) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                        Text(if (movieProgress == null) " Play" else " Watch again")
                                    }
                                } else {
                                    Button(
                                        onClick = { onPlay(0, 0, 0, movieProgress.positionMillis) },
                                        modifier = Modifier.progressOutline(moviePercent, buttonShape),
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                        Text(" Continue watching")
                                    }
                                    OutlinedButton(onClick = { onPlay(0, 0, 0, 0L) }) {
                                        Text("Watch again")
                                    }
                                }
                            } else if (media.seasons.isNotEmpty()) {
                                val firstSeason = media.seasons.firstOrNull { it.number > 0 } ?: media.seasons.first()
                                val resumeSeason = latestSeriesProgress?.season ?: firstSeason.number
                                val resumeEpisode = latestSeriesProgress?.episode
                                    ?: firstSeason.episodes.firstOrNull()?.number ?: 1
                                val label = if (latestSeriesProgress == null) {
                                    "Play · S$resumeSeason:E$resumeEpisode"
                                } else {
                                    "Continue watching · S$resumeSeason:E$resumeEpisode"
                                }
                                Button(
                                    onClick = {
                                        onPlay(
                                            resumeSeason,
                                            resumeEpisode,
                                            0,
                                            seriesResumePosition,
                                        )
                                    },
                                    modifier = latestSeriesProgress?.let {
                                        Modifier.progressOutline(progressPercent(it), buttonShape)
                                    }
                                        ?: Modifier,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Text(" $label")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (state.trailerUrl?.let(::youtubeVideoId) != null) {
                                        showingTrailer = true
                                    } else {
                                        viewModel.loadTrailer(media.id)
                                    }
                                },
                                enabled = !state.trailerLoading,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Text(if (state.trailerLoading) " Loading trailer…" else " Watch trailer")
                            }
                            IconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    if (state.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (state.favorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (state.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        state.trailerError?.let { message ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(message, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { viewModel.loadTrailer(media.id) }) {
                                    Text("Retry")
                                }
                            }
                        }
                        if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        androidx.compose.foundation.layout.Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LaunchedEffect(media.id) { viewModel.loadCast(media.id) }
                            Text("Cast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            when {
                                state.castLoading -> androidx.compose.material3.CircularProgressIndicator()
                                state.castError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(state.castError!!, color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = { viewModel.loadCast(media.id) }) {
                                        Text("Retry")
                                    }
                                }
                                state.cast.isEmpty() -> Text(
                                    "No cast information available.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                else -> {
                                    LaunchedEffect(state.cast, castRowState, visibleCastCount) {
                                        androidx.compose.runtime.snapshotFlow {
                                            castRowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                        }.collect { lastVisible ->
                                            if (lastVisible != null &&
                                                lastVisible >= visibleCastCount - 2 &&
                                                visibleCastCount < state.cast.size
                                            ) {
                                                visibleCastCount = (visibleCastCount + 8).coerceAtMost(state.cast.size)
                                            }
                                        }
                                    }
                                    LazyRow(
                                        modifier = Modifier.focusGroup(),
                                        state = castRowState,
                                        contentPadding = PaddingValues(end = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(state.cast.take(visibleCastCount), key = { it.id }) { member ->
                                            var focused by remember(member.id) { mutableStateOf(false) }
                                            val scale by animateFloatAsState(
                                                targetValue = if (focused) 1.05f else 1f,
                                                label = "castCardScale",
                                            )
                                            val cardShape = RoundedCornerShape(14.dp)
                                            Column(
                                                Modifier.width(190.dp)
                                                    .graphicsLayer {
                                                        scaleX = scale
                                                        scaleY = scale
                                                    }
                                                    .clip(cardShape)
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(
                                                        width = if (focused) 2.dp else 1.dp,
                                                        color = if (focused) MaterialTheme.colorScheme.primary
                                                        else Color.White.copy(alpha = .12f),
                                                        shape = cardShape,
                                                    )
                                                    .onFocusChanged { focused = it.isFocused }
                                                    .focusable()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                            ) {
                                                Box(
                                                    Modifier.fillMaxWidth().height(132.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (member.profileUrl != null) {
                                                        AsyncImage(
                                                            model = member.profileUrl,
                                                            imageLoader = LocalEchoImageLoader.current,
                                                            contentDescription = member.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Rounded.Person,
                                                            contentDescription = null,
                                                            modifier = Modifier.width(44.dp).height(44.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                                Text(
                                                    member.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 2,
                                                    minLines = 2,
                                                )
                                                Text(
                                                    member.character,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    minLines = 2,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (media.type == MediaType.SERIES && media.seasons.isNotEmpty()) {
                            Text("Episodes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Box {
                                OutlinedButton(onClick = { showSeasons = true }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Season ${media.seasons.getOrNull(seasonIndex)?.number ?: 1}")
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(expanded = showSeasons, onDismissRequest = { showSeasons = false }) {
                                    media.seasons.forEachIndexed { index, season ->
                                        DropdownMenuItem(
                                            text = { Text("Season ${season.number}") },
                                            onClick = {
                                                seasonIndex = index
                                                episodeIndex = 0
                                                showSeasons = false
                                                viewModel.loadStreams(season.number, season.episodes.firstOrNull()?.number)
                                            },
                                        )
                                    }
                                }
                            }
                            media.seasons.getOrNull(seasonIndex)?.episodes?.forEachIndexed { index, episode ->
                                val episodeProgress = watchProgress.firstOrNull {
                                    it.mediaId == media.id && it.type == MediaType.SERIES &&
                                        it.season == media.seasons[seasonIndex].number && it.episode == episode.number
                                }
                                val episodeFraction = episodeProgress?.let(progressPercent) ?: 0f
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth().drawBehind {
                                        if (episodeProgress != null) {
                                            drawRoundRect(
                                                color = progressOutlineColor.copy(alpha = .22f),
                                                size = size.copy(width = size.width * episodeFraction),
                                                cornerRadius = CornerRadius(12.dp.toPx()),
                                            )
                                        }
                                    },
                                    onClick = {
                                        episodeIndex = index
                                        viewModel.loadStreams(media.seasons[seasonIndex].number, episode.number)
                                        launchEpisode(media.seasons[seasonIndex].number, episode.number)
                                    },
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = Color.Transparent,
                                    ),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier.width(176.dp).height(100.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (episode.stillUrl != null) {
                                                AsyncImage(
                                                    model = episode.stillUrl,
                                                    imageLoader = LocalEchoImageLoader.current,
                                                    contentDescription = "${episode.title} still",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.PlayArrow,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Text(
                                                "E${episode.number.toString().padStart(2, '0')}",
                                                modifier = Modifier.align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color.Black.copy(alpha = .72f))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            if (episodeProgress != null) {
                                                Box(
                                                    Modifier.align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .background(Color.White.copy(alpha = .3f)),
                                                ) {
                                                    Box(
                                                        Modifier.fillMaxWidth(episodeFraction)
                                                            .height(4.dp)
                                                            .background(MaterialTheme.colorScheme.primary),
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(
                                            Modifier.weight(1f).widthIn(min = 0.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(episode.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(
                                                buildString {
                                                    append("Season ${media.seasons[seasonIndex].number} · Episode ${episode.number}")
                                                    if (episode.durationMinutes > 0) append(" · ${episode.durationMinutes} min")
                                                    else append(" · Runtime unavailable")
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                            )
                                            Text(
                                                episode.overview.ifBlank { "Play this episode." },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                            )
                                        }
                                        IconButton(onClick = { launchEpisode(media.seasons[seasonIndex].number, episode.number) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${episode.title}")
                                        }
                                    }
                                }
                            }
                        }
                        pendingEpisode?.let { (pendingSeason, pendingEpisodeNumber) ->
                            val progress = watchProgress.firstOrNull {
                                it.mediaId == media.id && it.season == pendingSeason && it.episode == pendingEpisodeNumber
                            }
                            AlertDialog(
                                onDismissRequest = { pendingEpisode = null },
                                containerColor = Color(0xFF15151C),
                                title = { Text("Resume episode?") },
                                text = {
                                    DialogBackdropEffect()
                                    Text(
                                        "You have watched ${((progress?.let(progressPercent) ?: 0f) * 100).toInt()}% of " +
                                            "S$pendingSeason:E$pendingEpisodeNumber.",
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        pendingEpisode = null
                                        onPlay(
                                            pendingSeason,
                                            pendingEpisodeNumber,
                                            0,
                                            progress?.positionMillis ?: 0L,
                                        )
                                    }) { Text("Continue watching") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        pendingEpisode = null
                                        onPlay(pendingSeason, pendingEpisodeNumber, 0, 0L)
                                    }) { Text("Watch again") }
                                },
                            )
                        }
                    }
                }
            }
            if (showingTrailer) {
                state.trailerUrl
                    ?.let(::youtubeVideoId)
                    ?.let { videoId ->
                        PlatformTrailerPlayer(videoId, onClose = { showingTrailer = false })
                    }
            }
        }
    }
}

private fun youtubeVideoId(url: String): String? {
    val withoutFragment = url.substringBefore('#')
    val host = withoutFragment
        .substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .lowercase()
    val id = when {
        host == "youtu.be" || host == "www.youtu.be" ->
            withoutFragment.substringAfter("youtu.be/", "").substringBefore('?').substringBefore('&')
        host.endsWith("youtube.com") ->
            when {
                "v=" in withoutFragment -> withoutFragment.substringAfter("v=").substringBefore('&')
                "/embed/" in withoutFragment -> withoutFragment.substringAfter("/embed/").substringBefore('?').substringBefore('&')
                "/shorts/" in withoutFragment -> withoutFragment.substringAfter("/shorts/").substringBefore('?').substringBefore('&')
                "/live/" in withoutFragment -> withoutFragment.substringAfter("/live/").substringBefore('?').substringBefore('&')
                else -> ""
            }
        else -> ""
    }
    return id.takeIf(String::isNotBlank)
}

private fun Modifier.progressOutline(progress: Float, shape: androidx.compose.ui.graphics.Shape): Modifier =
    drawWithContent {
        drawContent()
        val stroke = 2.5.dp.toPx()
        val inset = stroke / 2
        val outline = shape.createOutline(
            Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
            layoutDirection,
            this,
        )
        val outlinePath = Path().apply {
            when (outline) {
                is Outline.Generic -> addPath(outline.path)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Rectangle -> addRect(outline.rect)
            }
        }
        val measure = PathMeasure().apply { setPath(outlinePath, false) }
        val length = measure.length
        if (length > 0f) {
            val progressPath = Path()
            measure.getSegment(0f, length * progress.coerceIn(0f, 1f), progressPath, startWithMoveTo = true)
            withTransform({
                translate(inset, inset)
            }) {
                drawPath(outlinePath, Color.White.copy(alpha = .48f), style = Stroke(stroke))
                drawPath(progressPath, Color.White, style = Stroke(stroke))
            }
        }
    }
