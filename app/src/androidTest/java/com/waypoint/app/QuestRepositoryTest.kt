package com.waypoint.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestRepositoryTest {
    private lateinit var database: QuestDatabase
    private lateinit var repository: QuestRepository
    private var now = QuestTime(1000, 1_800_000_000_000, 3)
    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QuestDatabase::class.java).build()
        repository = QuestRepository(database, QuestTimeSource { now })
    }
    @After fun close() { database.close() }
    @Test fun concurrentCompletionsOnlyAwardOnceAndUndoReversesIt() = runBlocking {
        val quest = repository.save(QuestRecord(title = "Test quest"))
        val outcomes = (1..8).map { async(Dispatchers.Default) { repository.complete(quest.id) } }.awaitAll()
        assertEquals(1, outcomes.count { it })
        assertEquals(20, repository.dao.observeRewards().first().single().xp)
        assertTrue(repository.reopen(quest.id))
        assertTrue(repository.dao.observeRewards().first().isEmpty())
        assertTrue(repository.complete(quest.id))
        assertEquals(1, repository.dao.observeRewards().first().size)
    }
    @Test fun staleNotificationCannotCompleteAnEditedOrReopenedQuest() = runBlocking {
        val original = repository.save(QuestRecord(title = "First title"))
        val edited = repository.save(original.copy(title = "Changed title"))
        assertFalse(repository.complete(original.id, original.revision))
        assertTrue(repository.complete(edited.id, edited.revision))
        repository.reopen(edited.id)
        assertFalse(repository.complete(edited.id, edited.revision))
    }
    @Test fun focusSurvivesClockChangesAndOnlyAwardsOnExplicitCompletion() = runBlocking {
        val quest = repository.save(QuestRecord(title = "Read", hasFocus = true, focusMinutes = 10))
        repository.startFocus(quest.id, false)
        now = now.copy(wall = now.wall + 10_000_000)
        assertNull(repository.reconcileFocus())
        now = now.copy(elapsed = now.elapsed + 600_000)
        assertEquals("FINISHED", repository.reconcileFocus()?.status)
        assertTrue(repository.dao.observeRewards().first().isEmpty())
        repository.complete(quest.id)
        assertEquals(30, repository.dao.observeRewards().first().single().xp)
    }
    @Test fun onlyOneTimerCanRunAndFinishingEarlyDoesNotEarnBonus() = runBlocking {
        val one = repository.save(QuestRecord(title = "One", focusMinutes = 10))
        val two = repository.save(QuestRecord(title = "Two"))
        val timer = repository.startFocus(one.id, false)
        try { repository.startFocus(two.id, false); fail("Second timer should be refused") } catch (_: IllegalStateException) { }
        repository.changeFocus(timer.id, "cancel")
        repository.complete(one.id)
        assertEquals(20, repository.dao.observeRewards().first().single().xp)
        repository.startFocus(two.id, false)
        Unit
    }
    @Test fun deletionRemovesPrivateDetailsButKeepsAnonymousCredit() = runBlocking {
        val quest = repository.save(QuestRecord(title = "Private title", notes = "Private notes"))
        repository.complete(quest.id)
        repository.delete(quest.id)
        assertNull(repository.dao.quest(quest.id))
        assertEquals(20, repository.dao.observeRewards().first().single().xp)
    }
    @Test fun rebootExpiryRequiresConfirmationBeforeFocusCredit() = runBlocking {
        val quest = repository.save(QuestRecord(title = "Read after reboot", hasFocus = true, focusMinutes = 10))
        val focus = repository.startFocus(quest.id, false)
        now = now.copy(boot = now.boot + 1, elapsed = 1000, wall = now.wall + 700_000)
        assertEquals("NEEDS_CONFIRMATION", repository.reconcileFocus()?.status)
        assertFalse(repository.dao.quest(quest.id)!!.focusQualified)
        assertTrue(repository.changeFocus(focus.id, "confirm"))
        assertFalse(repository.changeFocus(focus.id, "confirm"))
        repository.complete(quest.id)
        assertEquals(30, repository.dao.observeRewards().first().single().xp)
    }
    @Test fun reminderIsConsumedOnceAndSnoozeInvalidatesItsActions() = runBlocking {
        val quest = repository.save(QuestRecord(title = "Reminder", reminderAt = now.wall))
        assertNotNull(repository.takeReminder(quest.id, quest.revision))
        assertNull(repository.takeReminder(quest.id, quest.revision))
        assertTrue(repository.snooze(quest.id, quest.revision))
        assertFalse(repository.complete(quest.id, quest.revision))
        assertEquals(now.wall + 600_000, repository.dao.quest(quest.id)?.reminderAt)
    }

    @Test fun databaseReopenPreservesQuestPausedTimerAndRewards(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "quest-persistence-test-${java.util.UUID.randomUUID()}.db"
        var disk = Room.databaseBuilder(context, QuestDatabase::class.java, name).build()
        try {
            val first = QuestRepository(disk, QuestTimeSource { now })
            val quest = first.save(QuestRecord(title = "Persistent quest", notes = "Private note", focusMinutes = 25))
            val timer = first.startFocus(quest.id, false)
            now = now.copy(elapsed = now.elapsed + 10_000)
            first.changeFocus(timer.id, "pause")
            val completed = first.save(QuestRecord(title = "Completed quest"))
            first.complete(completed.id)
            disk.close()
            disk = Room.databaseBuilder(context, QuestDatabase::class.java, name).build()
            val restored = QuestRepository(disk, QuestTimeSource { now })
            assertEquals("Private note", restored.dao.quest(quest.id)?.notes)
            assertEquals("PAUSED", restored.dao.focus()?.status)
            assertEquals(1_490_000L, restored.dao.focus()?.remainingMs)
            assertEquals(20, restored.dao.observeRewards().first().single().xp)
        } finally {
            disk.close()
            context.deleteDatabase(name)
        }
    }
}
