package com.waypoint.app

import kotlin.math.abs

/**
 * Camera rules that must hold whatever the Compose effects are doing. They are plain Kotlin so the
 * behaviour the user sees (tilt only pitches, locate only recentres) is unit tested.
 */

/** A toggled tilt changes pitch only: no camera request, so no effect re-targets the user or destination. */
internal fun NavState.withTiltToggled(): NavState = copy(tilted = !tilted)

/** Camera direction: course/phone heading by default, or true north when the compass is toggled. */
internal fun mapCameraBearing(state: NavState, currentBearing: Float): Float = when {
    state.keepMapNorthUp -> 0f
    state.headingReliable -> state.bearing
    else -> currentBearing
}

/** A reliable heading may turn the camera; north-up is always a controlled orientation. */
internal fun mapCameraBearingIsControlled(state: NavState): Boolean =
    state.keepMapNorthUp || state.headingReliable

/** Smallest pitch change worth animating; below this the camera is already at the requested tilt. */
private const val TILT_EPSILON = 0.5

/** The pitch to ease to, or null when the camera is already there. Target, zoom and bearing are never part of it. */
internal fun pitchChange(currentTilt: Double, desiredTilt: Double): Double? =
    if (abs(currentTilt - desiredTilt) > TILT_EPSILON) desiredTilt else null

/** Distance the camera centre may sit from the user before the map counts as panned away from them. */
internal fun recentreThresholdMetres(metresPerPixel: Double): Double =
    maxOf(25.0, 40.0 * metresPerPixel.coerceIn(0.0, 5000.0))

/**
 * Whether the recentre control has something to do. While guiding, "away" is the follow policy's pause
 * after a pan (the padded driving camera never sits on the fix itself); browsing compares the camera
 * centre with the fix, so a deliberate pan or zoom elsewhere shows it and being centred hides it.
 */
internal fun cameraAwayFromUser(
    navigating: Boolean, followPaused: Boolean, cameraToFixMetres: Double?, metresPerPixel: Double,
): Boolean {
    if (navigating) return followPaused
    val gap = cameraToFixMetres ?: return false
    return gap > recentreThresholdMetres(metresPerPixel)
}

internal enum class LocateAction { REQUEST_PERMISSION, ENABLE_GPS, RECENTRE, NONE }

/** One decision for every locate / recentre control, so they cannot disagree. */
internal fun locateAction(hasPermission: Boolean, hasFix: Boolean, gpsReady: Boolean, away: Boolean): LocateAction = when {
    !hasPermission -> LocateAction.REQUEST_PERMISSION
    !hasFix || !gpsReady -> LocateAction.ENABLE_GPS
    away -> LocateAction.RECENTRE
    // Already centred and following: a tap must not change zoom, bearing or tilt under the user.
    else -> LocateAction.NONE
}

/**
 * Decides which [NavState.cameraRequest]s may move the camera. A request moves it once; a replay of the
 * same request (a style reload, new panel insets) is allowed only until the user takes the camera over,
 * and a passive request (first GPS fix) never overrides a pan the user has already made.
 */
internal class CameraRequestGate {
    private var applied = -1
    private var userMoved = false

    fun userMovedCamera() { userMoved = true }

    fun accept(request: Int, passive: Boolean): Boolean {
        val fresh = request != applied
        if (!fresh) return !userMoved
        applied = request
        if (passive && userMoved) return false
        userMoved = false
        return true
    }
}
