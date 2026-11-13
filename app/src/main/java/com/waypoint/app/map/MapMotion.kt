package com.waypoint.app

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import android.view.Choreographer

private const val LOW_MEMORY_MAP_FRAME_INTERVAL_MILLIS=33L
private const val LOW_MEMORY_DEVICE_RAM_BYTES=4L*1024L*1024L*1024L

internal fun mapMotionFrameIntervalMillis(context:Context):Long {
    val activityManager=context.getSystemService(ActivityManager::class.java)
    val memoryInfo=ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memoryInfo)
    val constrained=activityManager?.isLowRamDevice==true ||
        memoryInfo.totalMem in 1L..LOW_MEMORY_DEVICE_RAM_BYTES
    return if(constrained)LOW_MEMORY_MAP_FRAME_INTERVAL_MILLIS else 0L
}

/** Requests frames only while there is movement; cancellation releases captured native sources. */
internal class MapMotionFrames(
    private val minimumFrameIntervalMillis:Long=0L,
    private val render:(Long)->Boolean,
):Choreographer.FrameCallback {
    private val frames=Choreographer.getInstance()
    private var cancelled=false
    private var lastRenderedFrameNanos=0L
    override fun doFrame(frameTimeNanos:Long) {
        if(cancelled)return
        val minimumFrameIntervalNanos=minimumFrameIntervalMillis.coerceAtLeast(0L)*1_000_000L
        if(minimumFrameIntervalNanos==0L || lastRenderedFrameNanos==0L ||
            frameTimeNanos-lastRenderedFrameNanos>=minimumFrameIntervalNanos) {
            lastRenderedFrameNanos=frameTimeNanos
            if(!render(SystemClock.elapsedRealtime()))return
        }
        if(!cancelled)frames.postFrameCallback(this)
    }
    fun start() { frames.postFrameCallback(this) }
    fun cancel() { cancelled=true;frames.removeFrameCallback(this) }
}

/** A bounded interpolation of accepted fixes, never extrapolated travel. Main-thread owned. */
internal class MapMotion {
    data class Pose(val point:Coordinate?,val bearing:Float,val cameraBearing:Float)
    private var from:Coordinate?=null
    private var target:Coordinate?=null
    private var positionStart=0L
    private var positionDuration=0L
    private var lastFixAt:Long?=null
    private var bearingFrom=0f
    private var bearingTarget=0f
    private var bearingStart=0L
    private var bearingDuration=0L
    private var cameraBearingFrom=0f
    private var cameraBearingTarget=0f
    private var cameraBearingStart=0L
    private var cameraBearingDuration=0L
    private var cameraBearingSeeded=false

    fun update(point:Coordinate?,bearing:Float,now:Long,course:Boolean,animate:Boolean=true,
               cameraBearing:Float=bearing,currentCameraBearing:Float=cameraBearing) {
        val wasCameraBearingSeeded=cameraBearingSeeded
        val current=sample(now)
        if(!cameraBearingSeeded) {
            cameraBearingFrom=normaliseBearing(currentCameraBearing)
            cameraBearingTarget=cameraBearingFrom
            cameraBearingSeeded=true
        }
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
        if(kotlin.math.abs(headingDelta(bearingTarget,bearing))>=MARKER_BEARING_DEADBAND_DEGREES) {
            bearingFrom=current.bearing;bearingTarget=bearing;bearingStart=now
            bearingDuration=if(!animate)0L else if(course && newFix && positionDuration>0)positionDuration else 180L
        }
        if(kotlin.math.abs(headingDelta(cameraBearingTarget,cameraBearing))>=CAMERA_BEARING_DEADBAND_DEGREES) {
            cameraBearingFrom=if(wasCameraBearingSeeded)current.cameraBearing else normaliseBearing(currentCameraBearing)
            cameraBearingTarget=normaliseBearing(cameraBearing)
            cameraBearingStart=now;cameraBearingDuration=if(animate)240L else 0L
        }
        if(!animate) { positionDuration=0L;bearingDuration=0L;cameraBearingDuration=0L }
    }

    fun sample(now:Long):Pose {
        val p=fraction(now,positionStart,positionDuration)
        val a=from;val b=target
        val point=if(a==null || b==null || p>=1.0)b else Coordinate(a.lat+(b.lat-a.lat)*p,
            wrappedLongitude(a.lon+shortestLongitudeDelta(a.lon,b.lon)*p))
        val heading=bearingFrom+headingDelta(bearingFrom,bearingTarget)*fraction(now,bearingStart,bearingDuration).toFloat()
        val cameraHeading=cameraBearingFrom+headingDelta(cameraBearingFrom,cameraBearingTarget)*fraction(now,cameraBearingStart,cameraBearingDuration).toFloat()
        return Pose(point,normaliseBearing(heading),normaliseBearing(cameraHeading))
    }

    fun remainingMillis(now:Long):Long=maxOf(positionStart+positionDuration-now,bearingStart+bearingDuration-now,cameraBearingStart+cameraBearingDuration-now,0L)
    private fun normaliseBearing(value:Float)=((value%360f)+360f)%360f
    private fun fraction(now:Long,start:Long,duration:Long)=if(duration==0L)1.0 else ((now-start).toDouble()/duration).coerceIn(0.0,1.0)
    private companion object {
        const val MARKER_BEARING_DEADBAND_DEGREES=.55f
        const val CAMERA_BEARING_DEADBAND_DEGREES=1.5f
    }
}

/** Holds the rendered position inside GPS noise while any transport mode is stopped. */
internal class StationaryPositionFilter {
    private var stable:Coordinate?=null
    private var candidate:Coordinate?=null

    fun update(point:Coordinate?,mode:TravelMode,speedMetersPerSecond:Float,accuracyMeters:Float):Coordinate? {
        if(point==null) {
            stable=point;candidate=null
            return point
        }
        val anchor=stable
        if(anchor==null) {
            stable=point;candidate=null
            return point
        }
        // A real moving speed exits the deadband immediately; the filter only masks stopped GPS drift.
        val movingThreshold=if(mode==TravelMode.WALK)WALKING_SPEED_MPS else VEHICLE_SPEED_MPS
        if(speedMetersPerSecond.isFinite() && speedMetersPerSecond>=movingThreshold) {
            stable=point;candidate=null
            return point
        }

        val accuracy=accuracyMeters.takeIf{it.isFinite() && it>0f}?.toDouble() ?: 0.0
        val deadband=maxOf(MIN_STATIONARY_DEADBAND_METERS,(accuracy*0.75).coerceAtMost(MAX_STATIONARY_DEADBAND_METERS))
        if(distance(anchor,point)<=deadband) {
            candidate=null
            return anchor
        }

        // If speed is temporarily unavailable, a second nearby fix confirms that travel is real.
        val previousCandidate=candidate
        val confirmationRadius=(accuracy*0.6).coerceIn(MIN_CONFIRMATION_RADIUS_METERS,MAX_CONFIRMATION_RADIUS_METERS)
        if(previousCandidate!=null && distance(previousCandidate,point)<=confirmationRadius && distance(anchor,point)>deadband) {
            stable=point;candidate=null
            return point
        }
        candidate=point
        return anchor
    }

    private companion object {
        const val VEHICLE_SPEED_MPS=0.8f
        const val WALKING_SPEED_MPS=0.25f
        const val MIN_STATIONARY_DEADBAND_METERS=4.0
        const val MAX_STATIONARY_DEADBAND_METERS=9.0
        const val MIN_CONFIRMATION_RADIUS_METERS=3.0
        const val MAX_CONFIRMATION_RADIUS_METERS=7.0
    }
}
