package com.nowshowing.tv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class DesignSystemTest {

    private fun contrastRatio(fg: Color, bg: Color): Float {
        val l1 = fg.luminance()
        val l2 = bg.luminance()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun colorTokens_nearBlackBackground() {
        // Background must be near-black base (#000000 to #0B0B0D)
        assertEquals(Color(0xFF0B0B0D), NsBackground)
        assertTrue("Background luminance should be near-black", NsBackground.luminance() < 0.05f)
    }

    @Test
    fun colorTokens_singleCalmAccent() {
        // Exactly one accent: calm blue #0A84FF
        assertEquals(Color(0xFF0A84FF), NsAccent)
        assertEquals(Color(0xFFFF453A), NsDestructive)
    }

    @Test
    fun colorTokens_contrastTargets() {
        // Target: Primary text at least 7:1 on Surface1
        val primaryContrast = contrastRatio(NsTextPrimary, NsSurface1)
        assertTrue("Primary text contrast on Surface1 must be >= 7:1, was $primaryContrast", primaryContrast >= 7.0f)

        // Target: Secondary text at least 4.5:1 on Surface1
        val secondaryContrast = contrastRatio(NsTextSecondary, NsSurface1)
        assertTrue("Secondary text contrast on Surface1 must be >= 4.5:1, was $secondaryContrast", secondaryContrast >= 4.5f)
    }

    @Test
    fun typography_noTextBelow16Sp() {
        val sizes = listOf(
            NsTypography.Display.fontSize,
            NsTypography.Title.fontSize,
            NsTypography.Headline.fontSize,
            NsTypography.Body.fontSize,
            NsTypography.Caption.fontSize,
            NsTypography.Micro.fontSize
        )
        for (size in sizes) {
            assertTrue("Typography size $size must be >= 16 sp for TV couch readability", size.value >= 16f)
        }
    }

    @Test
    fun spacing_followsEightPointScale() {
        assertEquals(8f, NsSpacing.xs.value, 0.01f)
        assertEquals(16f, NsSpacing.sm.value, 0.01f)
        assertEquals(24f, NsSpacing.md.value, 0.01f)
        assertEquals(32f, NsSpacing.lg.value, 0.01f)
        assertEquals(48f, NsSpacing.xl.value, 0.01f)
        assertEquals(64f, NsSpacing.xxl.value, 0.01f)
        assertEquals(96f, NsSpacing.giant.value, 0.01f)
    }

    @Test
    fun shapes_standardTvRadii() {
        assertEquals(12f, NsShape.smallControlRadius.value, 0.01f)
        assertEquals(16f, NsShape.cardRadius.value, 0.01f)
        assertEquals(20f, NsShape.dialogRadius.value, 0.01f)
        assertEquals(28f, NsShape.heroRadius.value, 0.01f)
    }
}

