package com.waypoint.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WelcomeUiTest {
    @Test fun welcomeSelectsAWorldBeforeOpeningTheMap() {
        val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext;val device=UiDevice.getInstance(inst)
        val output=File(context.getExternalFilesDir(null),"atlas-review").apply{mkdirs()}
        val config=Configurator.getInstance();val idle=config.waitForIdleTimeout;config.waitForIdleTimeout=350
        val font=device.executeShellCommand("settings get system font_scale").trim()
        val animation=device.executeShellCommand("settings get global animator_duration_scale").trim()
        val prefs=context.getSharedPreferences("waypoint",0)
        val oldOnboarding=prefs.getBoolean("onboardingCompleted",false)
        val oldMapOpened=prefs.getBoolean("mapOpened",false)
        val oldWelcomeVersion=prefs.getInt("welcomeShownVersion",0)
        val oldFavorites=prefs.getString("favoriteWorlds",null)
        try {
            prefs.edit().putBoolean("onboardingCompleted",false).putBoolean("mapOpened",false)
                .putInt("welcomeShownVersion",0).putString("favoriteWorlds",World.NIGHT_CITY.name).commit()
            inst.runOnMainSync{(context.applicationContext as WaypointApplication).navigation.clearDestination()}
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.desc("Continue")),10000));SystemClock.sleep(700)
            device.takeScreenshot(File(output,"welcome.png"))
            device.executeShellCommand("settings put system font_scale 1.3");SystemClock.sleep(1000)
            device.takeScreenshot(File(output,"welcome-large-text.png"))
            device.executeShellCommand("settings put global animator_duration_scale 0")
            device.executeShellCommand("settings put system font_scale 2.0")
            SystemClock.sleep(1000)
            device.takeScreenshot(File(output,"welcome-large-reduced-motion.png"))
            assertTrue(device.hasObject(By.desc("Continue")))
            device.executeShellCommand("settings put system font_scale $font")
            fun continuePage() {
                assertTrue(device.wait(Until.hasObject(By.desc("Continue")),5000))
                device.findObject(By.desc("Continue")).click()
            }
            continuePage();assertTrue(device.wait(Until.hasObject(By.text("Favorite Game")),5000))
            continuePage();assertTrue(device.wait(Until.hasObject(By.text("your phone")),5000))
            continuePage();assertTrue(device.wait(Until.hasObject(By.text("Where did you hear\nabout us?")),5000))
            device.findObject(By.text("GitHub")).click();continuePage()
            assertTrue(device.wait(Until.hasObject(By.text("What are your\nfavorite games?")),5000))
            device.findObject(By.text("The Witcher 3: Wild Hunt")).click();continuePage()
            assertTrue(device.wait(Until.hasObject(By.text("Select Map to")),5000))
            device.findObject(By.desc("Next map")).click()
            assertTrue(device.wait(Until.hasObject(By.text("The Rivia III")),5000))
            device.takeScreenshot(File(output,"witcher-picker.png"))
            continuePage();assertTrue(device.wait(Until.hasObject(By.text("Allow location for")),5000))
            val finish=device.wait(Until.findObject(By.text("Open map")),1000)
                ?: device.wait(Until.findObject(By.text("Continue without location")),4000)
            assertNotNull(finish);finish!!.click()
            assertTrue(device.wait(Until.hasObject(By.text("Worlds")),5000))
            assertEquals(World.RIVIA,(context.applicationContext as WaypointApplication).navigation.state.value.world)
        } catch(error:Throwable) {
            device.takeScreenshot(File(output,"welcome-failure.png"));device.dumpWindowHierarchy(File(output,"welcome-failure.xml"));throw error
        } finally {
            val restore=prefs.edit().putBoolean("onboardingCompleted",oldOnboarding)
                .putBoolean("mapOpened",oldMapOpened).putInt("welcomeShownVersion",oldWelcomeVersion)
            if(oldFavorites==null)restore.remove("favoriteWorlds") else restore.putString("favoriteWorlds",oldFavorites)
            restore.commit()
            device.executeShellCommand("settings put system font_scale $font")
            device.executeShellCommand("settings put global animator_duration_scale $animation")
            config.waitForIdleTimeout=idle
        }
    }

    @Test fun completedOnboardingDoesNotReturnForANewVersion() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("waypoint",0)
        val oldOnboarding=prefs.getBoolean("onboardingCompleted",false)
        val oldMapOpened=prefs.getBoolean("mapOpened",false)
        val oldWelcomeVersion=prefs.getInt("welcomeShownVersion",0)
        try {
            prefs.edit().putBoolean("onboardingCompleted",true).putBoolean("mapOpened",true)
                .putInt("welcomeShownVersion",1_001_000).commit()
            assertFalse((context.applicationContext as WaypointApplication).navigation.shouldShowLanding())
        } finally {
            prefs.edit().putBoolean("onboardingCompleted",oldOnboarding).putBoolean("mapOpened",oldMapOpened)
                .putInt("welcomeShownVersion",oldWelcomeVersion).commit()
        }
    }
}
