package com.waypoint.app

import android.location.Location
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

/** Starts guidance to Kolkata Airport, waits for other ways to appear, switches to one and checks guidance continues. */
@RunWith(AndroidJUnit4::class)
class DriveAlternativesTest {
    @Test fun alternativesAppearWhileDrivingAndCanBeTaken() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val output = File(context.getExternalFilesDir(null), "drive-alt").apply { deleteRecursively(); mkdirs() }
        val log = File(output, "log.txt")
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(World.LOS_SANTOS) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        SystemClock.sleep(3500)
        val place = Place("test:c", "Kolkata Airport", "cross town", Coordinate(22.6547, 88.4467), "place")
        instrumentation.runOnMainSync { app.navigation.select(place) }
        var waited = 0
        while ((app.navigation.state.value.route == null || app.navigation.state.value.routing) && waited < 40000) { SystemClock.sleep(500); waited += 500 }
        val route = app.navigation.state.value.route
        assertNotNull("Route", route)
        val start = route!!.points.first()
        app.navigation.testLocationsOnly = true
        try {
            fun fix(c: Coordinate) = app.navigation.injectTestLocation(Location("gps").apply {
                latitude = c.lat; longitude = c.lon; accuracy = 5f
                time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
            fix(start); SystemClock.sleep(800); fix(start)
            instrumentation.runOnMainSync { app.navigation.startNavigation() }
            // Hold at the start; alternatives are requested from the car's position.
            var seen = 0
            waited = 0
            while (app.navigation.state.value.driveAlternatives.isEmpty() && waited < 60000) { fix(start); SystemClock.sleep(1000); waited += 1000 }
            val before = app.navigation.state.value
            seen = before.driveAlternatives.size
            log.appendText("alternatives while driving: $seen minutes=${before.driveAlternatives.map { Math.round(it.seconds / 60) }} current=${Math.round(before.route!!.seconds / 60)}\n")
            device.takeScreenshot(File(output, "nav-with-alternatives.png"))
            assertTrue("Other ways offered while driving", seen > 0)
            val oldRoute = before.route
            instrumentation.runOnMainSync { app.navigation.switchToAlternate(0) }
            waited = 0
            while (app.navigation.state.value.route === oldRoute && waited < 20000) { fix(start); SystemClock.sleep(500); waited += 500 }
            SystemClock.sleep(3000)
            repeat(4) { fix(start); SystemClock.sleep(700) }
            val after = app.navigation.state.value
            log.appendText("after switch: navigating=${after.navigating} routeChanged=${after.route !== oldRoute} minutes=${after.route?.seconds?.let { Math.round(it / 60) }} message=${after.message}\n")
            device.takeScreenshot(File(output, "nav-after-switch.png"))
            assertTrue(after.navigating)
            assertTrue("Route changed", after.route !== oldRoute)
        } finally {
            app.navigation.testLocationsOnly = false
            instrumentation.runOnMainSync { app.navigation.stopNavigation(); app.navigation.clearDestination() }
        }
    }
}
