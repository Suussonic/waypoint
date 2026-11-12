package com.waypoint.app

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** The follow-camera pose for a driving navigation view. */
internal data class NavigationCameraPose(val zoom: Double, val tilt: Double, val bearing: Double)

private const val NAV_FAST_ZOOM = 16.2
private const val NAV_SPEED_LIMIT_MPS = 30.0
private const val NAV_TURN_ZOOM_FLOOR = 14.5
private const val NAV_TURN_MARGIN_DP = 72.0
private const val NAV_TILT_FULL_ZOOM = 15.0
private const val NAV_TILT_FLAT_ZOOM = 12.5
private const val NAV_TURN_YAW_CAP_DEGREES = 90.0
private const val NAV_TURN_YAW_BLEND_LEVELS = 1.0
private const val NAV_WORLD_PIXELS_AT_ZOOM_ZERO = 512.0
private const val NAV_EARTH_CIRCUMFERENCE_METERS = 40_075_016.686
private const val NAV_CAMERA_FOV_RADIANS = 0.6435011087932844 // 36.87 degrees
private val NON_ACTIONABLE_CAMERA_STEPS = setOf("depart", "continue", "new name")

/** Pull back gently with speed while keeping the road closer in the car viewport. */
internal fun navigationSpeedZoom(speedMetersPerSecond: Double): Double {
    if (!speedMetersPerSecond.isFinite()) return NAV_ZOOM
    val fraction = (speedMetersPerSecond / NAV_SPEED_LIMIT_MPS).coerceIn(0.0, 1.0)
    return NAV_ZOOM + (NAV_FAST_ZOOM - NAV_ZOOM) * fraction
}

/** A wide camera naturally flattens so the horizon and excess distant tiles do not dominate. */
internal fun navigationTiltAtZoom(zoom: Double, wantedTilt: Double = DRIVE_TILT): Double {
    if (!zoom.isFinite() || !wantedTilt.isFinite()) return 0.0
    val fraction = ((zoom - NAV_TILT_FLAT_ZOOM) /
        (NAV_TILT_FULL_ZOOM - NAV_TILT_FLAT_ZOOM)).coerceIn(0.0, 1.0)
    return wantedTilt.coerceIn(0.0, DRIVE_TILT) * fraction
}

/**
 * Zoom level that brings a maneuver to just below the turn card. Distances are projected through
 * the same pitched camera geometry as the map, so the camera zooms out for a far turn and back in
 * as it approaches. Returns NaN when the available viewport cannot frame the point safely.
 */
internal fun navigationTurnZoom(
    groundMeters: Double,
    latitude: Double,
    mapHeightPx: Int,
    density: Double,
    chromeBottomPx: Double,
    wantedTilt: Double = DRIVE_TILT,
    topPaddingFraction: Double = DRIVE_TOP_PADDING_FRACTION,
): Double {
    if (!groundMeters.isFinite() || groundMeters <= 0.0 || !latitude.isFinite() ||
        mapHeightPx <= 0 || !density.isFinite() || density <= 0.0 ||
        !chromeBottomPx.isFinite() || !topPaddingFraction.isFinite()) return Double.NaN

    val heightDp = mapHeightPx / density
    val arrowY = mapHeightPx * (1.0 + topPaddingFraction) / 2.0
    val upDp = (arrowY - chromeBottomPx) / density - NAV_TURN_MARGIN_DP
    if (heightDp <= 0.0 || upDp <= 0.0) return Double.NaN

    fun zoomAtTilt(tiltDegrees: Double): Double {
        val tilt = Math.toRadians(tiltDegrees)
        val cameraDistanceDp = 0.5 * heightDp / tan(NAV_CAMERA_FOV_RADIANS / 2.0)
        val angle = tilt + atan(upDp / cameraDistanceDp)
        if (angle >= PI / 2.0 - 1e-6) return Double.NaN
        val groundAheadDp = cameraDistanceDp * cos(tilt) * tan(angle) - cameraDistanceDp * kotlin.math.sin(tilt)
        if (!groundAheadDp.isFinite() || groundAheadDp <= 0.0) return Double.NaN
        val scale = NAV_EARTH_CIRCUMFERENCE_METERS * cos(Math.toRadians(latitude.coerceIn(-85.0, 85.0)))
        return log2(scale * groundAheadDp / (NAV_WORLD_PIXELS_AT_ZOOM_ZERO * groundMeters))
    }

    val atWantedTilt = zoomAtTilt(wantedTilt.coerceIn(0.0, DRIVE_TILT))
    if (atWantedTilt.isNaN() || navigationTiltAtZoom(atWantedTilt, wantedTilt) >= wantedTilt - 1e-6)
        return atWantedTilt

    var low = zoomAtTilt(0.0)
    var high = atWantedTilt
    if (!low.isFinite() || !high.isFinite() || low > high) return atWantedTilt
    repeat(12) {
        val middle = (low + high) / 2.0
        if (zoomAtTilt(navigationTiltAtZoom(middle, wantedTilt)) > middle) low = middle else high = middle
    }
    return (low + high) / 2.0
}

