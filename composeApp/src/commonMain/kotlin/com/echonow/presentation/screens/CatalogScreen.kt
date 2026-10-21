package com.echonow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import com.echonow.presentation.components.EchoButton as Button
import com.echonow.domain.model.Media
import com.echonow.domain.model.MediaType
import com.echonow.presentation.components.EmptyPanel
import com.echonow.presentation.components.ErrorPanel
import com.echonow.presentation.components.AnimatedLoadingDots
import com.echonow.presentation.components.EchoOutlinedButton as OutlinedButton
import com.echonow.presentation.components.MediaCarousel
import com.echonow.presentation.viewmodel.CatalogViewModel
import org.koin.compose.viewmodel.koinViewModel

private data class MediaCategory(
    val title: String,
    val items: List<Media>,
)

@Composable
fun CatalogScreen(type: MediaType, onOpen: (Media) -> Unit) {
    val catalog: CatalogViewModel = koinViewModel()
    val state by (if (type == MediaType.MOVIE) catalog.movies else catalog.series).collectAsState()
    val listState = rememberLazyListState()
    val selectedGenre = state.selectedGenre
    val filteredItems = if (selectedGenre == null) state.items
    else state.items.filter { item -> item.genres.any { it.equals(selectedGenre, ignoreCase = true) } }
    var selectedSort by remember(type) { mutableStateOf("Recommended") }
    val genres = state.items.flatMap(Media::genres).distinct().sorted()
    val visibleItems = when (selectedSort) {
        "Title A–Z" -> filteredItems.sortedBy { it.title.lowercase() }
        "Year: newest" -> filteredItems.sortedWith(compareByDescending<Media> { it.year }.thenBy { it.title })
        "Year: oldest" -> filteredItems.sortedWith(compareBy<Media> { it.year }.thenBy { it.title })
        "Top rated" -> filteredItems.sortedWith(compareByDescending<Media> { it.rating }.thenBy { it.title })
        "Shortest runtime" -> filteredItems.sortedWith(compareBy<Media> { it.runtimeMinutes }.thenBy { it.title })
        "Longest runtime" -> filteredItems.sortedWith(compareByDescending<Media> { it.runtimeMinutes }.thenBy { it.title })
        else -> filteredItems
    }
    val categories = remember(visibleItems, type) {
        buildList {
            if (visibleItems.isNotEmpty()) {
                add(
                    MediaCategory(
                        "Trending Now",
                        visibleItems.sortedWith(
                            compareByDescending<Media> { it.rating }.thenByDescending { it.year },
                        ),
                    ),
                )
                add(
                    MediaCategory(
                        if (type == MediaType.MOVIE) "Popular Movies" else "Popular Series",
                        visibleItems.sortedWith(
                            compareByDescending<Media> { it.rating }.thenBy { it.title },
                        ),
                    ),
                )
                visibleItems.flatMap(Media::genres)
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .take(4)
                    .forEach { (genre, _) ->
                        val genreItems = visibleItems.filter { media ->
                            media.genres.any { it.equals(genre, ignoreCase = true) }
                        }
                        if (genreItems.isNotEmpty()) add(MediaCategory(genre, genreItems))
                    }
            }
        }
    }

    LaunchedEffect(type) {
        if (state.items.isEmpty() && !state.loading) catalog.load(type)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().pointerInput(listState, state.refreshing) {
            var dragDistance = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { change, amount ->
                    if (
                        amount > 0 &&
                        listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0
                    ) {
                        dragDistance += amount
                        change.consume()
                    }
                },
                onDragEnd = {
                    if (dragDistance > 90 && !state.refreshing) catalog.refresh(type)
                    dragDistance = 0f
                },
                onDragCancel = { dragDistance = 0f },
            )
        },
        state = listState,
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "catalog-header") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (type == MediaType.MOVIE) "Movies" else "Series",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Explore something worth watching",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Genre", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (listOf<String?>(null) + genres).forEach { genre ->
                            if (selectedGenre == genre) {
                                Button(onClick = { catalog.selectGenre(type, genre) }) { Text(genre ?: "All genres") }
                            } else {
                                OutlinedButton(onClick = { catalog.selectGenre(type, genre) }) { Text(genre ?: "All genres") }
                            }
                        }
                    }
                    Text("Sort", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "Recommended",
                            "Title A–Z",
                            "Year: newest",
                            "Year: oldest",
                            "Top rated",
                            "Shortest runtime",
                            "Longest runtime",
                        ).forEach { sortOption ->
                            if (selectedSort == sortOption) {
                                Button(onClick = { selectedSort = sortOption }) { Text(sortOption) }
                            } else {
                                OutlinedButton(onClick = { selectedSort = sortOption }) { Text(sortOption) }
                            }
                        }
                    }
                }
            }
        }

        if (state.error != null && state.items.isEmpty()) {
            item(key = "catalog-error") {
                ErrorPanel(state.error!!) { catalog.refresh(type) }
            }
        } else if (state.loading && state.items.isEmpty()) {
            item(key = "catalog-loading") {
                AnimatedLoadingDots(Modifier.fillMaxWidth().padding(vertical = 48.dp))
            }
        }
        if (state.refreshing && state.items.isNotEmpty()) {
            item(key = "catalog-refreshing") {
                AnimatedLoadingDots(Modifier.fillMaxWidth().height(56.dp))
            }
        }

        if (visibleItems.isEmpty() && !state.loading && state.error == null) {
            item(key = "catalog-empty") {
                EmptyPanel(
                    "Nothing here yet",
                    if (selectedGenre == null) "Refresh to check for new titles."
                    else "No titles in this genre on the loaded pages.",
                )
            }
        }

        items(categories, key = { it.title }) { category ->
            MediaCarousel(
                title = category.title,
                items = category.items,
                onOpen = onOpen,
                onLoadMore = {
                    if (!state.endReached && !state.loading && !state.loadingMore) {
                        catalog.load(type, next = true)
                    }
                },
                isLoadingMore = state.loadingMore,
            )
        }
    }
}
