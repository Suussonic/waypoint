package com.waypoint.app

import android.app.ActivityManager
import android.location.Location
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in: a real DHU operator presses Start/End; only the route and GPS input are simulated. */
class CarHostJourneyTest {
    @Suppress("UNCHECKED_CAST")
    @Test fun carStartsAndEndsSharedGuidanceWhilePhoneIsBackgrounded() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hostJourney")=="true")
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val controller=(context.applicationContext as WaypointApplication).navigation
        val state=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        assumeTrue("End any existing Preview trip before this fixture",!original.navigating)
        val enabled=NavigationController::class.java.getDeclaredField("enabled").apply { isAccessible=true }
        val wasEnabled=enabled.getBoolean(controller)
        val originField=NavigationController::class.java.getDeclaredField("routeOrigin").apply { isAccessible=true }
        val locationField=NavigationController::class.java.getDeclaredField("lastAcceptedLocation").apply { isAccessible=true }
        val oldOrigin=originField.get(controller)
        val oldLocation=locationField.get(controller)
        val prefs=context.getSharedPreferences("waypoint",0)
        val recent=prefs.getString("recent",null)
        val start=Coordinate(51.5081,-0.1280)
        val end=Coordinate(51.5014,-0.1419)
        val route=runBlocking { withTimeout(30_000) { MapRepository().route(start,end) } }
        fun report(text:String)=inst.sendStatus(2,android.os.Bundle().apply { putString("stream","$text\n") })
        fun await(label:String,timeout:Long=120_000,predicate:()->Boolean) {
            val until=SystemClock.elapsedRealtime()+timeout
            while(!predicate() && SystemClock.elapsedRealtime()<until)SystemClock.sleep(200)
            assertTrue(label,predicate())
        }
        @Suppress("DEPRECATION")
        fun foregroundRunning()=context.getSystemService(ActivityManager::class.java).getRunningServices(100).any { it.service.className==NavigationService::class.java.name && it.foreground }
        try {
            report("Open Waypoint Preview on the DHU.")
            await("Real car session connected") { state.value.carConnected }
            UiDevice.getInstance(inst).pressHome()
            inst.runOnMainSync {
                NavigationController::class.java.getDeclaredMethod("stopTracking").apply { isAccessible=true }.invoke(controller)
                enabled.setBoolean(controller,false)
                originField.set(controller,start)
                locationField.set(controller,Location("fixture").apply {
                    latitude=start.lat;longitude=start.lon;accuracy=3f;time=System.currentTimeMillis();elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()
                })
                state.value=state.value.copy(world=World.DIVISION,mode=TravelMode.CAR,fix=start,accuracy=3f,gpsReady=true,
                    city="London · simulation",gpsStatus="Simulated drive",selected=Place("host-journey","Buckingham Palace","London · simulation",end),
                    route=route,progress=null,navigating=false,routing=false,routeError=null,voice=false)
                controller.enableAutoDrive()
            }
            report("Fixture ready. Press the actual car Start action.")
            await("Car Start starts shared guidance") { state.value.navigating }
            await("Ferrostar produces progress",30_000) { state.value.progress!=null }
            await("Navigation service entered foreground",10_000) { foregroundRunning() }
            val initial=state.value.progress!!.traveled
            await("Simulated journey advances",30_000) { (state.value.progress?.traveled ?: initial)>initial+5 }
            report("START, foreground service and advancing Ferrostar progress verified. Press End on the car.")
            await("Car End stops guidance") { !state.value.navigating }
            await("Navigation foreground service removed",10_000) { !foregroundRunning() }
            report("END and foreground service removal verified.")
        } finally {
            inst.runOnMainSync {
                controller.stopNavigation();controller.leaveCarSession()
                enabled.setBoolean(controller,wasEnabled)
                originField.set(controller,oldOrigin)
                locationField.set(controller,oldLocation)
                state.value=original.copy(carConnected=state.value.carConnected,navigating=false)
                controller.refreshCarLocation()
            }
            prefs.edit().apply { if(recent==null)remove("recent") else putString("recent",recent) }.commit()
        }
    }
}
