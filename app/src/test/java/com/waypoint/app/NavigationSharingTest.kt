package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class NavigationSharingTest {
    @Test fun plainAddressesAndCoordinatesUseTheExistingParser() {
        assertEquals(NavigationImport.Search("12 Park Street, Kolkata"), NavigationSharing.parseText("12 Park Street,\nKolkata"))
        assertTrue(NavigationSharing.parseText("22.5726,88.3639") is NavigationImport.Point)
        assertNull(NavigationSharing.parseText("91,88"))
        assertNull(NavigationSharing.parseText("22.5,nonsense"))
        assertNull(NavigationSharing.parseText("x".repeat(4097)))
    }
    @Test fun googleMapsParametersIdentifyTheDestinationNotTheCamera() {
        assertTrue(NavigationSharing.parseText("https://www.google.com/maps/search/?api=1&query=22.5%2C88.3") is NavigationImport.Point)
        assertEquals(NavigationImport.Search("Victoria Memorial"), NavigationSharing.parseText("https://www.google.com/maps/dir/?api=1&destination=Victoria+Memorial"))
        assertEquals(NavigationImport.Search("Victoria Memorial"), NavigationSharing.parseText("Victoria Memorial\nhttps://www.google.com/maps/place/Victoria+Memorial/@22,88,13z"))
        assertNull(NavigationSharing.parseText("https://www.google.com/maps/@22,88,13z"))
        assertNull(NavigationSharing.parseText("https://www.google.com/maps/?q=22,88&q=23,89"))
    }
    @Test fun onlyApprovedHttpsMapUrlsMayBeResolved() {
        assertNotNull(NavigationSharing.supportedUrl("https://maps.app.goo.gl/Example"))
        listOf("http://maps.app.goo.gl/Example", "https://maps.app.goo.gl.evil.example/x", "https://maps.app.goo.gl@evil.example/x",
            "https://maps.app.goo.gl:8443/x", "https://127.0.0.1/maps/x", "https://www.google.com/url?q=evil", "https://goo.gl/unrelated").forEach {
            assertNull(it, NavigationSharing.supportedUrl(it))
        }
    }
    @Test fun unknownSchemesAndWebPagesDoNotBecomeGeocodingRequests() {
        listOf("file:///etc/passwd", "javascript:alert(1)", "https://example.com/a", "intent://anything", "content://private/photo").forEach {
            assertNull(it, NavigationSharing.parseText(it))
        }
    }
    @Test fun outboundDirectionsPreserveModeWithoutSharingCurrentLocation() {
        val link = NavigationSharing.googleDirections(Place("x", "Place", "Address", Coordinate(22.5, 88.3)), TravelMode.WALK)
        assertTrue(link.contains("destination=22.5%2C88.3"))
        assertTrue(link.endsWith("travelmode=walking"))
        assertFalse(link.contains("origin="))
    }
    @Test fun cyclingHandoffPreservesTheBicycleMode() {
        val link=NavigationSharing.googleDirections(Place("cycle", "Park", "London", Coordinate(51.5,-.1)), TravelMode.BICYCLE)
        assertTrue(link.endsWith("travelmode=bicycling"))
        assertFalse(link.contains("origin="))
    }

}
