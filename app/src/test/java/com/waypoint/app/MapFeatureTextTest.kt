package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class MapFeatureTextTest {
    @Test fun tileLabelsAreBoundedBeforeSelection() {
        assertEquals("Cafe", safeMapFeatureText("  Cafe\n\u0000  "))
        assertEquals("x".repeat(120), safeMapFeatureText("x".repeat(100_000)))
        assertEquals("shop", safeMapFeatureText("shop".repeat(100), 4))
        assertNull(safeMapFeatureText("\n\u0000  "))
    }
}
