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
        val rotation=device.executeShellCommand("settings get system user_rotation").trim()
        val autoRotation=device.executeShellCommand("settings get system accelerometer_rotation").trim()
        fun click(label:String) {
            val themedText=By.text(Pattern.compile("^${Pattern.quote(label)}$",Pattern.CASE_INSENSITIVE))
                .pkg(context.packageName)
            repeat(5) {
                val node=device.wait(Until.findObject(By.desc(label).pkg(context.packageName)),800)
                    ?: device.wait(Until.findObject(themedText),800)
                if(node!=null)try { node.click();return } catch(_:StaleObjectException) { SystemClock.sleep(150) }
            }
            fail("Click $label")
        }
        fun toggleMapKey(world:World,opening:Boolean) {
            val beforeLabel="${if(opening) "Open" else "Close"} ${world.title} map key"
            val afterLabel="${if(opening) "Close" else "Open"} ${world.title} map key"
            val before=By.desc(beforeLabel).pkg(context.packageName)
            val after=By.desc(afterLabel).pkg(context.packageName)
            val panel=By.text("LIVE PLACE DATA · OSM").pkg(context.packageName)
            fun assertUnchanged() {
                assertTrue("$beforeLabel disappeared before the key changed",device.hasObject(before))
                assertFalse("$afterLabel appeared without a confirmed touch",device.hasObject(after))
                assertEquals("Map key panel changed without $afterLabel",!opening,device.hasObject(panel))
            }
            assertTrue("Expected $beforeLabel before toggle",device.wait(Until.hasObject(before),4000))
            assertUnchanged()
            repeat(3) {
                assertUnchanged()
                val node=device.findObject(before)
                assertNotNull("$beforeLabel disappeared before touch",node)
                val bounds=try {
                    node!!.visibleBounds
                } catch(_:StaleObjectException) {
                    assertUnchanged()
                    return@repeat
                }
                assertTrue("$beforeLabel has no visible touch bounds",bounds.width()>0 && bounds.height()>0)
                assertTrue("Could not touch $beforeLabel",device.click(bounds.centerX(),bounds.centerY()))
                val panelChanged=if(opening)device.wait(Until.hasObject(panel),5000)
                    else device.wait(Until.gone(panel),5000)
                if(panelChanged) {
                    assertTrue("Expected $afterLabel after the panel changed",device.wait(Until.hasObject(after),5000))
                    assertFalse("$beforeLabel remained after toggle",device.hasObject(before))
                    return
                }
                // A touch can be lost during composition; retry only while
                // both the button and panel remain in their original state.
                assertUnchanged()
            }
            fail("$beforeLabel did not change the map key after three touches")
        }
        try {
            val app=context.applicationContext as WaypointApplication
            instrumentation.runOnMainSync {
                app.navigation.completeOnboarding(listOf(World.LOS_SANTOS))
                app.navigation.markMapOpened()
                app.navigation.changeWorld(World.LOS_SANTOS)
                app.markWhatsNewSeen();app.updateAlerts.markExplained()
            }
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10000))
            device.wait(Until.findObject(By.text("Open map")),3000)?.click()
            for(world in World.entries) {
                val worldsText=By.text(Pattern.compile("^Worlds$",Pattern.CASE_INSENSITIVE)).pkg(context.packageName)
                assertTrue("HUD library control in ${world.title}",device.wait(Until.hasObject(By.desc("Worlds")),4000) || device.wait(Until.hasObject(worldsText),4000))
                click("Worlds")
                val search=device.wait(Until.findObject(By.clazz("android.widget.EditText")),4000)
                assertNotNull("Theme search",search);search!!.text=world.title
                SystemClock.sleep(400)
                assertTrue("Theme result ${world.title}",device.wait(Until.hasObject(By.desc("Preview ${world.title} theme")),4000))
                click("Preview ${world.title} theme")
                click("Use this world")
                assertTrue("Library closed for ${world.title}",device.wait(Until.gone(By.clazz("android.widget.EditText")),4000))
                val legend=WorldLegendProfiles.forWorld(world)
                toggleMapKey(world,opening=true)
                assertTrue("Themed legend title ${world.title}",device.wait(Until.hasObject(By.text(legend.title)),3000))
                assertTrue("Themed route mark ${world.title}",device.wait(Until.hasObject(By.desc(legend.navigation.route)),3000))
                assertTrue("Themed place section ${world.title}",device.wait(Until.hasObject(By.text(legend.groupTitles.getValue(legend.groupOrder.first()))),3000))
                assertTrue("Live place data source ${world.title}",device.wait(Until.hasObject(By.text("LIVE PLACE DATA · OSM")),3000))
                assertTrue("Legend screenshot ${world.title}",device.takeScreenshot(File(output,"legend-${world.name.lowercase()}.png")))
                toggleMapKey(world,opening=false)
                // Allow map tile rendering, camera transition, and multiple heading animations.
                SystemClock.sleep(1600)
                assertEquals("App survived ${world.title}",context.packageName,device.currentPackageName)
                assertTrue(device.takeScreenshot(File(output,"atlas-${world.name.lowercase()}.png")))
            }
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.executeShellCommand("settings put system user_rotation 1")
            val landscapeDeadline=SystemClock.elapsedRealtime()+8000
            while(device.displayWidth<=device.displayHeight && SystemClock.elapsedRealtime()<landscapeDeadline)
                SystemClock.sleep(100)
            assertTrue("Map did not rotate to landscape",device.displayWidth>device.displayHeight)
            val minimumTarget=(48f*context.resources.displayMetrics.density).toInt()-2
            for(world in listOf(World.NIER,World.NIGHT_CITY,World.HOME_CONSOLE)) {
                instrumentation.runOnMainSync { app.navigation.changeWorld(world) }
                SystemClock.sleep(1600)
                val tilt=device.wait(Until.findObject(By.desc("Toggle 2D and 3D").pkg(context.packageName)),5000)
                val locate=device.wait(Until.findObject(By.desc("Use my location").pkg(context.packageName)),5000)
                assertNotNull("Tilt control in ${world.title}",tilt)
                assertNotNull("Locate control in ${world.title}",locate)
                val tiltBounds=tilt!!.visibleBounds
                val locateBounds=locate!!.visibleBounds
                assertTrue("Landscape map tools must share a row in ${world.title}",
                    kotlin.math.abs(tiltBounds.centerY()-locateBounds.centerY())<=minimumTarget/2)
                assertTrue("Landscape map tools must keep 48 dp targets in ${world.title}",
                    listOf(tiltBounds,locateBounds).all { it.width()>=minimumTarget && it.height()>=minimumTarget })
                assertFalse("Landscape map tools overlap in ${world.title}",
                    android.graphics.Rect.intersects(tiltBounds,locateBounds))
                assertEquals("App survived landscape ${world.title}",context.packageName,device.currentPackageName)
                assertTrue(device.takeScreenshot(File(output,"landscape-${world.name.lowercase()}.png")))
            }
        } catch(error:Throwable) {
            device.takeScreenshot(File(output,"failure.png"))
            device.dumpWindowHierarchy(File(output,"failure.xml"))
            throw error
        } finally {
            device.executeShellCommand("settings put system user_rotation $rotation")
            device.executeShellCommand("settings put system accelerometer_rotation $autoRotation")
            config.waitForIdleTimeout=oldIdle
            config.waitForSelectorTimeout=oldSelector
        }
    }
}
