package com.nowshowing.tv.ui.player

import org.junit.Assert.*
import org.junit.Test

class PointerMotionTest {
    @Test fun tapsMoveLessThanSustainedHolds() {
        assertTrue(PointerMotion.stepDp(0) < PointerMotion.stepDp(8))
        assertTrue(PointerMotion.stepDp(8) < PointerMotion.stepDp(20))
    }
    @Test fun longHoldsStayBounded() {
        assertEquals(PointerMotion.stepDp(20), PointerMotion.stepDp(10000))
    }
    @Test fun stepsNeverDecreaseAsHoldContinues() {
        val steps = (0..100).map { PointerMotion.stepDp(it) }
        assertTrue(steps.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(steps.all { it > 0f && it <= 28f })
    }

    @Test fun speedMultiplierScalesAndClamps() {
        assertEquals(4f, PointerMotion.stepDp(0, 0.5f), 0.01f)
        assertEquals(8f, PointerMotion.stepDp(0, 1.0f), 0.01f)
        assertEquals(10.8f, PointerMotion.stepDp(0, 1.35f), 0.01f)
        // Upper bound clamped at 48f
        assertEquals(48f, PointerMotion.stepDp(20, 2.5f), 0.01f)
    }
}
