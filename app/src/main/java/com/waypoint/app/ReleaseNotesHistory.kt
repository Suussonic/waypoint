package com.waypoint.app

internal data class BundledReleaseNotes(val versionCode: Int, val asset: String)

/** Published Android releases, oldest first. Bundling notes keeps skipped versions available offline. */
internal val publishedReleaseNotes = listOf(
    BundledReleaseNotes(2_001, "release-notes-0.2.1.md"),
    BundledReleaseNotes(2_003, "release-notes-0.2.3.md"),
    BundledReleaseNotes(1_000_000, "release-notes-1.0.0.md"),
    BundledReleaseNotes(1_001_000, "release-notes-1.1.0.md"),
    BundledReleaseNotes(1_005_000, "release-notes-1.5.0.md"),
    BundledReleaseNotes(2_000_000, "release-notes-2.0.0.md"),
    BundledReleaseNotes(2_000_001, "release-notes.md"),
)

internal fun releaseNotesAfter(lastSeenCode: Int, installedCode: Int): List<BundledReleaseNotes> =
    publishedReleaseNotes.filter { it.versionCode > lastSeenCode && it.versionCode <= installedCode }
