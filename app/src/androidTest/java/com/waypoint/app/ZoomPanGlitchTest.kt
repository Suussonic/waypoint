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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapView
import java.io.File

/**
 * Visual glitch hunt. For every world (or the comma separated World names in the "worlds" argument):
 * locate, then step the zoom out (13, 11, 9, 7, 5, 3), pan two ways with real swipes at zoom 11 and
 * return to street zoom, saving a screenshot after each step. It only switches the active world; it never resets
 * onboarding, saved places or quests. Captures are private evidence in the app's external files folder.
 *
 * The test runs inside the app process, so it sets the camera on the live MapLibre view directly.
 * A single real swipe first stops the follow-me logic from snapping the camera back to the GPS fix.
 *
 * Run on an unlocked development phone, installed by hand so Gradle does not uninstall the app:
 *   adb shell am instrument -w -e class com.waypoint.app.ZoomPanGlitchTest [-e worlds OLD_WEST,PIXEL] \
 *     <test package>/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ZoomPanGlitchTest {
    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMapView(view.getChildAt(i))?.let { return it }
        return null
    }

    @Test fun zoomOutAndPanEveryWorld() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val output = File(context.getExternalFilesDir(null), "glitch-review").apply { mkdirs() }
        val wanted = InstrumentationRegistry.getArguments().getString("worlds")?.split(',')?.map { it.trim() }
        val app = context.applicationContext as WaypointApplication

        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))

        fun mapView(): MapView? {
            var found: MapView? = null
            instrumentation.runOnMainSync {
                val activity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                found = activity?.window?.decorView?.let { findMapView(it) }
            }
            return found
        }
        fun camera(block: (org.maplibre.android.maps.MapLibreMap) -> Unit) {
            val view = mapView()
            assertNotNull("MapView", view)
            instrumentation.runOnMainSync { view!!.getMapAsync(block) }
        }
        fun shot(world: World, tag: String) {
            assertTrue(device.takeScreenshot(File(output, "${world.name.lowercase()}-$tag.png")))
        }
        fun zoomTo(world: World, z: Double, tag: String) {
            camera { it.moveCamera(CameraUpdateFactory.zoomTo(z)) }
            SystemClock.sleep(3200)
            shot(world, tag)
        }
        fun swipe(world: World, fx: Int, fy: Int, tx: Int, ty: Int, tag: String) {
            device.swipe(fx, fy, tx, ty, 20)  // fast enough to be a pan, not a long press that drops a pin
            SystemClock.sleep(3200)
            shot(world, tag)
        }

        for (world in World.entries.filter { wanted == null || it.name in wanted }) {
            instrumentation.runOnMainSync { app.navigation.changeWorld(world) }
            SystemClock.sleep(3500)
            device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
            SystemClock.sleep(5000)
            shot(world, "a-locate")
            // One real, fast swipe marks a user gesture; the follow policy then leaves the camera alone.
            device.swipe(540, 1100, 580, 1000, 6)
            SystemClock.sleep(800)
            for ((z, tag) in listOf(13.0 to "b-z13", 11.0 to "c-z11", 9.0 to "d-z9", 7.0 to "e-z7", 5.0 to "f-z5", 3.0 to "g-z3")) zoomTo(world, z, tag)
            zoomTo(world, 11.0, "h-z11")
            swipe(world, 800, 1500, 250, 800, "i-pan1")
            swipe(world, 250, 700, 900, 1500, "j-pan2")
            zoomTo(world, 15.0, "k-street")
        }
    }
}
