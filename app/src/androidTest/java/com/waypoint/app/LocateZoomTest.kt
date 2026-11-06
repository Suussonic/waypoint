package com.waypoint.app

import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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

class LocateZoomTest {
    private fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findMap(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    @Test fun locateRestoresStreetZoomAfterMapWasZoomedOut() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as WaypointApplication
        app.markWhatsNewSeen()
        val controller = app.navigation
        app.updateAlerts.markExplained()
        val originalFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val originalCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (originalFine) instrumentation.uiAutomation.revokeRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (originalCoarse) instrumentation.uiAutomation.revokeRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        // Public city-center fixture; never commit coordinates from a tester's device.
        val actualFix = Coordinate(22.5726, 88.3639)
        lateinit var flow: MutableStateFlow<NavState>
        try {
            instrumentation.runOnMainSync {
                controller.completeOnboarding(listOf(World.SKYRIM))
                controller.markMapOpened()
                val stateField = controller.javaClass.getDeclaredField("_state").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                flow = stateField.get(controller) as MutableStateFlow<NavState>
                flow.value = flow.value.copy(world = World.SKYRIM, fix = actualFix, gpsReady = true, accuracy = 4f)
            }

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val device = UiDevice.getInstance(instrumentation)
            val locate = device.wait(Until.findObject(By.desc("Use my location")), 10000)
            val visibleState=listOf("Continue","What's new","Worlds","Settings","Locate")
                .filter { device.hasObject(By.text(it)) || device.hasObject(By.desc(it)) }
            assertNotNull("Locate button (package=${device.currentPackageName}, landing=${controller.shouldShowLanding()}, world=${controller.state.value.world}, visible=$visibleState)", locate)

            val ready = CountDownLatch(1)
            var map: MapLibreMap? = null
            scenario.onActivity { activity ->
                // Activity.onStart resumes real GPS; stop it before the synthetic
                // fix so the device cannot replace this test's camera target.
                controller.setVisible(false)
                flow.value = flow.value.copy(fix = actualFix, gpsReady = true, accuracy = 4f)
                val mapView = findMap(activity.window.decorView)
                assertNotNull("Native map", mapView)
                mapView!!.getMapAsync { map = it; ready.countDown() }
            }
            assertTrue("Map camera ready", ready.await(10, TimeUnit.SECONDS))
            scenario.onActivity {
                map!!.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(20.0, 78.0)).zoom(3.0).build()
                ))
                assertTrue(map!!.cameraPosition.zoom < 4.0)
            }

            scenario.onActivity { controller.recenter() }
            Thread.sleep(1300)
            scenario.onActivity {
                val camera = map!!.cameraPosition
                assertTrue("Locate should restore street-level zoom: ${camera.zoom}", camera.zoom >= 18.0)
                assertEquals(actualFix.lat, camera.target!!.latitude, 0.0005)
                assertEquals(actualFix.lon, camera.target!!.longitude, 0.0005)
            }

            val destination = Coordinate(22.58, 88.37)
            scenario.onActivity {
                val route = RoadRoute(listOf(actualFix, destination), listOf(
                    Maneuver(actualFix, "depart", "straight", "Test Road", 1800.0, 180.0),
                    Maneuver(destination, "arrive", "", "", 0.0, 0.0),
                ), 1800.0, 180.0, TravelMode.CAR)
                flow.value = flow.value.copy(
                    selected = Place("test", "Test destination", "Synthetic route", destination),
                    route = route,
                    gpsReady = true,
                    cameraRequest = flow.value.cameraRequest + 1,
                )
            }
            val routeLocate = device.wait(Until.findObject(By.desc("Use my location")), 2500)
                ?: device.wait(Until.findObject(By.text("Use my location")), 2500)
            val visible=mutableListOf<String>()
            fun collect(node:AccessibilityNodeInfo?) {
                if(node==null || visible.size>=100)return
                node.text?.toString()?.takeIf(String::isNotBlank)?.let(visible::add)
                node.contentDescription?.toString()?.takeIf(String::isNotBlank)?.let(visible::add)
                for(index in 0 until node.childCount)collect(node.getChild(index))
            }
            collect(instrumentation.uiAutomation.rootInActiveWindow)
            assertNotNull("Locate button in route preview (visible=${visible.distinct()})", routeLocate)
            Thread.sleep(900)
            scenario.onActivity {
                map!!.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(20.0, 78.0)).zoom(3.0).build()
                ))
            }
            scenario.onActivity { controller.recenter() }
            Thread.sleep(1300)
            scenario.onActivity {
                val camera = map!!.cameraPosition
                assertTrue("Route preview Locate should restore street-level zoom: ${camera.zoom}", camera.zoom >= 18.0)
                assertEquals(actualFix.lat, camera.target!!.latitude, 0.0005)
                assertEquals(actualFix.lon, camera.target!!.longitude, 0.0005)
            }
            }
        } finally {
            if (originalCoarse) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (originalFine) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
