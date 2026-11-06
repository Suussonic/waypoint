package com.waypoint.app

import kotlin.math.atan2
import kotlin.math.hypot

/** Bearing of the screen's forward direction, with a continuous upright-camera fallback.
 * Android rotation matrices map device vectors into east/north/up coordinates.
 * The top edge is used flat; the back-facing camera axis contributes as the top rises.
 */
fun projectedPhoneHeading(matrix:FloatArray):Float? {
    require(matrix.size==9)
    val east=matrix[1]-matrix[2]*matrix[7]
    val north=matrix[4]-matrix[5]*matrix[7]
    if(!east.isFinite() || !north.isFinite() || hypot(east,north)<.1f)return null
    return ((Math.toDegrees(atan2(east,north).toDouble()).toFloat()%360f)+360f)%360f
}
/** COURSE is the GPS (or derived) direction of travel; ROUTE is the bearing of the route line under the vehicle. */
enum class HeadingSource { NONE, COMPASS, COURSE, ROUTE;
    /** Sources that describe where the vehicle is heading, not where the phone is pointing. */
    val isTravelDirection: Boolean get() = this == COURSE || this == ROUTE
}

/** Initial great-circle bearing from a to b in degrees clockwise from true north. */
fun bearingBetween(a: Coordinate, b: Coordinate): Float {
    val p1 = Math.toRadians(a.lat); val p2 = Math.toRadians(b.lat); val dl = Math.toRadians(b.lon - a.lon)
    val y = kotlin.math.sin(dl) * kotlin.math.cos(p2)
    val x = kotlin.math.cos(p1) * kotlin.math.sin(p2) - kotlin.math.sin(p1) * kotlin.math.cos(p2) * kotlin.math.cos(dl)
    return ((Math.toDegrees(atan2(y, x)).toFloat() % 360f) + 360f) % 360f
}
data class HeadingReading(val degrees:Float,val reliable:Boolean,val source:HeadingSource,val status:String)

/** Rejects stationary GPS bearings, noisy speed, and stale measurements; avoids threshold chatter. */
class HeadingArbiter {
    private var compass:HeadingReading?=null
    private var compassAt=Long.MIN_VALUE
    private var course:Float?=null
    private var courseAt=Long.MIN_VALUE
    private var courseActive=false
    private var route:Float?=null
    private var routeAt=Long.MIN_VALUE
    private var last=0f
    fun compass(degrees:Float,reliable:Boolean,status:String,now:Long):HeadingReading {
        compass=HeadingReading(degrees,reliable && degrees.isFinite(),HeadingSource.COMPASS,status);compassAt=now
        return current(now)
    }
    fun location(bearing:Float?,speed:Float,bearingAccuracy:Float?,speedAccuracy:Float?,fixReliable:Boolean,mode:TravelMode,now:Long):HeadingReading {
        if(now-courseAt !in 0..2500)courseActive=false
        val enter=if(mode==TravelMode.WALK)1.1f else 2.5f
        val exit=if(mode==TravelMode.WALK).65f else 1.5f
        val threshold=if(courseActive)exit else enter
        val valid=fixReliable && bearing!=null && bearing.isFinite() && speed.isFinite() && speed>=threshold &&
            (bearingAccuracy==null || bearingAccuracy in 0f..25f) &&
            (speedAccuracy==null || speedAccuracy>=0f && speed-speedAccuracy>if(mode==TravelMode.WALK).35f else .8f)
        courseActive=valid
        if(valid){course=((bearing!!%360)+360)%360;courseAt=now}
        return current(now)
    }
    /** While navigating on the route, face along the route line when there is no valid GPS course. */
    fun route(bearing:Float,now:Long):HeadingReading {
        if(bearing.isFinite()){route=((bearing%360)+360)%360;routeAt=now}
        return current(now)
    }
    fun clearRoute(){route=null;routeAt=Long.MIN_VALUE}
    fun current(now:Long):HeadingReading {
        if(courseActive && course!=null && now-courseAt in 0..2500) {
            last=course!!;return HeadingReading(last,true,HeadingSource.COURSE,"GPS course · direction of travel")
        }
        if(route!=null && now-routeAt in 0..2500) {
            last=route!!;return HeadingReading(last,true,HeadingSource.ROUTE,"Route direction · along the road")
        }
        val c=compass
        if(c!=null && now-compassAt in 0..2000 && c.reliable) {
            last=c.degrees;return c
        }
        return HeadingReading(last,false,HeadingSource.NONE,if(c!=null && now-compassAt in 0..2000)c.status else "Heading unavailable · waiting for sensors")
    }
    fun reset(){route=null;routeAt=Long.MIN_VALUE;compass=null;course=null;compassAt=Long.MIN_VALUE;courseAt=Long.MIN_VALUE;courseActive=false}
}

/** Geographic accuracy circle in metres, independent of zoom or map rotation. */
fun accuracyBoundary(center:Coordinate,meters:Double):List<Coordinate> {
    if(!meters.isFinite() || meters<=0 || meters>10000)return emptyList()
    val radius=meters/6371000.0
    val lat=Math.toRadians(center.lat);val lon=Math.toRadians(center.lon)
    return (0..48).map { i->
        val bearing=2*Math.PI*i/48
        val latitude=kotlin.math.asin(kotlin.math.sin(lat)*kotlin.math.cos(radius)+kotlin.math.cos(lat)*kotlin.math.sin(radius)*kotlin.math.cos(bearing))
        val longitude=lon+kotlin.math.atan2(kotlin.math.sin(bearing)*kotlin.math.sin(radius)*kotlin.math.cos(lat),kotlin.math.cos(radius)-kotlin.math.sin(lat)*kotlin.math.sin(latitude))
        Coordinate(Math.toDegrees(latitude),((Math.toDegrees(longitude)+540)%360)-180)
    }
}
