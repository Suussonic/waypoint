package com.waypoint.app

import android.graphics.Rect
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.SymbolLayer

/** Exercise the actual projected-map renderer; a stationary style switch must redraw its marker. */
class CarMapRenderingTest {
    @Test fun vehicleAndFollowingCameraRotateTogetherThroughATurn() {
        val inst=InstrumentationRegistry.getInstrumentation()
        check(inst.targetContext.packageName.endsWith(".preview"))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var view:WaypointCarLiveMap
            scenario.onActivity { activity ->
                view=WaypointCarLiveMap(activity,WaypointCarOverlay(activity))
                activity.setContentView(view);view.start()
            }
            val mapField=WaypointCarLiveMap::class.java.getDeclaredField("map").apply { isAccessible=true }
            val worldField=WaypointCarLiveMap::class.java.getDeclaredField("loadedWorld").apply { isAccessible=true }
            // A synthetic U-turn around public Trafalgar Square, deliberately
            // rounded to four decimals; no device-derived positions are stored.
            val points=listOf(Coordinate(51.5081,-.1272),Coordinate(51.5083,-.1273),
                Coordinate(51.5085,-.1273),Coordinate(51.5086,-.1272),
                Coordinate(51.5086,-.1271),Coordinate(51.5085,-.1271),Coordinate(51.5081,-.1271))
            val route=RoadRoute(points,emptyList(),120.0,30.0)
            try {
                for(index in 0 until points.lastIndex) {
                    val progress=RouteProgress(index*10.0,120.0-index*10,30.0,0.0,0,20.0,points[index],segmentIndex=index)
                    val base=NavState(world=World.STALKER,mode=TravelMode.CAR,fix=points[index],route=route,
                        navigating=true,progress=progress,gpsReady=true)
                    val course=carTravelBearing(base).toFloat()
                    val state=base.copy(headingReliable=true,headingSource=HeadingSource.COURSE,bearing=course,speed=8f)
                    var ready=false
                    val deadline=SystemClock.elapsedRealtime()+15_000
                    while(!ready && SystemClock.elapsedRealtime()<deadline) {
                        scenario.onActivity {
                            val w=view.width.coerceAtLeast(1);val h=view.height.coerceAtLeast(1)
                            view.update(state,carCamera(state,w,h),Rect(0,0,w,h),false,true)
                            ready=worldField.get(view)==World.STALKER
                        }
                        if(!ready)SystemClock.sleep(150)
                    }
                    assertTrue("Map style ready",ready)
                    // GPS cadence controls the duration; do not assume a half-second step.
                    var settled=false
                    val settlingDeadline=SystemClock.elapsedRealtime()+2000
                    while(!settled && SystemClock.elapsedRealtime()<settlingDeadline) {
                        scenario.onActivity {
                            val map=mapField.get(view) as MapLibreMap
                            val rotation=map.style!!.getLayerAs<SymbolLayer>("car-position-marker")!!.iconRotate.value!!
                            assertEquals("Vehicle and camera stay aligned during the turn",0f,
                                headingDelta(rotation,map.cameraPosition.bearing.toFloat()),1f)
                            settled=kotlin.math.abs(headingDelta(course,rotation))<.5f
                        }
                        SystemClock.sleep(50)
                    }
                    assertTrue("Turn settles at the requested course",settled)
                    SystemClock.sleep(1000)
                    scenario.onActivity {
                        val map=mapField.get(view) as MapLibreMap
                        val rotation=map.style!!.getLayerAs<SymbolLayer>("car-position-marker")!!.iconRotate.value!!
                        assertEquals("Vehicle follows road segment $index",0f,headingDelta(course,rotation),1f)
                        assertEquals("Camera follows the same course $index",0f,headingDelta(course,map.cameraPosition.bearing.toFloat()),1f)
                        val screen=map.projection.toScreenLocation(LatLng(points[index].lat,points[index].lon))
                        assertTrue("Vehicle remains at current position",map.queryRenderedFeatures(screen,"car-position-marker").isNotEmpty())
                    }
                }
            } finally { scenario.onActivity { view.destroy() } }
        }
    }

    @Test fun stationaryVehicleSurvivesWorldAndTravelModeChanges() {
        val inst=InstrumentationRegistry.getInstrumentation()
        check(inst.targetContext.packageName.endsWith(".preview"))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var view:WaypointCarLiveMap
            scenario.onActivity { activity ->
                view=WaypointCarLiveMap(activity,WaypointCarOverlay(activity))
                activity.setContentView(view)
                view.start()
            }
            try {
                val mapField=WaypointCarLiveMap::class.java.getDeclaredField("map").apply { isAccessible=true }
                val worldField=WaypointCarLiveMap::class.java.getDeclaredField("loadedWorld").apply { isAccessible=true }
                val point=Coordinate(51.5081,-0.1280)
                for((world,mode) in listOf(World.DIVISION to TravelMode.CAR,World.STALKER to TravelMode.CAR,
                    World.OLD_WEST to TravelMode.CAR,World.PIXEL to TravelMode.CAR,World.PIXEL to TravelMode.MOTORCYCLE,World.NIER to TravelMode.CAR,
                    World.NIER to TravelMode.MOTORCYCLE,World.NIER to TravelMode.BICYCLE)) {
                    val state=NavState(world=world,mode=mode,fix=point,gpsReady=true,headingReliable=true,bearing=45f)
                    var rendered=false
                    val deadline=SystemClock.elapsedRealtime()+15_000
                    while(!rendered && SystemClock.elapsedRealtime()<deadline) {
                        scenario.onActivity {
                            val width=view.width.coerceAtLeast(1);val height=view.height.coerceAtLeast(1)
                            view.update(state,carCamera(state,width,height),Rect(0,0,width,height),true,true)
                            val map=mapField.get(view) as? MapLibreMap
                            if(map!=null && worldField.get(view)==world) {
                                val screen=map.projection.toScreenLocation(LatLng(point.lat,point.lon))
                                rendered=map.queryRenderedFeatures(screen,"car-position-marker").isNotEmpty()
                            }
                        }
                        if(!rendered)SystemClock.sleep(150)
                    }
                    assertTrue("${world.name} $mode stationary marker rendered at its actual coordinate",rendered)
                }
            } finally {
                scenario.onActivity { view.destroy() }
            }
        }
    }
}
