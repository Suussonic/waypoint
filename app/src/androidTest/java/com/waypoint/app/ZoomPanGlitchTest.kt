package com.waypoint.app

import android.content.SharedPreferences
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.io.File
import kotlin.math.abs

/**
 * Checks the real map screen while zooming in and out for every world theme. Pass a comma separated
 * list in the "worlds" argument to narrow a local run. Screenshots are private evidence under the
 * app's external files directory; the test restores the user's selected world and onboarding flags.
 */
@RunWith(AndroidJUnit4::class)
class ZoomPanGlitchTest {
    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount)
            findMapView(view.getChildAt(index))?.let { return it }
        return null
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    private fun restorePreferences(preferences: SharedPreferences, saved: Map<String, *>) {
        val edit = preferences.edit()
        for (key in preferences.all.keys - saved.keys) edit.remove(key)
        for ((key, value) in saved) when (value) {
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            is String -> edit.putString(key, value)
            is Set<*> -> edit.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
        edit.commit()
    }

    @Test fun zoomInAndOutEveryWorld() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val preferences = context.getSharedPreferences("waypoint", 0)
        val original = preferences.all.toMap()
        val alertsPreferences = context.getSharedPreferences("update-alerts", 0)
        val originalAlerts = alertsPreferences.all.toMap()
        val args = InstrumentationRegistry.getArguments().getString("worlds")
        val requestedWorlds = args?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()
        val worlds = World.entries.filter { requestedWorlds == null || it.name in requestedWorlds }
        assertTrue("No matching worlds requested", worlds.isNotEmpty())
        val originalWorld = original["world"] as? String
            ?: app.navigation.state.value.world.name
        val restoreWorld = World.entries.firstOrNull { it.name == originalWorld } ?: World.RIVIA
        val restoreTilt = original["tilted"] as? Boolean ?: (restoreWorld.mapArt().tilt > 0.0)
        val navigation = app.navigation

        preferences.edit()
            .putBoolean("onboardingCompleted", true)
            .putBoolean("mapOpened", true)
            .putBoolean("tilted", false)
            .putInt("welcomeShownVersion", BuildConfig.VERSION_CODE)
            .putString("world", originalWorld)
            .commit()
        if (navigation.state.value.tilted) navigation.toggleTilt()
        navigation.changeWorld(restoreWorld)
        app.markWhatsNewSeen()
        app.updateAlerts.markExplained()
        // The screenshot API writes as the target app, so keep evidence in its app-specific folder.
        val output = File(context.getExternalFilesDir(null), "glitch-review").apply { mkdirs() }

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                var map: MapLibreMap? = null
                scenario.onActivity { activity ->
                    navigation.setVisible(false) // Keep device GPS and heading updates out of the zoom check.
                    val mapView = findMapView(activity.window.decorView)
                    assertNotNull("MapView is missing after onboarding is marked complete", mapView)
                    mapView!!.getMapAsync { map = it }
                }
                assertTrue("MapLibre did not initialize", waitUntil(15_000) { map?.style != null })

                val center = LatLng(22.5535, 88.3520) // Dense real-world POIs for repeatable visual review.
                val zoomSteps = listOf(
                    14.2 to "a-wide-in",
                    16.0 to "b-mid-in",
                    18.4 to "c-close-in",
                    20.0 to "d-detail-in",
                    18.4 to "e-close-out",
                    16.0 to "f-mid-out",
                    14.2 to "g-wide-out",
                )

                for (world in worlds) {
                    scenario.onActivity { navigation.changeWorld(world) }
                    assertTrue("${world.name} did not become active", waitUntil(5_000) {
                        navigation.state.value.world == world
                    })
                    // Allow the world style and its icon sprites to finish loading on the phone.
                    SystemClock.sleep(2_000)

                    for ((index, step) in zoomSteps.withIndex()) {
                        val (zoom, tag) = step
                        scenario.onActivity {
                            val liveMap = requireNotNull(map)
                            liveMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder(liveMap.cameraPosition)
                                    .target(center)
                                    .zoom(zoom)
                                    .bearing(0.0)
                                    .tilt(0.0)
                                    .build()
                            ))
                        }
                        assertTrue("${world.name} did not reach zoom $zoom", waitUntil(3_000) {
                            map?.let { abs(it.cameraPosition.zoom - zoom) < 0.1 } == true
                        })
                        SystemClock.sleep(1_000)
                        scenario.onActivity {
                            val liveMap = requireNotNull(map)
                            assertTrue("${world.name} tilted during zoom $zoom", liveMap.cameraPosition.tilt < 1.0)
                            assertTrue("${world.name} lost its map style at zoom $zoom", liveMap.style != null)
                            assertTrue("${world.name} still renders the removed red-dot layer", liveMap.style?.getLayer("nearby-place-dot") == null)
                        }

                        // Capture the initial neighborhood, closest view, and zoom-out return.
                        if (index == 0 || index == 3 || index == zoomSteps.lastIndex) {
                            val screenshot = File(output, "${world.name.lowercase()}-$tag.png")
                            assertTrue("Could not capture ${world.name} at zoom $zoom to ${screenshot.absolutePath} (directory exists=${output.exists()})",
                                device.takeScreenshot(screenshot))
                        }
                    }
                }

                // Put the user's map back where it started before closing the test activity.
                scenario.onActivity { navigation.changeWorld(restoreWorld) }
                if (navigation.state.value.tilted != restoreTilt) navigation.toggleTilt()
                SystemClock.sleep(1_000)
            }
        } finally {
            if (navigation.state.value.world != restoreWorld) navigation.changeWorld(restoreWorld)
            if (navigation.state.value.tilted != restoreTilt) navigation.toggleTilt()
            restorePreferences(preferences, original)
            restorePreferences(alertsPreferences, originalAlerts)
        }
    }
}
