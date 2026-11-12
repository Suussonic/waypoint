package com.waypoint.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import org.json.JSONArray

/** Isolated Preview-only GPS replay source for exercising Maps and Waypoint on one road trace. */
class MockRouteProviderService : Service() {
    private data class Fix(val latitude: Double,val longitude: Double,val speed: Float,val bearing: Float,val delayMs: Long)

    private val handler=Handler(Looper.getMainLooper())
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private var fixes=emptyList<Fix>()
    private var index=0
    private var providerAdded=false

    private val emitFix=object: Runnable {
        override fun run() {
            if(index !in fixes.indices) { stopSelf();return }
            val fix=fixes[index++]
            val location=Location(LocationManager.GPS_PROVIDER).apply {
                latitude=fix.latitude
                longitude=fix.longitude
                accuracy=4f
                speed=fix.speed.coerceAtLeast(0f)
                bearing=((fix.bearing%360f)+360f)%360f
                time=System.currentTimeMillis()
                elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()
                if(Build.VERSION.SDK_INT>=26) {
                    speedAccuracyMetersPerSecond=1.0f
                    bearingAccuracyDegrees=5.0f
                }
            }
            runCatching { locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER,location) }
                .onFailure { stopSelf();return }
            handler.postDelayed(this,fix.delayMs.coerceIn(500L,3000L))
        }
    }

    override fun onBind(intent: Intent?): IBinder?=null

    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action==ACTION_STOP) { stopSelf();return START_NOT_STICKY }
        val encoded=intent?.getStringExtra(EXTRA_TRACE64)
        if(encoded.isNullOrBlank()) { stopSelf();return START_NOT_STICKY }
        if(encoded.length>MAX_ENCODED_TRACE_CHARS) { stopSelf();return START_NOT_STICKY }
        fixes=runCatching { parseTrace(Base64.decode(encoded,Base64.DEFAULT)) }.getOrDefault(emptyList())
        if(fixes.size<2) { stopSelf();return START_NOT_STICKY }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER,false,false,false,false,
                true,true,true,Criteria.POWER_HIGH,Criteria.ACCURACY_FINE)
            providerAdded=true
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER,true)
            index=0
            handler.removeCallbacks(emitFix)
            handler.post(emitFix)
        } catch(_: SecurityException) {
            stopSelf()
        } catch(_: IllegalArgumentException) {
            // An existing provider is never removed or replaced by the test helper.
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(emitFix)
        if(providerAdded) {
            runCatching { locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER,false) }
            runCatching { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) }
            providerAdded=false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun notification(): Notification {
        val stop=PendingIntent.getService(this,NOTIFICATION_ID,
            Intent(this,MockRouteProviderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Waypoint test route")
            .setContentText("Replaying a temporary route through the mock GPS provider")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause,"Stop",stop)
            .build()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL_ID,"Test route replay",NotificationManager.IMPORTANCE_LOW))
    }

    private fun parseTrace(bytes: ByteArray): List<Fix> {
        if(bytes.size !in 2..MAX_TRACE_BYTES) return emptyList()
        val json=JSONArray(String(bytes,Charsets.UTF_8))
        if(json.length() !in 2..MAX_TRACE_FIXES) return emptyList()
        val fixes=ArrayList<Fix>(json.length())
        for(index in 0 until json.length()) {
            val row=json.optJSONArray(index) ?: return emptyList()
            if(row.length() !in 2..5) return emptyList()
            val latitude=row.optDouble(0,Double.NaN)
            val longitude=row.optDouble(1,Double.NaN)
            val speed=row.optDouble(2,8.0).toFloat()
            val bearing=row.optDouble(3,0.0).toFloat()
            if(!latitude.isFinite() || latitude !in -90.0..90.0 ||
                !longitude.isFinite() || longitude !in -180.0..180.0 ||
                !speed.isFinite() || speed !in 0f..150f ||
                !bearing.isFinite() || bearing !in 0f..360f) return emptyList()
            fixes.add(Fix(latitude,longitude,speed,bearing,row.optLong(4,1000L).coerceIn(500L,3000L)))
        }
        return fixes
    }

    companion object {
        const val ACTION_STOP="com.waypoint.app.preview.STOP_MOCK_ROUTE"
        const val EXTRA_TRACE64="trace64"
        private const val CHANNEL_ID="waypoint-test-route"
        private const val NOTIFICATION_ID=7403
        private const val MAX_TRACE_BYTES=256*1024
        private const val MAX_TRACE_FIXES=2_000
        private const val MAX_ENCODED_TRACE_CHARS=((MAX_TRACE_BYTES+2)/3)*4
    }
}
