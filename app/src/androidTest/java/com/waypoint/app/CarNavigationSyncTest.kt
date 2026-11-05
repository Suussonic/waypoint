package com.waypoint.app

import android.Manifest
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.navigation.TestNavigationManager
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class CarNavigationSyncTest {
    @Suppress("UNCHECKED_CAST")
    @Test fun hostReceivesStartProgressStopAndReconnectWithoutStoppingPhoneGuidance() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        inst.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        inst.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        val controller = (context.applicationContext as WaypointApplication).navigation
        val state = NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }.get(controller) as MutableStateFlow<NavState>
        val original = state.value
        val start = Coordinate(51.5000,-0.1200)
        val end = Coordinate(51.5050,-0.1150)
        val place = Place("car-sync-test","Test destination","Public test fixture",end)
        val route = RoadRoute(listOf(start,end),listOf(Maneuver(start,"turn","right","Test road",800.0,90.0)),800.0,90.0)
        val progress = RouteProgress(100.0,700.0,80.0,0.0,0,100.0,start)
        lateinit var lifecycle: ScreenController
        lateinit var manager: TestNavigationManager
        fun connect() = inst.runOnMainSync {
            val car = TestCarContext.createCarContext(context)
            manager = car.getCarService(TestNavigationManager::class.java)
            lifecycle = ScreenController(WaypointCarScreen(car))
            lifecycle.moveToState(Lifecycle.State.RESUMED)
        }
        inst.runOnMainSync { state.value=NavState(selected=place,route=route,fix=start,gpsReady=true) }
        connect()
        try {
            inst.runOnMainSync { state.value=state.value.copy(navigating=true,progress=progress) }
            inst.waitForIdleSync()
            inst.runOnMainSync {
                assertEquals(1,manager.navigationStartedCount)
                assertTrue(manager.tripsSent.isNotEmpty())
                assertEquals("Test destination",manager.tripsSent.last().destinations.first().name.toString())
            }
            inst.runOnMainSync { state.value=state.value.copy(progress=progress.copy(remaining=500.0,toTurn=50.0,seconds=60.0)) }
            inst.waitForIdleSync()
            inst.runOnMainSync {
                assertEquals(1,manager.navigationStartedCount)
                assertTrue(manager.tripsSent.size >= 2)
                state.value=state.value.copy(navigating=false)
            }
            inst.waitForIdleSync()
            inst.runOnMainSync {
                assertEquals(1,manager.navigationEndedCount)
                state.value=state.value.copy(navigating=true)
            }
            inst.waitForIdleSync()
            inst.runOnMainSync {
                assertEquals(2,manager.navigationStartedCount)
                lifecycle.moveToState(Lifecycle.State.DESTROYED)
                assertTrue("Disconnect must leave phone guidance running",state.value.navigating)
                assertEquals(2,manager.navigationEndedCount)
                assertNull(manager.navigationManagerCallback)
            }
            connect()
            inst.waitForIdleSync()
            inst.runOnMainSync {
                assertEquals("Reconnect must publish current guidance",1,manager.navigationStartedCount)
                assertTrue(manager.tripsSent.isNotEmpty())
            }
        } finally {
            inst.runOnMainSync {
                lifecycle.moveToState(Lifecycle.State.DESTROYED)
                state.value=original
            }
        }
    }
}
