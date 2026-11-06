package com.waypoint.app

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.SystemClock
import android.view.Surface
import kotlin.math.abs

/** Signed shortest angular displacement, including the north/360-degree boundary. */
fun headingDelta(from:Float,to:Float):Float = ((to-from+540f)%360f)-180f
fun smoothHeading(from:Float,to:Float,weight:Float=0.22f):Float = (from+headingDelta(from,to)*weight+360f)%360f

/** Android's rotation vector fuses gyroscope, accelerometer and magnetic north. */
class HeadingSensor(context:Context,private val onHeading:(Float,Boolean,String)->Unit):SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java)
    private val rotation=manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    private val accelerometer=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer=manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val matrix=FloatArray(9)
    private val remapped=FloatArray(9)
    private var headingErrorRadians:Float?=null
    private var gravity:FloatArray?=null
    private var magnetic:FloatArray?=null
    private var running=false
    private var accuracy=SensorManager.SENSOR_STATUS_ACCURACY_LOW
    private var lastEmit=0L
    private var smoothed:Float?=null
    private var declination=0f
    private var declinationFix:Coordinate?=null
    var displayRotation:Int=Surface.ROTATION_0
        set(value){if(field!=value){field=value;smoothed=null}}
    val description:String get()=when {
        rotation?.type==Sensor.TYPE_ROTATION_VECTOR->"Gyroscope + accelerometer + compass"
        rotation!=null->"Accelerometer + compass"
        accelerometer!=null && magnetometer!=null->"Accelerometer + compass"
        else->"Compass unavailable · GPS course only"
    }
    fun updateLocation(location:Location) {
        val fix=Coordinate(location.latitude,location.longitude)
        if (!fix.isValid()) return
        if(declinationFix==null || distance(declinationFix!!,fix)>10000) {
            val altitude = if(location.hasAltitude() && location.altitude.isFinite()) location.altitude.coerceIn(-12000.0,100000.0).toFloat() else 0f
            declination=GeomagneticField(location.latitude.toFloat(),location.longitude.toFloat(),altitude,System.currentTimeMillis()).declination
            declinationFix=fix
        }
    }
    fun start() {
        if(running)return
        smoothed=null;lastEmit=0L;accuracy=SensorManager.SENSOR_STATUS_ACCURACY_LOW;headingErrorRadians=null
        running=if(rotation!=null) manager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME)
        else if(accelerometer!=null && magnetometer!=null) {
            val a=manager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME)
            val m=manager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME)
            if(!a || !m)manager.unregisterListener(this)
            a && m
        } else false
        if(!running)onHeading(0f,false,"Compass unavailable · GPS course only")
    }
    fun stop() { manager.unregisterListener(this);running=false;gravity=null;magnetic=null }
    override fun onAccuracyChanged(sensor:Sensor?,value:Int) {
        if(sensor?.type in listOf(Sensor.TYPE_ROTATION_VECTOR,Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,Sensor.TYPE_MAGNETIC_FIELD))accuracy=value
    }
    override fun onSensorChanged(event:SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR,Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR->{
                SensorManager.getRotationMatrixFromVector(matrix,event.values)
                accuracy=event.accuracy
                headingErrorRadians=event.values.getOrNull(4)?.takeIf{it>=0 && it.isFinite()}
            }
            Sensor.TYPE_ACCELEROMETER->{gravity=event.values.copyOf();if(magnetic==null || !SensorManager.getRotationMatrix(matrix,null,gravity,magnetic))return}
            Sensor.TYPE_MAGNETIC_FIELD->{magnetic=event.values.copyOf();accuracy=event.accuracy;if(gravity==null || !SensorManager.getRotationMatrix(matrix,null,gravity,magnetic))return}
            else->return
        }
        val axes=when(displayRotation) {
            Surface.ROTATION_90->SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180->SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270->SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else->SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix,axes.first,axes.second,remapped)
        val projected=projectedPhoneHeading(remapped)
        val heading=projected?.let{(it+declination+360f)%360f} ?: smoothed ?: 0f
        smoothed=smoothed?.let{smoothHeading(it,heading)} ?: heading
        val now=SystemClock.elapsedRealtime()
        if(now-lastEmit<100)return
        lastEmit=now
        val reliable=projected!=null && accuracy>=SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM &&
            (headingErrorRadians==null || headingErrorRadians!!<=Math.toRadians(45.0))
        onHeading(smoothed!!,reliable,if(reliable)if(declinationFix==null)"Compass · magnetic north" else "Compass · true north" else "Compass needs calibration · move away from magnets")
    }
}
