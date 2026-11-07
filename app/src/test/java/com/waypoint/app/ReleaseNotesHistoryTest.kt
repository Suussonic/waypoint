package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesHistoryTest {
    @Test fun skippedPublishedVersionsAppearInOrder() {
        assertEquals(
            listOf("release-notes-1.5.0.md", "release-notes-2.0.0.md", "release-notes-2.0.1.md", "release-notes-2.5.0.md"),
            releaseNotesAfter(1_001_000, 2_005_000).map { it.asset }
        )
        assertEquals(listOf("release-notes-2.5.0.md"), releaseNotesAfter(2_000_001, 2_005_000).map { it.asset })
        assertEquals(listOf("release-notes-2.5.0.md", "release-notes-2.5.1.md"),
            releaseNotesAfter(2_000_001, 2_005_001).map { it.asset })
        assertEquals(listOf("release-notes-2.5.1.md", "release-notes-2.5.2.md"),
            releaseNotesAfter(2_005_000, 2_005_002).map { it.asset })
        assertEquals(listOf("release-notes-2.5.2.md", "release-notes-2.5.3.md"),
            releaseNotesAfter(2_005_001, 2_005_003).map { it.asset })
        assertEquals(listOf("release-notes-2.5.3.md", "release-notes-2.5.4.md"),
            releaseNotesAfter(2_005_002, 2_005_004).map { it.asset })
        assertEquals(emptyList<String>(), releaseNotesAfter(2_005_000, 2_005_000).map { it.asset })
    }
}
