package com.nowshowing.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * NowShowing Design Tokens — Minimal, cinematic, calm, modern TV restraint.
 * Based on Section E1 of PRODUCT-EXCELLENCE-AGENT-HANDOFF.
 */

// Backgrounds
val NsBackground = Color(0xFF0B0B0D) // Near-black base
val NsSurface1 = Color(0xFF161618)   // Elevated surface 1
val NsSurface2 = Color(0xFF1F1F22)   // Elevated surface 2 / controls

// Typography / Text Tokens
val NsTextPrimary = Color(0xFFF5F5F7)               // High-contrast readable off-white
val NsTextSecondary = Color(0xA3F5F5F7)             // rgba(245,245,247,0.64)
val NsTextTertiary = Color(0x66F5F5F7)              // rgba(245,245,247,0.40)
val NsSeparator = Color(0x14FFFFFF)                 // rgba(255,255,255,0.08)

// Accents (Exactly one accent + system red for errors/destructive)
val NsAccent = Color(0xFF0A84FF)                    // Calm Apple-like blue
val NsDestructive = Color(0xFFFF453A)               // Destructive / error red
val NsRatingStar = Color(0xFFFFD60A)                // Restrained gold star for ratings only

// Focus Tokens
val NsFocusBorder = NsAccent
val NsFocusShadow = Color(0x4D000000)               // Soft black drop shadow at 30%

// Backward-compatible aliases for existing screens during incremental migration
val BgDark = NsBackground
val SurfaceDark = NsSurface1
val SurfaceVariant = NsSurface2
val AccentCyan = NsAccent
val AccentGold = NsAccent
val TextPrimary = NsTextPrimary
val TextSecondary = NsTextSecondary
val TextMuted = NsTextTertiary
val FocusBorder = NsFocusBorder
val FocusGlow = Color(0x260A84FF)
val CardBorder = NsSeparator
