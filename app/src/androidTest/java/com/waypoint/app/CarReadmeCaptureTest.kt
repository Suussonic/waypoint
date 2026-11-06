package com.waypoint.app

import android.location.Location
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in public DHU capture fixture. It runs the real flow (search result, route choices, Start,
 * guidance) for a public road through Hampstead Heath, feeding position fixes from inside the app.
 * The phone's real GPS is ignored, so no device position or personal destination reaches the car,
 * and the map is pinned to its light theme whatever the hour. Nothing system-wide is changed.
 * The operator takes the DHU screenshots when the log markers appear.
 */
@RunWith(AndroidJUnit4::class)
class CarReadmeCaptureTest {
    @Volatile private var current = Coordinate(0.0, 0.0)
    @Volatile private var feeding = true

    @Test fun showPublicLondonRouteOnHeadUnit() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("captureReadmeCar") == "true")
        val controller = (instrumentation.targetContext.applicationContext as WaypointApplication).navigation
        // Spaniards Road through Hampstead Heath: a public road with woodland either side.
        val start = Coordinate(51.5594, -0.1775)
        val place = Place("readme-spaniards-inn", "The Spaniards Inn", "Hampstead Heath, London", Coordinate(51.5716, -0.1680))
        val world = InstrumentationRegistry.getArguments().getString("world")?.let { World.valueOf(it) } ?: World.OLD_WEST

        current = start
        feeding = true
        fun inject(c: Coordinate) = controller.injectTestLocation(Location("gps").apply {
            latitude = c.lat; longitude = c.lon; accuracy = 5f
            time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        })
        val feeder = Thread {
            while (feeding) { inject(current); SystemClock.sleep(1000) }
        }.apply { isDaemon = true }

        try {
            mapThemeNightOverride = false
            controller.testLocationsOnly = true
            instrumentation.runOnMainSync { controller.clearDestination(); controller.changeWorld(world) }
            feeder.start()
            SystemClock.sleep(4000)
            instrumentation.runOnMainSync { controller.select(place) }
            var waited = 0
            while ((controller.state.value.routeOptions.isEmpty() || controller.state.value.route == null) && waited < 40_000) {
                SystemClock.sleep(500); waited += 500
            }
            val route = controller.state.value.route
            assertNotNull("Route", route)
            Log.i("CarReadmeCapture", "${world.name}_PREVIEW_READY options=${controller.state.value.routeOptions.size}")
            SystemClock.sleep(22_000)

            val points = route!!.points
            val cumulative = DoubleArray(points.size)
            for (i in 1 until points.size) cumulative[i] = cumulative[i - 1] + distance(points[i - 1], points[i])
            fun pointAt(metres: Double): Coordinate {
                val s = metres.coerceIn(0.0, cumulative.last())
                var i = 1
                while (i < points.size - 1 && cumulative[i] < s) i++
                val span = (cumulative[i] - cumulative[i - 1]).coerceAtLeast(0.001)
                val t = ((s - cumulative[i - 1]) / span).coerceIn(0.0, 1.0)
                return Coordinate(points[i - 1].lat + (points[i].lat - points[i - 1].lat) * t,
                    points[i - 1].lon + (points[i].lon - points[i - 1].lon) * t)
            }
            instrumentation.runOnMainSync { controller.startNavigation() }
            Log.i("CarReadmeCapture", "${world.name}_GUIDANCE_READY")
            var along = 0.0
            val end = SystemClock.elapsedRealtime() + 70_000
            while (SystemClock.elapsedRealtime() < end) {
                current = pointAt(along)
                along += 11.0
                SystemClock.sleep(1000)
            }
        } finally {
            feeding = false
            mapThemeNightOverride = null
            controller.testLocationsOnly = false
            instrumentation.runOnMainSync { controller.stopNavigation(); controller.clearDestination() }
        }
    }
}
