package com.waypoint.app

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Real Compose settings at phone/tablet sizes; display overrides are always restored. */
class AndroidAutoSettingsUiTest {
    @Test fun setupRemainsReadableWithLargeTextAndLandscape() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val app = context.applicationContext as WaypointApplication
        val device = UiDevice.getInstance(inst)
        val output = File(context.getExternalFilesDir(null),"android-auto-review").apply { mkdirs() }
        val originalWorld = app.navigation.state.value.world
        val originalSize = device.executeShellCommand("wm size").lineSequence().find { it.startsWith("Override size:") }?.substringAfter(": ")
        val originalDensity = device.executeShellCommand("wm density").lineSequence().find { it.startsWith("Override density:") }?.substringAfter(": ")
        val keys = listOf("system font_scale","system accelerometer_rotation","system user_rotation","global animator_duration_scale","global window_animation_scale","global transition_animation_scale")
        val oldSettings = keys.associateWith { device.executeShellCommand("settings get $it").trim() }
        val oldIdle = Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout = 350
        fun showSettings(name: String, landscape: Boolean = false) {
            SystemClock.sleep(1200)
            if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()
            assertEquals("Actual layout orientation for $name",landscape,device.displayWidth>device.displayHeight)
            ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use {
                val settings = device.wait(Until.findObject(By.desc("Settings")),5000)
                    ?: device.wait(Until.findObject(By.text("Settings")),2000)
                assertNotNull("Settings control",settings)
                settings!!.click()
                assertTrue("Settings sheet opened",device.wait(Until.hasObject(By.text("Make it your own")),5000))
                fun scrollNode(node: AccessibilityNodeInfo?): Boolean {
                    if(node==null)return false
                    if(node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true
                    return (0 until node.childCount).any { scrollNode(node.getChild(it)) }
                }
                var voiceScrolls=0
                while(!device.hasObject(By.desc("Game-style voice")) && voiceScrolls++ < 12) {
                    scrollNode(inst.uiAutomation.rootInActiveWindow)
                    SystemClock.sleep(400)
                    if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()
                }
                val gameVoice=device.wait(Until.findObject(By.desc("Game-style voice")),3000)
                assertNotNull("Labelled game-style voice switch in $name",gameVoice)
                if(name=="phone-settings") {
                    val before=app.navigation.state.value.gameVoice
                    try {
                        gameVoice!!.click()
                        assertTrue(device.wait(Until.hasObject(By.desc("Game-style voice").checked(!before)),3000))
                        assertEquals(!before,app.navigation.state.value.gameVoice)
                        assertEquals(!before,context.getSharedPreferences("waypoint",0).getBoolean("gameVoice",before))
                    } finally {
                        inst.runOnMainSync { if(app.navigation.state.value.gameVoice!=before)app.navigation.toggleGameVoice() }
                    }
                }
                assertTrue(device.takeScreenshot(File(output,"$name-voice.png")))
                device.dumpWindowHierarchy(File(output,"$name-voice.xml"))
                var attempts=0
                while(!device.hasObject(By.text("Android Auto settings")) && attempts++ < 30) {
                    scrollNode(inst.uiAutomation.rootInActiveWindow)
                    SystemClock.sleep(1000)
                    if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()
                }
                device.takeScreenshot(File(output,"$name.png"))
                device.dumpWindowHierarchy(File(output,"$name.xml"))
                assertTrue("Android Auto settings button in $name",device.wait(Until.hasObject(By.text("Android Auto settings")),3000))
                assertTrue(device.takeScreenshot(File(output,"$name.png")))
                device.dumpWindowHierarchy(File(output,"$name.xml"))
                device.pressBack()
                assertFalse(device.hasObject(By.text("Make it your own")))
            }
        }
        try {
            inst.runOnMainSync {
                app.navigation.completeOnboarding(listOf(World.DIVISION))
                app.navigation.markMapOpened()
                app.navigation.changeWorld(World.DIVISION)
                app.navigation.clearDestination()
            }
            app.markWhatsNewSeen()
            listOf("animator_duration_scale","window_animation_scale","transition_animation_scale").forEach { device.executeShellCommand("settings put global $it 0") }
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.executeShellCommand("settings put system user_rotation 0")
            device.executeShellCommand("wm size 1080x1920")
            device.executeShellCommand("wm density 480")
            device.executeShellCommand("settings put system font_scale 1.0")
            showSettings("phone-settings")
            device.executeShellCommand("settings put system font_scale 2.0")
            showSettings("phone-settings-large-text")
            device.executeShellCommand("settings put system font_scale 1.0")
            device.executeShellCommand("wm size 1920x1080")
            showSettings("phone-settings-landscape",true)
            device.executeShellCommand("wm size 2560x1600")
            device.executeShellCommand("wm density 320")
            showSettings("tablet-settings-landscape",true)
        } finally {
            device.executeShellCommand("wm size ${originalSize ?: "reset"}")
            device.executeShellCommand("wm density ${originalDensity ?: "reset"}")
            oldSettings.forEach { (key,value) -> device.executeShellCommand(if(value=="null")"settings delete $key" else "settings put $key $value") }
            inst.runOnMainSync { app.navigation.changeWorld(originalWorld) }
            Configurator.getInstance().waitForIdleTimeout = oldIdle
        }
    }
}
