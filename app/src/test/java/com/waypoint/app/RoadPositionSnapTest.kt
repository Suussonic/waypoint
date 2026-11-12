package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadPositionSnapTest {
    @Test fun snapsOnlyWithinTheBoundedCorrectionRadius() {
        val position=Coordinate(0.0,0.0)
        val road=listOf(Coordinate(position.lat-.0001,position.lon-.00004),Coordinate(position.lat+.0001,position.lon-.00004))
        val snapped=nearestRoadProjection(position,listOf(road),null,6.0)
        assertTrue(snapped!=null && distance(position,snapped)<6.0)
        assertNull(nearestRoadProjection(position,listOf(road),null,3.0))
    }

    @Test fun headingKeepsTheMarkerOnTheRoadItIsTravellingAlongAtAnIntersection() {
        val position=Coordinate(.00004,.00003)
        val eastWest=listOf(Coordinate(.00004,-.0001),Coordinate(.00004,.0001))
        val northSouth=listOf(Coordinate(-.0001,0.0),Coordinate(.0001,0.0))
        val snapped=nearestRoadProjection(position,listOf(eastWest,northSouth),0f,8.0)
        assertEquals(0.0,snapped!!.lon,1e-10)
        assertEquals(position.lat,snapped.lat,1e-10)
    }
}
