package com.waypoint.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run with -e questRecovery prepare, stop the Preview process, then rerun with -e questRecovery verify. */
@RunWith(AndroidJUnit4::class)
class QuestProcessRecoveryTest {
    @Test fun processRecovery(): Unit = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("questRecovery")
        assumeTrue(phase in listOf("prepare", "verify"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".preview"))
        val app = context.applicationContext as WaypointApplication
        val repo = app.quests.repository
        val fixture = context.getSharedPreferences("quest-recovery-test", 0)
        if (phase == "prepare") {
            check(repo.dao.focus()?.status !in listOf("RUNNING", "PAUSED"))
            check(!fixture.contains("quest")) { "Clean up the previous recovery fixture first." }
            val quest = repo.save(QuestRecord(title = "Process recovery test", notes = "Keep this note", hasFocus = true))
            val focus = repo.startFocus(quest.id, false)
            val earned = repo.save(QuestRecord(title = "Recovery reward test"))
            repo.complete(earned.id)
            repo.dao.presented(listOf(earned.id))
            assertTrue(fixture.edit().putString("quest", quest.id).putString("earned", earned.id)
                .putString("focus", focus.id).putLong("started", focus.startedElapsedMs).commit())
        } else {
            val id = requireNotNull(fixture.getString("quest", null))
            val earned = requireNotNull(fixture.getString("earned", null))
            try {
                val focus = requireNotNull(repo.dao.focus())
                val now = AndroidQuestTime(context).now()
                assertEquals("Keep this note", repo.dao.quest(id)?.notes)
                assertEquals(fixture.getString("focus", null), focus.id)
                assertEquals("RUNNING", focus.status)
                assertEquals(fixture.getLong("started", -1), focus.startedElapsedMs)
                assertTrue("Timer must continue instead of resetting", focus.clock().remaining(now.elapsed, now.wall, now.boot) < focus.durationMs - 2000)
                assertEquals(20, repo.dao.observeRewards().first().single { it.questId == earned }.xp)
            } finally {
                repo.delete(id)
                repo.reopen(earned)
                repo.delete(earned)
                fixture.edit().clear().commit()
                app.quests.reminders.restore()
            }
        }
    }
}
