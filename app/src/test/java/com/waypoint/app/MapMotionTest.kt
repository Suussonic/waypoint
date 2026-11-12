package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class MapMotionTest {
    private val start=Coordinate(51.5081,-.1280)
    private val end=Coordinate(51.5082,-.1280)

    @Test fun duplicateSensorUpdatesDoNotDelayGpsOrHeading() {
        val motion=MapMotion()
        motion.update(start,350f,0,true,false)
        motion.update(end,10f,1000,true)
        for(now in 1050L..1950L step 50)motion.update(end,10f,now,true)
        val pose=motion.sample(1750)
        assertEquals(start.lat+(end.lat-start.lat)*.75,pose.point!!.lat,1e-10)
        assertEquals(5f,pose.bearing,.01f)
        assertEquals(end,motion.sample(2000).point)
        assertEquals(0L,motion.remainingMillis(2000))
    }

    @Test fun retargetingStartsAtTheRenderedPositionWithoutOvershoot() {
        val motion=MapMotion()
        motion.update(start,0f,0,true)
        motion.update(end,90f,1000,true)
        val before=motion.sample(1500)
        val next=Coordinate(51.5083,-.1280)
        motion.update(next,100f,1500,true)
        assertEquals(before,motion.sample(1500))
        val midway=motion.sample(1750).point!!
        assertTrue(midway.lat>before.point!!.lat && midway.lat<next.lat)
        assertEquals(next,motion.sample(2000).point)
    }

    @Test fun cameraOrientationTakesTheShortestArcAndFinishesAtItsTarget() {
        val motion=MapMotion()
        motion.update(start,350f,0,false,false,cameraBearing=350f,currentCameraBearing=350f)
        motion.update(start,10f,100,false,true,cameraBearing=10f,currentCameraBearing=350f)
        assertEquals(0f,motion.sample(220).cameraBearing,.01f)
        assertEquals(10f,motion.sample(340).cameraBearing,.01f)
        assertEquals(0L,motion.remainingMillis(340))
    }

    @Test fun smallCompassNoiseDoesNotRetargetTheFollowCamera() {
        val motion=MapMotion()
        motion.update(start,0f,0,false,false,cameraBearing=0f,currentCameraBearing=0f)
        for((time,bearing) in listOf(100L to .7f,200L to 1.1f,300L to .4f,400L to 1.3f))
            motion.update(start,0f,time,false,true,cameraBearing=bearing,currentCameraBearing=0f)
        assertEquals(0f,motion.sample(700).cameraBearing,.01f)
        motion.update(start,0f,800,false,true,cameraBearing=2f,currentCameraBearing=0f)
        assertEquals(2f,motion.sample(1040).cameraBearing,.01f)
        assertEquals(0L,motion.remainingMillis(1040))
    }

    @Test fun smallCompassNoiseDoesNotWobbleThePositionPointer() {
        val motion=MapMotion()
        motion.update(start,90f,0,false,false,cameraBearing=0f,currentCameraBearing=0f)
        for((time,bearing) in listOf(100L to 90.2f,200L to 90.4f,300L to 89.9f,400L to 90.5f))
            motion.update(start,bearing,time,false,true,cameraBearing=0f,currentCameraBearing=0f)
        assertEquals(90f,motion.sample(700).bearing,.01f)
        motion.update(start,91f,800,false,true,cameraBearing=0f,currentCameraBearing=0f)
        assertEquals(91f,motion.sample(980).bearing,.01f)
    }

    @Test fun lostFixLongGapAndRemoveAnimationsNeverInventTravel() {
        val motion=MapMotion()
        motion.update(start,0f,0,true)
        motion.update(end,45f,60_000,true)
        assertEquals(1000L,motion.remainingMillis(60_000))
        motion.update(null,45f,60_100,true)
        assertNull(motion.sample(60_100).point)
        motion.update(end,45f,60_200,true,false)
        assertEquals(end,motion.sample(60_200).point)
        assertEquals(0L,motion.remainingMillis(60_200))
        val remote=Coordinate(51.51,-.12)
        motion.update(remote,45f,61_000,true)
        assertEquals(remote,motion.sample(61_000).point)
    }

    @Test fun shortAntimeridianCrossingStaysNearTheDateline() {
        val motion=MapMotion()
        motion.update(Coordinate(0.0,179.9998),0f,0,false)
        motion.update(Coordinate(0.0,-179.9998),0f,1000,false)
        assertEquals(180.0,kotlin.math.abs(motion.sample(1500).point!!.lon),1e-8)
    }
}
