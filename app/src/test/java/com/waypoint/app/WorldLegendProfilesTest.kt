package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldLegendProfilesTest {
    @Test fun everyWorldHasItsOwnSectionAndNavigationVocabulary() {
        val profiles = World.entries.map(WorldLegendProfiles::forWorld)

        assertEquals(World.entries.size, profiles.size)
        assertTrue(profiles.all { it.groupTitles.keys == setOf("discovery", "community", "essential", "support", "utility", "social", "other") })
        assertEquals(World.entries.size, profiles.map { it.groupTitles.values.toSet() }.toSet().size)
        assertEquals(World.entries.size, profiles.map { it.navigation }.toSet().size)
    }

    @Test fun themedPlaceNamesKeepTheirCanonicalOsmLabelAvailable() {
        val losSantos = WorldLegendProfiles.forWorld(World.LOS_SANTOS)
        assertEquals("Grill / eatery", losSantos.aliases.getValue("restaurant"))
        assertEquals("TOWNS / RAIL", WorldLegendProfiles.forWorld(World.OLD_WEST).groupTitles.getValue("community"))
    }
}
