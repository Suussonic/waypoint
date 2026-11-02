package com.waypoint.app

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@RunWith(AndroidJUnit4::class)
class MapTiltWorldsTest {
    @Test fun tiltControlMovesTheCameraInEveryWorld() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(inst)
        inst.targetContext.getSharedPreferences("waypoint",0).edit()
            .putBoolean("onboardingCompleted",true).putBoolean("mapOpened",true)
            .putInt("welcomeShownVersion",BuildConfig.VERSION_CODE).putBoolean("tilted",false)
            .putString("world",World.LOS_SANTOS.name).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var map:MapLibreMap?=null
            var controller:NavigationController?=null
            scenario.onActivity { activity ->
                controller=(activity.application as WaypointApplication).navigation
                val mapView=findMapView(activity.window.decorView)
                assertNotNull("MapView is missing",mapView)
                mapView!!.getMapAsync { map=it }
            }
            assertTrue("MapLibre did not initialize",waitUntil(10000) { map!=null })
            for(world in World.entries) {
                scenario.onActivity { activity ->
                    (activity.application as WaypointApplication).navigation.changeWorld(world)
                }
                val button=By.desc("Toggle 2D and 3D")
                assertTrue("${world.name} has no tilt button",device.wait(Until.hasObject(button),10000))
                assertTrue("${world.name} tilt button did not respond",clickTilt(device,button) { controller?.state?.value?.tilted==true })
                assertTrue("${world.name} did not tilt",waitUntil(10000) {
                    var angle=0.0
                    scenario.onActivity { angle=map?.cameraPosition?.tilt ?: 0.0 }
                    angle>=20.0
                })
                assertTrue("${world.name} flat button did not respond",clickTilt(device,button) { controller?.state?.value?.tilted==false })
                assertTrue("${world.name} did not flatten",waitUntil(10000) {
                    var angle=90.0
                    scenario.onActivity { angle=map?.cameraPosition?.tilt ?: 90.0 }
                    angle<=1.0
                })
            }
        }
    }

    private fun clickTilt(device:UiDevice,button:androidx.test.uiautomator.BySelector,changed:()->Boolean):Boolean {
        repeat(5) {
            if(changed())return true
            try { device.findObject(button)?.click() } catch(_:StaleObjectException) { }
            if(waitUntil(1500,changed))return true
        }
        return changed()
    }

    private fun findMapView(view:View):MapView? {
        if(view is MapView)return view
        if(view is ViewGroup) for(i in 0 until view.childCount)
            findMapView(view.getChildAt(i))?.let {return it}
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
