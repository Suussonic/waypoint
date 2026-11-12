package com.waypoint.app

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.abs

class MapOrientationTest {
    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount)
            findMap(view.getChildAt(index))?.let { return it }
        return null
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            android.os.SystemClock.sleep(100)
        }
        return condition()
    }

    private fun waitForBearing(map: MapLibreMap, expected: Float): Boolean = waitUntil(5000) {
        abs(headingDelta(map.cameraPosition.bearing.toFloat(), expected)) < 4f
    }

    @Test fun compassTogglesHeadingAndNorthWithoutRecenteringOrZooming() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val fix = Coordinate(51.5081, -0.1280)
        val model = mutableStateOf(NavState(
            fix = fix,
            gpsReady = true,
            headingReliable = true,
            headingSource = HeadingSource.COMPASS,
            bearing = 90f,
            accuracy = 4f,
            tilted = false,
        ))
        var map: MapLibreMap? = null
        val mapView = arrayOfNulls<MapView>(1)
        val actions = AtlasActions({}, {}, {}, {}, {}, {}, {}, {}, toggleMapOrientation = {
            val current = model.value
            model.value = current.copy(keepMapNorthUp = !current.keepMapNorthUp,
                orientationRequest = current.orientationRequest + 1)
        })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                (activity.application as WaypointApplication).navigation.setVisible(false)
                activity.setContent {
                    Box(Modifier.fillMaxSize()) {
                        WorldMap(model.value, {}, {}, Modifier.fillMaxSize())
                        MapChrome(model.value, model.value.bearing, actions, Modifier.fillMaxSize())
                    }
                }
            }
            assertTrue("MapView was not composed", waitUntil(10000) {
                scenario.onActivity { activity -> mapView[0] = findMap(activity.window.decorView) }
                mapView[0] != null
            })
            scenario.onActivity { mapView[0]!!.getMapAsync { map = it } }
            assertTrue("MapLibre did not initialize", waitUntil(15000) { map?.style != null })
            val liveMap = requireNotNull(map)
            assertTrue("Heading-up did not follow the stationary phone compass", waitForBearing(liveMap, 90f))
            android.os.SystemClock.sleep(1000)

            val before = liveMap.cameraPosition
            val beforeTarget = requireNotNull(before.target)
            val headingUpCompass = By.desc("Heading 90 degrees. Heading-up. Tap to Keep map north-up.")
            assertTrue("Heading-up compass control is missing", device.wait(Until.hasObject(headingUpCompass), 5000))
            device.findObject(headingUpCompass).click()
            assertTrue("Compass tap did not rotate the map north-up", waitForBearing(liveMap, 0f))
            val northUpCompass = By.desc("Heading 90 degrees. North-up. Tap to Follow heading and travel direction.")
            assertTrue("Compass did not announce north-up mode", device.wait(Until.hasObject(northUpCompass), 5000))
            android.os.SystemClock.sleep(400)
            val northUp = liveMap.cameraPosition
            val northUpTarget = requireNotNull(northUp.target)
            assertTrue("Orientation toggle moved the map centre",
                distance(Coordinate(beforeTarget.latitude, beforeTarget.longitude),
                    Coordinate(northUpTarget.latitude, northUpTarget.longitude)) < 10.0)
            assertEquals("Orientation toggle changed zoom", before.zoom, northUp.zoom, 0.1)

            device.findObject(northUpCompass).click()
            assertTrue("Compass tap did not restore heading-up", waitForBearing(liveMap, 90f))
            assertTrue("Compass action did not clear north-up state", waitUntil(1000) { !model.value.keepMapNorthUp })

            instrumentation.runOnMainSync {
                model.value = model.value.copy(navigating = true, headingSource = HeadingSource.COURSE, bearing = 130f)
            }
            assertTrue("Moving course did not turn the map", waitForBearing(liveMap, 130f))
        }
    }
}
