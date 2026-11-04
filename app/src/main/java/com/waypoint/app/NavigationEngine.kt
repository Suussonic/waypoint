package com.waypoint.app

import kotlin.math.*

data class Coordinate(val lat: Double, val lon: Double)
internal fun shortestLongitudeDelta(from: Double,to: Double): Double = ((to-from+540.0)%360.0)-180.0
internal fun wrappedLongitude(value: Double): Double = ((value+540.0)%360.0)-180.0
data class Place(val id: String, val name: String, val address: String, val point: Coordinate, val kind: String = "place", val source: String = "")
data class Maneuver(val point: Coordinate, val type: String, val modifier: String, val road: String, val distance: Double, val duration: Double, val exit: Int = 0,
    val uTurnRight: Boolean = false) {
    val instruction: String get() {
        val action = when (type) {
            "depart" -> "Head ${modifier.ifBlank { "straight" }}"
            "arrive" -> "Arrive at your destination"
            "roundabout", "rotary", "roundabout turn" -> if (exit > 0) "At the roundabout, take exit $exit" else "Enter the roundabout"
            "exit roundabout", "exit rotary" -> "Exit the roundabout"
            "merge" -> "Merge ${modifier.ifBlank { "ahead" }}"
            "on ramp" -> "Take the ramp ${modifier}"
            "off ramp" -> "Take the exit ${modifier}"
            "fork" -> "Keep ${modifier.ifBlank { "straight" }}"
            else -> when (modifier) {
                "uturn" -> "Make a U-turn"
                "left", "right" -> "Turn $modifier"
                "slight left", "slight right" -> "Bear ${modifier.removePrefix("slight ")}"
                "sharp left", "sharp right" -> "Turn $modifier"
                else -> "Continue straight"
            }
        }
        return if (road.isNotBlank() && type != "arrive") "$action onto $road" else action
    }
}

/** Preserve the provider's actual turn direction, including a crossing of north. */
internal fun isRightUTurn(before:Double?,after:Double?,drivingSide:String?):Boolean {
    if(before!=null && after!=null && before.isFinite() && after.isFinite() &&
        before in 0.0..360.0 && after in 0.0..360.0) {
        val change=((after-before+540.0)%360.0)-180.0
        if(abs(change)>1.0 && abs(change)<179.0)return change>0.0
    }
    // Exactly opposite bearings do not distinguish the arc. In left-hand traffic
    // a normal U-turn bends right; missing metadata retains the existing default.
    return drivingSide=="left"
}
data class RoadRoute(val points: List<Coordinate>, val steps: List<Maneuver>, val meters: Double, val seconds: Double, val mode:TravelMode=TravelMode.CAR,
    val ferrostarRoute: uniffi.ferrostar.Route? = null)
data class RouteProgress(val traveled: Double, val remaining: Double, val seconds: Double, val offRoute: Double, val nextStep: Int, val toTurn: Double, val snapped: Coordinate,
    val segmentIndex: Int = 0, val projectedAlong: Double = traveled,
    val remainingShape: List<Coordinate>? = null, val guidanceText: String? = null)

data class RouteGuidance(val instruction:String,val maneuver:Maneuver?,val continuingToArrival:Boolean)

/** Keep long final legs readable without announcing arrival hundreds of metres early. */
fun routeGuidance(route:RoadRoute?,progress:RouteProgress?):RouteGuidance {
    if(progress?.guidanceText!=null)return RouteGuidance(progress.guidanceText,route?.steps?.getOrNull(progress.nextStep),false)
    val step=route?.steps?.getOrNull(progress?.nextStep ?: 0)
        ?: return RouteGuidance("Finding your route",null,false)
    if(step.type=="arrive" && progress!=null && progress.toTurn>150.0) {
        val lastRoad=route.steps.getOrNull((progress.nextStep-1).coerceAtLeast(0))?.road.orEmpty()
        return RouteGuidance(if(lastRoad.isNotBlank())"Continue on $lastRoad" else "Continue toward your destination",step,true)
    }
    return RouteGuidance(step.instruction,step,false)
}

/** Trim at the accepted route projection, never at an off-road GPS coordinate. */
fun RoadRoute.remainingPoints(progress: RouteProgress?): List<Coordinate> = if (progress == null) points else
    progress.remainingShape ?: (listOf(progress.snapped) + points.drop((progress.segmentIndex + 1).coerceIn(1, points.lastIndex)))

