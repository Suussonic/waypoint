package com.waypoint.app

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test

class WorldsScrollPositionTest {
    @Test fun bottomWorldPreviewApplyAndRecreationKeepTheSamePosition() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        check(context.packageName.endsWith(".preview"))
        val application=context.applicationContext as WaypointApplication
        application.markWhatsNewSeen()
        application.updateAlerts.markExplained()
        val navigation=application.navigation
        inst.runOnMainSync { navigation.completeOnboarding(navigation.favoriteWorlds()) }
        val original=navigation.state.value.world
        val device=UiDevice.getInstance(inst)
        val config=Configurator.getInstance();val oldIdle=config.waitForIdleTimeout
        config.waitForIdleTimeout=350
        val output=java.io.File(context.getExternalFilesDir(null),"worlds-scroll-review").apply{mkdirs()}
        val bottom=By.desc("Preview ${World.entries.last().title} theme")
        fun click(label:String) {
            val control=device.wait(Until.findObject(By.desc(label)),2000)
                ?: device.wait(Until.findObject(By.text(label)),2000)
            assertNotNull("Missing $label",control);control!!.click()
        }
        fun trace(label:String) {
            device.takeScreenshot(java.io.File(output,"$label.png"))
            inst.sendStatus(2,android.os.Bundle().apply { putString("stream","\n$label: viewport=${device.findObject(By.scrollable(true))?.visibleBounds}; card=${device.findObject(bottom)?.visibleBounds}\n") })
        }
        fun verify(top:Int) {
            val card=device.wait(Until.findObject(bottom),5000)
            assertNotNull("Last world should still be visible",card)
            assertFalse("Catalogue jumped to its header",device.hasObject(By.text("Find your next world")))
            assertEquals("Preserve the exact scroll offset",top.toDouble(),card!!.visibleBounds.top.toDouble(),4.0)
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)),10_000))
                device.wait(Until.findObject(By.text("Open map")),1000)?.click()
                click("Worlds")
                assertTrue(device.wait(Until.hasObject(By.text("Find your next world")),5000))
                repeat(20) {
                    if(!device.hasObject(bottom)) {
                        device.findObject(By.scrollable(true)).apply{setGestureMargin(120)}.fling(Direction.DOWN)
                        SystemClock.sleep(200)
                    }
                }
                assertTrue("Reach the last world",device.wait(Until.hasObject(bottom),5000))
                SystemClock.sleep(600)
                trace("before-preview")
                val top=device.findObject(bottom).visibleBounds.top
                device.findObject(bottom).click()
                click("Back to worlds")
                verify(top)
                device.findObject(bottom).click()
                click("Use this world")
                SystemClock.sleep(350)
                click("Worlds")
                SystemClock.sleep(350)
                verify(top)
                trace("after-apply")
                scenario.recreate()
                SystemClock.sleep(700) // Wait for restored window insets and the destination transition.
                trace("after-recreate")
                verify(top)
            }
        } finally {
            inst.runOnMainSync { navigation.changeWorld(original) }
            config.waitForIdleTimeout=oldIdle
        }
    }
}
