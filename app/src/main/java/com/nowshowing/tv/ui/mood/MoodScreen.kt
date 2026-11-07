package com.nowshowing.tv.ui.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.domain.mood.MoodItem
import com.nowshowing.tv.domain.mood.MoodRecommendationEngine
import com.nowshowing.tv.ui.common.*
import com.nowshowing.tv.ui.theme.*

@Composable
fun MoodScreen(
    viewModel: MoodViewModel,
    onNavigate: (String) -> Unit,
    onTitleClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showYears by remember { mutableStateOf(false) }
    var restoreYearFocus by remember { mutableStateOf(false) }
    val yearFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    if (showYears) {
        YearPicker(uiState.selectedYear, onDismiss = { restoreYearFocus = true; showYears = false }, onApply = {
            viewModel.selectYear(it); restoreYearFocus = true; showYears = false
        })
    }
    LaunchedEffect(showYears) {
        if (!showYears && restoreYearFocus) {
            yearFocus.requestFocus(); restoreYearFocus = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvTopBar(currentRoute = "mood", onNavigate = onNavigate)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            // Header Row: Title & "Surprise Me" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "How are you feeling?",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.statusMessage,
                        color = TextMuted,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Surprise Me Pill Button
                Box(Modifier.focusRequester(yearFocus)) {
                    com.nowshowing.tv.ui.player.TvPlayerActionButton("Year: ${uiState.selectedYear ?: "All Years"}", null) { showYears = true }
                }
                Spacer(Modifier.width(12.dp))
                SurpriseMeButton(onClick = { viewModel.surpriseMe() })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Mood Palette Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MoodRecommendationEngine.MOODS, key = { it.name }) { mood ->
                    MoodPill(
                        mood = mood,
                        isSelected = uiState.selectedMood?.name == mood.name,
                        onClick = { viewModel.selectMood(mood) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Grid
            if (uiState.isLoading && uiState.recommendations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (uiState.recommendations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NsStateMessage(
                        title = if (uiState.selectedMood == null) "Select a Mood" else "No matches in current collection",
                        message = if (uiState.selectedMood == null) "Choose how you're feeling to discover matching titles." else uiState.statusMessage,
                        actionLabel = if (uiState.selectedYear != null) "Clear Year Filter" else if (uiState.selectedMood != null) "Try Again" else null,
                        onAction = {
                            if (uiState.selectedYear != null) viewModel.selectYear(null)
                            else viewModel.retry()
                        }
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.recommendations, key = { it.imdbID }) { movie ->
                        NsPosterCard(
                            focusKey = "mood:${movie.imdbID}",
                            title = movie.title,
                            posterUrl = movie.poster,
                            year = movie.year,
                            badge = if (movie.rating > 0f) "★ ${movie.rating}" else null,
                            width = 145.dp,
                            height = 218.dp,
                            onClick = { onTitleClick(movie.imdbID, movie.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodPill(
    mood: MoodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "mood_pill_scale"
    )

    val bg = when {
        isFocused -> NsSurface2
        isSelected -> NsSurface2.copy(alpha = 0.85f)
        else -> NsSurface1
    }

    val textColor = when {
        isFocused || isSelected -> NsTextPrimary
        else -> NsTextSecondary
    }

    Row(
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 10.dp else 0.dp,
                shape = RoundedCornerShape(NsShape.cardRadius),
                spotColor = NsFocusShadow
            )
            .clip(RoundedCornerShape(NsShape.cardRadius))
            .background(bg)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) NsAccent else if (isSelected) NsAccent.copy(alpha = 0.6f) else NsSeparator,
                shape = RoundedCornerShape(NsShape.cardRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mood.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = mood.label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun SurpriseMeButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "surprise_btn_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 10.dp else 0.dp,
                shape = RoundedCornerShape(NsShape.smallControlRadius),
                spotColor = NsFocusShadow
            )
            .clip(RoundedCornerShape(NsShape.smallControlRadius))
            .background(if (isFocused) NsSurface2 else NsSurface1)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) NsAccent else NsSeparator,
                shape = RoundedCornerShape(NsShape.smallControlRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Surprise Me",
            tint = if (isFocused) NsAccent else NsTextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Surprise Me",
            color = if (isFocused) NsAccent else NsTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

@Composable
internal fun YearPicker(selected: Int?, onDismiss: () -> Unit, onApply: (Int?) -> Unit) {
    val years = remember { listOf<Int?>(null) + (java.time.Year.now().value downTo 1888).toList() }
    val index = years.indexOf(selected).coerceAtLeast(0)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val focus = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(300.dp)
                .background(NsSurface1, RoundedCornerShape(NsShape.dialogRadius))
                .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.dialogRadius))
                .padding(24.dp)
        ) {
            Text("Release year", style = NsTypography.Title, color = NsTextPrimary)
            Text("OK selects · Back cancels", style = NsTypography.Caption, color = NsTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyColumn(state = list, modifier = Modifier.height(260.dp)) {
                items(years.size) { row ->
                    val rowModifier = if (row == index) Modifier.focusRequester(focus).onGloballyPositioned {
                        if (!initialFocusRequested) {
                            initialFocusRequested = true
                            focus.requestFocus()
                        }
                    } else Modifier
                    com.nowshowing.tv.ui.player.TvPlayerActionButton(
                        years[row]?.toString() ?: "All Years", null, modifier = rowModifier
                    ) { onApply(years[row]) }
                }
            }
        }
    }
}
