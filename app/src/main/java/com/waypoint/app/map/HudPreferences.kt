package com.waypoint.app

/** Size of the controls drawn over the phone map. Tap targets stay at least 48 dp. */
enum class HudSize(val label: String) {
    COMPACT("Compact"), STANDARD("Standard"), LARGE("Large"),
}

/** An explicit side overrides a world's usual placement of its map tool cluster. */
enum class HudSide(val label: String) {
    LEFT("Left"), RIGHT("Right"),
}

internal fun hudSizeFromPreference(value: String?): HudSize =
    HudSize.entries.firstOrNull { it.name == value } ?: HudSize.STANDARD

internal fun hudSideFromPreference(value: String?): HudSide? =
    HudSide.entries.firstOrNull { it.name == value }
