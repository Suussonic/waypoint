package com.waypoint.app

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import java.io.File

/** Public fixtures in the separate Preview app; never modifies the production installation. */
class AndroidTwoFeaturesTest {
    @Test fun savedNamePersistsAndBicycleModeIsDistinctFromMotorcycle() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val app=context.applicationContext as WaypointApplication
        val navigation=app.navigation
        val device=UiDevice.getInstance(inst)
        val previous=navigation.state.value
        val fixture=Place("android-two-public-fixture","Tokyo Station","Marunouchi, Tokyo",Coordinate(35.6812,139.7671))
        val folder=File(context.getExternalFilesDir(null),"android-two-review").apply{mkdirs()}
        folder.listFiles()?.filter { it.extension=="png" }?.forEach { it.delete() }
        val config=Configurator.getInstance();val oldIdle=config.waitForIdleTimeout;config.waitForIdleTimeout=350
        fun click(label:String) {
            assertEquals("Phone must remain in Waypoint Preview for UI testing",context.packageName,device.currentPackageName)
            val node=device.wait(Until.findObject(By.desc(label).pkg(context.packageName)),2500) ?: device.wait(Until.findObject(By.text(label).pkg(context.packageName)),2500)
            if(node==null && device.currentPackageName==context.packageName) { device.takeScreenshot(File(folder,"failure.png"));device.dumpWindowHierarchy(File(folder,"failure.xml")) }
            assertNotNull("Find $label",node);node!!.click()
        }
        fun scrollTo(label:String) {
            repeat(5) {
                assertEquals(context.packageName,device.currentPackageName)
                if(device.hasObject(By.text(label).pkg(context.packageName)))return
                device.findObject(By.scrollable(true).pkg(context.packageName))?.scroll(Direction.DOWN,.65f)
            }
            assertTrue("Reach $label",device.hasObject(By.text(label).pkg(context.packageName)))
        }
        try {
            inst.runOnMainSync {
                app.markWhatsNewSeen();app.updateAlerts.markExplained()
                navigation.completeOnboarding(navigation.favoriteWorlds())
                navigation.clearDestination();navigation.changeWorld(World.NIER)
                navigation.state.value.saved.firstOrNull { it.id==fixture.id }?.let(navigation::toggleSaved)
                navigation.toggleSaved(fixture)
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Saved")
                click("Options for Tokyo Station");click("Rename")
                val input=device.wait(Until.findObject(By.clazz("android.widget.EditText")),3000)
                assertNotNull(input);input!!.text="Work"
                SystemClock.sleep(350)
                device.takeScreenshot(File(folder,"saved-name-editor.png"))
                click("Save name")
                assertTrue(device.wait(Until.hasObject(By.text("Work")),3000))
                val renamed=navigation.state.value.saved.single { it.id==fixture.id }
                assertEquals(fixture.copy(name="Work"),renamed)
                inst.runOnMainSync {
                    navigation.select(renamed.copy(address="Updated address for the same place",point=Coordinate(35.6813,139.7672)))
                    assertTrue(navigation.renameSaved(fixture.id,"Work"))
                    assertEquals("Work",navigation.state.value.selected!!.name)
                    assertEquals("Updated address for the same place",navigation.state.value.selected!!.address)
                    assertEquals(Coordinate(35.6813,139.7672),navigation.state.value.selected!!.point)
                    navigation.clearDestination()
                }
                scenario.recreate()
                assertTrue(device.wait(Until.hasObject(By.text("Work")),5000))
                val prefs=context.getSharedPreferences("waypoint",0)
                val saved=JSONArray(prefs.getString("saved","[]"))
                assertTrue((0 until saved.length()).any { saved.getJSONObject(it).optString("id")==fixture.id && saved.getJSONObject(it).optString("name")=="Work" })
                device.takeScreenshot(File(folder,"saved-renamed.png"))
                click("Work")
                click("Bicycle")
                assertEquals(TravelMode.BICYCLE,navigation.state.value.mode)
                assertEquals("BICYCLE",prefs.getString("mode",null))
                assertEquals(fixture.point,navigation.state.value.selected!!.point)
                assertEquals("vehicle-bicycle",playerMarkerImage(TravelMode.BICYCLE,true))
                SystemClock.sleep(350)
                device.takeScreenshot(File(folder,"bicycle-route-preview.png"))
                scrollTo("More")
                SystemClock.sleep(250)
                device.takeScreenshot(File(folder,"bicycle-route-actions.png"))
                scenario.recreate()
                // The same scrolled details position may restore below the modes.
                assertTrue(device.wait(Until.hasObject(By.desc("Close route preview")),5000))
                assertEquals(TravelMode.BICYCLE,navigation.state.value.mode)
                click("Close route preview")
                SystemClock.sleep(600)
                device.takeScreenshot(File(folder,"nier-map.png"))
                click("Quests")
                SystemClock.sleep(350)
                device.takeScreenshot(File(folder,"nier-quests.png"))
                click("Saved");click("Options for Work");click("Remove from saved")
                if(navigation.state.value.saved.isEmpty()) {
                    scrollTo("Find a place")
                    device.takeScreenshot(File(folder,"saved-empty-action.png"))
                    click("Find a place")
                    assertTrue(device.wait(Until.hasObject(By.clazz("android.widget.EditText")),3000))
                }
            }
        } finally {
            inst.runOnMainSync {
                navigation.state.value.saved.firstOrNull { it.id==fixture.id }?.let(navigation::toggleSaved)
                navigation.clearDestination();navigation.changeMode(previous.mode);navigation.changeWorld(previous.world)
            }
            config.waitForIdleTimeout=oldIdle
        }
    }
}
