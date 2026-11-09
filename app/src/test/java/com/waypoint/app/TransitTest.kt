package com.waypoint.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TransitTest {
    private fun leg(mode: String, start: String, end: String, from: String, to: String, extra: String = "") =
        """{"mode":"$mode","startTime":"$start","endTime":"$end","from":{"name":"$from"},"to":{"name":"$to"}$extra}"""

    private val bus = """
        {"itineraries":[
          {"startTime":"2026-11-09T07:45:00Z","endTime":"2026-11-09T08:20:00Z","transfers":0,"legs":[
            ${leg("WALK", "2026-11-09T07:45:00Z", "2026-11-09T07:52:00Z", "Start", "Market Stop", ""","distance":503""")},
            ${leg("BUS", "2026-11-09T07:52:00Z", "2026-11-09T08:15:00Z", "Market Stop", "Station Road", ""","routeShortName":"335","headsign":"Airport","realTime":true,"agencyName":"City Buses","intermediateStops":[{},{},{}]""")},
            ${leg("WALK", "2026-11-09T08:15:00Z", "2026-11-09T08:20:00Z", "Station Road", "Destination", ""","distance":300""")}
          ]},
          {"startTime":"2026-11-09T07:45:00Z","endTime":"2026-11-09T08:30:00Z","transfers":0,"legs":[
            ${leg("WALK", "2026-11-09T07:45:00Z", "2026-11-09T08:30:00Z", "Start", "Destination", ""","distance":3200""")}
          ]}
        ]}"""

    @Test fun keepsTripsThatRideAVehicleAndReadsTheLine() {
        val trips = parseTransitPlan(JSONObject(bus))
        assertEquals(1, trips.size)
        val trip = trips[0]
        assertEquals(35, trip.minutes)
        assertEquals(12, trip.walkMinutes)
        val ride = trip.rides.single()
        assertEquals("Bus", ride.modeLabel)
        assertEquals("335", ride.line)
        assertEquals("Airport", ride.headsign)
        assertEquals("Market Stop", ride.from)
        assertEquals("Station Road", ride.to)
        assertEquals(23, ride.minutes)
        assertEquals(4, ride.stops)
        assertTrue(ride.liveTimes)
    }

    @Test fun keepsAWalkOnlyAnswerWhenNothingElseExists() {
        val json = """{"itineraries":[{"startTime":"2026-11-09T07:45:00Z","endTime":"2026-11-09T08:00:00Z","legs":[
            ${leg("WALK", "2026-11-09T07:45:00Z", "2026-11-09T08:00:00Z", "A", "B")}]}]}"""
        val trips = parseTransitPlan(JSONObject(json))
        assertEquals(1, trips.size)
        assertTrue(trips[0].rides.isEmpty())
    }

    @Test fun ignoresBrokenEntries() {
        assertTrue(parseTransitPlan(JSONObject("{}")).isEmpty())
        val json = """{"itineraries":[{"startTime":"not a time","endTime":"x","legs":[]},{"startTime":"2026-11-09T08:00:00Z","endTime":"2026-11-09T07:00:00Z","legs":[{}]}]}"""
        assertTrue(parseTransitPlan(JSONObject(json)).isEmpty())
    }

    @Test fun namesModesPlainly() {
        assertEquals("Bus", transitModeLabel("BUS"))
        assertEquals("Metro", transitModeLabel("SUBWAY"))
        assertEquals("Train", transitModeLabel("REGIONAL_RAIL"))
        assertEquals("Tram", transitModeLabel("tram"))
        assertEquals("Transit", transitModeLabel("SOMETHING_NEW"))
    }
}
