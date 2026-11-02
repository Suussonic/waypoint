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
        assertEquals(50.0,mapCameraTilt(World.SAN_ANDREAS,true,navigating=true),0.001)
        assertEquals(0.0,mapCameraTilt(World.SAN_ANDREAS,false,navigating=true),0.001)
        assertEquals(48.0,mapCameraTilt(World.SAN_ANDREAS,false,previewTilt=48.0),0.001)
    }
}
