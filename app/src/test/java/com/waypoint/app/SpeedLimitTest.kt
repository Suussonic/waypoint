package com.waypoint.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpeedLimitTest {
    @Test fun readsPlainKilometresAndMiles() {
        assertEquals(50, parseMaxSpeedKmh("50"))
        assertEquals(60, parseMaxSpeedKmh(" 60 km/h "))
        assertEquals(48, parseMaxSpeedKmh("30 mph"))
        assertEquals(80, parseMaxSpeedKmh("50mph"))
    }

    @Test fun ignoresValuesWithoutANumber() {
        listOf(null, "", "none", "signals", "walk", "IN:urban", "50;70", "0", "500").forEach {
            assertNull("$it", parseMaxSpeedKmh(it))
        }
    }

    @Test fun displaysInTheChosenUnit() {
        assertEquals(50, speedLimitForDisplay(50, imperial = false))
        assertEquals(30, speedLimitForDisplay(48, imperial = true))
        assertEquals(70, speedLimitForDisplay(113, imperial = true))
    }

    @Test fun warnsOnceWhenOverAndStaysQuietUntilBackUnderTheLimit() {
        val tracker = SpeedWarningTracker(toleranceKmh = 3, alertGapMillis = 60_000)
        val limit = 50
        assertFalse(tracker.update(13.5f, limit, 0))   // 48.6 km/h
        assertFalse(tracker.over)
        assertFalse(tracker.update(14.2f, limit, 1_000)) // 51.1: inside tolerance
        assertTrue(tracker.update(15.0f, limit, 2_000))  // 54 km/h: over
        assertTrue(tracker.over)
        assertFalse(tracker.update(15.0f, limit, 3_000)) // no repeat
        assertFalse(tracker.update(13.6f, limit, 4_000)) // 49 km/h: clear
        assertFalse(tracker.over)
    }

    @Test fun spacesRepeatedAlertsAndClearsWithoutALimit() {
        val tracker = SpeedWarningTracker(toleranceKmh = 3, alertGapMillis = 60_000)
        assertTrue(tracker.update(16f, 50, 0))
        tracker.update(10f, 50, 10_000)
        assertFalse("too soon", tracker.update(16f, 50, 20_000))
        tracker.update(10f, 50, 30_000)
        assertTrue(tracker.update(16f, 50, 70_000))
        assertFalse(tracker.update(30f, null, 71_000))
        assertFalse(tracker.over)
    }

    @Test fun choosesTheNearestRoadWithALimit() {
        val point = Coordinate(28.4950, 77.0890)
        val json = JSONObject("""
            {"elements":[
              {"type":"way","tags":{"highway":"primary","maxspeed":"80"},"geometry":[{"lat":28.49500,"lon":77.08890},{"lat":28.49500,"lon":77.08910}]},
              {"type":"way","tags":{"highway":"residential","maxspeed":"30"},"geometry":[{"lat":28.49510,"lon":77.08890},{"lat":28.49510,"lon":77.08910}]},
              {"type":"way","tags":{"highway":"service","maxspeed":"signals"},"geometry":[{"lat":28.49500,"lon":77.08900},{"lat":28.49500,"lon":77.08900}]}
            ]}""")
        assertEquals(80, nearestSpeedLimitKmh(json, point))
    }

    @Test fun returnsNothingWhenNoRoadIsClose() {
        val point = Coordinate(28.4950, 77.0890)
        val json = JSONObject("""{"elements":[{"tags":{"maxspeed":"50"},"geometry":[{"lat":28.5,"lon":77.1},{"lat":28.5,"lon":77.101}]}]}""")
        assertNull(nearestSpeedLimitKmh(json, point))
        assertNull(nearestSpeedLimitKmh(JSONObject("{}"), point))
    }
}
