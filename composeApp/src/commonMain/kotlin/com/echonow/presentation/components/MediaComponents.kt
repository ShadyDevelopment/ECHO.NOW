package com.echonow.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.usecase.GetSeasonCount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import com.echonow.theme.EchoTokens
import org.koin.compose.koinInject

@Composable
fun PosterCard(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isFavorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    width: Dp = 170.dp,
) {
    var focused by remember(media.id) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(if (isFocused || focused) 3.dp else 1.dp, if (isFocused || focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .08f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(7.dp),
    ) {
        Box {
            AsyncImage(
                model = media.posterUrl,
                imageLoader = LocalEchoImageLoader.current,
                contentDescription = "${media.title} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(10.dp)),
            )
            if (onFavorite != null) {
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(38.dp)) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
                    )
                }
            }
            Text(
                text = if (media.type == MediaType.MOVIE) "FILM" else "SERIES",
                modifier = Modifier.align(Alignment.BottomStart).padding(7.dp)
                    .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = .72f))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${media.year}  ·  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Icon(Icons.Rounded.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
            Text(" ${media.rating}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MediaCard(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 190.dp,
) {
    val getSeasonCount: GetSeasonCount = koinInject()
    var focused by remember(media.id) { mutableStateOf(false) }
    val interactionSource = remember(media.id) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val emphasized = focused || hovered
    val knownSeasonCount = media.seasons.count { it.number > 0 }
    var seasonCount by remember(media.id) { mutableStateOf(knownSeasonCount) }
    var seasonLookupDone by remember(media.id) { mutableStateOf(knownSeasonCount > 0 || media.type == MediaType.MOVIE) }
    var seasonLookupFailed by remember(media.id) { mutableStateOf(false) }
    LaunchedEffect(emphasized, media.id) {
        if (emphasized && media.type == MediaType.SERIES && !seasonLookupDone) {
            delay(500)
            try {
                seasonCount = getSeasonCount(media.id) ?: 0
                seasonLookupFailed = seasonCount == 0
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                seasonLookupFailed = true
            }
            seasonLookupDone = true
        }
    }
    var previewActive by remember(media.id) { mutableStateOf(false) }
    var previewMounted by remember(media.id) { mutableStateOf(false) }
    var previewLoaded by remember(media.id) { mutableStateOf(false) }
    LaunchedEffect(emphasized, media.backdropUrl) {
        if (emphasized && media.backdropUrl.isNotBlank()) {
            delay(650)
            previewMounted = true
            previewActive = true
        } else {
            previewActive = false
            delay(500)
            previewMounted = false
            previewLoaded = false
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (emphasized) 1.08f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "mediaCardScale",
    )
    val previewAlpha by animateFloatAsState(
        targetValue = if (previewActive && previewLoaded) .84f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "mediaCardPreview",
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(.68f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (emphasized) 1f else 0f)
            .clip(shape)
            .shadow(if (emphasized) 18.dp else 2.dp, shape)
            .background(
                if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = EchoTokens.focusBackgroundAlpha)
                else MaterialTheme.colorScheme.surface,
                shape,
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .12f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(),
    ) {
        AsyncImage(
            model = media.posterUrl,
            imageLoader = LocalEchoImageLoader.current,
            contentDescription = "${media.title} poster",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (previewMounted) {
            AsyncImage(
                model = media.backdropUrl,
                imageLoader = LocalEchoImageLoader.current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = previewAlpha },
                onSuccess = { previewLoaded = true },
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(146.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = .42f),
                            Color.Black.copy(alpha = .94f),
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    media.title,
                    modifier = Modifier.weight(1f).basicMarquee(
                        iterations = if (emphasized) Int.MAX_VALUE else 0,
                        repeatDelayMillis = 1_500,
                        initialDelayMillis = 1_800,
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = if (emphasized) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildString {
                        append(if (media.year > 0) media.year.toString() else "Year unavailable")
                        if (media.type == MediaType.SERIES) {
                            append("  ·  ")
                            append(
                                when {
                                    seasonCount > 0 -> if (seasonCount == 1) "1 Season" else "$seasonCount Seasons"
                                    seasonLookupFailed -> "Season unavailable"
                                    else -> "Season…"
                                },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = .82f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (media.rating > 0) "★ ${"%.1f".format(media.rating)}" else "★ N/A",
                    modifier = Modifier.padding(start = 8.dp),
                    color = Color(0xFFFFC857),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        if (!emphasized) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = .9f)),
                        ),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    media.title,
                    modifier = Modifier.fillMaxWidth().basicMarquee(
                        iterations = if (emphasized) Int.MAX_VALUE else 0,
                        repeatDelayMillis = 1_500,
                        initialDelayMillis = 1_800,
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        buildString {
                            append(if (media.year > 0) media.year.toString() else "Year unavailable")
                            if (media.type == MediaType.SERIES) {
                                append("  ·  ")
                                append(
                                    when {
                                        seasonCount > 0 -> if (seasonCount == 1) "1 Season" else "$seasonCount Seasons"
                                        seasonLookupFailed -> "Season unavailable"
                                        else -> "Season…"
                                    },
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = .82f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (media.rating > 0) "★ ${"%.1f".format(media.rating)}" else "★ N/A",
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color(0xFFFFC857),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MediaMetadataPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(EchoTokens.badgeBackground)
            .border(1.dp, EchoTokens.badgeBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = EchoTokens.primaryText,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MediaCarousel(
    title: String,
    items: List<Media>,
    onOpen: (Media) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val listState = rememberLazyListState()

    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= items.size - 5) onLoadMore()
            }
    }

    Column(modifier) {
        Text(
            title,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items, key = { it.id }) { media ->
                MediaCard(media = media, onClick = { onOpen(media) })
            }
            if (isLoadingMore) {
                item(key = "loading-dots-$title") {
                    AnimatedLoadingDots(Modifier.width(90.dp).height(280.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedLoadingDots(modifier: Modifier = Modifier) {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            dotCount = if (dotCount == 3) 1 else dotCount + 1
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Loading${".".repeat(dotCount)}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PosterGridCard(
    media: Media,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteOnFocusOnly: Boolean = false,
) {
    var focused by remember(media.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.1f else 1f,
        animationSpec = tween(180),
        label = "posterGridCardScale",
    )
    Column(
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus }
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (focused) 22.dp.toPx() else 0f
                shape = RoundedCornerShape(16.dp)
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .08f),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(8.dp),
    ) {
        Box {
            AsyncImage(
                model = media.posterUrl,
                imageLoader = LocalEchoImageLoader.current,
                contentDescription = "${media.title} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(11.dp)),
            )
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(112.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = .48f), Color.Black.copy(alpha = .94f)),
                        ),
                    ),
            )
            if (!showFavoriteOnFocusOnly || focused) {
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
                    )
                }
            }
            Text(
                if (media.type == MediaType.MOVIE) "MOVIE" else "SERIES",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = .7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 10.sp,
            )
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${media.year}  ·  ", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                    Icon(Icons.Rounded.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Text(" ${media.rating}", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LoadingGrid() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = .35f,
        targetValue = .8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
        repeat(4) {
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(14.dp)).background(Color.Gray.copy(alpha = alpha)))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(.7f).height(14.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(.45f).height(11.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorPanel(message: String, retry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        EchoButton(onClick = retry) { Text("Try again") }
    }
}

@Composable
fun EmptyPanel(title: String, message: String, padding: PaddingValues = PaddingValues(32.dp)) {
    Column(
        Modifier.fillMaxWidth().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
