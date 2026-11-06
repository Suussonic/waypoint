package com.waypoint.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

/** Automatic release checks, with notification delivery controlled by Android's permissions. */
class UpdateAlerts(private val context:Context) {
    private val preferences=context.getSharedPreferences("update-alerts",Context.MODE_PRIVATE)
    private val notifications=context.getSystemService(NotificationManager::class.java)
    private val scheduler=context.getSystemService(JobScheduler::class.java)
    val explained:Boolean get()=preferences.getBoolean("permissionsExplained",false)
    fun markExplained() { preferences.edit().putBoolean("permissionsExplained",true).apply() }
    fun allowed():Boolean=(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) &&
        notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    fun batteryUnrestricted():Boolean=context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    fun scheduled():Boolean=scheduler.getPendingJob(JOB_ID)!=null

    fun schedule() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL,"App updates",NotificationManager.IMPORTANCE_LOW).apply {
            description="New Waypoint releases. Downloads and installation are always your choice."
        })
        if(!scheduled())runCatching {
            scheduler.schedule(JobInfo.Builder(JOB_ID,ComponentName(context,UpdateCheckJob::class.java))
                .setPeriodic(TimeUnit.HOURS.toMillis(12),TimeUnit.HOURS.toMillis(2))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true)
                .setPersisted(true).setBackoffCriteria(TimeUnit.MINUTES.toMillis(30),JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
        }
        if(preferences.getInt("lastNotifiedVersion",0)<=BuildConfig.VERSION_CODE)notifications.cancel(NOTIFICATION_ID)
    }

    /** Main-thread only: final permission/driving checks also cover a change during the HTTP call. */
    fun notifyIfNew(release:ReleaseUpdate):Boolean {
        val app=context.applicationContext as WaypointApplication
        if(!shouldAnnounce(release.versionCode,BuildConfig.VERSION_CODE,preferences.getInt("lastNotifiedVersion",0),allowed(),app.navigation.isDriving))return false
        val open=PendingIntent.getActivity(context,NOTIFICATION_ID,
            Intent(context,MainActivity::class.java).setAction(OPEN_UPDATES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            notifications.notify(NOTIFICATION_ID,NotificationCompat.Builder(context,CHANNEL)
                .setSmallIcon(R.drawable.ic_navigation)
                .setContentTitle("Waypoint ${release.version} is available")
                .setContentText("See what's new and choose when to update.")
                .setCategory(NotificationCompat.CATEGORY_STATUS).setContentIntent(open)
                .setAutoCancel(true).setOnlyAlertOnce(true).build())
            preferences.edit().putInt("lastNotifiedVersion",release.versionCode).apply()
            true
        } catch(_:SecurityException) { false }
    }

    fun openNotificationSettings()=openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID,CHANNEL))
    fun openBatterySettings()=openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    private fun openSettings(intent:Intent) {
        val fallback=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
    companion object {
        const val CHANNEL="app_updates"
        const val JOB_ID=7001
        const val NOTIFICATION_ID=7002
        const val OPEN_UPDATES="com.waypoint.app.OPEN_UPDATES"
        internal fun shouldAnnounce(version:Int,installed:Int,lastNotified:Int,allowed:Boolean,driving:Boolean)=
            version>installed && version>lastNotified && allowed && !driving
    }
}
