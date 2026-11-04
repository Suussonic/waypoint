package com.waypoint.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class QuestReminders(private val context: Context, private val repository: QuestRepository) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var scheduled = emptySet<String>()
    init {
        notifications.createNotificationChannels(listOf(
            NotificationChannel("quest_reminders", "Quest reminders", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("focus_alerts", "Focus alerts", NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }
    fun canNotify() = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    fun canAlertOnTime() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    private fun alarmIntent(id: String, revision: String, focus: Boolean): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, QuestAlarmReceiver::class.java).setAction(if (focus) "FOCUS_DUE" else "QUEST_DUE")
            .setData(Uri.parse("waypoint://quest-alarm/${if (focus) "focus" else id}"))
            .putExtra("quest_id", id).putExtra("revision", revision), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun schedule(at: Long, intent: PendingIntent) {
        val due = at.coerceAtLeast(System.currentTimeMillis() + 1000)
        try {
            if (canAlertOnTime()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
        } catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent) }
    }

    @Synchronized fun sync(quests: List<QuestRecord>, focus: FocusRecord?) {
        val active = quests.filter { it.completedAt == null && it.reminderAt != null }
        val next = active.map { it.id }.toSet()
        (scheduled - next).forEach { alarms.cancel(alarmIntent(it, "", false)) }
        active.forEach { schedule(it.reminderAt!!, alarmIntent(it.id, it.revision, false)) }
        scheduled = next
        if (focus?.status == "RUNNING" && focus.alerts) {
            val now = AndroidQuestTime(context).now()
            schedule(now.wall + focus.clock().remaining(now.elapsed, now.wall, now.boot), alarmIntent(focus.id, focus.revision, true))
        } else alarms.cancel(alarmIntent("", "", true))
    }
    suspend fun restore() = sync(repository.dao.reminders(), repository.dao.focus())

    fun cancelQuest(id: String) {
        alarms.cancel(alarmIntent(id, "", false))
        notifications.cancel("quest:$id", 31)
        notifications.cancel("focus:$id", 32)
    }

    private fun action(quest: QuestRecord, action: String): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, QuestAlarmReceiver::class.java).setAction(action)
            .setData(Uri.parse("waypoint://quest-action/${quest.id}/${quest.revision}/$action"))
            .putExtra("quest_id", quest.id).putExtra("revision", quest.revision), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun open(id: String): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).setAction("OPEN_QUEST").setData(Uri.parse("waypoint://quest/$id"))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun notification(quest: QuestRecord, channel: String, text: String): Notification {
        val app = context.applicationContext as WaypointApplication
        val quiet = app.navigation.isDriving
        return NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle(quest.title).setContentText(text).setContentIntent(open(quest.id))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true).setSilent(quiet).setOnlyAlertOnce(true)
            .addAction(0, "Open quest", open(quest.id))
            .addAction(0, "Complete", action(quest, "COMPLETE"))
            .addAction(0, "Snooze 10 min", action(quest, "SNOOZE")).build()
    }
    fun showReminder(quest: QuestRecord) {
        if (!canNotify()) return
        try { notifications.notify("quest:${quest.id}", 31, notification(quest, "quest_reminders", "A little reminder for your quest.")) } catch (_: SecurityException) { }
    }
    suspend fun showFocus(focus: FocusRecord) {
        if (!focus.alerts || !canNotify()) return
        val quest = repository.dao.quest(focus.questId)?.takeIf { it.completedAt == null } ?: return
        try { notifications.notify("focus:${quest.id}", 32, notification(quest, "focus_alerts", "Your focus time is finished. Ready to complete this quest?")) } catch (_: SecurityException) { }
    }
}

/** Not exported: only immutable app-owned PendingIntents and system broadcasts reach this receiver. */
class QuestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8000) {
                val controller = (context.applicationContext as WaypointApplication).quests
                val repository = controller.repository
                val reminders = controller.reminders
                val id = intent.getStringExtra("quest_id") ?: ""
                val revision = intent.getStringExtra("revision") ?: ""
                when (intent.action) {
                    "QUEST_DUE" -> repository.takeReminder(id, revision)?.let(reminders::showReminder)
                    "FOCUS_DUE" -> {
                        val current = repository.dao.focus()
                        if (current?.id == id && current.revision == revision) repository.reconcileFocus()?.let { reminders.showFocus(it) }
                    }
                    "COMPLETE" -> if (repository.complete(id, revision)) reminders.cancelQuest(id)
                    "SNOOZE" -> if (repository.snooze(id, revision)) reminders.cancelQuest(id)
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                        repository.reconcileFocus()?.let { reminders.showFocus(it) }
                    else -> return@withTimeout
                }
                reminders.restore()
            } } catch (_: Exception) {
                // The persisted task remains available for reconciliation on the next foreground entry.
            } finally { pending.finish() }
        }
    }
}
