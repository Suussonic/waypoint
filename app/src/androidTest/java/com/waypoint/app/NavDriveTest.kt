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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Five minute simulated drive in one world. Starts a real route, then feeds position fixes along it from
 * inside the app (the real GPS is ignored while it runs, and nothing system-wide is changed):
 *   60 s drive at 15 m/s through the first turns, 20 s backwards (a turn-around), 50 s forward again,
 *   20 s on a street 250 m away (off route, then a real reroute), about 110 s following the NEW route to the
 *   destination (a stop 2.6 km along the original route), then parked there until the time is up.
 * The fixes carry no bearing or speed, which exercises the derived-course and route-direction heading path.
 * It photographs the screen about every 4 s and logs the navigation state, the heading source and the
 * displayed heading beside the road's true direction. Captures stay on the device (nav-drive folder).
 * Arguments: world (World name, default LOS_SANTOS), seconds (default 300).
 */
@RunWith(AndroidJUnit4::class)
class NavDriveTest {
    @Test fun captureADrive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val worldName = InstrumentationRegistry.getArguments().getString("world") ?: "LOS_SANTOS"
        val seconds = (InstrumentationRegistry.getArguments().getString("seconds") ?: "300").toInt()
        val output = File(context.getExternalFilesDir(null), "nav-drive").apply { deleteRecursively(); mkdirs() }
        val place = Place("test:station", "Barrackpore Railway Station", "Barrackpore", Coordinate(22.7637, 88.3743), "place")

        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(World.valueOf(worldName)) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        SystemClock.sleep(3500)
        instrumentation.runOnMainSync { app.navigation.select(place) }
        var waited = 0
        while (app.navigation.state.value.route == null && waited < 25000) { SystemClock.sleep(500); waited += 500 }
        val route = app.navigation.state.value.route
        assertNotNull("Route", route)
        // A drive of a few minutes cannot reach a stop 40 km away, so pick a destination 2.6 km along this route.
        val farPoints = route!!.points
        val farCumulative = DoubleArray(farPoints.size)
        for (i in 1 until farPoints.size) farCumulative[i] = farCumulative[i - 1] + distance(farPoints[i - 1], farPoints[i])
        val targetAt = minOf(2600.0, farCumulative.last() * 0.5)
        var t = 1
        while (t < farPoints.size - 1 && farCumulative[t] < targetAt) t++
        val near = Place("test:near", "Test destination", "2.6 km ahead", farPoints[t], "place")
        instrumentation.runOnMainSync { app.navigation.clearDestination() }
        SystemClock.sleep(1000)
        instrumentation.runOnMainSync { app.navigation.select(near) }
        waited = 0
        while (app.navigation.state.value.route == null && waited < 25000) { SystemClock.sleep(500); waited += 500 }
        val nearRoute = app.navigation.state.value.route
        assertNotNull("Near route", nearRoute)
        var points = nearRoute!!.points
        var cumulative = DoubleArray(points.size)
        fun rebuild() {
            val c = DoubleArray(points.size)
            for (i in 1 until points.size) c[i] = c[i - 1] + distance(points[i - 1], points[i])
            cumulative = c
        }
        rebuild()

        fun pointAt(d: Double): Coordinate {
            val s = d.coerceIn(0.0, cumulative.last())
            var i = 1
            while (i < points.size - 1 && cumulative[i] < s) i++
            val span = (cumulative[i] - cumulative[i - 1]).coerceAtLeast(0.001)
            val t = ((s - cumulative[i - 1]) / span).coerceIn(0.0, 1.0)
            return Coordinate(points[i - 1].lat + (points[i].lat - points[i - 1].lat) * t, points[i - 1].lon + (points[i].lon - points[i - 1].lon) * t)
        }
        fun inject(c: Coordinate, accuracy: Float = 5f) {
            val location = Location("gps").apply {
                latitude = c.lat; longitude = c.lon; this.accuracy = accuracy
                time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            app.navigation.injectTestLocation(location)
        }

        File(output, "drive-route.txt").writeText(points.joinToString("\n") { "${it.lat},${it.lon}" })
        File(output, "drive-steps.txt").writeText(nearRoute.steps.joinToString("\n") { "${it.type}|${it.modifier}|${it.road}|${it.point.lat},${it.point.lon}" })
        app.navigation.testLocationsOnly = true
        inject(points.first()); SystemClock.sleep(1000); inject(points.first())
        instrumentation.runOnMainSync { app.navigation.startNavigation() }

        var distanceAlong = 0.0
        val feeder = Thread {
            fun drive(sec: Int, step: Double) {
                val end = SystemClock.elapsedRealtime() + sec * 1000L
                while (SystemClock.elapsedRealtime() < end) {
                    inject(pointAt(distanceAlong)); distanceAlong = (distanceAlong + step).coerceAtLeast(0.0); SystemClock.sleep(1000)
                }
            }
            fun sideways(sec: Int, metres: Double) {
                val here = pointAt(distanceAlong); val ahead = pointAt(distanceAlong + 30)
                val heading = Math.atan2(ahead.lon - here.lon, ahead.lat - here.lat)
                val off = Coordinate(
                    here.lat + metres / 111320.0 * cos(heading + Math.PI / 2),
                    here.lon + metres / (111320.0 * cos(Math.toRadians(here.lat))) * sin(heading + Math.PI / 2))
                val end = SystemClock.elapsedRealtime() + sec * 1000L
                while (SystemClock.elapsedRealtime() < end) { inject(off); SystemClock.sleep(1000) }
            }
            drive(60, 15.0); drive(20, -15.0); drive(50, 15.0); sideways(20, 250.0)
            // The reroute replaced the route; carry on along the new one from the off-road spot.
            SystemClock.sleep(1500)
            val newRoute = app.navigation.state.value.route
            if (newRoute != null && newRoute.points.size > 1) { points = newRoute.points; rebuild(); distanceAlong = 0.0 }
            drive(110, 15.0)
            while (true) { inject(points.last(), 4f); SystemClock.sleep(1000) }
        }.apply { isDaemon = true; start() }

        val log = File(output, "drive-log.csv")
        log.writeText("frame,seconds,navigating,offRoute,arrived,routing,nextStep,toTurn,remaining,speed,type,modifier,road,headingSource,bearing,routeBearing\n")
        val started = SystemClock.elapsedRealtime()
        var frame = 0
        while ((SystemClock.elapsedRealtime() - started) / 1000 < seconds) {
            val s = app.navigation.state.value
            val step = s.route?.steps?.getOrNull(s.progress?.nextStep ?: 0)
            val routeBearing = s.progress?.let { p -> s.route?.let { r -> routeBearingAt(r.points, r.meters, p.traveled) } }
            device.takeScreenshot(File(output, "f%03d.png".format(frame)))
            log.appendText("$frame,${(SystemClock.elapsedRealtime() - started) / 1000},${s.navigating},${s.offRoute},${s.arrived},${s.routing}," +
                "${s.progress?.nextStep},${s.progress?.toTurn?.toInt()},${s.progress?.remaining?.toInt()},${"%.1f".format(s.speed)}," +
                "${step?.type},${step?.modifier},\"${step?.road?.replace("\"", "")}\",${s.headingSource},${"%.0f".format(s.bearing)}," +
                "${routeBearing?.let { "%.0f".format(it) } ?: ""}\n")
            frame++
            SystemClock.sleep(3500)
        }
        app.navigation.testLocationsOnly = false
        instrumentation.runOnMainSync { app.navigation.stopNavigation() }
    }
}
