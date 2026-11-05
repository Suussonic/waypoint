package com.waypoint.app

import android.Manifest
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.MutableStateFlow
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

@RunWith(AndroidJUnit4::class)
class MapLocateWorldsTest {
    @Test fun locateRestoresNeighborhoodZoomInEveryWorldDespitePositionUpdates() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(inst)
        inst.targetContext.getSharedPreferences("waypoint",0).edit()
            .putBoolean("onboardingCompleted",true).putBoolean("mapOpened",true)
            .putInt("welcomeShownVersion",BuildConfig.VERSION_CODE)
            .putString("world",World.LOS_SANTOS.name).commit()
        inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        val start=Coordinate(22.5726,88.3639)
        val latest=Coordinate(22.5727,88.3640)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var map:MapLibreMap?=null
            lateinit var controller:NavigationController
            lateinit var stateFlow:MutableStateFlow<NavState>
            scenario.onActivity { activity ->
                controller=(activity.application as WaypointApplication).navigation
                // This test drives the position explicitly; the device's live GPS would
                // otherwise race the synthetic fix during the recenter assertion.
                controller.setVisible(false)
                val field=NavigationController::class.java.getDeclaredField("_state").apply{isAccessible=true}
                @Suppress("UNCHECKED_CAST")
                stateFlow=field.get(controller) as MutableStateFlow<NavState>
                stateFlow.value=NavState(world=World.LOS_SANTOS,fix=start,gpsReady=true,accuracy=5f,gpsStatus="GPS connected")
                val mapView=findMapView(activity.window.decorView)
                assertNotNull("MapView is missing",mapView)
                mapView!!.getMapAsync { map=it }
            }
            assertTrue("MapLibre did not initialize",waitUntil(10000){map!=null})
            for(world in World.entries) {
                scenario.onActivity {
                    controller.changeWorld(world)
                    stateFlow.value=stateFlow.value.copy(fix=start)
                }
                SystemClock.sleep(400)
                scenario.onActivity {
                    map!!.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                        .target(LatLng(20.0,90.0)).zoom(2.5).tilt(0.0).build()))
                }
                if(world==World.LOS_SANTOS) {
                    val locate=By.desc("Use my location")
                    assertTrue("Locate button is missing",device.wait(Until.hasObject(locate),5000))
                    device.findObject(locate).click()
                } else scenario.onActivity { controller.recenter() }
                SystemClock.sleep(100)
                scenario.onActivity { stateFlow.value=stateFlow.value.copy(fix=latest) }
                SystemClock.sleep(1200)
                var camera:CameraPosition?=null
                scenario.onActivity { camera=map?.cameraPosition }
                assertNotNull("${world.name} camera is missing",camera)
                assertTrue("${world.name} stayed zoomed out: ${camera!!.zoom}",camera!!.zoom>=16.9)
                val center=camera!!.target
                assertTrue("${world.name} did not center on the synthetic fix: $center",center!=null &&
                    distance(Coordinate(center.latitude,center.longitude),latest)<100.0)
                if(world==World.LOS_SANTOS) {
                    val screenshot=File(inst.targetContext.getExternalFilesDir(null),"map-locate-reticle.png")
                    assertTrue("Could not capture the recentered map",device.takeScreenshot(screenshot))
                }
            }
        }
    }

    private fun findMapView(view:View):MapView? {
        if(view is MapView)return view
        if(view is ViewGroup) for(i in 0 until view.childCount)
            findMapView(view.getChildAt(i))?.let{return it}
        return null
    }

    private fun waitUntil(timeoutMs:Long,condition:()->Boolean):Boolean {
        val deadline=SystemClock.elapsedRealtime()+timeoutMs
        while(SystemClock.elapsedRealtime()<deadline) {
            if(condition())return true
            SystemClock.sleep(100)
        }
        return condition()
    }
}
