package com.waypoint.app

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class NavigationService : Service() {
    private val controller get() = (application as WaypointApplication).navigation
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var serviceSessionId: String?=null
    override fun onCreate() {
        super.onCreate()
        serviceSessionId=controller.navigationSessionId
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("navigation","Active navigation",NotificationManager.IMPORTANCE_LOW))
        startForeground(10,notification("Navigation is starting"))
        scope.launch {
            controller.state.map { s ->
                if(!s.navigating)null else if(!s.gpsReady)s.gpsStatus
                else if(s.routing)"Rerouting from your current position"
                else if(s.offRoute)"Off route · finding a new route"
                else routeGuidance(s.route,s.progress).instruction
            }.distinctUntilChanged().collect { text ->
                if(text!=null)getSystemService(NotificationManager::class.java).notify(10,notification(text))
            }
        }
    }
    private fun notification(text: String): Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val token=controller.navigationSessionId
        val stopIntent=Intent(this,NavigationService::class.java).setAction("STOP")
            .setData(Uri.parse("waypoint://navigation/stop/${token ?: "inactive"}"))
            .putExtra("navigation_session",token)
        val stop=PendingIntent.getService(this,1,stopIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this,"navigation").setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle("Waypoint · ${controller.state.value.selected?.name ?: "Navigation"}")
            .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION).addAction(0,"End navigation",stop).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action!="STOP")serviceSessionId=controller.navigationSessionId
        else {
            if(controller.acceptsNavigationSession(intent.getStringExtra("navigation_session"))) {
                controller.stopNavigation()
                stopSelf(startId)
            } else if(!controller.state.value.navigating) stopSelf(startId)
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); controller.serviceStopped(serviceSessionId); super.onDestroy() }
}
