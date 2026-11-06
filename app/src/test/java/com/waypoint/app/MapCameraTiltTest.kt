package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCameraTiltTest {
    @Test fun everyWorldCanSwitchBetweenFlatAndTilted() {
        World.entries.forEach { world ->
            assertEquals("${world.name} flat",0.0,mapCameraTilt(world,false),0.001)
            assertTrue("${world.name} tilted",mapCameraTilt(world,true)>=20.0)
        }
    }

    @Test fun navigationAndGlobePreviewKeepTheirRequestedAngles() {
        assertEquals(55.0,mapCameraTilt(World.SAN_ANDREAS,true,navigating=true),0.001)
        assertEquals(0.0,mapCameraTilt(World.SAN_ANDREAS,false,navigating=true),0.001)
        assertEquals(48.0,mapCameraTilt(World.SAN_ANDREAS,false,previewTilt=48.0),0.001)
    }

    @Test fun routeGrammarFollowsTheWorldAndTravelMode() {
        assertEquals(RouteGrammar.SOLID,World.NIGHT_CITY.routeGrammar(TravelMode.CAR,navigating=true))
        assertEquals(RouteGrammar.DOTS,World.NIGHT_CITY.routeGrammar(TravelMode.WALK,navigating=true))
        assertEquals(RouteGrammar.DASHED,World.OLD_WEST.routeGrammar())
        assertEquals(RouteGrammar.DOTS,World.SAINTS.routeGrammar(TravelMode.CAR,navigating=true))
        assertEquals(RouteGrammar.SOLID,World.SAINTS.routeGrammar(TravelMode.CAR,navigating=false))
    }

    @Test fun walkingPositionGlyphsUseDistinctSilhouettesAcrossWorlds() {
        assertTrue(World.entries.map { it.mapArt().player }.toSet().size >= 14)
    }
}
