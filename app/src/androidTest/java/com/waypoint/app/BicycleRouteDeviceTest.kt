package com.waypoint.app

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Explicit live-provider contract check, using public London landmarks, never device GPS. */
class BicycleRouteDeviceTest {
    @Test fun liveCyclingRouteIsUsableByNativeGuidance() = runBlocking {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".preview"))
        val route=withTimeout(45_000) {
            MapRepository().route(Coordinate(51.5079,-.1684),Coordinate(51.5033,-.1195),TravelMode.BICYCLE)
        }
        assertEquals(TravelMode.BICYCLE,route.mode)
        assertTrue(route.meters>1000 && route.seconds>60)
        assertTrue(route.points.all { it.isValid() })
        assertNotNull("Cycling response must parse into the real guidance engine",route.ferrostarRoute)
        assertEquals(route.points.size,route.ferrostarRoute!!.geometry.size)
        assertEquals(route.steps.size,route.ferrostarRoute!!.steps.size)
        InstrumentationRegistry.getInstrumentation().sendStatus(2,android.os.Bundle().apply {
            putString("stream","\nPublic London cycling fixture: ${route.meters} m, ${route.seconds} s, ${route.steps.size} steps\n")
        })
    }
}
