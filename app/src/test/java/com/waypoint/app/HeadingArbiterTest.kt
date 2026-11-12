package com.waypoint.app
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
class HeadingArbiterTest {
    private fun matrix(yaw:Double,pitch:Double):FloatArray {
        val y=Math.toRadians(yaw);val p=Math.toRadians(pitch)
        return floatArrayOf(cos(y).toFloat(),(sin(y)*cos(p)).toFloat(),(-sin(y)*sin(p)).toFloat(),
            -sin(y).toFloat(),(cos(y)*cos(p)).toFloat(),(-cos(y)*sin(p)).toFloat(),0f,sin(p).toFloat(),cos(p).toFloat())
    }
    @Test fun uprightPhoneKeepsHeadingWithoutAzimuthSingularity(){for(p in listOf(0.0,45.0,80.0,89.0,90.0,-80.0))assertEquals("pitch $p",72f,projectedPhoneHeading(matrix(72.0,p))!!,.01f)}
    @Test fun phoneProjectionHandlesEveryCardinalDirection(){for(y in listOf(0.0,90.0,180.0,270.0,359.0))assertEquals(y.toFloat(),projectedPhoneHeading(matrix(y,90.0))!!,.01f)}
    @Test fun invalidMatrixDoesNotInventHeading(){assertNull(projectedPhoneHeading(FloatArray(9)));assertNull(projectedPhoneHeading(FloatArray(9){Float.NaN}))}
    @Test fun stationaryGpsBearingCannotOverrideCompass(){val a=HeadingArbiter();a.compass(40f,true,"Compass",100);assertEquals(40f,a.location(180f,0f,5f,.2f,true,TravelMode.CAR,200).degrees,0f)}
    @Test fun routeDirectionTakesPriorityOverCompassWhileGuiding(){val a=HeadingArbiter();a.route(90f,100);a.compass(40f,true,"Compass",200);assertEquals(HeadingSource.ROUTE,a.current(250).source);assertEquals(90f,a.current(250).degrees,0f)}
    @Test fun accurateWalkingCourseCanGuideAtWalkingSpeed(){val a=HeadingArbiter();assertEquals(HeadingSource.COURSE,a.location(90f,1.4f,12f,.2f,true,TravelMode.WALK,200).source)}
    @Test fun noisyCourseAndSpeedAreRejected(){val a=HeadingArbiter();a.compass(40f,true,"Compass",100);assertEquals(HeadingSource.COMPASS,a.location(90f,4f,60f,.2f,true,TravelMode.CAR,200).source);assertEquals(HeadingSource.COMPASS,a.location(90f,4f,10f,4f,true,TravelMode.CAR,300).source)}
    @Test fun freshMovingCourseWinsOverPhoneRotation(){val a=HeadingArbiter();a.location(80f,12f,8f,.2f,true,TravelMode.CAR,100);assertEquals(80f,a.compass(240f,true,"Compass",200).degrees,0f)}
    @Test fun speedHysteresisPreventsSourceChatter(){val a=HeadingArbiter();a.location(80f,3f,8f,.2f,true,TravelMode.CAR,100);assertEquals(HeadingSource.COURSE,a.location(82f,2f,8f,.2f,true,TravelMode.CAR,200).source);a.compass(40f,true,"Compass",200);assertEquals(HeadingSource.COMPASS,a.location(84f,.3f,8f,.2f,true,TravelMode.CAR,300).source)}
    @Test fun staleCourseFallsBackToFreshCompass(){val a=HeadingArbiter();a.location(80f,12f,8f,.2f,true,TravelMode.CAR,100);assertEquals(HeadingSource.COMPASS,a.compass(40f,true,"Compass",3000).source)}
    @Test fun reacquiredCourseMustPassEntrySpeedAfterExpiry(){val a=HeadingArbiter();a.location(80f,3f,8f,.2f,true,TravelMode.CAR,100);a.compass(40f,true,"Compass",3000);assertEquals(HeadingSource.COMPASS,a.location(82f,2f,8f,.2f,true,TravelMode.CAR,3100).source)}
    @Test fun staleOrUnreliableSensorsProduceAnUndirectedPosition(){val a=HeadingArbiter();a.compass(40f,false,"Calibrate",100);assertFalse(a.current(200).reliable);a.compass(40f,true,"Compass",300);assertFalse(a.current(3000).reliable)}
    @Test fun accuracyCircleHasMeasuredRadiusAcrossLatitudes(){for(lat in listOf(0.0,45.0,80.0)) {val c=Coordinate(lat,179.999);val ring=accuracyBoundary(c,80.0);assertEquals(49,ring.size);ring.forEach{assertEquals(80.0,distance(c,it),.1)}}}
    @Test fun invalidAccuracyDoesNotDrawFalsePrecision(){assertTrue(accuracyBoundary(Coordinate(0.0,0.0),Double.NaN).isEmpty());assertTrue(accuracyBoundary(Coordinate(0.0,0.0),0.0).isEmpty())}
}
