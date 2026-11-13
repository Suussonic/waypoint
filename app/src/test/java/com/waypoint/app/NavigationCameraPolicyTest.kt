package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationCameraPolicyTest {
    @Test fun speedPullsTheCameraBackOnFasterRoads() {
        assertEquals(NAV_ZOOM, navigationSpeedZoom(0.0), 0.001)
        assertTrue(navigationSpeedZoom(15.0) < navigationSpeedZoom(5.0))
        assertEquals(16.2, navigationSpeedZoom(30.0), 0.001)
        assertEquals(16.2, navigationSpeedZoom(80.0), 0.001)
    }

    @Test fun nextTurnPullsBackThenReturnsToCloseView() {
        val far = navigationTurnZoom(1400.0, 22.0, 2400, 3.0, 450.0)
        val near = navigationTurnZoom(150.0, 22.0, 2400, 3.0, 450.0)
        assertTrue("turn should be framable", far.isFinite() && near.isFinite())
        assertTrue("far turn should pull the zoom out", far < NAV_ZOOM)
        assertTrue("camera should zoom in again near the turn", near > far)
        assertEquals(NAV_ZOOM, navigationZoomForTurn(NAV_ZOOM, near), 0.001)
        assertEquals(14.5, navigationZoomForTurn(NAV_ZOOM, 10.0), 0.001)
    }

    @Test fun pitchMatchesNavigationPerspectiveAndFlattensAtWideZoom() {
        assertEquals(55.0, navigationTiltAtZoom(18.0), 0.001)
        assertEquals(0.0, navigationTiltAtZoom(12.5), 0.001)
        assertEquals(27.5, navigationTiltAtZoom(13.75), 0.001)
        assertEquals(0.0, navigationTiltAtZoom(18.0, wantedTilt = 0.0), 0.001)
    }

    @Test fun cameraTurnsTowardFarManeuverAndFadesYawNearIt() {
        assertEquals(45.0, navigationBearingTowardTurn(0.0, 90.0, 17.0, 16.5), 0.001)
        assertEquals(0.0, navigationBearingTowardTurn(0.0, 90.0, 17.0, 17.2), 0.001)
        assertEquals(315.0, navigationBearingTowardTurn(0.0, -90.0, 17.0, 16.5), 0.001)
    }

    @Test fun cameraPoseCanFollowTheSmoothedHeadingBetweenLocationUpdates() {
        val pose = navigationCameraPose(NavState(navigating = true), 0f, 2400, 3.0, 450.0, 55.0, 31f)
        assertEquals(31.0, pose.bearing, 0.001)
    }

    @Test fun northUpAndReroutingDoNotAimAtAStaleTurn() {
        val fix = Coordinate(0.0, 0.0)
        val nextTurn = Coordinate(0.012, 0.0)
        val route = RoadRoute(
            points = listOf(fix, nextTurn),
            steps = listOf(
                Maneuver(fix, "depart", "straight", "", 0.0, 0.0),
                Maneuver(nextTurn, "turn", "right", "Next Road", 1400.0, 90.0),
            ), meters = 1400.0, seconds = 90.0,
        )
        val base = NavState(
            fix = fix, route = route, navigating = true, speed = 5f,
            progress = RouteProgress(0.0, 1400.0, 90.0, 0.0, 0, 1400.0, fix),
        )
        val northUp = navigationCameraPose(base.copy(keepMapNorthUp = true), 90f, 2400, 3.0, 450.0, 55.0)
        assertEquals(0.0, northUp.bearing, 0.001)
        assertEquals(navigationSpeedZoom(5.0), northUp.zoom, 0.001)

        val rerouting = navigationCameraPose(base.copy(routing = true), 0f, 2400, 3.0, 450.0, 55.0)
        assertEquals(navigationSpeedZoom(5.0), rerouting.zoom, 0.001)
        assertEquals(0.0, rerouting.bearing, 0.001)
    }
}
