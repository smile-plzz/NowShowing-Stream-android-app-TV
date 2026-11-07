package com.nowshowing.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 10-foot TV Spacing & Overscan tokens (Section E3).
 */
object NsSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 16.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 48.dp
    val xxl: Dp = 64.dp
    val giant: Dp = 96.dp

    // 5% TV Overscan Safe Margins for 1080p
    val safeMarginHorizontal: Dp = 56.dp
    val safeMarginVertical: Dp = 36.dp
}

/**
 * TV Shape tokens (Section E3).
 */
object NsShape {
    val smallControlRadius: Dp = 12.dp
    val cardRadius: Dp = 16.dp
    val dialogRadius: Dp = 20.dp
    val heroRadius: Dp = 28.dp
}

/**
 * TV Typography Scale (Section E2).
 * Strictly enforced >= 16 sp to guarantee 10-foot couch legibility.
 */
object NsTypography {
    val Display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp,
        color = NsTextPrimary
    )
    val Title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = NsTextPrimary
    )
    val Headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = NsTextPrimary
    )
    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = NsTextSecondary
    )
    val Caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = NsTextSecondary
    )
    val Micro = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = NsTextTertiary
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NsAccent,
    secondary = NsAccent,
    background = NsBackground,
    surface = NsSurface1,
    surfaceVariant = NsSurface2,
    onPrimary = NsBackground,
    onSecondary = NsBackground,
    onBackground = NsTextPrimary,
    onSurface = NsTextPrimary,
    onSurfaceVariant = NsTextSecondary,
    error = NsDestructive,
    onError = NsTextPrimary
)

@Composable
fun NowShowingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
