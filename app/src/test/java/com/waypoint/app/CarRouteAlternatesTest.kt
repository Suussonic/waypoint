package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarRouteAlternatesTest {
    private fun route(seconds: Double, lonEnd: Double) =
        RoadRoute(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, lonEnd)), emptyList(), seconds * 10, seconds)

    @Test fun previewShowsEveryOptionExceptTheChosenOne() {
        val a = route(600.0, .01); val b = route(660.0, .011); val c = route(720.0, .012)
        val state = NavState(route = b, routeOptions = listOf(a, b, c))
        assertEquals(listOf(0, 2), carAlternateRoutes(state).map { it.index })
    }

    @Test fun aSingleOptionHasNothingToCompare() {
        val a = route(600.0, .01)
        assertTrue(carAlternateRoutes(NavState(route = a, routeOptions = listOf(a))).isEmpty())
        assertTrue(carAlternateRoutes(NavState(routeOptions = listOf(a, route(1.0, .02)))).isEmpty())
    }

    @Test fun guidanceShowsTheFreshAlternativesWithTheirOwnIndexes() {
        val current = route(900.0, .02); val x = route(800.0, .021); val y = route(1000.0, .022)
        val state = NavState(route = current, navigating = true, routeOptions = listOf(route(1.0, .03)), driveAlternatives = listOf(x, y))
        assertEquals(listOf(0, 1), carAlternateRoutes(state).map { it.index })
        assertTrue(carAlternateRoutes(state).all { it.value === x || it.value === y })
    }

    @Test fun gapLabelsRoundUpLikeThePhoneChips() {
        assertEquals("+2 min", carAlternateGapLabel(719.0, 600.0))
        assertEquals("−2 min", carAlternateGapLabel(481.0, 600.0))
        assertEquals("same time", carAlternateGapLabel(600.0, 600.0))
        assertEquals("same time", carAlternateGapLabel(600.0, null))
    }

    @Test fun guidanceComparesWithTimeLeftAndPreviewWithFullDuration() {
        val r = route(900.0, .02)
        val progress = RouteProgress(100.0, 800.0, 420.0, 0.0, 0, 0.0, r.points.first())
        assertEquals(420.0, carAlternateBaseSeconds(NavState(route = r, navigating = true, progress = progress))!!, 0.001)
        assertEquals(900.0, carAlternateBaseSeconds(NavState(route = r))!!, 0.001)
    }

    @Test fun previewCameraFramesEveryRouteOnOffer() {
        val short = RoadRoute(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, .01)), emptyList(), 1100.0, 200.0)
        val long = RoadRoute(listOf(Coordinate(0.0, 0.0), Coordinate(.05, .01)), emptyList(), 6000.0, 700.0)
        val one = carCamera(NavState(route = short), 1280, 720)
        val both = carCamera(NavState(route = short, routeOptions = listOf(short, long)), 1280, 720)
        assertTrue("Alternatives must not be clipped", both.zoom < one.zoom)
        assertTrue(both.target!!.latitude > one.target!!.latitude)
    }
}
