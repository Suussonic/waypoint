package com.waypoint.app

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in public DHU capture fixture. No device position or personal destination is used. */
@RunWith(AndroidJUnit4::class)
class CarReadmeCaptureTest {
    @Suppress("UNCHECKED_CAST")
    @Test fun showPublicLondonRouteOnHeadUnit() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("captureReadmeCar")=="true")
        val start=Coordinate(51.5081,-0.1280)
        val destination=Coordinate(51.5014,-0.1419)
        val route=runBlocking { withTimeout(30_000) { MapRepository().route(start,destination) } }
        val sample=route.points[(route.points.size/5).coerceAtLeast(1)]
        val progress=NavigationEngine(route).update(sample)
        val controller=(instrumentation.targetContext.applicationContext as WaypointApplication).navigation
        val stateField=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }
        val state=stateField.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        val place=Place("readme-buckingham-palace","Buckingham Palace","London, UK",destination)
        try {
            instrumentation.runOnMainSync {
                state.value=NavState(world=World.OLD_WEST,fix=sample,accuracy=5f,bearing=215f,
                    headingReliable=true,gpsReady=true,gpsStatus="Simulated drive",city="London · demonstration",
                    selected=place,route=route)
            }
            Log.i("CarReadmeCapture","OLD_WEST_PREVIEW_READY")
            SystemClock.sleep(20_000)
            instrumentation.runOnMainSync {
                state.value=state.value.copy(navigating=true,progress=progress,speed=8f)
                controller.enableAutoDrive()
            }
            Log.i("CarReadmeCapture","OLD_WEST_GUIDANCE_READY")
            SystemClock.sleep(25_000)
            instrumentation.runOnMainSync { state.value=state.value.copy(world=World.NIGHT_CITY) }
            Log.i("CarReadmeCapture","NIGHT_CITY_GUIDANCE_READY")
            SystemClock.sleep(25_000)
        } finally {
            instrumentation.runOnMainSync {
                controller.leaveCarSession()
                state.value=original
            }
        }
    }
}
