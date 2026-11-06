package com.waypoint.app

import kotlin.math.ceil

/**
 * Routes drawn quietly beside the chosen line on the projected map, matching the phone: the other
 * route options while previewing, and the fresh drive alternatives while guiding. Each keeps its
 * index in the list the controller will act on ([NavState.routeOptions] or [NavState.driveAlternatives]).
 */
internal fun carAlternateRoutes(state: NavState): List<IndexedValue<RoadRoute>> {
    val chosen = state.route ?: return emptyList()
    return when {
        state.navigating -> state.driveAlternatives.withIndex().toList()
        state.routeOptions.size < 2 -> emptyList()
        else -> state.routeOptions.withIndex().filter { it.value !== chosen }
    }
}

/** Time left on the chosen route while guiding, otherwise its full duration. */
internal fun carAlternateBaseSeconds(state: NavState): Double? =
    if (state.navigating) state.progress?.seconds ?: state.route?.seconds else state.route?.seconds

/** Rounded up like the phone's route chips, so the two never disagree by a minute. */
internal fun carAlternateGapLabel(routeSeconds: Double, baseSeconds: Double?): String {
    val gap = routeSeconds - (baseSeconds ?: routeSeconds)
    val minutes = if (gap >= 0) ceil(gap / 60.0).toInt() else -ceil(-gap / 60.0).toInt()
    return when {
        minutes > 0 -> "+$minutes min"
        minutes < 0 -> "−${-minutes} min"
        else -> "same time"
    }
}
