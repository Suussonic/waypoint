package com.waypoint.app

import android.content.res.Configuration
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OnboardingIssuesTest {
    @Test fun multipleFavoritesSurviveBackRecreationAndCompletion() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val device = UiDevice.getInstance(inst)
        val prefs = context.getSharedPreferences("waypoint", 0)
        val original = prefs.all
        val controller = (context.applicationContext as WaypointApplication).navigation
        val world = controller.state.value.world
        val config = Configurator.getInstance()
        val idle = config.waitForIdleTimeout
        config.waitForIdleTimeout = 350
        val font = device.executeShellCommand("settings get system font_scale").trim()
        val output = File(context.getExternalFilesDir(null), "issue-review").apply { mkdirs() }
        fun continuePage() {
            val button = device.wait(Until.findObject(By.desc("Continue")), 5000)
            assertNotNull(button); assertTrue(button!!.isEnabled); button.click()
            SystemClock.sleep(350)
        }
        try {
            prefs.edit().putBoolean("onboardingCompleted", false).putBoolean("mapOpened", false)
                .putInt("welcomeShownVersion", 0).remove("favoriteWorlds").commit()
            inst.runOnMainSync { controller.clearDestination() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                repeat(3) { continuePage() }
                assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.favorite_games_question))), 5000))
                device.findObject(By.text("Red Dead Redemption 2")).click()
                device.findObject(By.text("Minecraft")).click()
                assertTrue(device.wait(Until.hasObject(By.text("2 selected")), 3000))
                assertEquals(2, selectedAccessibleGames())
                device.findObject(By.text("Red Dead Redemption 2")).click()
                assertTrue(device.wait(Until.hasObject(By.text("1 selected")), 3000))
                device.findObject(By.text("Red Dead Redemption 2")).click()
                device.pressBack()
                assertTrue(device.wait(Until.hasObject(By.text("your phone")), 5000))
                continuePage()
                assertTrue(device.wait(Until.hasObject(By.text("2 selected")), 3000))
                scenario.recreate()
                assertTrue(device.wait(Until.hasObject(By.text("2 selected")), 5000))
                assertEquals(2, selectedAccessibleGames())
                device.takeScreenshot(File(output, "favorites-multiple.png"))
                device.executeShellCommand("settings put system font_scale 1.3")
                SystemClock.sleep(1000)
                assertTrue(device.hasObject(By.desc("Continue")))
                device.takeScreenshot(File(output, "favorites-large-text.png"))
                device.executeShellCommand("settings put system font_scale $font")
                SystemClock.sleep(700)
                continuePage(); continuePage()
                val finish = device.wait(Until.findObject(By.text("Continue without location")), 3000)
                    ?: device.wait(Until.findObject(By.text("Open map")), 3000)
                assertNotNull(finish); finish!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("Worlds")), 5000))
                assertEquals(listOf(World.PIXEL, World.OLD_WEST), controller.favoriteWorlds())
                assertEquals(World.PIXEL, controller.state.value.world)
            }
        } catch (failure: Throwable) {
            device.takeScreenshot(File(output, "failure.png"))
            device.dumpWindowHierarchy(File(output, "failure.xml"))
            throw failure
        } finally {
            inst.runOnMainSync { controller.changeWorld(world) }
            val edit = prefs.edit()
            for (key in listOf("onboardingCompleted", "mapOpened", "welcomeShownVersion", "favoriteWorlds", "world", "tilted")) {
                when (val value = original[key]) {
                    null -> edit.remove(key)
                    is Boolean -> edit.putBoolean(key, value)
                    is Int -> edit.putInt(key, value)
                    is String -> edit.putString(key, value)
                }
            }
            edit.commit()
            device.executeShellCommand("settings put system font_scale $font")
            config.waitForIdleTimeout = idle
        }
    }

    // Assert the announced state directly: older UiAutomator versions only read
    // the deprecated Boolean checked property on Android 16+.
    private fun selectedAccessibleGames(): Int {
        val inst = InstrumentationRegistry.getInstrumentation()
        if (android.os.Build.VERSION.SDK_INT >= 33) inst.uiAutomation.clearCache()
        fun count(node: android.view.accessibility.AccessibilityNodeInfo): Int {
            node.refresh()
            val selected = node.isCheckable && androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(node).stateDescription?.toString() ==
                inst.targetContext.getString(R.string.game_selected)
            return (if (selected) 1 else 0) + (0 until node.childCount).sumOf { index ->
                node.getChild(index)?.let(::count) ?: 0
            }
        }
        return inst.uiAutomation.rootInActiveWindow?.let(::count) ?: 0
    }

    @Test fun englishRegionsResolveTheirOwnSpellingWithoutChangingGameNames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun resources(locale: Locale) = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) }).resources
        val uk = resources(Locale.UK)
        val us = resources(Locale.US)
        assertEquals("Favourite Game", uk.getString(R.string.favorite_game))
        assertEquals("Favorite Game", us.getString(R.string.favorite_game))
        assertTrue(uk.getString(R.string.favorite_games_question).contains("favourite"))
        assertTrue(us.getString(R.string.favorite_games_question).contains("favorite"))
        assertEquals("RECENTRE", uk.getString(R.string.recenter))
        assertEquals("RECENTER", us.getString(R.string.recenter))
        assertTrue(uk.getString(World.LIBERTY.descriptionRes).contains("grey"))
        assertTrue(us.getString(World.LIBERTY.descriptionRes).contains("gray"))
        assertTrue(uk.getString(World.MAFIA.descriptionRes).contains("colours"))
        assertTrue(us.getString(World.MAFIA.descriptionRes).contains("colors"))
        assertEquals("Kilometres and metres", uk.getString(R.string.metric_units))
        assertEquals("Kilometers and meters", us.getString(R.string.metric_units))
        assertEquals("Map data & licences", uk.getString(R.string.map_data_licenses))
        assertEquals("Map data & licenses", us.getString(R.string.map_data_licenses))
        assertEquals("2 selected", uk.getQuantityString(R.plurals.games_selected, 2, 2))
    }
}
