package com.echonow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.echonow.presentation.components.EchoTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echonow.domain.model.Media
import com.echonow.presentation.components.EmptyPanel
import com.echonow.presentation.components.PosterGridCard
import com.echonow.presentation.components.SectionTitle
import com.echonow.presentation.viewmodel.FavoritesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoritesScreen(onOpen: (Media) -> Unit) {
    val viewModel: FavoritesViewModel = koinViewModel()
    val items by viewModel.items.collectAsState()
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionTitle("My list", "Saved for whenever you're ready")
            if (items.isNotEmpty()) TextButton(onClick = viewModel::clear, modifier = Modifier.padding(top = 16.dp, end = 12.dp)) { androidx.compose.material3.Text("Clear all") }
        }
        if (items.isEmpty()) EmptyPanel("Your list is waiting", "Save a movie or series to find it here.")
        else LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { it.id }) { media ->
                PosterGridCard(media, onClick = { onOpen(media) }, isFavorite = true, onFavorite = { viewModel.toggle(media.id) })
            }
        }
    }
}
