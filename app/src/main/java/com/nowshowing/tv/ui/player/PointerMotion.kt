package com.nowshowing.tv.ui.player

internal object PointerMotion {
    /** Short taps are precise; a held direction accelerates without becoming uncontrollable. */
    fun stepDp(repeatCount: Int, speedMultiplier: Float = 1.0f): Float {
        val base = when {
            repeatCount < 4 -> 8f
            repeatCount < 12 -> 16f
            else -> 28f
        }
        return (base * speedMultiplier).coerceIn(4f, 48f)
    }
}