fun distance(a: Coordinate, b: Coordinate): Double {
    val p = Math.toRadians(b.lat - a.lat); val q = Math.toRadians(b.lon - a.lon)
    val v = sin(p/2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(q/2).pow(2)
    return 6371000.0 * 2 * atan2(sqrt(v.coerceIn(0.0,1.0)), sqrt((1-v).coerceIn(0.0,1.0)))
}

/** Projects onto segments, not vertices, and uses route distance to sequence maneuvers. */
class NavigationEngine(val route: RoadRoute) {
    private val cumulative = DoubleArray(route.points.size).also { c ->
        for (i in 1 until c.size) c[i] = c[i-1] + distance(route.points[i-1], route.points[i])
    }
    val length: Double get() = cumulative.lastOrNull() ?: 0.0
    private val stepPositions = route.steps.map { it.point }.let { points ->
        var floor = 0.0
        points.map { p -> project(p, floor - 1, Double.POSITIVE_INFINITY).also { floor = it.along }.along }
    }
    private var lastProgress = 0.0
    private var progressPoint = route.points.first()
    private var progressSegment = 0
    private var initialized = false
    fun reset() { lastProgress = 0.0; initialized = false; progressPoint = route.points.first(); progressSegment = 0 }
    private data class Projection(val along: Double, val away: Double, val point: Coordinate, val segment: Int = 0)
    private fun project(p: Coordinate, minAlong: Double, maxAlong: Double): Projection {
        var best = Projection(0.0, Double.MAX_VALUE, route.points.firstOrNull() ?: p)
        for (i in 0 until route.points.size - 1) {
            if (cumulative[i+1] < minAlong || cumulative[i] > maxAlong) continue
            val a = route.points[i]; val b = route.points[i+1]
            val scale = cos(Math.toRadians(p.lat))
            val ax = (a.lon-p.lon)*scale; val ay = a.lat-p.lat
            val dx = (b.lon-a.lon)*scale; val dy = b.lat-a.lat
            val span = cumulative[i+1]-cumulative[i]
            val low = if(span > 0) ((minAlong-cumulative[i])/span).coerceIn(0.0,1.0) else 0.0
            val high = if(span > 0) ((maxAlong-cumulative[i])/span).coerceIn(low,1.0) else 1.0
            val t = if (dx*dx+dy*dy == 0.0) low else (-(ax*dx+ay*dy)/(dx*dx+dy*dy)).coerceIn(low,high)
            val snap = Coordinate(a.lat+(b.lat-a.lat)*t, a.lon+(b.lon-a.lon)*t)
            val along = cumulative[i]+(cumulative[i+1]-cumulative[i])*t
            val away = distance(p,snap)
            if (away < best.away) best = Projection(along,away,snap,i)
        }
        return best
    }
    fun update(p: Coordinate, movementAllowance: Double = 500.0, matchTolerance: Double = 25.0): RouteProgress {
        val hit = project(p, if (initialized) max(0.0,lastProgress-250) else 0.0,
            if (initialized) lastProgress+movementAllowance else Double.POSITIVE_INFINITY)
        // Do not jump to a later parallel/crossing road on a noisy off-route fix.
        if (hit.away < matchTolerance && hit.along >= lastProgress) {
            lastProgress = hit.along; progressPoint = hit.point; progressSegment = hit.segment; initialized = true
        }
        val along = lastProgress.coerceAtMost(length)
        val index = stepPositions.indexOfFirst { it > along+8 }.let { if(it < 0) route.steps.lastIndex else it }
        val remaining = max(0.0,length-along)
        return RouteProgress(along, remaining, if(length>0) route.seconds*remaining/length else 0.0,
            hit.away,index.coerceAtLeast(0), max(0.0,(stepPositions.getOrNull(index) ?: length)-along),progressPoint,progressSegment,hit.along)
    }
    fun hasArrived(p: Coordinate, progress: RouteProgress): Boolean = progress.remaining < 35 && distance(p,route.points.last()) < 35
}

fun formatDistance(meters: Double, imperial: Boolean = false): String = if (imperial) {
    if(meters < 160.934) "${(meters*3.28084/10).roundToInt()*10} ft" else String.format(java.util.Locale.US,"%.1f mi",meters/1609.344)
} else if(meters < 1000) "${(meters/10).roundToInt()*10} m" else String.format(java.util.Locale.US,"%.1f km",meters/1000)
