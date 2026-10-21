package com.echonow.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.echonow.domain.model.Media
import com.echonow.presentation.components.EmptyPanel
import com.echonow.presentation.components.ErrorPanel
import com.echonow.presentation.components.PosterGridCard
import com.echonow.presentation.viewmodel.FavoritesViewModel
import com.echonow.presentation.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(onOpen: (Media) -> Unit) {
    val search: SearchViewModel = koinViewModel()
    val favorites: FavoritesViewModel = koinViewModel()
    val state by search.state.collectAsState()
    val favoriteIds by favorites.ids.collectAsState()
    val gridState = rememberLazyGridState()
    val searchFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focusedResultIndex by remember { mutableIntStateOf(-1) }
    var focusResultsAfterSearch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    LaunchedEffect(state.results.size, state.loading, state.searched) {
        if (focusResultsAfterSearch && !state.loading) {
            if (state.results.isNotEmpty()) firstResultFocusRequester.requestFocus()
            focusResultsAfterSearch = false
        }
    }

    LaunchedEffect(gridState, state.results.size, state.loading, state.hasMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= state.results.size - 8) {
                    search.loadNextPage()
                }
            }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val safeMargin = (maxWidth * .05f).coerceAtLeast(16.dp)
        val columnCount = ((maxWidth - safeMargin * 2 + 18.dp) / (168.dp + 18.dp)).toInt().coerceIn(2, 8)
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = safeMargin, end = safeMargin, top = 18.dp, bottom = 14.dp)) {
            Text("Search", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("Find your next movie or series", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = safeMargin).focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = search::queryChanged,
                modifier = Modifier.weight(1f)
                    .focusRequester(searchFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && state.results.isNotEmpty()) {
                            firstResultFocusRequester.requestFocus()
                            true
                        } else false
                    },
                singleLine = true,
                placeholder = { Text("Titles, genres…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.loading) CircularProgressIndicator(Modifier.padding(10.dp))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusResultsAfterSearch = state.query.isNotBlank()
                        keyboard?.hide()
                        if (state.results.isNotEmpty()) {
                            firstResultFocusRequester.requestFocus()
                            focusResultsAfterSearch = false
                        }
                    },
                ),
            )
            if (state.query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        search.clear()
                        searchFocusRequester.requestFocus()
                    },
                    modifier = Modifier.focusRequester(clearFocusRequester),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
        Text(
            text = when {
                state.loading -> "Searching…"
                state.error != null && state.results.isEmpty() -> "Search unavailable"
                state.query.isBlank() -> "Search movies and series"
                state.results.isEmpty() -> "No results found"
                else -> "${state.results.size} results found"
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = safeMargin, vertical = 12.dp).animateContentSize(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )

        when {
            state.error != null && state.results.isEmpty() ->
                ErrorPanel(state.error!!) { search.queryChanged(state.query) }
            state.query.isBlank() ->
                EmptyPanel("Search ECHO NOW", "Start typing a title or genre.")
            state.loading && state.results.isEmpty() ->
                SearchSkeletonGrid(columns = columnCount, horizontalPadding = safeMargin)
            state.results.isEmpty() ->
                EmptyPanel("No results found", "Try another title or genre.")
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                        state = gridState,
                        modifier = Modifier.fillMaxSize()
                            .focusRestorer()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionUp && focusedResultIndex in 0 until columnCount
                                ) {
                                    searchFocusRequester.requestFocus()
                                    true
                                } else false
                            },
                        contentPadding = PaddingValues(horizontal = safeMargin, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(state.results, key = { _, media -> "${media.type}:${media.id}" }) { index, media ->
                            PosterGridCard(
                                media = media,
                                onClick = { onOpen(media) },
                                isFavorite = media.id in favoriteIds,
                                onFavorite = { favorites.toggle(media.id) },
                                showFavoriteOnFocusOnly = true,
                                modifier = Modifier
                                    .then(if (index == 0) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                                    .onFocusChanged { if (it.isFocused) focusedResultIndex = index },
                            )
                        }
                        if (state.loadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SearchSkeletonFooter()
                            }
                        }
                        if (state.error != null && state.results.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Button(onClick = search::loadNextPage) { Text("Retry loading more") }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun SearchSkeletonFooter() {
    val transition = rememberInfiniteTransition(label = "searchMoreSkeleton")
    val alpha by transition.animateFloat(
        initialValue = .18f,
        targetValue = .48f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "searchMoreSkeletonAlpha",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Box(
                Modifier.weight(1f).aspectRatio(.68f).clip(RoundedCornerShape(14.dp))
                    .background(Color.Gray.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun SearchSkeletonGrid(
    cardCount: Int = 8,
    columns: Int = 4,
    horizontalPadding: Dp = 22.dp,
) {
    val transition = rememberInfiniteTransition(label = "searchSkeleton")
    val alpha by transition.animateFloat(
        initialValue = .18f,
        targetValue = .48f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "searchSkeletonAlpha",
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(cardCount) {
            Column {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(14.dp))
                        .background(Color.Gray.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth(.72f).height(14.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(.46f).height(11.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
            }
        }
    }
}
