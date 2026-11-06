package com.waypoint.app

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AppMotionTest {
    @Test fun mainDestinationsRemainUsableWithAnimationsEnabledAndRemoved() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        val app=context.applicationContext as WaypointApplication
        check(context.packageName.endsWith(".preview"))
        val device=UiDevice.getInstance(inst)
        val scale=device.executeShellCommand("settings get global animator_duration_scale").trim()
        val oldIdle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=350
        val output=File(context.getExternalFilesDir(null),"app-motion-review").apply { mkdirs() }
        try {
            inst.runOnMainSync {
                app.navigation.completeOnboarding(listOf(World.LOS_SANTOS))
                app.navigation.markMapOpened()
                app.navigation.changeWorld(World.LOS_SANTOS)
                app.markWhatsNewSeen();app.updateAlerts.markExplained()
            }
            for(setting in listOf("1.0","0.0")) {
                device.executeShellCommand("settings put global animator_duration_scale $setting")
                ActivityScenario.launch(MainActivity::class.java).use {
                    assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10_000))
                    device.wait(Until.findObject(By.text("Open map")),1000)?.click()
                    for(destination in listOf("Worlds","Saved","Quests","Map","Quests","Worlds")) {
                        val control=device.wait(Until.findObject(By.desc(destination)),3000)
                            ?: device.wait(Until.findObject(By.text(destination)),3000)
                        assertNotNull("Destination $destination is reachable",control)
                        control!!.click()
                        val works=when(destination) {
                            "Worlds" -> device.wait(Until.hasObject(By.text("Find your next world")),3000)
                            "Saved" -> device.wait(Until.hasObject(By.text("Your places")),3000)
                            "Quests" -> device.wait(Until.hasObject(By.desc("Add a quest")),3000)
                            else -> device.wait(Until.hasObject(By.desc("Worlds")),1500) || device.wait(Until.hasObject(By.text("Worlds")),1500)
                        }
                        assertTrue("$destination works with animation scale $setting",works)
                        SystemClock.sleep(350)
                        assertTrue(device.takeScreenshot(File(output,"${destination.lowercase()}-$setting.png")))
                    }
                }
            }
        } finally {
            if(scale.toFloatOrNull()!=null)device.executeShellCommand("settings put global animator_duration_scale $scale")
            else device.executeShellCommand("settings delete global animator_duration_scale")
            Configurator.getInstance().waitForIdleTimeout=oldIdle
        }
    }
}
