package com.waypoint.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises adaptive layout on one device with temporary display overrides; not tablet hardware coverage. */
@RunWith(AndroidJUnit4::class)
class QuestAdaptiveTest {
    @Test fun smallPhoneAndTabletLayoutsWithLargeText(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation(); val context = inst.targetContext
        val device = UiDevice.getInstance(inst); val app = context.applicationContext as WaypointApplication
        val repo = app.quests.repository
        val folder = File(context.getExternalFilesDir(null), "quest-review").apply { mkdirs() }
        val size = device.executeShellCommand("wm size").lineSequence().find { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = device.executeShellCommand("wm density").lineSequence().find { it.startsWith("Override density:") }?.substringAfter(": ")
        val settings = listOf("system font_scale", "system user_rotation", "system accelerometer_rotation", "global animator_duration_scale", "global window_animation_scale", "global transition_animation_scale")
            .associateWith { device.executeShellCommand("settings get $it").trim() }
        val originalWorld = app.navigation.state.value.world
        val oldIdle = Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout = 350
        val quest = repo.save(QuestRecord(title = "Pick up groceries", notes = "Vegetables, bread and a little treat.", hasFocus = true, focusMinutes = 25))
        fun click(label: String) {
            val node = device.wait(Until.findObject(By.desc(label)), 2000) ?: device.wait(Until.findObject(By.text(label)), 2000)
            assertNotNull(label, node); node!!.click()
        }
        fun capture(name: String) { SystemClock.sleep(1000); assertEquals(context.packageName, device.currentPackageName); assertTrue(device.takeScreenshot(File(folder, "$name.png"))) }
        fun intent() = Intent(context, MainActivity::class.java).setAction("OPEN_QUEST").setData(android.net.Uri.parse("waypoint://quest/${quest.id}"))
        try {
            inst.runOnMainSync { app.navigation.completeOnboarding(listOf(World.HOME_CONSOLE)); app.navigation.changeWorld(World.HOME_CONSOLE) }; app.markWhatsNewSeen()
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.executeShellCommand("settings put system user_rotation 0")
            device.executeShellCommand("settings put global animator_duration_scale 0")
            device.executeShellCommand("settings put global window_animation_scale 0")
            device.executeShellCommand("settings put global transition_animation_scale 0")
            device.executeShellCommand("wm size 1080x1920")
            device.executeShellCommand("wm density 480") // 360dp wide small phone
            device.executeShellCommand("settings put system font_scale 2.0")
            ActivityScenario.launch<MainActivity>(intent()).use {
                assertTrue(device.wait(Until.hasObject(By.text("Your quest")), 7000))
                capture("small-phone-quest-large-text")
                click("Edit"); capture("small-phone-editor-large-text")
                click("Back to quests"); click("Back to quests")
                capture("small-phone-list-large-text")
            }
            device.executeShellCommand("wm size 1600x2560")
            device.executeShellCommand("wm density 320") // 800dp tablet layout on phone hardware
            device.executeShellCommand("settings put system font_scale 1.0")
            ActivityScenario.launch<MainActivity>(intent()).use {
                assertTrue(device.wait(Until.hasObject(By.text("Your quest")), 7000))
                capture("tablet-layout-quest")
                click("Edit"); capture("tablet-layout-editor")
                click("Back to quests"); click("Back to quests")
                capture("tablet-layout-list")
                device.executeShellCommand("settings put system user_rotation 1")
                SystemClock.sleep(1200); capture("tablet-layout-landscape")
            }
        } finally {
            repo.delete(quest.id)
            device.executeShellCommand("wm size ${size ?: "reset"}")
            device.executeShellCommand("wm density ${density ?: "reset"}")
            settings.forEach { (key, value) -> device.executeShellCommand(if (value == "null") "settings delete $key" else "settings put $key $value") }
            inst.runOnMainSync { app.navigation.changeWorld(originalWorld) }
            Configurator.getInstance().waitForIdleTimeout = oldIdle
        }
    }
}
