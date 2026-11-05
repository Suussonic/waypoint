package com.waypoint.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Public-place fixtures only. Run against the separate Preview installation on an unlocked phone. */
@RunWith(AndroidJUnit4::class)
class QuestUiFlowTest {
    @Test(timeout = 180_000) fun everydayTasksTimersSharingAndWorldPreviews(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val app = context.applicationContext as WaypointApplication
        val navigation = app.navigation
        val repo = app.quests.repository
        val device = UiDevice.getInstance(inst)
        val oldIdle = Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout = 350
        val folder = File(context.getExternalFilesDir(null), "quest-review").apply { mkdirs() }
        val ids = mutableListOf<String>()
        val saved = Place("quest-ui-library", "City Library", "Connaught Place, New Delhi", Coordinate(28.6328, 77.2195))
        val world = navigation.state.value.world
        val previousProfile = repo.dao.profile() ?: QuestProfile()
        val previousFavorites = navigation.favoriteWorlds()
        val font = device.executeShellCommand("settings get system font_scale").trim()
        val rotation = device.executeShellCommand("settings get system user_rotation").trim()
        val auto = device.executeShellCommand("settings get system accelerometer_rotation").trim()
        fun phase(value: String) { inst.sendStatus(2, android.os.Bundle().apply { putString("stream", "\n$value\n") }) }
        fun find(label: String, timeout: Long = 2500) = device.wait(Until.findObject(By.desc(label)), timeout / 2)
            ?: device.wait(Until.findObject(By.text(label)), timeout / 2)
        fun click(label: String, scroll: Boolean = false) {
            assertEquals("Keep Waypoint Preview foreground during UI testing",context.packageName,device.currentPackageName)
            repeat(if (scroll) 7 else 4) {
                find(label)?.let { item -> try {
                    var target: UiObject2? = item
                    while (target != null && !target.isClickable) target = target.parent
                    if (target?.isEnabled == true) { target.click(); return }
                } catch (_: StaleObjectException) { } }
                if (scroll) device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 3, 24)
            }
            if(device.currentPackageName==context.packageName) {
                device.dumpWindowHierarchy(File(folder, "missing-control.xml"))
                device.takeScreenshot(File(folder, "missing-control.png"))
            }
            fail("Missing control: $label")
        }
        fun capture(name: String) {
            SystemClock.sleep(650)
            assertEquals(context.packageName, device.currentPackageName)
            assertTrue(device.takeScreenshot(File(folder, "$name.png")))
        }
        try {
            app.markWhatsNewSeen()
            inst.runOnMainSync {
                navigation.completeOnboarding(listOf(World.DIVISION)); navigation.clearDestination(); navigation.changeWorld(World.DIVISION)
                if (navigation.state.value.saved.none { it.id == saved.id }) navigation.toggleSaved(saved)
            }
            repo.updateProfile { it.copy(sound = false) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                assertTrue(device.wait(Until.hasObject(By.text("Quests")), 8000))
                capture("phone-map-main-tabs")
                click("Quests")
                capture("phone-quests-empty")
                click("Add a quest")
                val title = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 4000)!!
                title.click(); title.text = "Write a postcard"
                device.pressBack() // Close IME; stay in the form.
                capture("phone-add-quest")
                click("Add quest")
                assertTrue(device.wait(Until.hasObject(By.desc("Add a quest")), 6000))
                assertNotNull(find("Write a postcard", 6000))
                val created = repo.dao.observeQuests().first().first { it.title == "Write a postcard" }
                ids += created.id
                click("Write a postcard"); click("Complete quest", true)
                assertNotNull(find("Quest complete", 5000))
                click("Undo")
                SystemClock.sleep(300)
                assertNull(repo.dao.quest(created.id)!!.completedAt)
                click("Write a postcard"); click("Edit")
                click("Choose a place", true); click(saved.name)
                click("Save changes")
                assertTrue(device.wait(Until.hasObject(By.text("Your quest")), 6000))
                assertEquals(saved.point, repo.dao.quest(created.id)!!.place()!!.point)
                click("Navigate", true)
                assertTrue(device.wait(Until.hasObject(By.text("City Library")), 7000))
                assertFalse(navigation.state.value.navigating)
                inst.runOnMainSync { navigation.clearDestination() }
                val focusQuest = repo.save(QuestRecord(title = "Read a chapter", notes = "A quiet twenty-five minutes.", hasFocus = true))
                ids += focusQuest.id
                click("Quests"); click(focusQuest.title)
                click("Start focus", true)
                click("Pause", true)
                phase("Pause requested; persisted focus state: ${repo.dao.focus()?.status}")
                capture("phone-paused-timer")
                device.dumpWindowHierarchy(File(folder, "paused-timer.xml"))
                assertNotNull(find("Resume", 4000))
                assertEquals("PAUSED", repo.dao.focus()!!.status)
                scenario.recreate()
                click("Resume", true)
                assertNotNull(find("Pause", 4000))
                assertEquals("RUNNING", repo.dao.focus()!!.status)
                capture("phone-focus-timer")
                click("Finish early", true)
                click("Back to quests")
                for (i in 1..5) {
                    val earned = repo.save(QuestRecord(title = "A small step $i")); ids += earned.id
                    repo.complete(earned.id); repo.dao.presented(listOf(earned.id))
                }
                click("Your rewards and quest settings")
                capture("phone-rewards")
                click("Back to quests")
                capture("phone-quest-list")
                device.executeShellCommand("settings put system font_scale 1.6")
                SystemClock.sleep(700)
                capture("phone-quests-large-text")
                device.executeShellCommand("settings put system accelerometer_rotation 0")
                device.executeShellCommand("settings put system user_rotation 1")
                SystemClock.sleep(1200)
                capture("phone-quests-landscape")
                device.executeShellCommand("settings put system user_rotation 0")
                device.executeShellCommand("settings put system font_scale $font")
                SystemClock.sleep(1000)
                for (theme in listOf(World.SEA_CHART, World.AINCRAD, World.HOME_CONSOLE, World.PIXEL, World.NIGHT_CITY, World.STALKER, World.SKYRIM, World.DIVISION, World.COUNTER_STRIKE)) {
                    inst.runOnMainSync { navigation.changeWorld(theme) }
                    capture("phone-quests-${theme.file}")
                }
                click("Return to map")
                click("Worlds")
                click("New")
                capture("phone-worlds-new")
                val worldBeforePreview = navigation.state.value.world
                click("Preview DARK ZONE theme")
                assertEquals(worldBeforePreview, navigation.state.value.world)
                capture("phone-world-preview")
                click("Use this world")
                assertEquals(World.DIVISION, navigation.state.value.world)
                for (theme in newWorlds) {
                    inst.runOnMainSync { navigation.changeWorld(theme) }
                    capture("phone-map-${theme.file}")
                }
                phase("All user flows exercised; closing activity")
            }
            phase("Activity closed")
        } catch (failure: Throwable) {
            device.takeScreenshot(File(folder, "failure.png")); device.dumpWindowHierarchy(File(folder, "failure.xml")); throw failure
        } finally {
            phase("Removing test fixtures")
            ids.forEach { repo.dao.revokeReward(it); repo.delete(it) }
            repo.dao.saveProfile(previousProfile)
            inst.runOnMainSync {
                navigation.setFavoriteWorlds(previousFavorites); navigation.changeWorld(world); navigation.clearDestination()
                if (navigation.state.value.saved.any { it.id == saved.id }) navigation.toggleSaved(saved)
            }
            device.executeShellCommand("settings put system font_scale $font")
            device.executeShellCommand("settings put system user_rotation $rotation")
            device.executeShellCommand("settings put system accelerometer_rotation $auto")
            Configurator.getInstance().waitForIdleTimeout = oldIdle
            phase("Device settings and fixtures restored")
        }
    }
    @Test fun sharedPlaceRequiresReviewBeforeNavigation() {
        val inst = InstrumentationRegistry.getInstrumentation(); val context = inst.targetContext
        val app = context.applicationContext as WaypointApplication; val device = UiDevice.getInstance(inst)
        app.markWhatsNewSeen()
        inst.runOnMainSync { app.navigation.completeOnboarding(listOf(World.HOME_CONSOLE)); app.navigation.clearDestination() }
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://www.google.com/maps/search/?api=1&query=28.6328,77.2195")
        ActivityScenario.launch<MainActivity>(intent).use {
            assertTrue(device.wait(Until.hasObject(By.text("Open shared destination?")), 7000))
            assertFalse(app.navigation.state.value.navigating)
            assertNull(app.navigation.state.value.selected)
            val folder = File(context.getExternalFilesDir(null), "quest-review").apply { mkdirs() }
            device.takeScreenshot(File(folder, "phone-incoming-place.png"))
            device.findObject(By.text("Cancel"))!!.click()
            assertNull(app.navigation.state.value.selected)
        }
    }
}
