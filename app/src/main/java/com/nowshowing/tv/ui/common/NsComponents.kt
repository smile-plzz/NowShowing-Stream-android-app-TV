package com.nowshowing.tv.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nowshowing.tv.ui.theme.*

/**
 * 1. NsPosterCard: 2:3 stable aspect ratio, flat Surface 1 placeholder,
 * calm focus ring (3dp NsAccent) with subtle 1.06x scale and soft shadow.
 */
@Composable
fun NsPosterCard(
    title: String,
    posterUrl: String,
    year: String = "",
    badge: String? = null,
    width: Dp = 150.dp,
    height: Dp = 225.dp,
    focusKey: String = "$title:$posterUrl",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (returnRequester, rememberSelection) = rememberReturnFocusRequester(focusKey)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "poster_scale"
    )

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .shadow(
                    elevation = if (isFocused) 12.dp else 0.dp,
                    shape = RoundedCornerShape(NsShape.cardRadius),
                    spotColor = NsFocusShadow
                )
                .clip(RoundedCornerShape(NsShape.cardRadius))
                .background(NsSurface1)
                .border(
                    BorderStroke(
                        width = if (isFocused) 3.dp else 1.dp,
                        color = if (isFocused) NsAccent else NsSeparator
                    ),
                    shape = RoundedCornerShape(NsShape.cardRadius)
                )
                .semantics { contentDescription = listOf(title, year, badge.orEmpty()).filter { it.isNotBlank() }.joinToString(", ") }
                .focusRequester(returnRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { rememberSelection(); onClick() }
                )
        ) {
            var artworkFailed by remember(posterUrl) { mutableStateOf(false) }
            val isMissing = artworkFailed || posterUrl.isBlank() || posterUrl == "N/A"

            if (isMissing) {
                // Flat Surface 1 placeholder, no heavy CPU shimmer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = NsTextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No Poster",
                        style = NsTypography.Micro,
                        color = NsTextTertiary
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(posterUrl)
                        .size(300, 450)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { artworkFailed = true },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle dark bottom gradient on focus for title readability
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NsBackground.copy(alpha = 0.92f))
                            )
                        )
                )
            }

            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(NsShape.smallControlRadius))
                        .background(NsSurface2.copy(alpha = 0.90f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = NsTypography.Micro,
                        color = NsTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Caption below poster
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = NsTypography.Micro,
            color = if (isFocused) NsTextPrimary else NsTextSecondary,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (year.isNotBlank()) {
            Text(
                text = year,
                style = NsTypography.Micro,
                color = NsTextTertiary,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * 2. NsShelf: Headline title, horizontal LazyRow, start padding aligned to TV safe margins.
 */
@Composable
fun <T> NsShelf(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    emptyNotice: String? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty() && emptyNotice == null) return

    Column(modifier = modifier.fillMaxWidth().padding(vertical = NsSpacing.sm)) {
        Text(
            text = title,
            style = NsTypography.Headline,
            color = NsTextPrimary,
            modifier = Modifier.padding(horizontal = NsSpacing.safeMarginHorizontal)
        )
        Spacer(modifier = Modifier.height(NsSpacing.sm))
        if (items.isEmpty()) {
            emptyNotice?.let { notice ->
                Text(
                    text = notice,
                    style = NsTypography.Body,
                    color = NsTextTertiary,
                    modifier = Modifier.padding(horizontal = NsSpacing.safeMarginHorizontal)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = NsSpacing.safeMarginHorizontal),
                horizontalArrangement = Arrangement.spacedBy(NsSpacing.sm)
            ) {
                items(items) { item ->
                    itemContent(item)
                }
            }
        }
    }
}

/**
 * 3. NsNavBar: Text tabs with Apple-like restraint.
 * Selected: primary text + 2dp accent underline; others secondary text. No heavy pill backgrounds.
 */
data class NsNavDestination(val route: String, val label: String, val icon: ImageVector? = null)

@Composable
fun NsNavBar(
    destinations: List<NsNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(NsBackground)
            .padding(horizontal = NsSpacing.safeMarginHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Wordmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(NsAccent)
            )
            Spacer(modifier = Modifier.width(NsSpacing.xs))
            Text(
                text = "NOWSHOWING",
                style = NsTypography.Headline,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = NsTextPrimary
            )
        }

        // Navigation text tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(NsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("primary-navigation")
        ) {
            destinations.forEach { dest ->
                val isSelected = dest.route == currentRoute
                val interaction = remember { MutableInteractionSource() }
                val isFocused by interaction.collectIsFocusedAsState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { if (!isSelected) onNavigate(dest.route) }
                        )
                        .focusable(interactionSource = interaction)
                        .padding(horizontal = NsSpacing.xs, vertical = NsSpacing.xs)
                        .semantics { selected = isSelected }
                ) {
                    Text(
                        text = dest.label,
                        style = NsTypography.Caption,
                        fontSize = 17.sp,
                        fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isFocused -> NsAccent
                            isSelected -> NsTextPrimary
                            else -> NsTextSecondary
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 2dp accent underline for selected or focused
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(if (isSelected || isFocused) 24.dp else 0.dp)
                            .background(if (isFocused) NsAccent else if (isSelected) NsTextPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}

/**
 * 4. NsButton: Consistent 56dp height TV buttons.
 * Primary: filled NsAccent.
 * Secondary: translucent NsSurface2.
 * Text-only: transparent surface.
 */
enum class NsButtonVariant { Primary, Secondary, Destructive, Text }

@Composable
fun NsButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NsButtonVariant = NsButtonVariant.Secondary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "btn_scale"
    )

    val background = when {
        !enabled -> NsSurface1.copy(alpha = 0.5f)
        variant == NsButtonVariant.Primary -> if (isFocused) NsAccent.copy(alpha = 0.9f) else NsAccent
        variant == NsButtonVariant.Destructive -> if (isFocused) NsDestructive.copy(alpha = 0.9f) else NsDestructive.copy(alpha = 0.25f)
        variant == NsButtonVariant.Secondary -> if (isFocused) NsSurface2 else NsSurface2.copy(alpha = 0.65f)
        else -> if (isFocused) NsSurface2.copy(alpha = 0.4f) else Color.Transparent
    }

    val textColor = when {
        !enabled -> NsTextTertiary
        variant == NsButtonVariant.Primary -> NsTextPrimary
        variant == NsButtonVariant.Destructive -> if (isFocused) NsTextPrimary else NsDestructive
        isFocused -> NsTextPrimary
        else -> NsTextSecondary
    }

    Row(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(NsShape.smallControlRadius))
            .background(background)
            .border(
                BorderStroke(
                    width = if (isFocused && enabled) 3.dp else 1.dp,
                    color = if (isFocused && enabled) (if (variant == NsButtonVariant.Destructive) NsDestructive else NsAccent) else NsSeparator
                ),
                shape = RoundedCornerShape(NsShape.smallControlRadius)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = NsSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(NsSpacing.xs))
        }
        Text(
            text = label,
            style = NsTypography.Body,
            fontSize = 18.sp,
            fontWeight = if (isFocused || variant == NsButtonVariant.Primary) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * 5. NsSegmented: Clean segmented control for TV mode switching.
 */
@Composable
fun NsSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(NsShape.smallControlRadius))
            .background(NsSurface1)
            .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.smallControlRadius))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            val isFocused by interaction.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NsSurface2 else Color.Transparent)
                    .border(
                        if (isFocused) 2.dp else 0.dp,
                        if (isFocused) NsAccent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelectIndex(index) }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = NsTypography.Caption,
                    color = if (isSelected || isFocused) NsTextPrimary else NsTextSecondary,
                    fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 7. NsEmptyState & NsErrorState: One truthful sentence, one clear recovery action.
 */
@Composable
fun NsStateMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.Info,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) NsDestructive else NsTextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(NsSpacing.sm))
        Text(
            text = title,
            style = NsTypography.Title,
            color = NsTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NsSpacing.xs))
        Text(
            text = message,
            style = NsTypography.Body,
            color = NsTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 500.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(NsSpacing.md))
            NsButton(
                label = actionLabel,
                onClick = onAction,
                variant = NsButtonVariant.Primary
            )
        }
    }
}

/**
 * 8. NsToast: Non-intrusive bottom notification, auto-dismissing, never steals focus.
 */
@Composable
fun NsToast(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = NsSpacing.xl),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(NsShape.cardRadius))
                .background(NsSurface2.copy(alpha = 0.95f))
                .border(1.dp, NsSeparator, RoundedCornerShape(NsShape.cardRadius))
                .padding(horizontal = NsSpacing.md, vertical = NsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NsSpacing.sm)
        ) {
            Text(
                text = message,
                style = NsTypography.Body,
                color = NsTextPrimary
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = NsTypography.Body,
                    color = NsAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

