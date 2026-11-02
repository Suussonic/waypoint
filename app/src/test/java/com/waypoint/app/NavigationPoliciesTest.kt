package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class NavigationPoliciesTest {
    @Test fun navigationActionExpiresWhenItsTripEnds() {
        val gate = NavigationSessionGate()
        val first = gate.begin()
        assertTrue(gate.accepts(first))
        gate.end()
        assertFalse(gate.accepts(first))
        val second = gate.begin()
        assertNotEquals(first, second)
        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(second))
        assertFalse(gate.accepts(null))
    }
    @Test fun carSurfaceBudgetRejectsMalformedAndOversizedDisplays() {
        assertTrue(validCarSurfaceSize(1920, 720, 240))
        assertTrue(validCarSurfaceSize(7680, 1080, 320))
        assertFalse(validCarSurfaceSize(7680, 4320, 320))
        assertFalse(validCarSurfaceSize(0, 480, 160))
        assertFalse(validCarSurfaceSize(Int.MAX_VALUE, Int.MAX_VALUE, 160))
        assertFalse(validCarSurfaceSize(8000, 8000, 320))
        assertFalse(validCarSurfaceSize(800, 480, 0))
    }
    private fun progress(away:Double=0.0,along:Double=100.0,projected:Double=along)=RouteProgress(along,1000.0,60.0,away,1,100.0,Coordinate(0.0,0.0),0,projected)
    @Test fun missedTurnTriggersAfterSustainedAccurateFixes() {
        val policy=ReroutePolicy()
        assertFalse(policy.observe(progress(35.0),4f,12f,TravelMode.MOTORCYCLE,0,false).request)
        assertFalse(policy.observe(progress(40.0),4f,12f,TravelMode.MOTORCYCLE,1000,false).request)
        assertTrue(policy.observe(progress(45.0),4f,12f,TravelMode.MOTORCYCLE,3000,false).request)
    }
    @Test fun threeBatchedSamplesDoNotTriggerAnImmediateRequest() {
        val policy=ReroutePolicy()
        repeat(3) { assertFalse(policy.observe(progress(60.0),4f,12f,TravelMode.CAR,it*10L,false).request) }
    }
    @Test fun inaccurateFixesAndStationaryJitterDoNotReroute() {
        val policy=ReroutePolicy()
        repeat(10) {
            assertFalse(policy.observe(progress(60.0),40f,12f,TravelMode.CAR,it*1000L,false).request)
            assertFalse(policy.observe(progress(8.0,100.0,90.0),4f,0f,TravelMode.CAR,it*1000L,false).offRoute)
        }
    }
    @Test fun drivingBackwardsAlongSameRoadTriggersReroute() {
        val policy=ReroutePolicy()
        for(t in listOf(0L,1000L,3000L)) {
            val d=policy.observe(progress(0.0,200.0,160.0),4f,5f,TravelMode.CAR,t,false)
            assertEquals(t==3000L,d.request)
        }
    }
    @Test fun successfulReturnToRouteClearsDeviation() {
        val policy=ReroutePolicy()
        policy.observe(progress(50.0),4f,10f,TravelMode.CAR,0,false)
        assertFalse(policy.observe(progress(),4f,10f,TravelMode.CAR,3000,false).offRoute)
        assertFalse(policy.observe(progress(50.0),4f,10f,TravelMode.CAR,4000,false).request)
    }
    @Test fun pendingRequestAndFailureBackoffPreventRequestStorms() {
        val policy=ReroutePolicy()
        for(t in 0L..3000L step 1000) policy.observe(progress(50.0),4f,10f,TravelMode.CAR,t,false)
        policy.requested(3000)
        assertFalse(policy.observe(progress(50.0),4f,10f,TravelMode.CAR,12000,true).request)
        policy.failed(12000)
        assertFalse(policy.observe(progress(50.0),4f,10f,TravelMode.CAR,19000,false).request)
        assertTrue(policy.observe(progress(50.0),4f,10f,TravelMode.CAR,20000,false).request)
    }
    @Test fun idleMapFollowsButDestinationOverviewDoesNot() {
        val p=MapFollowPolicy()
        assertTrue(p.shouldFollow(false,false,0));assertFalse(p.shouldFollow(false,true,0))
    }
    @Test fun navigationPanResumesEightSecondsAfterGestureEnds() {
        val p=MapFollowPolicy();p.gestureStarted(0)
        assertFalse(p.shouldFollow(true,true,15000))
        p.gestureEnded(15000)
        assertFalse(p.shouldFollow(true,true,22999));assertTrue(p.shouldFollow(true,true,23000))
    }
    @Test fun browsePanStaysPutUntilRecenter() {
        val p=MapFollowPolicy();p.gestureStarted(0);p.gestureEnded(100)
        assertFalse(p.shouldFollow(false,false,20000));p.resume();assertTrue(p.shouldFollow(false,false,20001))
    }
    @Test fun traveledLineIsRemovedWithoutDiagonalShortcut() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.002),Coordinate(.002,.002)),emptyList(),444.0,90.0)
        val engine=NavigationEngine(route)
        val p=engine.update(Coordinate(0.0,.001))
        val remaining=route.remainingPoints(p)
        assertEquals(Coordinate(0.0,.001),remaining.first());assertEquals(route.points[1],remaining[1])
        val off=engine.update(Coordinate(-.001,.0015))
        assertEquals(remaining,route.remainingPoints(off))
        val next=engine.update(Coordinate(.001,.002))
        assertEquals(2,route.remainingPoints(next).size)
        assertEquals(Coordinate(.001,.002),route.remainingPoints(next).first())
    }
    @Test fun longSegmentProjectionClipsToProgressWindowInsteadOfLosingTheRoad() {
        val route=RoadRoute(listOf(Coordinate(0.0,0.0),Coordinate(0.0,.1)),emptyList(),11120.0,600.0)
        val engine=NavigationEngine(route);engine.update(Coordinate(0.0,0.0))
        val next=engine.update(Coordinate(0.0,.02),movementAllowance=500.0)
        assertTrue(next.offRoute.isFinite());assertTrue(next.offRoute<2000);assertEquals(0.0,next.traveled,.01)
    }
}
