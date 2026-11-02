package com.waypoint.app

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import uniffi.ferrostar.BoundingBox
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

/** Drives production location handling in isolation. Never replaces the phone's GPS provider. */
class NavigationJourneyTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val a=Coordinate(51.5000,-.1200)
    private val b=Coordinate(51.5000,-.1100)
    private val c=Coordinate(51.5100,-.1100)
    private fun road(points:List<Coordinate>):RoadRoute {
        val shape=points.map { GeographicCoordinate(it.lat,it.lon) }
        val steps=points.mapIndexed { index,point ->
            val last=index==points.lastIndex
            Maneuver(point,if(index==0)"depart" else if(last)"arrive" else "turn",if(last)"" else "left","Demo Road",
                if(last)0.0 else 1000.0,if(last)0.0 else 90.0)
        }
        val ferroSteps=points.mapIndexed { index,_ ->
            val last=index==points.lastIndex
            RouteStep(if(last)listOf(shape.last(),shape.last()) else shape.subList(index,index+2),
                if(last)0.0 else 1000.0,if(last)0.0 else 90.0,
                if(last)null else "Demo Road",emptyList(),steps[index].instruction,emptyList(),emptyList(),null,emptyList(),null,null)
        }
        val ferro=Route(shape,BoundingBox(
            GeographicCoordinate(points.minOf { it.lat },points.minOf { it.lon }),
            GeographicCoordinate(points.maxOf { it.lat },points.maxOf { it.lon })),
            2000.0,listOf(Waypoint(shape.first(),WaypointKind.BREAK,null),Waypoint(shape.last(),WaypointKind.BREAK,null)),ferroSteps)
        return RoadRoute(points,steps,2000.0,180.0,TravelMode.MOTORCYCLE,ferro)
    }
    private class FakeRepository(val routeFrom:(Coordinate)->RoadRoute):NavigationRepository {
        var calls=0;var failure=false
        override suspend fun search(query:String,near:Coordinate)=emptyList<Place>()
        override suspend fun reverse(point:Coordinate)="Test area"
        override suspend fun route(from:Coordinate,to:Coordinate,mode:TravelMode):RoadRoute {
            calls++;delay(200)
            if(failure)throw java.io.IOException("Simulated network outage")
            return routeFrom(from)
        }
    }
    @Suppress("UNCHECKED_CAST")
    private fun state(controller:NavigationController)=field(controller,"_state") as MutableStateFlow<NavState>
    private fun field(controller:NavigationController,name:String)=controller.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(controller)
    private fun set(controller:NavigationController,name:String,value:Any){controller.javaClass.getDeclaredField(name).apply{isAccessible=true}.set(controller,value)}
    private fun awaitCondition(timeoutMs:Long=5000,condition:()->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeoutMs
        while(!condition() && SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50)
        assertTrue("Ferrostar state did not advance in time",condition())
    }
    private fun fix(controller:NavigationController,point:Coordinate,ageMs:Long=0,accuracy:Float=4f) {
        inst.runOnMainSync {
            val location=Location("gps").apply { latitude=point.lat;longitude=point.lon;this.accuracy=accuracy;speed=8f;bearing=90f;elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()-ageMs*1_000_000;time=System.currentTimeMillis()-ageMs }
            controller.javaClass.getDeclaredMethod("receiveLocation",Location::class.java).apply{isAccessible=true}.invoke(controller,location)
        }
    }
    private fun fixture(block:(NavigationController,FakeRepository)->Unit) {
        val app=inst.targetContext.applicationContext as Application
        val prefs=app.getSharedPreferences("journey-test",0);prefs.edit().clear().putBoolean("voice",false).commit()
        val repository=FakeRepository { from -> road(listOf(from,Coordinate(from.lat,-.1080),c)) }
        lateinit var controller:NavigationController
        inst.runOnMainSync {
            controller=NavigationController(app,repository,"journey-test",null)
            set(controller,"listening",true);set(controller,"platformListening",true)
            state(controller).value=NavState(fix=a,accuracy=4f,gpsReady=true,route=road(listOf(a,b,c)),selected=Place("demo","Demo destination","Synthetic test",c),mode=TravelMode.MOTORCYCLE,voice=false)
            set(controller,"routeOrigin",a)
        }
        fix(controller,a)
        inst.runOnMainSync { controller.startNavigation() }
        awaitCondition { controller.state.value.progress!=null }
        try { block(controller,repository) } finally {
            inst.runOnMainSync {
                controller.stopNavigation()
                set(controller,"listening",false)
                (field(controller,"scope") as CoroutineScope).cancel()
                (field(controller,"guidance") as FerrostarGuidance).shutdown()
            }
            prefs.edit().clear().commit()
        }
    }
    @Test fun movementMissedTurnAndReplacementRouteReachState()=fixture { controller,repository ->
        fix(controller,Coordinate(a.lat,-.1180));awaitCondition { (controller.state.value.progress?.traveled ?: 0.0)>50 };val first=controller.state.value
        fix(controller,Coordinate(a.lat,-.1160));awaitCondition { (controller.state.value.progress?.traveled ?: 0.0)>first.progress!!.traveled };val moved=controller.state.value
        assertNotEquals(first.fix,moved.fix);assertTrue(moved.progress!!.traveled>first.progress!!.traveled)
        val oldRoute=moved.route
        fix(controller,Coordinate(a.lat-.001,-.1150))
        SystemClock.sleep(150)
        fix(controller,Coordinate(a.lat-.001,-.1150))
        awaitCondition { repository.calls>0 }
        awaitCondition { controller.state.value.route!=oldRoute }
        val rerouted=controller.state.value
        assertEquals(1,repository.calls);assertNotEquals(oldRoute,rerouted.route)
        assertEquals(rerouted.fix,rerouted.route!!.points.first());assertTrue(rerouted.navigating)
        assertEquals(TravelMode.MOTORCYCLE,rerouted.route.mode);assertFalse(rerouted.routing);assertFalse(rerouted.offRoute)
        val next=Coordinate(rerouted.fix!!.lat,-.1130);fix(controller,next)
        assertEquals(next,controller.state.value.fix)
        awaitCondition { (controller.state.value.progress?.traveled ?: 0.0)>50 }
    }
    @Test fun outdatedAndInaccurateFixesCannotTriggerRouteReplacement()=fixture { controller,repository ->
        fix(controller,Coordinate(a.lat,-.1170));val accepted=controller.state.value.fix
        fix(controller,Coordinate(a.lat+.1,-.1170),ageMs=20000)
        assertEquals(accepted,controller.state.value.fix)
        repeat(4) { fix(controller,Coordinate(a.lat+.002,-.1170),accuracy=80f);SystemClock.sleep(50) }
        assertFalse(controller.state.value.gpsReady);assertEquals(0,repository.calls)
    }
    @Test fun failedRerouteRetainsDestinationAndRetriesAfterBackoff()=fixture { controller,repository ->
        repository.failure=true
        fix(controller,Coordinate(a.lat-.001,-.1180))
        SystemClock.sleep(150)
        fix(controller,Coordinate(a.lat-.001,-.1180))
        awaitCondition { repository.calls==1 && controller.state.value.routeError!=null }
        assertNotNull(controller.state.value.routeError);assertEquals("demo",controller.state.value.selected!!.id)
        repeat(3) { fix(controller,Coordinate(a.lat-.001,-.1180));SystemClock.sleep(100) }
        assertEquals(1,repository.calls)
        repository.failure=false
        SystemClock.sleep(5500);fix(controller,Coordinate(a.lat-.002,-.1180))
        awaitCondition { repository.calls==2 && controller.state.value.routeError==null }
        assertEquals(2,repository.calls);assertNull(controller.state.value.routeError);assertTrue(controller.state.value.navigating)
    }
    @Test fun failedPreviewRecomputeDoesNotDrawAStaleRoute()=fixture { controller,repository ->
        inst.runOnMainSync {
            controller.stopNavigation(clear=false)
            state(controller).value=controller.state.value.copy(fix=Coordinate(35.6895,139.6917),gpsReady=true)
            repository.failure=true
            controller.requestRoute()
        }
        assertNull("The old preview line must disappear during replacement",controller.state.value.route)
        awaitCondition { controller.state.value.routeError!=null }
        assertNull("A failed replacement must not restore the old route",controller.state.value.route)
        assertEquals("demo",controller.state.value.selected?.id)
    }
}
