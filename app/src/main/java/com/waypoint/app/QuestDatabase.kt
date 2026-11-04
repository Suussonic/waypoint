package com.waypoint.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "quests")
data class QuestRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val placeName: String? = null,
    val placeAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val reminderAt: Long? = null,
    val focusMinutes: Int = 25,
    val hasFocus: Boolean = false,
    val focusQualified: Boolean = false,
    val characterName: String = "",
    val portrait: String = "scout",
    val imageName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val revision: String = UUID.randomUUID().toString()
) {
    fun place(): Place? {
        val point = Coordinate(latitude ?: return null, longitude ?: return null)
        if (!point.isValid()) return null
        return Place("quest:$id", placeName ?: "Quest destination", placeAddress.orEmpty(), point)
    }
}

@Entity(tableName = "quest_rewards")
data class QuestReward(
    @PrimaryKey val questId: String,
    val xp: Int,
    val focused: Boolean,
    val presented: Boolean = false
)

@Entity(tableName = "focus_session")
data class FocusRecord(
    @PrimaryKey val slot: Int = 1,
    val id: String = UUID.randomUUID().toString(),
    val questId: String,
    val durationMs: Long,
    val remainingMs: Long,
    val startedElapsedMs: Long,
    val deadlineWallMs: Long,
    val boot: Int,
    val status: String = FocusStatus.RUNNING.name,
    val alerts: Boolean = false,
    val revision: String = UUID.randomUUID().toString()
) {
    fun clock() = FocusClock(remainingMs, startedElapsedMs, deadlineWallMs, boot, FocusStatus.valueOf(status))
}

@Entity(tableName = "quest_profile")
data class QuestProfile(@PrimaryKey val id: Int = 1, val showProgress: Boolean = true, val frame: String = "Simple", val sound: Boolean = true)

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY completedAt IS NOT NULL, createdAt DESC") fun observeQuests(): Flow<List<QuestRecord>>
    @Query("SELECT * FROM quest_rewards") fun observeRewards(): Flow<List<QuestReward>>
    @Query("SELECT * FROM focus_session WHERE slot = 1") fun observeFocus(): Flow<FocusRecord?>
    @Query("SELECT * FROM quest_profile WHERE id = 1") fun observeProfile(): Flow<QuestProfile?>
    @Query("SELECT * FROM quests WHERE id = :id") suspend fun quest(id: String): QuestRecord?
    @Query("SELECT imageName FROM quests WHERE imageName IS NOT NULL") suspend fun imageNames(): List<String>
    @Query("SELECT * FROM quests WHERE completedAt IS NULL AND reminderAt IS NOT NULL") suspend fun reminders(): List<QuestRecord>
    @Query("SELECT * FROM focus_session WHERE slot = 1") suspend fun focus(): FocusRecord?
    @Query("SELECT * FROM quest_profile WHERE id = 1") suspend fun profile(): QuestProfile?
    @Upsert suspend fun save(quest: QuestRecord)
    @Upsert suspend fun saveFocus(focus: FocusRecord)
    @Upsert suspend fun saveProfile(profile: QuestProfile)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun reward(reward: QuestReward): Long
    @Query("DELETE FROM quest_rewards WHERE questId = :id") suspend fun revokeReward(id: String)
    @Query("DELETE FROM quests WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM focus_session WHERE questId = :id") suspend fun deleteFocus(id: String)
    @Query("UPDATE quest_rewards SET presented = 1 WHERE questId IN (:ids)") suspend fun presented(ids: List<String>)
}

@Database(entities = [QuestRecord::class, QuestReward::class, FocusRecord::class, QuestProfile::class], version = 1, exportSchema = true)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun quests(): QuestDao
    companion object {
        fun open(context: Context): QuestDatabase = Room.databaseBuilder(context.applicationContext, QuestDatabase::class.java, "quests.db").build()
    }
}
