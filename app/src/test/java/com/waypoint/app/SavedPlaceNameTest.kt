package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class SavedPlaceNameTest {
    @Test fun personalNamesKeepUnicodeAndRejectBlankOrControlCharacters() {
        assertEquals("Mum’s house", savedPlaceName("  Mum’s house  "))
        assertEquals("বাড়ি", savedPlaceName("বাড়ি"))
        assertNull(savedPlaceName("  "))
        assertNull(savedPlaceName("A\nB"))
        assertNull(savedPlaceName("A".repeat(SAVED_PLACE_NAME_LIMIT+1)))
    }
}
