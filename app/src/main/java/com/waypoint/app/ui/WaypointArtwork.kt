package com.waypoint.app

import androidx.annotation.DrawableRes

/** Retained artwork used by Waypoint map controls and world cards. */
@DrawableRes
internal fun waypointControlArtworkFor(label: String): Int? = when (label) {
    "Set a destination", "Search places", "Find a place" -> R.drawable.waypoint_search
    "Use my location", "Locate" -> R.drawable.waypoint_crosshair
    "Toggle 2D and 3D" -> R.drawable.waypoint_compass
    "Settings" -> R.drawable.waypoint_settings
    "Saved" -> R.drawable.waypoint_bookmark
    "Worlds" -> R.drawable.waypoint_worlds
    else -> null
}

@DrawableRes
internal fun waypointWorldCardArtworkFor(world: World): Int? = when (world) {
    World.LOS_SANTOS -> R.drawable.waypoint_card_los_santos
    World.NIGHT_CITY -> R.drawable.waypoint_card_night_city
    World.PIXEL -> R.drawable.waypoint_card_pixel
    World.OLD_WEST -> R.drawable.waypoint_card_old_west
    World.SAN_ANDREAS -> R.drawable.waypoint_card_san_andreas
    else -> null
}
