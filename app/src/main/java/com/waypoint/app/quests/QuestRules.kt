package com.waypoint.app

import kotlin.math.ceil

/** Personal progress is earned by explicit completion, never by distance or speed. */
data class QuestProgress(val xp: Int = 0, val completed: Int = 0, val focused: Int = 0) {
    val level: Int get() = 1 + xp / 100
    val levelProgress: Float get() = (xp % 100) / 100f
    val nextLevelTasks: Int get() = ceil((100 - xp % 100) / 20.0).toInt()
    val badges: List<String> get() = buildList {
        if (completed >= 1) add("First step")
        if (completed >= 10) add("Finding your rhythm")
        if (completed >= 50) add("Seasoned adventurer")
        if (focused >= 5) add("Time well spent")
    }
    val frames: List<String> get() = buildList {
        add("Simple")
        if (level >= 2) add("Scout")
        if (level >= 5) add("Explorer")
        if (level >= 10) add("Wayfinder")
    }
    val chapters: List<JourneyChapter> get() = journeyChapters.filter { completed >= it.quests }
}

data class JourneyChapter(val quests: Int, val title: String, val text: String)
val journeyChapters = listOf(
    JourneyChapter(5, "A place to begin", "Five small promises, kept. The map has not changed, but your place in it has. Every journey begins with something ordinary that you decide to do."),
    JourneyChapter(20, "Making your own way", "The familiar paths now carry a different story: the things you finished, the time you made, the people and places you chose. You are drawing a route of your own."),
    JourneyChapter(50, "The road ahead", "Fifty quests behind you, and no final destination to chase. Take a moment to look back. The next chapter can be as small as a quiet afternoon or as wide as a new horizon.")
)

enum class FocusStatus { RUNNING, PAUSED, NEEDS_CONFIRMATION, FINISHED, CANCELLED }

/** Wall time is only a reboot fallback; clock edits cannot advance a live countdown. */
data class FocusClock(
    val remainingMs: Long,
    val startedElapsedMs: Long,
    val deadlineWallMs: Long,
    val boot: Int,
    val status: FocusStatus
) {
    fun remaining(nowElapsed: Long, nowWall: Long, currentBoot: Int): Long = when {
        status != FocusStatus.RUNNING -> remainingMs.coerceAtLeast(0)
        boot == currentBoot -> (remainingMs - (nowElapsed - startedElapsedMs).coerceAtLeast(0)).coerceAtLeast(0)
        else -> (deadlineWallMs - nowWall).coerceIn(0, remainingMs)
    }
}

internal fun questReward(focusFinished: Boolean, durationMs: Long): Int =
    20 + if (focusFinished && durationMs >= 10 * 60_000L) 10 else 0

internal fun validQuestTitle(value: String): String? = value.trim()
    .takeIf { it.isNotBlank() && it.length <= 160 && it.none(Char::isISOControl) }
