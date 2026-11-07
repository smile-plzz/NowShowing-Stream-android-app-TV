package com.nowshowing.tv.playback

import android.content.Context

class PlaybackPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
    var autoplay: Boolean
        get() = prefs.getBoolean("autoplay_next", true)
        set(value) { prefs.edit().putBoolean("autoplay_next", value).apply() }

    var pointerSpeed: String
        get() = prefs.getString("pointer_speed", "Normal") ?: "Normal"
        set(value) { prefs.edit().putString("pointer_speed", value).apply() }

    val pointerSpeedMultiplier: Float
        get() = when (pointerSpeed) {
            POINTER_SPEED_PRECISION -> 0.5f
            POINTER_SPEED_SLOW -> 0.75f
            POINTER_SPEED_FAST -> 1.35f
            else -> 1.0f
        }

    companion object {
        const val POINTER_SPEED_PRECISION = "Precision"
        const val POINTER_SPEED_SLOW = "Slow"
        const val POINTER_SPEED_NORMAL = "Normal"
        const val POINTER_SPEED_FAST = "Fast"
    }
}