/** Apply a floor and let the speed zoom win again close to the maneuver. */
internal fun navigationZoomForTurn(speedZoom: Double, turnZoom: Double): Double =
    if (!turnZoom.isFinite()) speedZoom else min(speedZoom, max(turnZoom, NAV_TURN_ZOOM_FLOOR))

/** The distant turn can gently pull the heading toward its road; that yaw fades before arrival. */
internal fun navigationBearingTowardTurn(
    headingDegrees: Double,
    turnDegrees: Double,
    speedZoom: Double,
    turnZoom: Double,
): Double {
    if (!headingDegrees.isFinite() || !turnDegrees.isFinite() || !turnZoom.isFinite())
        return wrappedBearing(headingDegrees)
    val difference = ((turnDegrees - headingDegrees + 540.0) % 360.0) - 180.0
    val weight = ((speedZoom - turnZoom) / NAV_TURN_YAW_BLEND_LEVELS).coerceIn(0.0, 1.0)
    return wrappedBearing(headingDegrees + difference.coerceIn(-NAV_TURN_YAW_CAP_DEGREES, NAV_TURN_YAW_CAP_DEGREES) * weight)
}

/** Build a stable follow pose from the active route and measured navigation viewport. */
internal fun navigationCameraPose(
    state: NavState,
    currentBearing: Float,
    mapHeightPx: Int,
    density: Double,
    chromeBottomPx: Double,
    wantedTilt: Double,
    headingOverride: Float? = null,
): NavigationCameraPose {
    val baseBearing = when {
        state.keepMapNorthUp -> 0.0
        headingOverride != null && headingOverride.isFinite() -> headingOverride.toDouble()
        else -> mapCameraBearing(state, currentBearing).toDouble()
    }
    if (state.mode != TravelMode.CAR) {
        val zoom = NAV_ZOOM
        return NavigationCameraPose(zoom, navigationTiltAtZoom(zoom, wantedTilt), baseBearing)
    }

    val speedZoom = navigationSpeedZoom(state.speed.toDouble())
    val turn = if (!state.keepMapNorthUp && !state.routing && !state.offRoute) {
        nextCameraManeuver(state.route, state.progress?.nextStep ?: 0)
    } else null
    val origin = state.fix
    val rawTurnZoom = if (turn != null && origin != null) {
        navigationTurnZoom(
            groundMeters = distance(origin, turn.point), latitude = origin.lat,
            mapHeightPx = mapHeightPx, density = density, chromeBottomPx = chromeBottomPx,
            wantedTilt = wantedTilt,
        )
    } else Double.NaN
    val zoom = navigationZoomForTurn(speedZoom, rawTurnZoom)
    val bearing = if (turn != null && origin != null && rawTurnZoom.isFinite()) {
        navigationBearingTowardTurn(
            baseBearing, bearingBetween(origin, turn.point).toDouble(), speedZoom, rawTurnZoom,
        )
    } else baseBearing
    return NavigationCameraPose(zoom, navigationTiltAtZoom(zoom, wantedTilt), bearing)
}

private fun nextCameraManeuver(route: RoadRoute?, index: Int): Maneuver? {
    val steps = route?.steps ?: return null
    for (stepIndex in index.coerceIn(0, steps.size) until steps.size) {
        val maneuver = steps[stepIndex]
        if (maneuver.type !in NON_ACTIONABLE_CAMERA_STEPS) return maneuver
    }
    return null
}

private fun wrappedBearing(value: Double): Double = if (value.isFinite()) (value % 360.0 + 360.0) % 360.0 else 0.0
