package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarGuidanceCameraTest {
    @Test fun guidanceZoomOpensRoadAheadAtSpeedAndClosesOnTurns() {
        assertEquals(17.1,carGuidanceZoom(0f,1000.0),0.001)
        assertEquals(16.6,carGuidanceZoom(10f,1000.0),0.001)
        assertEquals(16.1,carGuidanceZoom(20f,1000.0),0.001)
        assertEquals(15.6,carGuidanceZoom(30f,1000.0),0.001)
        assertEquals(17.3,carGuidanceZoom(30f,70.0),0.001)
    }

    @Test fun routeAheadSetsDirectionUntilDrivingCourseIsReliable() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.0002),Coordinate(.0002,.0002)),emptyList(),45.0,60.0)
        val state=NavState(route=route,navigating=true,headingReliable=false)
        assertEquals(90.0,carTravelBearing(state),0.01)
        assertEquals(270.0,carTravelBearing(state.copy(headingReliable=true,headingSource=HeadingSource.COURSE,speed=3f,bearing=270f)),0.01)
    }

    @Test fun cameraLooksAlongRemainingRoadWithoutJumpingToDestination() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.01)),emptyList(),1112.0,120.0)
        val ahead=carLookAhead(NavState(route=route,navigating=true))!!
        assertEquals(0.0,ahead.lat,0.000001)
        assertEquals(70.0,distance(route.points.first(),ahead),1.0)
        val faster=carLookAhead(NavState(route=route,navigating=true,speed=30f))!!
        assertEquals(90.0,distance(route.points.first(),faster),1.0)
        val turn=RouteProgress(0.0,1112.0,120.0,0.0,0,100.0,route.points.first())
        val approaching=carLookAhead(NavState(route=route,navigating=true,speed=30f,progress=turn))!!
        assertEquals(35.0,distance(route.points.first(),approaching),1.0)
    }

    @Test fun routeCrossingDateLineKeepsTheOverviewAndLookAheadNearby() {
        val start=Coordinate(0.0,179.999)
        val end=Coordinate(0.0,-179.999)
        val route=RoadRoute(listOf(start,end),emptyList(),222.0,30.0)
        val preview=carCamera(NavState(route=route,fix=start,selected=Place("test","Test","",end)),800,400)
        assertEquals(0.0,preview.tilt,0.001)
        assertEquals(180.0,kotlin.math.abs(preview.target!!.longitude),0.01)
        assertEquals(true,preview.zoom>10.0)
        val ahead=carLookAhead(NavState(route=route,navigating=true))!!
        assertEquals(70.0,distance(start,ahead),1.0)
        assertEquals(true,kotlin.math.abs(ahead.lon)>179.99)
    }

    @Test fun previewsStayLocalAcrossHemispheresAndDashboardSizes() {
        val journeys=listOf(
            Coordinate(35.6895,139.6917) to Coordinate(35.7095,139.7117), // Tokyo
            Coordinate(51.5072,-0.1276) to Coordinate(51.5272,-0.1076), // London
            Coordinate(-23.5505,-46.6333) to Coordinate(-23.5305,-46.6133), // São Paulo
            Coordinate(-33.8688,151.2093) to Coordinate(-33.8488,151.2293), // Sydney
            Coordinate(69.6492,18.9553) to Coordinate(69.6692,18.9753), // Tromsø
            Coordinate(-54.8019,-68.3030) to Coordinate(-54.7819,-68.2830), // Ushuaia
            Coordinate(-17.7134,179.9990) to Coordinate(-17.6934,-179.9810) // Date line
        )
        val displays=listOf(800 to 480,1280 to 720,1280 to 500,1920 to 1080)
        journeys.forEachIndexed { index,(start,end) ->
            val route=RoadRoute(listOf(start,end),emptyList(),distance(start,end),240.0)
            val state=NavState(fix=start,selected=Place("$index","Test destination","",end),route=route)
            displays.forEach { (width,height) ->
                val camera=carCamera(state,width,height)
                assertTrue("journey $index on $width×$height has invalid zoom",camera.zoom.isFinite() && camera.zoom in 3.0..17.0)
                assertEquals(0.0,camera.tilt,0.001)
                assertTrue("journey $index on $width×$height moved away from route",
                    kotlin.math.abs(camera.target!!.latitude-start.lat)<.03 &&
                    kotlin.math.abs(shortestLongitudeDelta(start.lon,camera.target!!.longitude))<.03)
            }
        }
    }
}
