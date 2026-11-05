package com.waypoint.app

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestPermissionFallbackTest {
    @Test @SdkSuppress(minSdkVersion = 33)
    fun deniedAlertsLeaveTimerUsable(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val app = context.applicationContext as WaypointApplication
        val repo = app.quests.repository
        val device = UiDevice.getInstance(inst)
        assertFalse("Run after denying Preview notifications", app.quests.reminders.canNotify())
        assertFalse("Run with Preview exact alarms denied", app.quests.reminders.canAlertOnTime())
        val quest = repo.save(QuestRecord(title = "Focus without alerts", hasFocus = true))
        val oldIdle = Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout = 350
        fun click(label: String) {
            repeat(5) {
                val node = device.findObject(By.desc(label)) ?: device.findObject(By.text(label))
                if (node != null) {
                    var control: UiObject2? = node
                    while (control != null && !control.isClickable) control = control.parent
                    if (control?.isEnabled == true) { control.click(); return }
                }
                device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                    device.displayWidth / 2, device.displayHeight / 3, 20)
            }
            fail("Missing control: $label")
        }
        try {
            inst.runOnMainSync { app.navigation.completeOnboarding(listOf(World.DIVISION)) }
            app.markWhatsNewSeen()
            val intent = Intent(context, MainActivity::class.java).setAction("OPEN_QUEST")
                .setData(Uri.parse("waypoint://quest/${quest.id}"))
            ActivityScenario.launch<MainActivity>(intent).use {
                assertTrue(device.wait(Until.hasObject(By.text("Your quest")), 7000))
                click("Alert me when time is up")
                val explanation = By.text("Allow notifications to receive your reminders. Your quests work without them.")
                repeat(5) {
                    if (!device.hasObject(explanation)) {
                        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                            device.displayWidth / 2, device.displayHeight / 3, 20)
                    }
                }
                assertTrue("Denied-alert guidance must be readable", device.hasObject(explanation))
                click("Start focus")
                val deadline = SystemClock.elapsedRealtime() + 5000
                while (repo.dao.focus()?.questId != quest.id && SystemClock.elapsedRealtime() < deadline) delay(100)
                assertEquals("RUNNING", repo.dao.focus()?.status)
                delay(650) // Let the setup panel collapse before using the newly positioned timer controls.
                click("Pause")
                val paused = device.wait(Until.hasObject(By.text("Resume")), 3000)
                if (!paused) {
                    val folder = java.io.File(context.getExternalFilesDir(null), "quest-review").apply { mkdirs() }
                    device.dumpWindowHierarchy(java.io.File(folder, "denied-alert-pause.xml"))
                    device.takeScreenshot(java.io.File(folder, "denied-alert-pause.png"))
                    inst.sendStatus(2, android.os.Bundle().apply { putString("stream", "Focus after Pause: ${repo.dao.focus()?.status}\n") })
                }
                assertTrue("Pause must expose Resume", paused)
                click("Resume")
                assertTrue(device.wait(Until.hasObject(By.text("Pause")), 3000))
                click("Finish early")
                assertNull(repo.dao.quest(quest.id)?.completedAt)
            }
        } finally {
            repo.delete(quest.id)
            app.quests.reminders.restore()
            Configurator.getInstance().waitForIdleTimeout = oldIdle
        }
    }

    @Test fun connectedCarKeepsQuestEditingOutOfPhoneFlow() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val app = context.applicationContext as WaypointApplication
        val device = UiDevice.getInstance(inst)
        val oldIdle = Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout = 350
        inst.runOnMainSync { app.navigation.completeOnboarding(listOf(World.DIVISION)); app.navigation.clearDestination() }
        app.markWhatsNewSeen()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { app.navigation.setCarVisible(true) }
                device.waitForIdle(1000)
                val quests = device.wait(Until.findObject(By.text("Quests")), 5000)
                assertNotNull(quests)
                var control: UiObject2? = quests
                while (control != null && !control.isClickable) control = control.parent
                assertNotNull("Quests navigation action", control)
                control!!.click()
                val blocked = device.wait(Until.hasObject(By.text("Quests are available after Android Auto disconnects.")), 5000)
                if (!blocked) {
                    val folder = java.io.File(context.getExternalFilesDir(null), "quest-review").apply { mkdirs() }
                    device.dumpWindowHierarchy(java.io.File(folder, "car-quest-guard.xml"))
                    device.takeScreenshot(java.io.File(folder, "car-quest-guard.png"))
                    inst.sendStatus(2, android.os.Bundle().apply { putString("stream", "Car connected: ${app.navigation.state.value.carConnected}; driving: ${app.navigation.isDriving}\n") })
                }
                assertTrue("The connected-car explanation must be shown", blocked)
                assertFalse(device.hasObject(By.desc("Add a quest")))
            } finally {
                scenario.onActivity { app.navigation.setCarVisible(false) }
                Configurator.getInstance().waitForIdleTimeout = oldIdle
            }
        }
    }
}
