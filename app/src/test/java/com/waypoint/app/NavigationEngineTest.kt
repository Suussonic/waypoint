package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class NavigationEngineTest {
    private val a=Coordinate(0.0,0.0)
    private val b=Coordinate(0.0,0.01)
    private val c=Coordinate(0.01,0.01)
    private fun route(points: List<Coordinate> = listOf(a,b,c))=RoadRoute(points,listOf(
        Maneuver(points.first(),"depart","straight","First Street",1112.0,100.0),
        Maneuver(points[1],"turn","left","Second Street",1112.0,100.0),
        Maneuver(points.last(),"arrive","","",0.0,0.0)),2224.0,200.0)
    @Test fun `distance uses meters`() { assertEquals(1112.0,distance(a,b),2.0) }
    @Test fun `projects onto segment between sparse vertices`() {
        val p=NavigationEngine(route()).update(Coordinate(0.0,0.005))
        assertEquals(556.0,p.traveled,2.0);assertEquals(0.0,p.offRoute,0.1);assertEquals(1,p.nextStep)
    }
    @Test fun `advances turn only after passing along route`() {
        val engine=NavigationEngine(route());engine.update(Coordinate(0.0,0.0099))
        val p=engine.update(Coordinate(0.0002,0.01));assertEquals(2,p.nextStep);assertTrue(p.remaining>1000)
    }
    @Test fun `gps jitter cannot move progress backward`() {
        val engine=NavigationEngine(route());val p=engine.update(Coordinate(0.0,0.005))
        val q=engine.update(Coordinate(0.0,0.0048));assertEquals(p.traveled,q.traveled,0.1)
    }
    @Test fun `off route fix does not advance progress`() {
        val engine=NavigationEngine(route());val p=engine.update(Coordinate(0.0,0.003))
        val q=engine.update(Coordinate(-0.004,0.006));assertTrue(q.offRoute>400);assertEquals(p.traveled,q.traveled,0.1)
    }
    @Test fun `near destination on earlier loop is not arrival`() {
        val nearStart=Coordinate(0.0001,0.0)
        val r=route(listOf(a,b,c,Coordinate(0.01,0.0),nearStart));val engine=NavigationEngine(r)
        val p=engine.update(a);assertFalse(engine.hasArrived(a,p))
    }
    @Test fun `arrival requires end of route and nearby physical location`() {
        val engine=NavigationEngine(route());val p=engine.update(c)
        assertTrue(engine.hasArrived(c,p));assertFalse(engine.hasArrived(Coordinate(0.02,0.01),p))
    }
    @Test fun `stationary start cannot jump to later crossing`() {
        val engine=NavigationEngine(route(listOf(a,b,c,a,Coordinate(-0.01,0.0))))
        val p=engine.update(a);assertEquals(0.0,p.traveled,0.1)
    }
    @Test fun `formatting respects imperial setting`() { assertEquals("1.0 mi",formatDistance(1609.344,true));assertEquals("500 m",formatDistance(500.0));assertEquals("1.5 km",formatDistance(1500.0)) }
    @Test fun `turn and roundabout instructions include road and exit`() {
        assertEquals("Turn left onto Broadway",Maneuver(a,"turn","left","Broadway",10.0,1.0).instruction)
        assertTrue(Maneuver(a,"roundabout","right","Road",10.0,1.0,3).instruction.contains("exit 3"))
    }
    @Test fun `long final leg shows continuation until arrival is near`() {
        val straight=RoadRoute(listOf(a,b),listOf(
            Maneuver(a,"depart","straight","Village Road",500.0,90.0),
            Maneuver(b,"arrive","","",0.0,0.0)),500.0,90.0)
        val far=NavigationEngine(straight).update(a)
        assertEquals("Continue on Village Road",routeGuidance(straight,far).instruction)
        assertTrue(routeGuidance(straight,far).continuingToArrival)
        val near=NavigationEngine(straight).update(Coordinate(0.0,0.009))
        assertEquals("Arrive at your destination",routeGuidance(straight,near).instruction)
        assertFalse(routeGuidance(straight,near).continuingToArrival)
    }
}
