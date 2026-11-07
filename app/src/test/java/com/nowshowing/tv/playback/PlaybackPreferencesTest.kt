package com.nowshowing.tv.playback

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class PlaybackPreferencesTest {
    @Test fun defaultOnAndPersistsOff() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("playback", 0).edit().clear().commit()
        assertTrue(PlaybackPreferences(context).autoplay)
        PlaybackPreferences(context).autoplay = false
        assertFalse(PlaybackPreferences(context).autoplay)
    }

    @Test fun pointerSpeedCycleAndMultiplier() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("playback", 0).edit().clear().commit()
        val prefs = PlaybackPreferences(context)
        assertEquals(PlaybackPreferences.POINTER_SPEED_NORMAL, prefs.pointerSpeed)
        assertEquals(1.0f, prefs.pointerSpeedMultiplier, 0.01f)

        prefs.pointerSpeed = PlaybackPreferences.POINTER_SPEED_FAST
        assertEquals(PlaybackPreferences.POINTER_SPEED_FAST, prefs.pointerSpeed)
        assertEquals(1.35f, prefs.pointerSpeedMultiplier, 0.01f)

        prefs.pointerSpeed = PlaybackPreferences.POINTER_SPEED_PRECISION
        assertEquals(PlaybackPreferences.POINTER_SPEED_PRECISION, prefs.pointerSpeed)
        assertEquals(0.5f, prefs.pointerSpeedMultiplier, 0.01f)
    }
}
