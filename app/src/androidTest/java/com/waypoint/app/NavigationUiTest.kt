package com.waypoint.app

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
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
        val displaySize=device.executeShellCommand("wm size")
        val originalSizeOverride=Regex("Override size: ([0-9]+x[0-9]+)")
            .find(displaySize)?.groupValues?.get(1)
        var narrowSizeActive=false
        fun restoreDisplaySize() {
            if(!narrowSizeActive)return
            device.executeShellCommand("wm size ${originalSizeOverride ?: "reset"}")
            narrowSizeActive=false
        }
        var addedTestPlace:Place?=null
        fun click(label:String) {
            val visibleText=if(label=="End navigation")"End trip" else label
            repeat(5) {
                assertEquals("Waypoint must stay in the foreground",context.packageName,device.currentPackageName)
                val item=device.wait(Until.findObject(By.desc(label).pkg(context.packageName)),700)
                    ?: device.wait(Until.findObject(By.text(visibleText).pkg(context.packageName)),700)
                if(item!=null) try { item.click();return } catch(_:StaleObjectException) { }
                SystemClock.sleep(200)
            }
            fail("Control: $label")
        }
        fun capture(name:String) {SystemClock.sleep(1500);assertEquals(context.packageName,device.currentPackageName);assertTrue(device.takeScreenshot(File(output,"$name.png")))}
        fun landscapeReady():Boolean {
            val deadline=SystemClock.elapsedRealtime()+8000
            while(SystemClock.elapsedRealtime()<deadline) {
                if(device.displayWidth>device.displayHeight)return true
                SystemClock.sleep(100)
            }
            return device.displayWidth>device.displayHeight
        }
        fun scrollPreview(direction:Direction):Boolean {
            // The route card can contain a horizontal alternatives row. Use the
            // tallest app scroller, which is the preview's vertical detail column.
            val column=device.findObjects(By.scrollable(true).pkg(context.packageName))
                .maxByOrNull { runCatching { it.visibleBounds.height() }.getOrDefault(0) }
                ?: return false
            return try { column.scroll(direction,.65f);true } catch(_:StaleObjectException) { false }
        }
        fun resetPreviewScroll() {
            val mode=By.desc("Car").pkg(context.packageName)
            val locate=By.desc("Use my location").pkg(context.packageName)
            repeat(8) {
                assertEquals("Waypoint must stay in the foreground",context.packageName,device.currentPackageName)
                if(device.hasObject(mode) && device.hasObject(locate))return
                scrollPreview(Direction.UP)
                SystemClock.sleep(200)
            }
            assertTrue("Route preview did not return to its mode and Locate controls",
                device.wait(Until.hasObject(mode),1000) && device.wait(Until.hasObject(locate),1000))
        }
        fun visibleControl(label:String,selector:BySelector,scroll:Boolean=false):Rect {
            repeat(if(scroll)6 else 3) {
                assertEquals("Waypoint must stay in the foreground",context.packageName,device.currentPackageName)
                val node=device.wait(Until.findObject(selector),1000)
                if(node!=null)try {
                    val bounds=node.visibleBounds
                    if(bounds.width()>0 && bounds.height()>0)return bounds
                } catch(_:StaleObjectException) { }
                if(scroll)scrollPreview(Direction.DOWN)
            }
            fail("Missing visible $label")
            return Rect()
        }
        fun touchTarget(label:String,bounds:Rect) {
            val minPixels=(48f*context.resources.displayMetrics.density).toInt()-2
            assertTrue("$label is clipped below a 48 dp target",bounds.width()>=minPixels && bounds.height()>=minPixels)
        }
        fun separated(first:String,a:Rect,second:String,b:Rect) {
            assertFalse("$first overlaps $second",Rect.intersects(a,b))
        }
        fun checkLandscapePreview(font:String) {
            assertTrue("Route preview did not rotate to landscape",landscapeReady())
            resetPreviewScroll()
            val back=visibleControl("Close route preview",By.desc("Close route preview").pkg(context.packageName))
            val settings=visibleControl("Settings",By.desc("Settings").pkg(context.packageName))
            val locate=visibleControl("Use my location",By.desc("Use my location").pkg(context.packageName))
            visibleControl("Car mode",By.desc("Car").pkg(context.packageName))
            touchTarget("Close route preview",back)
            touchTarget("Settings",settings)
            touchTarget("Use my location",locate)
            separated("Close route preview",back,"Settings",settings)
            capture("route-witcher-landscape-$font")
            visibleControl("enabled Start navigation",By.text("Start navigation").enabled(true).pkg(context.packageName),scroll=true)
            capture("route-witcher-landscape-$font-actions")
        }
        fun checkLandscapeGuidance(font:String) {
            assertTrue("Guidance did not rotate to landscape",landscapeReady())
            val mute=if(controller.state.value.voice)"Mute guidance" else "Unmute guidance"
            val controls=listOf("Worlds","End navigation","Route steps",mute,"Use my location").map { label ->
                label to visibleControl(label,By.desc(label).pkg(context.packageName))
            }
            controls.forEach { (label,bounds) -> touchTarget(label,bounds) }
            for(i in controls.indices)for(j in i+1 until controls.size)
                separated(controls[i].first,controls[i].second,controls[j].first,controls[j].second)
            val trip=visibleControl("trip summary",By.descContains("remaining.").pkg(context.packageName))
            controls.forEach { (label,bounds) -> separated("trip summary",trip,label,bounds) }
            // mapPlayer() is 96 px wide; the largest position-arrow iconSize is 0.55 * 1.1.
            val playerHalfWidthPx=(96f*0.55f*1.1f/2f).toInt()
            assertTrue("Guidance strip should leave the centred player marker clear",
                trip.left>device.displayWidth/2+playerHalfWidthPx+8)
            if(font=="200-narrow") {
                val actions=controls.toMap()
                assertTrue("Narrow guidance actions should reflow into two rows",
                    actions.getValue("End navigation").bottom<=actions.getValue(mute).top &&
                    actions.getValue("Route steps").bottom<=actions.getValue("Use my location").top)
                val banner=device.findObjects(By.descContains("Show route steps").pkg(context.packageName))
                    .mapNotNull { runCatching { it.visibleBounds }.getOrNull() }
                    .minByOrNull { it.top }
                assertNotNull("Narrow guidance maneuver banner is missing",banner)
                assertTrue("Maneuver banner should leave the centred player marker clear",
                    banner!!.right<device.displayWidth/2-playerHalfWidthPx-8)
            }
            device.findObject(By.descContains("Map scale:").pkg(context.packageName))?.visibleBounds?.let { scale ->
                controls.forEach { (label,bounds) -> separated("Map scale",scale,label,bounds) }
            }
            assertTrue("Guidance must remain active in landscape",controller.state.value.navigating)
            capture("guidance-steelport-landscape-$font")
        }
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
            checkLandscapePreview("130")
            assertEquals("Route survives 130% landscape preview",route,controller.state.value.route)
            device.executeShellCommand("settings put system font_scale 2.0")
            SystemClock.sleep(900) // Allow Compose to apply the larger system font before inspecting bounds.
            checkLandscapePreview("200")
            assertEquals("Route survives 200% landscape preview",route,controller.state.value.route)
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
            checkLandscapeGuidance("130")
            device.executeShellCommand("settings put system font_scale 2.0")
            SystemClock.sleep(900)
            checkLandscapeGuidance("200")
            // Exercise the two-row guidance actions on a narrow landscape phone.
            // At 450 dpi this is approximately the 1080x1360 display override.
            val density=context.resources.displayMetrics.density
            val narrowPortraitPixels=(384f*density).toInt()
            val narrowLandscapePixels=(484f*density).toInt()
            narrowSizeActive=true
            device.executeShellCommand("wm size ${narrowPortraitPixels}x${narrowLandscapePixels}")
            val narrowDeadline=SystemClock.elapsedRealtime()+8000
            while((device.displayWidth<=device.displayHeight || device.displayWidth/density !in 460f..550f) &&
                SystemClock.elapsedRealtime()<narrowDeadline)SystemClock.sleep(150)
            assertTrue("Narrow landscape size did not apply",
                landscapeReady() && device.displayWidth/density in 460f..550f)
            checkLandscapeGuidance("200-narrow")
            restoreDisplaySize()
            click("End navigation")
            val stopDeadline=SystemClock.elapsedRealtime()+5000
            while(controller.state.value.navigating && SystemClock.elapsedRealtime()<stopDeadline)SystemClock.sleep(100)
            assertFalse("Guidance ended",controller.state.value.navigating)
            device.executeShellCommand("settings put system user_rotation 0")
            device.executeShellCommand("settings put system font_scale 1.3")
            SystemClock.sleep(1500)
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
            restoreDisplaySize()
            device.executeShellCommand("settings put system font_scale $font")
            device.executeShellCommand("settings put system user_rotation $rotation")
            device.executeShellCommand("settings put system accelerometer_rotation $auto")
            config.waitForIdleTimeout=oldIdle
        }
    }
}
