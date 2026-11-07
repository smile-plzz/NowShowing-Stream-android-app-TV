package com.nowshowing.tv.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowshowing.tv.ui.common.*
import com.nowshowing.tv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onNavigate: (String) -> Unit,
    onTitleClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    var selectedType by rememberSaveable { mutableStateOf(WatchlistType.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(WatchlistSort.RECENT) }
    var isRemoveMode by rememberSaveable { mutableStateOf(false) }

    val visibleItems = remember(uiState.items, selectedType, selectedSort) {
        presentWatchlist(uiState.items, selectedType, selectedSort)
    }

    LaunchedEffect(Unit) {
        viewModel.loadWatchlist()
    }

    // Auto-dismiss undo message after 5 seconds
    LaunchedEffect(uiState.undoMessage) {
        if (uiState.undoMessage != null) {
            delay(5000)
            viewModel.dismissUndo()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NsBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TvTopBar(currentRoute = "watchlist", onNavigate = onNavigate)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NsSpacing.safeMarginHorizontal, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Watchlist",
                            style = NsTypography.Title,
                            color = NsTextPrimary
                        )
                        Text(
                            text = "${visibleItems.size} of ${uiState.items.size} saved titles",
                            style = NsTypography.Caption,
                            color = NsTextSecondary
                        )
                    }

                    // Action buttons: Filter, Sort, Mode, Find
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            NsButton(
                                label = selectedType.label,
                                variant = NsButtonVariant.Secondary,
                                onClick = {
                                    selectedType = WatchlistType.entries[(selectedType.ordinal + 1) % WatchlistType.entries.size]
                                }
                            )
                        }
                        item {
                            NsButton(
                                label = selectedSort.label,
                                variant = NsButtonVariant.Secondary,
                                onClick = {
                                    selectedSort = WatchlistSort.entries[(selectedSort.ordinal + 1) % WatchlistSort.entries.size]
                                }
                            )
                        }
                        item {
                            NsButton(
                                label = if (isRemoveMode) "Mode: Remove On Click" else "Mode: Open",
                                variant = if (isRemoveMode) NsButtonVariant.Destructive else NsButtonVariant.Secondary,
                                onClick = { isRemoveMode = !isRemoveMode }
                            )
                        }
                        item {
                            NsButton(
                                label = "Find Titles",
                                variant = NsButtonVariant.Secondary,
                                onClick = { onNavigate("search") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NsAccent)
                    }
                } else if (visibleItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NsStateMessage(
                            title = if (uiState.items.isEmpty()) "Your Watchlist is Empty" else "No saved ${selectedType.label.lowercase()}",
                            message = if (uiState.items.isEmpty()) "Save movies and series for your evening viewing." else "Try changing the filter to show all saved titles.",
                            actionLabel = if (uiState.items.isEmpty()) "Find Titles to Watch" else "Reset Filter",
                            onAction = {
                                if (uiState.items.isEmpty()) onNavigate("search")
                                else selectedType = WatchlistType.ALL
                            }
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = visibleItems, key = { it.imdbID }) { movie ->
                            NsPosterCard(
                                focusKey = "watchlist:${movie.imdbID}",
                                title = movie.title,
                                posterUrl = movie.poster,
                                year = movie.year,
                                badge = if (isRemoveMode) "Tap to remove" else if (movie.type == "series") "Series" else null,
                                onClick = {
                                    if (isRemoveMode) {
                                        viewModel.removeItem(movie)
                                    } else {
                                        onTitleClick(movie.imdbID, movie.type)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Undo Toast notification
        val undoMsg = uiState.undoMessage
        if (undoMsg != null) {
            NsToast(
                message = undoMsg,
                actionLabel = "Undo",
                onAction = { viewModel.undoRemove() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

