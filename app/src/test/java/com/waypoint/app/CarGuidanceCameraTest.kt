package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarGuidanceCameraTest {
    @Test fun guidanceKeepsVehicleAnchoredWhenTheRoadAheadBends() {
        val fix=Coordinate(51.5081,-.1280)
        val route=RoadRoute(listOf(fix,Coordinate(51.5086,-.1280),Coordinate(51.5086,-.126)),emptyList(),200.0,60.0)
        val camera=carCamera(NavState(fix=fix,route=route,navigating=true),1280,720,
            CarMapViewport(0,0,1100,600))
        assertEquals(fix.lat,camera.target!!.latitude,0.000001)
        assertEquals(fix.lon,camera.target!!.longitude,0.000001)
        assertEquals(CAR_NAVIGATION_TILT,camera.tilt,0.001)
        val padding=camera.padding!!
        val anchorX=(padding[0]+1280-padding[2])/2
        val anchorY=(padding[1]+720-padding[3])/2
        assertTrue("Driver must remain within unobscured map",anchorX in 100.0..1000.0 && anchorY in 350.0..550.0)
    }

    @Test fun stationaryGuidanceKeepsPitchEvenWhenBrowseMapPreferenceIsFlat() {
        val fix=Coordinate(51.5081,-.1280)
        val route=RoadRoute(listOf(fix,Coordinate(51.5081,-.1270)),emptyList(),70.0,20.0)
        val camera=carCamera(NavState(fix=fix,route=route,navigating=true,speed=0f,tilted=false),1280,720)
        assertEquals(CAR_NAVIGATION_TILT,camera.tilt,0.001)
    }

    @Test fun androidAutoKeepsTheSameRoadPitchAtAStopWhenZoomingOutForASplitPaneTurn() {
        val fix=Coordinate(0.0,0.0)
        val turn=Coordinate(0.0,.45)
        val route=RoadRoute(listOf(fix,turn,Coordinate(.1,.45)),
            listOf(Maneuver(turn,"turn","left","Road ahead",50_000.0,900.0)),55_000.0,900.0)
        val state=NavState(fix=fix,route=route,navigating=true,speed=0f,tilted=false,
            progress=RouteProgress(0.0,55_000.0,900.0,0.0,0,50_000.0,fix))
        val full=carCamera(state,1280,720)
        val split=carCamera(state,1280,720,CarMapViewport(0,0,448,720))
        assertTrue("the split pane still pulls back for a distant turn",split.zoom<full.zoom)
        assertEquals(CAR_NAVIGATION_TILT,full.tilt,0.001)
        assertEquals(CAR_NAVIGATION_TILT,split.tilt,0.001)
    }

    @Test fun guidanceFacesRoadAheadUnlessNorthUpWasSelected() {
        val fix=Coordinate(0.0,0.0)
        val route=RoadRoute(listOf(fix,Coordinate(0.0,.001)),emptyList(),111.0,20.0)
        val state=NavState(fix=fix,route=route,navigating=true,speed=0f,tilted=false)
        assertEquals(90.0,carCamera(state,1280,720).bearing,0.01)
        assertEquals(0.0,carCamera(state.copy(keepMapNorthUp=true),1280,720).bearing,0.01)
    }

    @Test fun narrowerHostMapAreaZoomsOutToPreserveBothRouteEndpoints() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.02)),emptyList(),2200.0,300.0)
        val state=NavState(route=route)
        val full=carCamera(state,1280,720)
        val panel=carCamera(state,1280,720,CarMapViewport(500,0,1280,720))
        assertTrue(panel.zoom<full.zoom)
        assertEquals(full.target,panel.target)
        val invalid=carCamera(state,1280,720,CarMapViewport(900,500,700,200))
        assertEquals(full.zoom,invalid.zoom,0.001)
    }

    @Test fun guidanceCameraPullsBackForSpeedAndPreviewsARoundabout() {
        val start=Coordinate(51.5081,-.1280)
        val roundabout=Coordinate(51.5027,-.1525)
        val route=RoadRoute(
            listOf(start,Coordinate(51.5060,-.1400),roundabout,Coordinate(51.5018,-.1533)),
            listOf(
                Maneuver(start,"depart","straight","Demo Road",0.0,0.0),
                Maneuver(roundabout,"roundabout","right","Demo Circle",1500.0,90.0,exit=2),
                Maneuver(Coordinate(51.5018,-.1533),"turn","left","Exit Road",80.0,20.0),
            ),1580.0,120.0)
        val far=NavState(fix=start,route=route,navigating=true,mode=TravelMode.CAR,speed=12f,
            progress=RouteProgress(0.0,1580.0,120.0,0.0,1,1500.0,start))
        val farCamera=carCamera(far,1280,720,density=1.5)
        val straight=far.copy(route=route.copy(steps=emptyList()))
        val slowCamera=carCamera(straight,1280,720,density=1.5)
        val fastCamera=carCamera(straight.copy(speed=25f),1280,720,density=1.5)
        val nearFix=Coordinate(51.5032,-.1520)
        val near=far.copy(fix=nearFix,progress=far.progress!!.copy(traveled=1450.0,remaining=130.0,
            toTurn=130.0,snapped=nearFix))
        val nearCamera=carCamera(near,1280,720,density=1.5)
        assertTrue("speed eases the projected camera out",fastCamera.zoom<slowCamera.zoom)
        assertTrue("a distant roundabout opens the view",farCamera.zoom<nearCamera.zoom)
        assertEquals(CAR_NAVIGATION_TILT,farCamera.tilt,0.001)
        assertEquals(CAR_NAVIGATION_TILT,slowCamera.tilt,0.001)
        assertEquals(CAR_NAVIGATION_TILT,fastCamera.tilt,0.001)
        assertEquals(CAR_NAVIGATION_TILT,nearCamera.tilt,0.001)
        assertEquals(start.lat,farCamera.target!!.latitude,0.000001)
        assertEquals(start.lon,farCamera.target!!.longitude,0.000001)
    }

    @Test fun routeAheadSetsDirectionUntilDrivingCourseIsReliable() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.0002),Coordinate(.0002,.0002)),emptyList(),45.0,60.0)
        val state=NavState(route=route,navigating=true,headingReliable=false)
        assertEquals(90.0,carTravelBearing(state),0.01)
        assertEquals(270.0,carTravelBearing(state.copy(headingReliable=true,headingSource=HeadingSource.COURSE,speed=3f,bearing=270f)),0.01)
    }

    @Test fun androidAutoKeepsArbitratedCourseThroughTheSlowdownHysteresisBand() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.0002)),emptyList(),22.2,20.0)
        val state=NavState(route=route,navigating=true,headingReliable=true,
            headingSource=HeadingSource.COURSE,speed=1.8f,bearing=270f)
        assertEquals(270.0,carTravelBearing(state),0.01)
    }

    @Test fun vehicleFollowsCurrentSegmentThroughATightUTurnInsteadOfCuttingTheCorner() {
        val points=listOf(Coordinate(0.0,0.0),Coordinate(.00004,0.0),Coordinate(.00004,.00003),Coordinate(-.0001,.00003))
        val route=RoadRoute(points,emptyList(),25.0,10.0)
        val state=NavState(route=route,navigating=true,headingReliable=false)
        assertEquals(0.0,carTravelBearing(state),.01)
        val atTurn=RouteProgress(4.4,20.0,10.0,0.0,0,0.0,points[1],segmentIndex=1)
        assertEquals(90.0,carTravelBearing(state.copy(progress=atTurn)),.01)
        assertEquals(180.0,carTravelBearing(state.copy(progress=atTurn.copy(snapped=points[2],segmentIndex=2))),.01)
    }

    @Test fun uTurnDirectionUsesArcThenDrivingSideForAmbiguousBearings() {
        assertTrue(isRightUTurn(347.0,53.0,"right"))
        assertEquals(false,isRightUTurn(13.0,307.0,"left"))
        assertTrue(isRightUTurn(0.0,180.0,"left"))
        assertEquals(false,isRightUTurn(0.0,180.0,"right"))
        assertEquals(false,isRightUTurn(Double.NaN,Double.POSITIVE_INFINITY,null))
        assertEquals(false,isRightUTurn(-1.0,700.0,"right"))
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
