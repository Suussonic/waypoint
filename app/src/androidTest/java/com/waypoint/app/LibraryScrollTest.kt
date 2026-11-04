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
class LibraryScrollTest {
    @Test fun catalogueScrollRemainsResponsive() {
        val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext
        val device=UiDevice.getInstance(inst);val config=Configurator.getInstance();val idle=config.waitForIdleTimeout
        config.waitForIdleTimeout=350
        val out=File(context.getExternalFilesDir(null),"atlas-review").apply{mkdirs()}
        try {
            SystemClock.sleep(1200) // Let a preceding font/rotation test finish its configuration change.
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10000))
            device.wait(Until.findObject(By.text("Open map")),1500)?.click()
            val picker=device.wait(Until.findObject(By.desc("Worlds")),1500) ?: device.findObject(By.text("Worlds")) ?: device.findObject(By.desc("HUDs")) ?: device.findObject(By.text("HUDs"))
            assertNotNull(picker);picker!!.click()
            assertNotNull(device.wait(Until.findObject(By.scrollable(true)),4000))
            SystemClock.sleep(350) // Let the destination's fade-through finish.
            fun fling(direction:Direction) {
                var sent=false
                repeat(3) {
                    if(!sent) try {
                        device.wait(Until.findObject(By.scrollable(true)),3000)!!.apply { setGestureMargin(120) }.fling(direction)
                        sent=true
                    } catch(_:StaleObjectException) { SystemClock.sleep(200) }
                }
                assertTrue("Catalogue remained available to scroll",sent)
                SystemClock.sleep(400)
            }
            device.executeShellCommand("dumpsys gfxinfo ${context.packageName} reset")
            repeat(5){fling(Direction.DOWN)}
            repeat(5){fling(Direction.UP)}
            assertEquals(context.packageName,device.currentPackageName)
            File(out,"scroll-gfxinfo.txt").writeText(device.executeShellCommand("dumpsys gfxinfo ${context.packageName}"))
            assertTrue(device.takeScreenshot(File(out,"library-scrolled.png")))
        } finally {config.waitForIdleTimeout=idle}
    }
}
