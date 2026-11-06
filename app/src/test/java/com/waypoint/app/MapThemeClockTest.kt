package com.waypoint.app

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapThemeClockTest {
    @Test fun localDayNightBoundaryIsStable() {
        assertTrue(isNightAtLocalTime(LocalTime.of(5,59)))
        assertFalse(isNightAtLocalTime(LocalTime.of(6,0)))
        assertFalse(isNightAtLocalTime(LocalTime.NOON))
        assertFalse(isNightAtLocalTime(LocalTime.of(18,59)))
        assertTrue(isNightAtLocalTime(LocalTime.of(19,0)))
        assertTrue(isNightAtLocalTime(LocalTime.of(23,59)))
    }

    @Test fun eachWorldSelectsItsDayAndNightStyle() {
        World.entries.filterNot { it == World.DIVISION }.forEach { world ->
            assertEquals(world.file,mapThemeStyleFile(world,false,false))
            assertEquals("${world.file}_night",mapThemeStyleFile(world,true,false))
        }
    }

    @Test fun darkZoneTiltSelectsHologramInBothPhases() {
        assertEquals("division",mapThemeStyleFile(World.DIVISION,false,false))
        assertEquals("division_night",mapThemeStyleFile(World.DIVISION,true,false))
        assertEquals("division_holo",mapThemeStyleFile(World.DIVISION,false,true))
        assertEquals("division_holo_night",mapThemeStyleFile(World.DIVISION,true,true))
        assertEquals("los_santos",mapThemeStyleFile(World.LOS_SANTOS,false,true))
    }

    @Test fun missingNightOrHologramStyleFallsBackToThatWorldsFlatStyle() {
        assertEquals("los_santos",resolveMapThemeStyleFile(
            World.LOS_SANTOS,true,false,setOf("los_santos")))
        assertEquals("division",resolveMapThemeStyleFile(
            World.DIVISION,true,true,setOf("division")))
    }
}
