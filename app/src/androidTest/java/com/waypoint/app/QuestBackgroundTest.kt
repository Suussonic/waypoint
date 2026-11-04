package com.waypoint.app

import android.app.NotificationManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses only the isolated Preview package. A short synthetic countdown exercises real Android alarms. */
@RunWith(AndroidJUnit4::class)
class QuestBackgroundTest {
    @Test(timeout = 100_000) fun lockedScreenFocusAlertAndNotificationActions(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        check(context.packageName.endsWith(".preview")) { "Run on the isolated Preview installation." }
        val app = context.applicationContext as WaypointApplication
        val repo = app.quests.repository
        val alerts = app.quests.reminders
        val device = UiDevice.getInstance(inst)
        val manager = context.getSystemService(NotificationManager::class.java)
        val power = context.getSystemService(android.os.PowerManager::class.java)
        assertTrue("Grant notification permission to the Preview test app", alerts.canNotify())
        assertTrue("Allow exact alarms for this short device check", alerts.canAlertOnTime())
        val powerMode = device.executeShellCommand("settings get global low_power").trim()
        val previousFocus = repo.dao.focus()
        check(previousFocus?.status !in listOf("RUNNING", "PAUSED")) { "Finish the Preview timer first." }
        val quest = repo.save(QuestRecord(title = "Background alarm test", hasFocus = true, focusMinutes = 10))
        try {
            device.pressHome()
            // A USB-connected phone suppresses battery saver; simulate unplugged power only for this check.
            device.executeShellCommand("cmd battery unplug")
            device.executeShellCommand("cmd power set-mode 1")
            delay(500)
            assertTrue("Battery saver must be active for this check", power.isPowerSaveMode)
            val now = AndroidQuestTime(context).now()
            repo.dao.saveFocus(FocusRecord(questId = quest.id, durationMs = 600_000, remainingMs = 12_000,
                startedElapsedMs = now.elapsed, deadlineWallMs = now.wall + 12_000, boot = now.boot, alerts = true))
            alerts.restore()
            device.sleep()
            val deadline = SystemClock.elapsedRealtime() + 60_000
            while (SystemClock.elapsedRealtime() < deadline &&
                manager.activeNotifications.none { it.tag == "focus:${quest.id}" }) delay(250)
            val notification = manager.activeNotifications.singleOrNull { it.tag == "focus:${quest.id}" }
            assertNotNull("Focus alert did not arrive with the screen off", notification)
            assertEquals("FINISHED", repo.dao.focus()?.status)
            assertNull("A timer must not complete the quest", repo.dao.quest(quest.id)?.completedAt)
            assertTrue(repo.dao.observeRewards().first().none { it.questId == quest.id })
            val complete = notification!!.notification.actions.single { it.title.toString() == "Complete" }.actionIntent
            complete.send(); complete.send()
            val actionDeadline = SystemClock.elapsedRealtime() + 5000
            while (SystemClock.elapsedRealtime() < actionDeadline && repo.dao.quest(quest.id)?.completedAt == null) delay(100)
            assertNotNull(repo.dao.quest(quest.id)?.completedAt)
            assertEquals(30, repo.dao.observeRewards().first().single { it.questId == quest.id }.xp)
            assertTrue(repo.reopen(quest.id))
            complete.send()
            delay(500)
            assertNull("Old notification action must not complete a reopened quest", repo.dao.quest(quest.id)?.completedAt)
        } finally {
            alerts.cancelQuest(quest.id)
            repo.reopen(quest.id)
            repo.delete(quest.id)
            previousFocus?.let { repo.dao.saveFocus(it) }
            alerts.restore()
            device.executeShellCommand("cmd power set-mode ${if (powerMode == "1") "1" else "0"}")
            device.executeShellCommand("cmd battery reset")
            device.wakeUp()
        }
    }
}
