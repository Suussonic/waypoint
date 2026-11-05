package com.waypoint.app

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import java.util.UUID

data class QuestTime(val elapsed: Long, val wall: Long, val boot: Int)
fun interface QuestTimeSource { fun now(): QuestTime }
class AndroidQuestTime(private val context: Context) : QuestTimeSource {
    override fun now() = QuestTime(SystemClock.elapsedRealtime(), System.currentTimeMillis(), Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0))
}

/** All state-changing decisions live in transactions, including notification actions. */
class QuestRepository(val database: QuestDatabase, private val time: QuestTimeSource) {
    val dao = database.quests()

    suspend fun save(draft: QuestRecord): QuestRecord = database.withTransaction {
        val title = validQuestTitle(draft.title) ?: error("Give your quest a title of up to 160 characters.")
        require(draft.notes.length <= 4000) { "Keep notes under 4,000 characters." }
        require(draft.characterName.length <= 60) { "Keep the character name under 60 characters." }
        require(draft.focusMinutes in 1..180) { "Choose a timer from 1 to 180 minutes." }
        require(draft.latitude == null && draft.longitude == null || draft.place() != null) { "Choose a valid place for this quest." }
        require(draft.imageName == null || QuestImages.isImageName(draft.imageName)) { "Choose the photo again." }
        val existing = dao.quest(draft.id)
        val saved = draft.copy(title = title, notes = draft.notes.trim(), characterName = draft.characterName.trim(),
            completedAt = existing?.completedAt, focusQualified = existing?.focusQualified ?: false,
            createdAt = existing?.createdAt ?: time.now().wall, revision = UUID.randomUUID().toString())
        dao.save(saved)
        saved
    }

    suspend fun complete(id: String, revision: String? = null): Boolean = database.withTransaction {
        val quest = dao.quest(id) ?: return@withTransaction false
        if (quest.completedAt != null || revision != null && revision != quest.revision) return@withTransaction false
        reconcileFocusInTransaction()
        val current = dao.quest(id) ?: return@withTransaction false
        dao.reward(QuestReward(id, questReward(current.focusQualified, if (current.focusQualified) 600_000 else 0), current.focusQualified))
        dao.save(current.copy(completedAt = time.now().wall, reminderAt = null, revision = UUID.randomUUID().toString()))
        dao.deleteFocus(id)
        true
    }

    suspend fun reopen(id: String): Boolean = database.withTransaction {
        val quest = dao.quest(id) ?: return@withTransaction false
        if (quest.completedAt == null) return@withTransaction false
        dao.revokeReward(id)
        dao.save(quest.copy(completedAt = null, revision = UUID.randomUUID().toString()))
        true
    }

    suspend fun delete(id: String): QuestRecord? = database.withTransaction {
        val quest = dao.quest(id) ?: return@withTransaction null
        dao.deleteFocus(id)
        dao.delete(id)
        // Rewards contain a random ID and counts only, so deleting private task text preserves earned progress.
        quest
    }

    suspend fun startFocus(id: String, alerts: Boolean): FocusRecord = database.withTransaction {
        val quest = dao.quest(id) ?: error("This quest is no longer available.")
        check(quest.completedAt == null) { "Reopen this quest to start a timer." }
        reconcileFocusInTransaction()
        val previous = dao.focus()
        check(previous == null || previous.status !in listOf("RUNNING", "PAUSED")) { "Finish your current focus session before starting another." }
        val now = time.now()
        val duration = quest.focusMinutes * 60_000L
        FocusRecord(questId = id, durationMs = duration, remainingMs = duration, startedElapsedMs = now.elapsed,
            deadlineWallMs = now.wall + duration, boot = now.boot, alerts = alerts).also { dao.saveFocus(it) }
    }

    suspend fun changeFocus(id: String, action: String): Boolean = database.withTransaction {
        reconcileFocusInTransaction()
        val current = dao.focus()?.takeIf { it.id == id } ?: return@withTransaction false
        val now = time.now()
        val remaining = current.clock().remaining(now.elapsed, now.wall, now.boot)
        val next = when (action) {
            "confirm" -> if (current.status == "NEEDS_CONFIRMATION") {
                if (current.durationMs >= 600_000L) dao.quest(current.questId)?.let { dao.save(it.copy(focusQualified = true)) }
                current.copy(status = "FINISHED")
            } else return@withTransaction false
            "pause" -> if (current.status == "RUNNING") current.copy(status = "PAUSED", remainingMs = remaining) else return@withTransaction false
            "resume" -> if (current.status == "PAUSED") current.copy(status = "RUNNING", startedElapsedMs = now.elapsed, deadlineWallMs = now.wall + remaining, boot = now.boot) else return@withTransaction false
            "cancel" -> if (current.status in listOf("RUNNING", "PAUSED")) current.copy(status = "CANCELLED", remainingMs = remaining) else return@withTransaction false
            else -> return@withTransaction false
        }
        dao.saveFocus(next.copy(revision = UUID.randomUUID().toString()))
        true
    }

    suspend fun reconcileFocus(): FocusRecord? = database.withTransaction { reconcileFocusInTransaction() }
    private suspend fun reconcileFocusInTransaction(): FocusRecord? {
        val current = dao.focus()?.takeIf { it.status == "RUNNING" } ?: return null
        val now = time.now()
        val remaining = current.clock().remaining(now.elapsed, now.wall, now.boot)
        if (remaining == 0L) {
            val needsConfirmation = current.boot != now.boot
            val finished = current.copy(status = if (needsConfirmation) "NEEDS_CONFIRMATION" else "FINISHED", remainingMs = 0)
            dao.saveFocus(finished)
            if (!needsConfirmation && current.durationMs >= 600_000L) dao.quest(current.questId)?.let { dao.save(it.copy(focusQualified = true)) }
            return finished
        }
        if (current.boot != now.boot) {
            // After a reboot the elapsed clock changed. Resume from the bounded wall-clock remainder.
            dao.saveFocus(current.copy(remainingMs = remaining, startedElapsedMs = now.elapsed, deadlineWallMs = now.wall + remaining, boot = now.boot))
        }
        return null
    }

    suspend fun takeReminder(id: String, revision: String): QuestRecord? = database.withTransaction {
        val quest = dao.quest(id) ?: return@withTransaction null
        if (quest.revision != revision || quest.completedAt != null || quest.reminderAt == null || quest.reminderAt > time.now().wall + 1000) return@withTransaction null
        dao.save(quest.copy(reminderAt = null))
        quest
    }

    suspend fun snooze(id: String, revision: String): Boolean = database.withTransaction {
        val quest = dao.quest(id) ?: return@withTransaction false
        if (quest.revision != revision || quest.completedAt != null) return@withTransaction false
        dao.save(quest.copy(reminderAt = time.now().wall + 10 * 60_000L, revision = UUID.randomUUID().toString()))
        true
    }

    suspend fun updateProfile(change: (QuestProfile) -> QuestProfile) = database.withTransaction {
        dao.saveProfile(change(dao.profile() ?: QuestProfile()))
    }
}
