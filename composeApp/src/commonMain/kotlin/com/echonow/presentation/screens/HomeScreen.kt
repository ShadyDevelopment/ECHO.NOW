package com.echonow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import com.echonow.presentation.components.EchoButton as Button
import com.echonow.presentation.components.EchoOutlinedButton as OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.domain.repository.WatchProgress
import com.echonow.domain.repository.WatchProgressRepository
import com.echonow.platform.configuredAppName
import com.echonow.presentation.components.ContinueWatchingCard
import com.echonow.presentation.components.LocalEchoImageLoader
import com.echonow.presentation.components.MediaCard
import com.echonow.presentation.components.MediaMetadataPill
import com.echonow.presentation.components.SectionTitle
import com.echonow.presentation.viewmodel.CatalogViewModel
import com.echonow.presentation.viewmodel.FavoritesViewModel
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeScreen(onOpen: (Media) -> Unit, onPlay: (Media) -> Unit, onResume: (WatchProgress) -> Unit) {
    val viewModel: CatalogViewModel = koinViewModel()
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val favoriteViewModel: FavoritesViewModel = koinViewModel()
    val favoriteItems by favoriteViewModel.items.collectAsState()
    val favoriteIds by favoriteViewModel.ids.collectAsState()
    val hero = movies.items
        .filter { it.backdropUrl.isNotBlank() }
        .maxByOrNull { it.rating }
    val progressRepository: WatchProgressRepository = koinInject()
    val allProgress by progressRepository.observe().collectAsState(emptyList())
    val heroProgress = hero?.let { candidate ->
        allProgress.firstOrNull {
            it.mediaId == candidate.id && it.type == MediaType.MOVIE &&
                it.durationMillis > 0L && !it.isComplete && it.watchedPercentage < .99f
        }
    }
    val latestSeriesProgress = allProgress
        .filter { it.type == MediaType.SERIES }
        .distinctBy { it.mediaId }
    val watching = (allProgress.filter { it.type == MediaType.MOVIE } + latestSeriesProgress)
        .filter { progress ->
        progress.durationMillis > 0L && when (progress.type) {
            MediaType.SERIES -> progress.episode > 0 && !progress.isComplete
            MediaType.MOVIE -> progress.watchedPercentage < .99f
        }
    }.sortedBy { progress -> allProgress.indexOf(progress) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp),
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(420.dp)
                    .background(Color(0xFF0D0F12)),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (hero != null) {
                    AsyncImage(
                        model = hero.backdropUrl,
                        imageLoader = LocalEchoImageLoader.current,
                        contentDescription = "${hero.title} backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF0D0F12).copy(alpha = .42f),
                                Color(0xFF0D0F12),
                            ),
                        ),
                    ),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0D0F12).copy(alpha = .9f),
                                Color(0xFF0D0F12).copy(alpha = .42f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                )
                Column(
                Modifier.width(560.dp).padding(horizontal = 22.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${configuredAppName()} CHOICE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(hero?.title ?: "Stories worth staying for", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    if (hero != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hero.year > 0) MediaMetadataPill(hero.year.toString())
                            if (hero.rating > 0) MediaMetadataPill("★ ${"%.1f".format(hero.rating)}")
                            hero.genres.take(2).forEach { genre -> MediaMetadataPill(genre) }
                        }
                        Text(hero.synopsis, maxLines = 3, color = Color.White.copy(alpha = .82f))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                if (heroProgress != null) onResume(heroProgress) else onPlay(hero)
                            }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Text(if (heroProgress != null) " Resume" else " Play now")
                            }
                            OutlinedButton(onClick = { onOpen(hero) }) { Text("More info") }
                            OutlinedButton(onClick = { favoriteViewModel.toggle(hero.id) }) {
                                Text(if (favoriteIds.contains(hero.id)) "Remove from My list" else "Add to My list")
                            }
                        }
                    } else {
                        Text("Discover something new to watch.", color = Color.White.copy(alpha = .82f))
                    }
                }
            }
        }
        if (watching.isNotEmpty()) {
            item {
                SectionTitle("Continue watching", "Pick up where you left off")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(watching, key = { it.entryId }) { item ->
                        ContinueWatchingCard(
                            progress = item,
                            onClick = { onResume(item) },
                        )
                    }
                }
            }
        }
        if (favoriteItems.isNotEmpty()) {
            item {
                SectionTitle("My favorites", "Saved movies and series")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(favoriteItems, key = { it.id }) { media ->
                        MediaCard(media, onClick = { onOpen(media) })
                    }
                }
            }
        }
        item {
            SectionTitle("Trending movies", "A good story starts here")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(movies.items, key = { it.id }) { media ->
                    MediaCard(media, onClick = { onOpen(media) })
                }
            }
        }
        item {
            SectionTitle("Series worth staying in for", "Your next favorite, episode after episode")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(series.items, key = { it.id }) { media ->
                    MediaCard(media, onClick = { onOpen(media) })
                }
            }
        }
    }
}
