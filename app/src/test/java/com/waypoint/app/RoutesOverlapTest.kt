package com.waypoint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesOverlapTest {
    private fun line(lonOffset: Double) = (0..60).map { Coordinate(22.0 + it * 0.0005, 88.0 + lonOffset) }

    @Test fun theSameRoadIsNotAnAlternative() {
        assertTrue(routesOverlap(line(0.0), line(0.0002)))   // about 20 m apart
    }

    @Test fun aParallelRoadAKilometreAwayIs() {
        assertFalse(routesOverlap(line(0.0), line(0.012)))
    }

    @Test fun routesThatShareOnlyTheStartAreDifferent() {
        val a = line(0.0)
        val b = a.take(8) + (1..52).map { Coordinate(22.0035 + it * 0.0005, 88.0 + it * 0.0004) }
        assertFalse(routesOverlap(a, b))
    }
}

class RouteOverlapShareTest {
    private fun line(lonOffset: Double) = (0..60).map { Coordinate(22.0 + it * 0.0005, 88.0 + lonOffset) }

    @Test fun identicalRoutesShareEverything() {
        org.junit.Assert.assertEquals(1.0, routeOverlapShare(line(0.0), line(0.0)), 1e-9)
    }

    @Test fun distantRoutesShareNothing() {
        org.junit.Assert.assertEquals(0.0, routeOverlapShare(line(0.0), line(0.02)), 1e-9)
    }
}
