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
import java.util.regex.Pattern

/** Real-device UI regression for native layer replacement during live sensor updates.
 * Captures are private local evidence. Run on an unlocked development phone.
 */
@RunWith(AndroidJUnit4::class)
class AtlasSmokeTest {
    @Test fun switchEveryAtlasWhileSensorsAreRunning() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val device=UiDevice.getInstance(instrumentation)
        val config=Configurator.getInstance()
        val oldIdle=config.waitForIdleTimeout
        val oldSelector=config.waitForSelectorTimeout
        config.waitForIdleTimeout=350
        config.waitForSelectorTimeout=3000
        val output=File(context.getExternalFilesDir(null),"atlas-review").apply{mkdirs()}
        try {
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10000))
            device.wait(Until.findObject(By.text("Open map")),3000)?.click()
            for(world in World.entries) {
                val hud=device.wait(Until.findObject(By.desc("Worlds")),4000) ?: device.wait(Until.findObject(By.text("Worlds")),4000)
                assertNotNull("HUD library control in ${world.title}",hud);hud!!.click()
                val search=device.wait(Until.findObject(By.clazz("android.widget.EditText")),4000)
                assertNotNull("Theme search",search);search!!.text=world.title
                SystemClock.sleep(400)
                val choice=device.wait(Until.findObject(By.desc("Preview ${world.title} theme")),4000)
                assertNotNull("Theme result ${world.title}",choice);choice!!.click()
                device.wait(Until.findObject(By.text("Use this world")),4000)!!.click()
                assertTrue("Library closed for ${world.title}",device.wait(Until.gone(By.clazz("android.widget.EditText")),4000))
                // Allow map tile rendering, camera transition, and multiple heading animations.
                SystemClock.sleep(1600)
                assertEquals("App survived ${world.title}",context.packageName,device.currentPackageName)
                assertTrue(device.takeScreenshot(File(output,"atlas-${world.name.lowercase()}.png")))
            }
        } catch(error:Throwable) {
            device.takeScreenshot(File(output,"failure.png"))
            device.dumpWindowHierarchy(File(output,"failure.xml"))
            throw error
        } finally {
            config.waitForIdleTimeout=oldIdle
            config.waitForSelectorTimeout=oldSelector
        }
    }
}
