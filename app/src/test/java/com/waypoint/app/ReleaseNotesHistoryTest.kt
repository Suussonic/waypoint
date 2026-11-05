package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesHistoryTest {
    @Test fun skippedPublishedVersionsAppearInOrder() {
        assertEquals(
            listOf("release-notes-1.5.0.md", "release-notes-2.0.0.md", "release-notes.md"),
            releaseNotesAfter(1_001_000, 2_000_001).map { it.asset }
        )
        assertEquals(listOf("release-notes.md"), releaseNotesAfter(2_000_000, 2_000_001).map { it.asset })
        assertEquals(emptyList<String>(), releaseNotesAfter(2_000_001, 2_000_001).map { it.asset })
    }
}
