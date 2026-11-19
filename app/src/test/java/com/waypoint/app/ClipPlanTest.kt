package com.waypoint.app

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ClipPlanTest {
    private val metres = listOf(20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 140, 150, 160, 180, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900)
    private val all: Set<String> = buildSet {
        metres.forEach { add("dist_m_$it") }
        listOf("1", "1p5", "2", "3", "4", "5", "10").forEach { add("dist_km_$it") }
        listOf(50, 100, 150, 200, 250, 300, 400, 500, 600, 800).forEach { add("dist_ft_$it") }
        listOf("0p1", "0p2", "0p25", "0p3", "0p4", "0p5", "0p6", "0p7", "0p75", "0p8", "0p9", "1", "1p5", "2", "3", "5").forEach { add("dist_mi_$it") }
        add("dist_now")
        addAll(listOf("man_left", "man_right", "man_slight_left", "man_slight_right", "man_straight", "man_uturn", "man_roundabout",
            "man_roundabout_1", "man_roundabout_2", "man_roundabout_3", "man_roundabout_4", "man_exit_right", "man_keep_left",
            "evt_arrive", "evt_arrive_left", "evt_reroute", "catch_1"))
    }
    private fun plan(text: String, trigger: Double? = null, imperial: Boolean = false) =
        ClipPlan.plan(text, Random(1), all, triggerMeters = trigger, imperial = imperial)

    @Test fun distanceAndTurnDropStreetName() =
        assertEquals(listOf("dist_m_200", "man_left"), plan("In 200 meters, turn left onto Baker Street"))
    @Test fun sayTheRealNumberNotARoundedOne() {
        assertEquals(listOf("dist_m_180", "man_right"), plan("In 180 meters, Turn right"))
        assertEquals(listOf("dist_m_350", "man_left"), plan("In 350 meters, turn left"))
        assertEquals(listOf("dist_m_120", "man_slight_right"), plan("In 120 meters, bear right"))
    }
    @Test fun imperialDistancesUseTheirOwnGrid() {
        assertEquals(listOf("dist_ft_500", "man_right"), plan("In 500 feet, turn right"))
        assertEquals(listOf("dist_ft_250", "man_left"), plan("In 250 feet, turn left"))
        assertEquals(listOf("dist_mi_0p3", "man_left"), plan("In 0.3 miles, turn left"))
        assertEquals(listOf("dist_mi_0p5", "man_straight"), plan("In half a mile, continue straight"))
        assertEquals(listOf("dist_mi_0p25", "man_right"), plan("In a quarter mile, turn right"))
    }
    @Test fun kilometresAndLongDistances() {
        assertEquals(listOf("dist_km_1p5", "man_straight"), plan("Continue straight for 1.5 kilometers"))
        assertEquals(listOf("dist_km_2", "man_right"), plan("In 2 km, turn right"))
        assertEquals(listOf("dist_m_500", "man_left"), plan("In 0.5 kilometers, turn left"))
    }
    @Test fun roundaboutExitNumbersAreRead() {
        assertEquals(listOf("dist_m_180", "man_roundabout_2"), plan("In 180 meters, At the roundabout, take exit 2 onto Grand Trunk Road"))
        assertEquals(listOf("dist_now", "man_roundabout_3"), plan("At the roundabout, take the 3rd exit"))
        assertEquals(listOf("dist_km_1", "man_roundabout_2"), plan("In 1 kilometer, take the second exit at the roundabout"))
    }
    @Test fun immediateInstructionUsesNow() = assertEquals(listOf("dist_now", "man_uturn"), plan("Make a U-turn"))
    @Test fun arrivalHasNoDistance() = assertEquals("evt_arrive_left", plan("You have arrived. Your destination is on the left.")!!.first())
    @Test fun triggerDistanceFillsMissingDistance() {
        assertEquals(listOf("dist_m_200", "man_left"), plan("Turn left", trigger = 196.0))
        assertEquals(listOf("dist_m_160", "man_left"), plan("Turn left", trigger = 163.0))
        assertEquals(listOf("dist_ft_500", "man_right"), plan("Turn right onto Elm Street", trigger = 150.0, imperial = true))
        assertEquals(listOf("dist_ft_250", "man_right"), plan("Turn right", trigger = 76.0, imperial = true))
        assertEquals(listOf("dist_mi_0p25", "man_left"), plan("Turn left", trigger = 410.0, imperial = true))
        assertEquals(listOf("dist_now", "man_left"), plan("Turn left", trigger = 20.0))
        assertEquals(listOf("dist_km_1", "man_left"), plan("Turn left", trigger = 1100.0))
    }
    @Test fun unknownTextFallsBack() = assertNull(plan("Gibberish with no direction at all"))
    @Test fun missingClipFallsBack() = assertNull(ClipPlan.plan("Turn left", Random(1), setOf("dist_now")))
    @Test fun wholeSentenceClipPreferredWhenAvailable() {
        val withWhole = all + setOf("comb_m_200__left", "comb_m_180__roundabout_2")
        assertEquals(listOf("comb_m_200__left"), ClipPlan.plan("In 200 meters, turn left onto Baker Street", Random(1), withWhole))
        assertEquals(listOf("comb_m_180__roundabout_2"), ClipPlan.plan("In 180 meters, take exit 2 at the roundabout", Random(1), withWhole))
        assertEquals(listOf("dist_m_120", "man_right"), ClipPlan.plan("In 120 meters, turn right", Random(1), withWhole))
    }
}
