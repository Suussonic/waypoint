package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimAheadTest {
    // A straight road running north, vertices roughly 500 m apart.
    private val road = listOf(Coordinate(22.0, 88.0), Coordinate(22.0045, 88.0), Coordinate(22.009, 88.0), Coordinate(22.0135, 88.0))

    @Test fun startsAtTheVehicleNotTheSegmentStart() {
        val car = Coordinate(22.0030, 88.00001)
        val trimmed = trimAhead(road, car)
        assertEquals(22.0030, trimmed.first().lat, 1e-6)
        assertEquals(88.0, trimmed.first().lon, 1e-6)
        assertEquals(road.size, trimmed.size)   // the vertex behind the car is replaced by the car position
        assertEquals(road[1], trimmed[1])
    }

    @Test fun nothingBehindTheVehicleSurvives() {
        val car = Coordinate(22.0070, 88.0)
        val trimmed = trimAhead(road, car)
        assertTrue(trimmed.all { it.lat >= 22.0070 - 1e-9 })
    }

    @Test fun offRouteFixLeavesTheLineAlone() {
        val far = Coordinate(22.0030, 88.0050)   // about 500 m sideways
        assertSame(road, trimAhead(road, far))
    }

    @Test fun shortInputIsReturned() {
        val one = listOf(Coordinate(22.0, 88.0))
        assertSame(one, trimAhead(one, Coordinate(22.0, 88.0)))
    }
}
