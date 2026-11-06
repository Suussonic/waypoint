package com.waypoint.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Requests routes to a few destinations, records how many alternatives come back, and captures the route preview. */
@RunWith(AndroidJUnit4::class)
class AlternateRoutesTest {
    @Test fun alternativesAreDrawnAndSelectable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val output = File(context.getExternalFilesDir(null), "alt-routes").apply { deleteRecursively(); mkdirs() }
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(World.LOS_SANTOS) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        SystemClock.sleep(3500)
        val places = listOf(
            Place("test:a", "Barrackpore Railway Station", "far", Coordinate(22.7637, 88.3743), "place"),
            Place("test:b", "Howrah Station", "city", Coordinate(22.5839, 88.3426), "place"),
            Place("test:c", "Kolkata Airport", "cross town", Coordinate(22.6547, 88.4467), "place"),
        )
        val log = File(output, "counts.txt")
        var drawn = false
        for ((i, place) in places.withIndex()) {
            instrumentation.runOnMainSync { app.navigation.clearDestination() }
            SystemClock.sleep(800)
            instrumentation.runOnMainSync { app.navigation.select(place) }
            var waited = 0
            while ((app.navigation.state.value.route == null || app.navigation.state.value.routing) && waited < 40000) { SystemClock.sleep(500); waited += 500 }
            val state = app.navigation.state.value
            log.appendText("${place.name}: options=${state.routeOptions.size} minutes=${state.routeOptions.map { Math.round(it.seconds / 60) }} km=${state.routeOptions.map { Math.round(it.meters / 100) / 10.0 }}\n")
            SystemClock.sleep(2500)
            device.takeScreenshot(File(output, "route-$i.png"))
            if (state.routeOptions.size > 1 && !drawn) {
                drawn = true
                instrumentation.runOnMainSync { app.navigation.selectRouteOption(1) }
                SystemClock.sleep(2500)
                device.takeScreenshot(File(output, "route-$i-second.png"))
                assertNotNull(app.navigation.state.value.route)
            }
        }
        instrumentation.runOnMainSync { app.navigation.clearDestination() }
    }
}
