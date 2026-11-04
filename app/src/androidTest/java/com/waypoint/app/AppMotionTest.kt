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
        check(context.packageName.endsWith(".preview"))
        val device=UiDevice.getInstance(inst)
        val scale=device.executeShellCommand("settings get global animator_duration_scale").trim()
        val oldIdle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=350
        val output=File(context.getExternalFilesDir(null),"app-motion-review").apply { mkdirs() }
        try {
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
                        val expected=when(destination) {
                            "Worlds" -> By.text("Find your next world")
                            "Saved" -> By.text("Your places")
                            "Quests" -> By.desc("Add a quest")
                            else -> By.desc("Worlds")
                        }
                        assertTrue("$destination works with animation scale $setting",device.wait(Until.hasObject(expected),3000))
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
