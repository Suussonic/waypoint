package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StationaryPositionFilterTest {
    private val start=Coordinate(0.0,0.0)
    private fun east(meters:Double)=Coordinate(start.lat,start.lon+meters/111_320.0)

    @Test fun stoppedVehicleKeepsTheRenderedPositionInsideItsGpsNoiseRadius() {
        val filter=StationaryPositionFilter()
        assertEquals(start,filter.update(start,TravelMode.CAR,0f,8f))
        for(offset in listOf(1.0,-2.0,3.0,-1.5,5.0,-4.0)) {
            assertEquals(start,filter.update(east(offset),TravelMode.CAR,0f,8f))
        }
    }

    @Test fun twoConsistentFixesReleaseTheDeadbandWhenSpeedIsUnavailable() {
        val filter=StationaryPositionFilter()
        filter.update(start,TravelMode.CAR,0f,3f)
        val first=east(5.0)
        assertEquals(start,filter.update(first,TravelMode.CAR,0f,3f))
        val second=east(5.5)
        assertEquals(second,filter.update(second,TravelMode.CAR,0f,3f))
    }

    @Test fun measuredMovementExitsTheDeadbandImmediately() {
        val filter=StationaryPositionFilter()
        filter.update(start,TravelMode.MOTORCYCLE,0f,8f)
        val moved=east(1.0)
        assertNotEquals(start,filter.update(moved,TravelMode.MOTORCYCLE,1f,8f))
    }

    @Test fun stationaryPositionIsFilteredOutsideNavigationAndForWalking() {
        val filter=StationaryPositionFilter()
        filter.update(start,TravelMode.WALK,0f,8f)
        assertEquals(start,filter.update(east(1.0),TravelMode.WALK,0f,8f))
        val moved=east(1.0)
        assertEquals(moved,filter.update(moved,TravelMode.WALK,.3f,8f))
    }
}
