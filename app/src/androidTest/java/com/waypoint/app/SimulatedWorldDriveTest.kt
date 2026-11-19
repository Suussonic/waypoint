package com.waypoint.app

import android.location.Location
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Drives a short Los Santos route near the device by injecting GPS fixes along the app's own route.
 * Trevor clip playback is logged by AssetClipVoice (logcat tag WorldClips); a summary and mid-drive screenshot
 * are written to the app's external files dir under drive-trevor/.
 */
@RunWith(AndroidJUnit4::class)
class SimulatedWorldDriveTest {
    private val worlds = listOf(World.LOS_SANTOS)

    private fun metres(a: Coordinate, b: Coordinate): Double {
        val dy = (b.lat - a.lat) * 111_320.0
        val dx = (b.lon - a.lon) * 111_320.0 * cos(Math.toRadians(a.lat))
        return hypot(dx, dy)
    }
    private fun bearing(a: Coordinate, b: Coordinate): Float {
        val dy = (b.lat - a.lat); val dx = (b.lon - a.lon) * cos(Math.toRadians(a.lat))
        return ((Math.toDegrees(atan2(dx, dy)) + 360.0) % 360.0).toFloat()
    }
    /** Points every ~[step] metres along the polyline. */
    private fun resample(points: List<Coordinate>, step: Double): List<Coordinate> {
        val out = mutableListOf(points.first())
        var carry = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            val len = metres(a, b)
            var d = step - carry
            while (d <= len) {
                val t = d / len
                out += Coordinate(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
                d += step
            }
            carry = len - (d - step)
        }
        if (out.last() != points.last()) out += points.last()
        return out
    }

    /**
     * Los Santos at a realistic pace (default about 14 mph, one fix per second) to check camera smoothness.
     * Arguments: world (default LOS_SANTOS), stepMeters (6.3), steps (90).
     */
    @Test fun driveOneWorldAtRealisticPace() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val world = World.valueOf(args.getString("world") ?: "LOS_SANTOS")
        val stepMeters = args.getString("stepMeters")?.toDouble() ?: 6.3
        val steps = args.getString("steps")?.toInt() ?: 90
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        device.wait(Until.hasObject(By.pkg(context.packageName)), 10000)
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(world) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        var waited = 0
        while (app.navigation.state.value.fix == null && waited < 30000) { SystemClock.sleep(500); waited += 500 }
        val start = app.navigation.state.value.fix
        assertNotNull("Device location", start)
        val destination = Place("sim:near", "Nearby test stop", "simulated drive",
            Coordinate(start!!.lat + 0.010, start.lon + 0.010), "place")
        instrumentation.runOnMainSync { app.navigation.select(destination) }
        waited = 0
        while ((app.navigation.state.value.route == null || app.navigation.state.value.routing) && waited < 40000) { SystemClock.sleep(500); waited += 500 }
        val route = app.navigation.state.value.route
        assertNotNull("Route", route)
        val path = resample(route!!.points, stepMeters)
        app.navigation.testLocationsOnly = true
        try {
            fun fix(c: Coordinate, next: Coordinate?) = app.navigation.injectTestLocation(Location("gps").apply {
                latitude = c.lat; longitude = c.lon; accuracy = 5f; speed = (stepMeters).toFloat()
                if (next != null) bearing = bearing(c, next)
                time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
            fix(path.first(), path.getOrNull(1)); SystemClock.sleep(800); fix(path.first(), path.getOrNull(1))
            instrumentation.runOnMainSync { app.navigation.startNavigation() }
            SystemClock.sleep(2000)
            android.util.Log.d("ShakeTest", "driving ${world.name} step=$stepMeters")
            for ((i, c) in path.take(steps).withIndex()) {
                fix(c, path.getOrNull(i + 1))
                SystemClock.sleep(1000)
            }
            SystemClock.sleep(9000)   // let a closing arrival line play out before the instrumentation process is torn down
            android.util.Log.d("ShakeTest", "done")
        } finally {
            app.navigation.testLocationsOnly = false
            instrumentation.runOnMainSync { app.navigation.stopNavigation(); app.navigation.clearDestination() }
        }
    }

    @Test fun driveTrevorInLosSantos() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val output = File(context.getExternalFilesDir(null), "drive-trevor").apply { deleteRecursively(); mkdirs() }
        val log = File(output, "summary.txt")
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        device.wait(Until.hasObject(By.pkg(context.packageName)), 10000)
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(worlds.first()) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        var waited = 0
        while (app.navigation.state.value.fix == null && waited < 30000) { SystemClock.sleep(500); waited += 500 }
        val start = app.navigation.state.value.fix
        assertNotNull("Device location", start)
        // About 1.5 km away; the router snaps both ends to the nearest road.
        val destination = Place("sim:near", "Nearby test stop", "simulated drive",
            Coordinate(start!!.lat + 0.010, start.lon + 0.010), "place")

        for (world in worlds) {
            val t0 = SystemClock.elapsedRealtime()
            instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(world) }
            SystemClock.sleep(2000)
            instrumentation.runOnMainSync { app.navigation.select(destination) }
            waited = 0
            while ((app.navigation.state.value.route == null || app.navigation.state.value.routing) && waited < 40000) { SystemClock.sleep(500); waited += 500 }
            val route = app.navigation.state.value.route
            if (route == null) { log.appendText("${world.name}: NO ROUTE (${app.navigation.state.value.routeError})\n"); continue }
            val path = resample(route.points, 30.0)
            app.navigation.testLocationsOnly = true
            try {
                fun fix(c: Coordinate, next: Coordinate?) = app.navigation.injectTestLocation(Location("gps").apply {
                    latitude = c.lat; longitude = c.lon; accuracy = 5f; speed = 30f
                    if (next != null) bearing = bearing(c, next)
                    time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                })
                fix(path.first(), path.getOrNull(1)); SystemClock.sleep(800); fix(path.first(), path.getOrNull(1))
                instrumentation.runOnMainSync { app.navigation.startNavigation() }
                SystemClock.sleep(2000)
                var shot = false
                for ((i, c) in path.withIndex()) {
                    fix(c, path.getOrNull(i + 1))
                    SystemClock.sleep(1000)
                    if (!shot && i >= path.size / 2) { device.takeScreenshot(File(output, "${world.name}.png")); shot = true }
                    if (app.navigation.state.value.arrived) break
                }
                repeat(3) { fix(path.last(), null); SystemClock.sleep(1000) }
                val s = app.navigation.state.value
                log.appendText("${world.name}: points=${path.size} arrived=${s.arrived} navigating=${s.navigating} " +
                    "seconds=${(SystemClock.elapsedRealtime() - t0) / 1000}\n")
            } finally {
                app.navigation.testLocationsOnly = false
                instrumentation.runOnMainSync { app.navigation.stopNavigation(); app.navigation.clearDestination() }
                SystemClock.sleep(1000)
            }
        }
    }
}
