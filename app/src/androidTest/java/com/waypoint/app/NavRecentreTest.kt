package com.waypoint.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** While guiding, panning the map away shows "Re-centre"; tapping it hides the pill and follows again. */
@RunWith(AndroidJUnit4::class)
class NavRecentreTest {
    @Test fun panShowsRecentreAndTapReturns() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app = context.applicationContext as WaypointApplication
        val place = Place("test:station", "Barrackpore Railway Station", "Barrackpore", Coordinate(22.7637, 88.3743), "place")
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        instrumentation.runOnMainSync { app.navigation.clearDestination(); app.navigation.changeWorld(World.LOS_SANTOS) }
        SystemClock.sleep(3000)
        device.findObject(By.desc("Use my location").pkg(context.packageName))?.click()
        SystemClock.sleep(3500)
        instrumentation.runOnMainSync { app.navigation.select(place) }
        var waited = 0
        while (app.navigation.state.value.route == null && waited < 25000) { SystemClock.sleep(500); waited += 500 }
        assertNotNull("Route", app.navigation.state.value.route)
        val start = app.navigation.state.value.route!!.points.first()
        app.navigation.testLocationsOnly = true
        try {
            fun fix() = app.navigation.injectTestLocation(android.location.Location("gps").apply {
                latitude = start.lat; longitude = start.lon; accuracy = 5f
                time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
            fix(); SystemClock.sleep(800); fix()
            instrumentation.runOnMainSync { app.navigation.startNavigation() }
            SystemClock.sleep(3000)
            assertNull("No pill while following", device.findObject(By.desc("Re-centre on my position")))
            device.swipe(800, 1500, 250, 700, 12)
            SystemClock.sleep(1500)
            val pill = device.wait(Until.findObject(By.desc("Re-centre on my position")), 4000)
            assertNotNull("Pill after panning", pill)
            pill.click()
            SystemClock.sleep(1500)
            assertTrue("Pill gone after tap", device.wait(Until.gone(By.desc("Re-centre on my position")), 4000))
        } finally {
            app.navigation.testLocationsOnly = false
            instrumentation.runOnMainSync { app.navigation.stopNavigation() }
        }
    }
}
