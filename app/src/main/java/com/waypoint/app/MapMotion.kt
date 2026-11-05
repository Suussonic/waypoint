package com.waypoint.app

import android.os.SystemClock
import android.view.Choreographer

/** Requests frames only while there is movement; cancellation releases captured native sources. */
internal class MapMotionFrames(private val render:(Long)->Boolean):Choreographer.FrameCallback {
    private val frames=Choreographer.getInstance()
    private var cancelled=false
    override fun doFrame(frameTimeNanos:Long) {
        if(!cancelled && render(SystemClock.elapsedRealtime()))frames.postFrameCallback(this)
    }
    fun start() { frames.postFrameCallback(this) }
    fun cancel() { cancelled=true;frames.removeFrameCallback(this) }
}

/** A bounded interpolation of accepted fixes, never extrapolated travel. Main-thread owned. */
internal class MapMotion {
    data class Pose(val point:Coordinate?,val bearing:Float)
    private var from:Coordinate?=null
    private var target:Coordinate?=null
    private var positionStart=0L
    private var positionDuration=0L
    private var lastFixAt:Long?=null
    private var bearingFrom=0f
    private var bearingTarget=0f
    private var bearingStart=0L
    private var bearingDuration=0L

    fun update(point:Coordinate?,bearing:Float,now:Long,course:Boolean,animate:Boolean=true) {
        val current=sample(now)
        val newFix=point!=target
        if(newFix) {
            // Fused location normally supplies 1 Hz fixes (occasionally 2 Hz).
            // Finish near the next fix, not halfway through the interval. A long
            // interruption must not make recovery crawl for several seconds.
            val interval=lastFixAt?.let { now-it } ?: 1000L
            val cadence=if(interval in 250L..1500L)interval.coerceAtLeast(500L) else 1000L
            from=current.point;target=point;positionStart=now;lastFixAt=now
            positionDuration=if(animate && from!=null && point!=null && distance(from!!,point)<150.0)cadence else 0L
        }
        if(kotlin.math.abs(headingDelta(bearingTarget,bearing))>.01f) {
            bearingFrom=current.bearing;bearingTarget=bearing;bearingStart=now
            bearingDuration=if(!animate)0L else if(course && newFix && positionDuration>0)positionDuration else 180L
        }
        if(!animate) { positionDuration=0L;bearingDuration=0L }
    }

    fun sample(now:Long):Pose {
        val p=fraction(now,positionStart,positionDuration)
        val a=from;val b=target
        val point=if(a==null || b==null || p>=1.0)b else Coordinate(a.lat+(b.lat-a.lat)*p,
            wrappedLongitude(a.lon+shortestLongitudeDelta(a.lon,b.lon)*p))
        val heading=bearingFrom+headingDelta(bearingFrom,bearingTarget)*fraction(now,bearingStart,bearingDuration).toFloat()
        return Pose(point,(heading+360f)%360f)
    }

    fun remainingMillis(now:Long):Long=maxOf(positionStart+positionDuration-now,bearingStart+bearingDuration-now,0L)
    private fun fraction(now:Long,start:Long,duration:Long)=if(duration==0L)1.0 else ((now-start).toDouble()/duration).coerceIn(0.0,1.0)
}
