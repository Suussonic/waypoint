package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class MapCameraPolicyTest {
    // Fixed public coordinates (Trafalgar Square), never a real device location.
    private val fix = Coordinate(51.5080, -0.1281)

    @Test fun tiltToggleFlipsPitchOnlyAndNeverRaisesACameraRequest() {
        val viewing = NavState(fix = fix, selected = Place("p", "Somewhere", "", Coordinate(48.8584, 2.2945)),
            tilted = false, cameraRequest = 7, focusUserCameraRequest = 3, passiveCameraRequest = 5)
        val toggled = viewing.withTiltToggled()
        assertTrue(toggled.tilted)
        assertEquals(7, toggled.cameraRequest)
        assertEquals(3, toggled.focusUserCameraRequest)
        assertEquals(viewing.copy(tilted = true), toggled)
        assertEquals(viewing, toggled.withTiltToggled())
    }

    @Test fun pitchChangeIgnoresNegligibleDifferences() {
        assertNull(pitchChange(45.0, 45.2))
        assertEquals(0.0, pitchChange(45.0, 0.0)!!, 0.0)
        assertEquals(45.0, pitchChange(0.0, 45.0)!!, 0.0)
    }

    @Test fun browsingAwayShowsRecentreAndBeingCentredHidesIt() {
        assertTrue(cameraAwayFromUser(false, false, 4_000_000.0, 0.3))
        assertFalse(cameraAwayFromUser(false, false, 3.0, 0.3))
        assertFalse(cameraAwayFromUser(false, false, null, 0.3))
        // Zoomed far out, a few pixels of drift is still "centred".
        assertFalse(cameraAwayFromUser(false, false, 600.0, 400.0))
    }

    @Test fun guidanceUsesTheFollowPause() {
        assertTrue(cameraAwayFromUser(true, true, 0.0, 0.3))
        assertFalse(cameraAwayFromUser(true, false, 9_000.0, 0.3))
    }

    @Test fun locateOnlyFliesBackWhenAwayAndDoesNothingWhenCentred() {
        assertEquals(LocateAction.REQUEST_PERMISSION, locateAction(false, false, false, true))
        assertEquals(LocateAction.ENABLE_GPS, locateAction(true, false, false, false))
        assertEquals(LocateAction.ENABLE_GPS, locateAction(true, true, false, true))
        assertEquals(LocateAction.RECENTRE, locateAction(true, true, true, true))
        assertEquals(LocateAction.NONE, locateAction(true, true, true, false))
    }

    @Test fun replayedRequestsCannotMoveAMapTheUserHasTakenOver() {
        val gate = CameraRequestGate()
        assertTrue(gate.accept(1, passive = false))
        // Style reload or new insets: same request replays while the user has not touched the map.
        assertTrue(gate.accept(1, passive = false))
        gate.userMovedCamera()
        assertFalse(gate.accept(1, passive = false))
    }

    @Test fun passiveRequestsRespectAPanButExplicitOnesDoNot() {
        val gate = CameraRequestGate()
        assertTrue(gate.accept(1, passive = false))
        gate.userMovedCamera()
        assertFalse("first fix must not yank a panned map", gate.accept(2, passive = true))
        assertFalse(gate.accept(2, passive = true))
        assertTrue("recentre or a new destination always moves", gate.accept(3, passive = false))
        assertTrue(gate.accept(3, passive = false))
    }

    @Test fun passiveRequestMovesAnUntouchedMap() {
        val gate = CameraRequestGate()
        assertTrue(gate.accept(1, passive = true))
    }
}
