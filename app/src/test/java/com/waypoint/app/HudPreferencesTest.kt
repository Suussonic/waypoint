package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HudPreferencesTest {
    @Test fun savedPresetsRoundTripAndUnknownValuesKeepDefaults() {
        HudSize.entries.forEach { assertEquals(it, hudSizeFromPreference(it.name)) }
        HudSide.entries.forEach { assertEquals(it, hudSideFromPreference(it.name)) }
        assertEquals(HudSize.STANDARD, hudSizeFromPreference(null))
        assertEquals(HudSize.STANDARD, hudSizeFromPreference("OLD_VALUE"))
        assertNull(hudSideFromPreference(null))
        assertNull(hudSideFromPreference("OLD_VALUE"))
    }
}
