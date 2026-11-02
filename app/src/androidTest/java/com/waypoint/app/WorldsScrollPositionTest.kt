package com.waypoint.app

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorldsScrollPositionTest {
    @Test fun returningToWorldsKeepsTheListPosition() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val device=UiDevice.getInstance(inst)
        inst.targetContext.getSharedPreferences("waypoint",0).edit()
            .putBoolean("onboardingCompleted",true).putBoolean("mapOpened",true)
            .putInt("welcomeShownVersion",BuildConfig.VERSION_CODE)
            .putString("world",World.LOS_SANTOS.name).commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue("Worlds control missing",device.wait(Until.hasObject(By.text("Worlds")),10000))
            device.findObject(By.text("Worlds")).click()
            assertTrue("World picker did not open",device.wait(Until.hasObject(By.text("CHOOSE A WORLD")),5000))

            repeat(5) {
                device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/4,70)
                SystemClock.sleep(300)
            }
            device.waitForIdle()
            assertTrue("World picker did not scroll",device.wait(Until.gone(By.text("CHOOSE A WORLD")),5000))

            device.pressBack()
            assertTrue("Map did not return",device.wait(Until.hasObject(By.text("Worlds")),5000))
            device.findObject(By.text("Worlds")).click()
            assertTrue("World picker did not reopen",device.wait(Until.hasObject(By.descContains("Apply ")),5000))
            assertFalse("World picker jumped to the top",device.hasObject(By.text("CHOOSE A WORLD")))

            device.findObject(By.descContains("Apply ")).click()
            val worldsControl=By.desc("Worlds")
            assertTrue("Selected world's map did not open",device.wait(Until.hasObject(worldsControl),10000))
            device.findObject(worldsControl).click()
            assertTrue("World picker did not reopen after selecting a theme",device.wait(Until.hasObject(By.descContains("Apply ")),5000))
            assertFalse("Selecting a theme reset the world list",device.hasObject(By.text("CHOOSE A WORLD")))
        }
    }
}
