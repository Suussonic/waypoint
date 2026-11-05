package com.waypoint.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in public-coordinate DHU visual fixture. Input stays inside Waypoint Preview. */
@RunWith(AndroidJUnit4::class)
class CarCameraComparisonTest {
    @Suppress("UNCHECKED_CAST")
    @Test fun comparePublicLondonJourney() {
        val inst = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("cameraComparison") == "true")
        val context = inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val app = context.applicationContext as WaypointApplication
        val controller = app.navigation
        val field = NavigationController::class.java.getDeclaredField("_state").apply { isAccessible = true }
        val state = field.get(controller) as MutableStateFlow<NavState>
        val original = state.value
        val start = Coordinate(51.5081, -0.1280)
        val destination = Coordinate(51.5014, -0.1419)
        val route = runBlocking { withTimeout(30_000) { MapRepository().route(start, destination) } }
        val place = Place("camera-buckingham-palace", "Buckingham Palace", "London · simulation", destination)
        val control = File(context.filesDir, "car-camera-control.txt").apply { writeText("preview") }
        val enabled = NavigationController::class.java.getDeclaredField("enabled").apply { isAccessible = true }
        val wasEnabled = enabled.getBoolean(controller)
        val engine = NavigationEngine(route)
        var movingSince = 0L
        try {
            inst.runOnMainSync {
                NavigationController::class.java.getDeclaredMethod("stopTracking").apply { isAccessible = true }.invoke(controller)
                enabled.setBoolean(controller, false)
            }
            val deadline = SystemClock.elapsedRealtime() + 300_000
            inst.sendStatus(2, android.os.Bundle().apply { putString("stream", "Camera fixture ready; public London coordinates only.\n") })
            while (SystemClock.elapsedRealtime() < deadline) {
                val command = control.takeIf { it.isFile && it.length() <= 128 }?.readText()?.trim()?.split(':').orEmpty()
                val phase = command.firstOrNull() ?: "stop"
                val world = World.entries.firstOrNull { it.name == command.getOrNull(1) } ?: World.DIVISION
                val mode = TravelMode.entries.firstOrNull { it.name == command.getOrNull(2) } ?: TravelMode.CAR
                if (phase == "stop") break
                if (phase == "moving" && movingSince == 0L) movingSince = SystemClock.elapsedRealtime()
                var meters = if (phase == "moving") (SystemClock.elapsedRealtime() - movingSince) / 1000.0 * 8
                    else if(phase=="at")command.getOrNull(3)?.toDoubleOrNull()?.takeIf { it.isFinite() }?.coerceIn(0.0,route.meters) ?: 0.0
                    else 0.0
                var point = route.points.first()
                for ((from, to) in route.points.zipWithNext()) {
                    val length = distance(from, to)
                    if (meters <= length && length > 0) {
                        val fraction = meters / length
                        point = Coordinate(from.lat + (to.lat - from.lat) * fraction, from.lon + (to.lon - from.lon) * fraction)
                        break
                    }
                    meters -= length
                    point = to
                }
                val progress = engine.update(point)
                val base = NavState(route = route, progress = progress, navigating = phase != "preview")
                val bearing = carTravelBearing(base).toFloat()
                val speed = if (phase == "moving" || phase=="at") 8f else 0f
                inst.runOnMainSync {
                    state.value = state.value.copy(world = world, mode = mode, fix = point, accuracy = 3f,
                        gpsReady = true, gpsStatus = "Simulated drive", city = "London · simulation",
                        speed = speed, headingReliable = true, headingSource = HeadingSource.COURSE, bearing = bearing,
                        selected = place, route = route, progress = if (phase == "preview") null else progress,
                        navigating = phase != "preview", routing = false, routeError = null, offRoute = false)
                }
                SystemClock.sleep(500)
            }
        } finally {
            inst.runOnMainSync {
                controller.leaveCarSession()
                enabled.setBoolean(controller, wasEnabled)
                state.value = original.copy(carConnected = state.value.carConnected)
                controller.refreshCarLocation()
            }
            control.delete()
        }
    }
}
