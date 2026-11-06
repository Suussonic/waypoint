package com.waypoint.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Starts a real route in every world (or the World names in the "worlds" argument) and captures the
 * navigation screen: banner, Then tab, trip bar, side controls. Needs a GPS fix and a network connection.
 * It only changes the active world and the temporary trip; saved places and quests are untouched.
 */
@RunWith(AndroidJUnit4::class)
class NavAllWorldsTest {
    @Test fun navigationLooksRightInEveryWorld() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val output = File(context.getExternalFilesDir(null), "nav-review").apply { mkdirs() }
        val wanted = InstrumentationRegistry.getArguments().getString("worlds")?.split(',')?.map { it.trim() }
        val place = Place("test:station", "Barrackpore Railway Station", "Barrackpore", Coordinate(22.7637, 88.3743), "place")
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        for (world in World.entries.filter { wanted == null || it.name in wanted }) {
            instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(world) }
            SystemClock.sleep(3000)
            device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
            SystemClock.sleep(3500)
            instrumentation.runOnMainSync { app.navigation.select(place) }
            var waited = 0
            while (app.navigation.state.value.route == null && waited < 25000) { SystemClock.sleep(500); waited += 500 }
            instrumentation.runOnMainSync { app.navigation.startNavigation() }
            SystemClock.sleep(5500)
            device.takeScreenshot(File(output, "${world.name.lowercase()}.png"))
            instrumentation.runOnMainSync { app.navigation.stopNavigation() }
            SystemClock.sleep(800)
        }
    }
}
