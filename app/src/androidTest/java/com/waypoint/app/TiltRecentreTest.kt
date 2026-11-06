package com.waypoint.app

import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The 2D/3D control pitches the map where it is; Locate/recentre is the only thing that flies to the user. */
class TiltRecentreTest {
    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findMap(view.getChildAt(index))?.let { return it }
        return null
    }

    @Test fun tiltKeepsTargetZoomAndBearingThenRecentreFliesBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as WaypointApplication
        app.markWhatsNewSeen()
        app.updateAlerts.markExplained()
        val controller = app.navigation
        // Public fixtures only: Trafalgar Square as the "user", the Eiffel Tower as where they look.
        val user = Coordinate(51.5080, -0.1281)
        val elsewhere = LatLng(48.8584, 2.2945)
        lateinit var flow: MutableStateFlow<NavState>
        instrumentation.runOnMainSync {
            controller.completeOnboarding(listOf(World.SKYRIM))
            controller.markMapOpened()
            val field = controller.javaClass.getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            flow = field.get(controller) as MutableStateFlow<NavState>
            flow.value = flow.value.copy(world = World.SKYRIM, fix = user, gpsReady = true, accuracy = 4f, tilted = false)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val ready = CountDownLatch(1)
            var map: MapLibreMap? = null
            scenario.onActivity { activity ->
                controller.setVisible(false)
                val view = findMap(activity.window.decorView)
                assertNotNull(view)
                view!!.getMapAsync { map = it; ready.countDown() }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            Thread.sleep(2500)
            scenario.onActivity {
                map!!.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(elsewhere).zoom(17.0).bearing(30.0).tilt(0.0).build()))
            }
            Thread.sleep(500)
            scenario.onActivity { controller.toggleTilt() }
            Thread.sleep(1500)
            scenario.onActivity {
                val camera = map!!.cameraPosition
                assertTrue("tilt must change: ${camera.tilt}", camera.tilt > 20.0)
                assertEquals(elsewhere.latitude, camera.target!!.latitude, 0.0005)
                assertEquals(elsewhere.longitude, camera.target!!.longitude, 0.0005)
                assertEquals(17.0, camera.zoom, 0.05)
                assertEquals(30.0, camera.bearing, 0.5)
            }
            scenario.onActivity { controller.toggleTilt() }
            Thread.sleep(1500)
            scenario.onActivity {
                val camera = map!!.cameraPosition
                assertEquals(0.0, camera.tilt, 1.0)
                assertEquals(elsewhere.latitude, camera.target!!.latitude, 0.0005)
            }
            scenario.onActivity { controller.recenter() }
            Thread.sleep(1500)
            scenario.onActivity {
                val camera = map!!.cameraPosition
                assertEquals(user.lat, camera.target!!.latitude, 0.0005)
                assertEquals(user.lon, camera.target!!.longitude, 0.0005)
                assertTrue("recentre restores street zoom: ${camera.zoom}", camera.zoom >= 18.0)
            }
        }
    }
}
