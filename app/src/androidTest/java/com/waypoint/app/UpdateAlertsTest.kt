package com.waypoint.app

import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.ComponentName
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateAlertsTest {
    @Test fun declinedNotificationsLeaveDefaultChecksAndTheAppUsable() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val alerts=(context.applicationContext as WaypointApplication).updateAlerts
        assertFalse("Run the denial case with Android notifications denied", alerts.allowed())
        context.getSharedPreferences("update-alerts",0).edit().putBoolean("permissionsExplained",false).commit()
        val device=UiDevice.getInstance(inst)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent { MaterialTheme { UpdateAlertPermissions(alerts,true) } } }
            assertTrue(device.wait(Until.hasObject(By.text("Know when Waypoint is updated")),5000))
            val folder=File(context.getExternalFilesDir(null),"update-alert-review").apply{mkdirs()}
            device.takeScreenshot(File(folder,"notification-explanation.png"))
            device.findObject(By.text("Not now")).click()
            SystemClock.sleep(250)
            assertTrue(alerts.explained)
            assertTrue(alerts.scheduled())
            assertFalse(alerts.allowed())
        }
    }

    @Test fun defaultJobExplanationAndNotificationAreUsableWithoutAutomaticDownloads() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val app=context.applicationContext as WaypointApplication
        val device=UiDevice.getInstance(inst)
        val alerts=app.updateAlerts
        val manager=context.getSystemService(NotificationManager::class.java)
        val prefs=context.getSharedPreferences("update-alerts",0)
        val previous=prefs.getInt("lastNotifiedVersion",0)
        val output=File(context.getExternalFilesDir(null),"update-alert-review").apply{mkdirs()}
        try {
            inst.runOnMainSync { alerts.schedule() }
            val job=context.getSystemService(JobScheduler::class.java).getPendingJob(UpdateAlerts.JOB_ID)
            assertNotNull("Default background release check exists",job)
            assertTrue(job!!.isPeriodic)
            assertTrue(job.isPersisted)
            assertEquals(TimeUnit.HOURS.toMillis(12),job.intervalMillis)
            val service=context.packageManager.getServiceInfo(ComponentName(context,UpdateCheckJob::class.java),0)
            assertEquals("android.permission.BIND_JOB_SERVICE",service.permission)
            assertTrue("Only isolated Preview with notifications already allowed",alerts.allowed())
            prefs.edit().putBoolean("permissionsExplained",false).putInt("lastNotifiedVersion",0).commit()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    MaterialTheme(colorScheme=darkColorScheme()) { UpdateAlertPermissions(alerts,true) }
                } }
                if(!alerts.batteryUnrestricted()) {
                    assertTrue(device.wait(Until.hasObject(By.text("Keep update alerts timely")),5000))
                    assertTrue(device.takeScreenshot(File(output,"battery-explanation.png")))
                    device.findObject(By.text("Not now")).click()
                }
                SystemClock.sleep(300)
                assertTrue("Declining battery access leaves the app usable without repeated prompts",alerts.explained)
            }
            val version=BuildConfig.VERSION_NAME.substringBeforeLast('.')+".1"
            val release=ReleaseUpdate(version,BuildConfig.VERSION_CODE+1,"https://github.com/${ReleaseUpdate.REPOSITORY}/releases/download/v$version/Waypoint.apk","a".repeat(64),100,"Test fixture")
            inst.runOnMainSync {
                assertTrue("A simulated newer release posts one alert",alerts.notifyIfNew(release))
                assertFalse("The same version never reposts",alerts.notifyIfNew(release))
            }
            val notification=manager.activeNotifications.single{it.id==UpdateAlerts.NOTIFICATION_ID}.notification
            assertEquals("Waypoint $version is available",notification.extras.getString("android.title"))
            assertEquals(context.packageName,notification.contentIntent.creatorPackage)
            if(Build.VERSION.SDK_INT>=31)assertTrue(notification.contentIntent.isImmutable)
            notification.contentIntent.send()
            assertTrue(device.wait(Until.hasObject(By.text("App updates")),7000))
            assertTrue(device.takeScreenshot(File(output,"update-details.png")))
            assertFalse("Opening the alert never downloads an APK",app.updater.state.value is UpdateStatus.Downloading)
        } finally {
            manager.cancel(UpdateAlerts.NOTIFICATION_ID)
            prefs.edit().putInt("lastNotifiedVersion",previous).putBoolean("permissionsExplained",true).commit()
        }
    }
}
