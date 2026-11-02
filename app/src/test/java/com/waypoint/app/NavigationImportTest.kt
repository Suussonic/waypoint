package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class NavigationImportTest {
    private fun parse(uri: String, action: String? = "android.intent.action.VIEW") = NavigationImport.parse(action, uri)
    @Test fun coordinatesKeepOrderAndLabel() {
        assertEquals(NavigationImport.Point(Coordinate(51.5,-0.12),"Museum"),parse("geo:0,0?q=51.5,-0.12(Museum)"))
    }
    @Test fun ordinaryAddressAndGoogleNavigationAreSupported() {
        assertEquals(NavigationImport.Search("Central Park"),parse("google.navigation:q=Central+Park&mode=w"))
        assertEquals(NavigationImport.Search("Cafe & Park"),parse("geo:0,0?q=Cafe%20%26%20Park"))
    }
    @Test fun assistantAndCarNavigationIntentsUseTheSameConsentGate() {
        for (action in listOf("android.intent.action.NAVIGATE", "androidx.car.app.action.NAVIGATE")) {
            assertEquals(NavigationImport.Search("Central Park"), parse("geo:0,0?q=Central+Park", action))
            assertEquals(NavigationImport.Point(Coordinate(51.5,-0.12),"Location at 51.50000, -0.12000"), parse("geo:51.5,-0.12?mode=w", action))
        }
    }
    @Test fun decodesExactlyOnceAfterParameterSplitting() {
        assertEquals(NavigationImport.Search("Cafe%26Park"),parse("geo:0,0?q=Cafe%2526Park"))
    }
    @Test fun ambiguousAndUnsupportedInputsAreRejected() {
        for (uri in listOf("geo:0,0?q=One&q=Two","https://example.com/?q=Park","geo","geo://host/point","google.navigation:q=")) assertNull(uri,parse(uri))
        assertNull(parse("geo:51.5,-0.12",null))
        assertNull(parse("geo:51.5,-0.12","android.intent.action.SEND"))
    }
    @Test fun invalidCoordinatesNeverBecomeSearches() {
        for (uri in listOf("geo:91,0","geo:0,181","geo:999999,0","geo:51.5,-0.12garbage")) assertNull(uri,parse(uri))
    }
    @Test fun rejectsOversizedMalformedAndControlInputs() {
        assertNull(parse("geo:0,0?q="+"a".repeat(257)))
        assertNull(parse("geo:0,0?q=%GG"))
        assertNull(parse("geo:0,0?q=Park%00Road"))
        assertNull(parse("geo:0,0?q="+"%61".repeat(1400)))
    }
    @Test fun newerImportAndUserSelectionInvalidateOldResult() {
        val gate=NavigationImportGate()
        val old=gate.begin(); val current=gate.begin()
        assertFalse(gate.canApply(old,false)); assertTrue(gate.canApply(current,false))
        gate.invalidate(); assertFalse(gate.canApply(current,false))
    }
    @Test fun activeGuidanceAlwaysRejectsDeferredImports() {
        val gate=NavigationImportGate(); val ticket=gate.begin()
        assertFalse(gate.canApply(ticket,true))
    }
    @Test fun externalDestinationNeedsExplicitAcceptance() {
        val consent = NavigationImportConsent()
        val first = NavigationImport.Search("Museum")
        val latest = NavigationImport.Point(Coordinate(22.57,88.36), "Kolkata")
        val dismissed = requireNotNull(consent.offer(first, false))
        assertTrue(consent.dismiss(dismissed))
        assertNull(consent.accept(dismissed, false))
        val old = requireNotNull(consent.offer(first, false))
        val current = requireNotNull(consent.offer(latest, false))
        assertNull(consent.accept(old, false))
        assertFalse(consent.dismiss(old))
        assertEquals(latest, consent.accept(current, false))
        assertNull(consent.accept(current, false))
        assertNull(consent.offer(first, true))
        val navigating = requireNotNull(consent.offer(first, false))
        assertNull(consent.accept(navigating, true))
    }
}
