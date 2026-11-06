package com.waypoint.app

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Stationary, connected-phone navigation UI checks. Requires GPS and a saved nearby place. */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @Test fun themesPreserveRouteAndGuidanceAtAccessibleSizes() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val app=context.applicationContext as WaypointApplication
        val controller=(context.applicationContext as WaypointApplication).navigation
        if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        if(context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        val device=UiDevice.getInstance(instrumentation)
        val config=Configurator.getInstance();val oldIdle=config.waitForIdleTimeout
        config.waitForIdleTimeout=350
        val output=File(context.getExternalFilesDir(null),"atlas-review").apply{mkdirs()}
        val font=device.executeShellCommand("settings get system font_scale").trim()
        val rotation=device.executeShellCommand("settings get system user_rotation").trim()
        val auto=device.executeShellCommand("settings get system accelerometer_rotation").trim()
        var addedTestPlace:Place?=null
        fun click(label:String) {
            val visibleText=if(label=="End navigation")"End trip" else label
            repeat(5) {
                val item=device.wait(Until.findObject(By.desc(label)),700)
                    ?: device.wait(Until.findObject(By.text(visibleText)),700)
                if(item!=null) try { item.click();return } catch(_:StaleObjectException) { }
                SystemClock.sleep(200)
            }
            fail("Control: $label")
        }
        fun capture(name:String) {SystemClock.sleep(1500);assertEquals(context.packageName,device.currentPackageName);assertTrue(device.takeScreenshot(File(output,"$name.png")))}
        fun world(world:World) {
            click("Worlds")
            device.wait(Until.findObject(By.clazz("android.widget.EditText")),4000)!!.text=world.title
            SystemClock.sleep(400)
            click("Preview ${world.title} theme")
            click("Use this world")
            assertTrue(device.wait(Until.gone(By.clazz("android.widget.EditText")),5000))
            assertEquals(world,controller.state.value.world)
        }
        try {
            instrumentation.runOnMainSync {
                controller.completeOnboarding(listOf(World.LOS_SANTOS))
                controller.markMapOpened()
                controller.changeWorld(World.LOS_SANTOS)
                app.markWhatsNewSeen();app.updateAlerts.markExplained()
                controller.clearDestination()
                controller.changeWorld(World.LOS_SANTOS)
            }
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10000))
            device.wait(Until.findObject(By.text("Open map")),3000)?.click()
            instrumentation.runOnMainSync { controller.enableLocation() }
            val deadline=SystemClock.elapsedRealtime()+15000
            while(controller.state.value.fix==null && SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(200)
            val fix=controller.state.value.fix
            assertNotNull("GPS fix required for device QA",fix)
            val destination=controller.state.value.saved.firstOrNull { it.id=="waypoint-device-qa" }
                ?: Place("waypoint-device-qa","Nearby test stop","",
                    Coordinate(fix!!.lat+0.008,fix.lon+0.008)).also { place ->
                    instrumentation.runOnMainSync { controller.toggleSaved(place) }
                    addedTestPlace=place
                }
            click("Saved");click(destination.name);click("Car")
            assertTrue("Real GPS route ready",device.wait(Until.hasObject(By.text("Start navigation").enabled(true)),30000))
            var route=controller.state.value.route!!
            val routeOptions=controller.state.value.routeOptions
            if(routeOptions.size>1) {
                val secondChoice=device.wait(Until.findObject(By.descContains("Route 2,")),3000)
                assertNotNull("Alternative route is visible and labelled",secondChoice)
                secondChoice!!.click()
                val selectionDeadline=SystemClock.elapsedRealtime()+3000
                while(controller.state.value.route!==routeOptions[1] && SystemClock.elapsedRealtime()<selectionDeadline)SystemClock.sleep(50)
                assertSame("The selected alternative becomes the active preview",routeOptions[1],controller.state.value.route)
                route=routeOptions[1]
            }
            capture("route-preview")
            world(World.RIVIA)
            assertEquals(route,controller.state.value.route)
            capture("route-witcher")
            device.executeShellCommand("settings put system font_scale 1.3")
            capture("route-witcher-large-text")
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.executeShellCommand("settings put system user_rotation 1")
            SystemClock.sleep(2500) // Native map surface and tile rendering settle after recreation.
            capture("route-witcher-landscape")
            device.executeShellCommand("settings put system user_rotation 0")
            device.executeShellCommand("settings put system font_scale $font")
            SystemClock.sleep(1200)
            world(World.NIGHT_CITY)
            assertEquals(destination,controller.state.value.selected)
            assertEquals(route,controller.state.value.route)
            capture("route-night-city")
            click("Start navigation")
            // The preview requests POST_NOTIFICATIONS after navigation starts. Dismiss
            // Android's system sheet so the route HUD can be observed; .preview is disposable.
            device.wait(Until.findObject(By.text("Don't allow")),1500)?.click()
            val startDeadline=SystemClock.elapsedRealtime()+8000
            while(!controller.state.value.navigating && SystemClock.elapsedRealtime()<startDeadline)SystemClock.sleep(100)
            assertTrue("Navigation controller starts the selected route",controller.state.value.navigating)
            assertTrue("Stop control is accessible",device.wait(Until.hasObject(By.desc("End navigation")),5000)
                || device.wait(Until.hasObject(By.text("End trip")),2000))
            world(World.SAINTS)
            assertTrue(controller.state.value.navigating)
            assertEquals(World.SAINTS,controller.state.value.world)
            assertEquals(route,controller.state.value.route)
            capture("guidance-steelport")
            device.executeShellCommand("settings put system font_scale 1.3")
            capture("guidance-large-text")
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.executeShellCommand("settings put system user_rotation 1")
            capture("guidance-landscape")
            device.executeShellCommand("settings put system user_rotation 0")
            SystemClock.sleep(1500)
            click("End navigation")
            val stopDeadline=SystemClock.elapsedRealtime()+5000
            while(controller.state.value.navigating && SystemClock.elapsedRealtime()<stopDeadline)SystemClock.sleep(100)
            assertFalse("Guidance ended",controller.state.value.navigating)
            click("Close route preview")
            world(World.OLD_WEST)
            capture("western-large-text")
            // Map information was removed from the map; credits live in Settings.
        } catch(error:Throwable) {
            device.takeScreenshot(File(output,"navigation-failure.png"))
            device.dumpWindowHierarchy(File(output,"navigation-failure.xml"))
            throw error
        } finally {
            instrumentation.runOnMainSync {if(controller.state.value.navigating)controller.stopNavigation();controller.setVisible(false)}
            addedTestPlace?.let { place -> instrumentation.runOnMainSync {
                if(controller.state.value.saved.any { it.id==place.id })controller.toggleSaved(place)
            } }
            // Permission belongs to the disposable .preview package; Gradle removes that package after this run.
            // Revoking it here kills the target/instrumentation process on Android 17 before JUnit can report failures.
            device.executeShellCommand("settings put system font_scale $font")
            device.executeShellCommand("settings put system user_rotation $rotation")
            device.executeShellCommand("settings put system accelerometer_rotation $auto")
            config.waitForIdleTimeout=oldIdle
        }
    }
}
