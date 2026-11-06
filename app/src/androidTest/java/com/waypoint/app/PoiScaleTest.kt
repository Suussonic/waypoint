package com.waypoint.app

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.*
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import java.io.File

/**
 * Frames of a busy city centre (Park Street, Kolkata) at several zooms, to judge how place markers and the
 * position marker scale. Arguments: world (default LOS_SANTOS). Saved in the poi-scale folder on the device.
 */
@RunWith(AndroidJUnit4::class)
class PoiScaleTest {
    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMapView(view.getChildAt(i))?.let { return it }
        return null
    }

    @Test fun placeMarkersAcrossZoom() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val world = World.valueOf(InstrumentationRegistry.getArguments().getString("world") ?: "LOS_SANTOS")
        val output = File(context.getExternalFilesDir(null), "poi-scale").apply { mkdirs() }
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        instrumentation.runOnMainSync { app.navigation.changeWorld(world) }
        SystemClock.sleep(3500)
        device.swipe(540, 1100, 580, 1000, 6)  // a user gesture, so the camera stops following the fix
        SystemClock.sleep(800)
        var view: MapView? = null
        instrumentation.runOnMainSync {
            val activity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
            view = activity?.window?.decorView?.let { findMapView(it) }
        }
        assertNotNull("MapView", view)
        for (z in listOf(13.0, 14.5, 16.0, 17.5, 19.0, 20.5)) {
            instrumentation.runOnMainSync {
                view!!.getMapAsync { it.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(LatLng(22.5535, 88.3520)).zoom(z).build())) }
            }
            SystemClock.sleep(5000)
            assertTrue(device.takeScreenshot(File(output, "${world.name.lowercase()}-z${z.toString().replace('.', '_')}.png")))
        }
    }
}
