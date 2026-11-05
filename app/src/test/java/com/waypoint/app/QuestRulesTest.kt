package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class QuestRulesTest {
    @Test fun countdownIgnoresWallClockChangesDuringTheSameBoot() {
        val clock = FocusClock(600_000, 20_000, 1_000_000, 7, FocusStatus.RUNNING)
        assertEquals(590_000, clock.remaining(30_000, 80_000_000, 7))
        assertEquals(590_000, clock.remaining(30_000, 0, 7))
    }
    @Test fun rebootFallbackCannotExtendTheOriginalRemainingTime() {
        val clock = FocusClock(600_000, 20_000, 1_000_000, 7, FocusStatus.RUNNING)
        assertEquals(600_000, clock.remaining(100, 1, 8))
        assertEquals(300_000, clock.remaining(100, 700_000, 8))
        assertEquals(0, clock.remaining(100, 2_000_000, 8))
    }
    @Test fun pausedTimeDoesNotTick() {
        val clock = FocusClock(123_000, 20_000, 1_000_000, 7, FocusStatus.PAUSED)
        assertEquals(123_000, clock.remaining(2_000_000, 2_000_000, 8))
    }
    @Test fun focusBonusRequiresTenMinutesAndCompletion() {
        assertEquals(20, questReward(false, 1_500_000))
        assertEquals(20, questReward(true, 599_999))
        assertEquals(30, questReward(true, 600_000))
    }
    @Test fun levelsCosmeticsAndStoryHaveIndependentThresholds() {
        assertEquals(1, QuestProgress(99).level)
        assertEquals(2, QuestProgress(100).level)
        assertEquals(1, QuestProgress(90).nextLevelTasks)
        assertEquals(listOf("Simple", "Scout", "Explorer"), QuestProgress(400).frames)
        assertEquals(2, QuestProgress(completed = 20).chapters.size)
        assertTrue("Time well spent" in QuestProgress(focused = 5).badges)
        assertEquals(emptyList<String>(), QuestProgress().badges)
    }
    @Test fun titlesAreUsefulBoundedText() {
        assertEquals("Buy milk", validQuestTitle(" Buy milk "))
        assertNull(validQuestTitle("\n\t"))
        assertNull(validQuestTitle("x".repeat(161)))
        assertNull(validQuestTitle("task\u0000title"))
    }
}
