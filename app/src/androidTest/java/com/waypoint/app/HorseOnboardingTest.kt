package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises the actual onboarding placement, visible motion, and background pause. */
@RunWith(AndroidJUnit4::class)
class HorseOnboardingTest {
    @Test fun horsePageMovesAndPausesWithItsLifecycle() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(inst)
        val output = File(inst.targetContext.getExternalFilesDir(null), "horse-loop").apply { mkdirs() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                (activity.application as WaypointApplication).navigation.setVisible(false)
                activity.setContent {
                    MaterialTheme {
                        ReferenceOnboarding(World.OLD_WEST, emptyList(), false, false, { _, _ -> }, {}, {})
                    }
                }
            }
            assertTrue(device.wait(Until.hasObject(By.desc("Continue")), 5000))
            device.findObject(By.desc("Continue")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Favorite Game")), 5000))
            SystemClock.sleep(400)
            device.findObject(By.desc("Continue")).click()
            assertTrue(device.wait(Until.hasObject(By.text("your phone")), 5000))
            assertTrue(device.hasObject(By.text("Follow real routes on a game-inspired map with clear turn-by-turn voice guidance.")))
            SystemClock.sleep(500)
            var hero: View? = null
            scenario.onActivity { hero = findHero(it.window.decorView) }
            assertNotNull("The phone onboarding must use the animated POV", hero)
            val target = hero!!
            val location = IntArray(2)
            inst.runOnMainSync { target.getLocationOnScreen(location) }
            val firstFile = File(output, "horse-onboarding.png")
            val secondFile = File(output, "horse-onboarding-next.png")
            assertTrue(device.takeScreenshot(firstFile))
            SystemClock.sleep(320)
            assertTrue(device.takeScreenshot(secondFile))
            val first = BitmapFactory.decodeFile(firstFile.path)
            val second = BitmapFactory.decodeFile(secondFile.path)
            val scale = target.width / 1080f
            // Restrict the comparison to the phone map, excluding the OS status bar.
            val cropX = location[0] + (220 * scale).toInt()
            val cropY = location[1] + (930 * scale).toInt()
            val cropW = (600 * scale).toInt()
            val cropH = (100 * scale).toInt()
            val a = Bitmap.createBitmap(first, cropX, cropY, cropW, cropH)
            val b = Bitmap.createBitmap(second, cropX, cropY, cropW, cropH)
            assertFalse("The map must visibly advance while the screen is open", a.sameAs(b))
            scenario.moveToState(Lifecycle.State.CREATED)
            val pausedA = snapshot(inst, target)
            SystemClock.sleep(200)
            val pausedB = snapshot(inst, target)
            assertTrue("Hidden onboarding must stop advancing", pausedA.sameAs(pausedB))
            scenario.moveToState(Lifecycle.State.RESUMED)
            SystemClock.sleep(200)
            assertFalse("Returning to onboarding must resume motion", pausedB.sameAs(snapshot(inst, target)))
            device.findObject(By.desc("Continue")).click()
            assertTrue("The horse page should lead directly to the favourite-games question", device.wait(Until.hasObject(By.text(inst.targetContext.getString(R.string.favorite_games_question))), 5000))
            device.pressBack()
            assertTrue("Back should return to the horse page", device.wait(Until.hasObject(By.text("your phone")), 5000))
        }
    }

    private fun findHero(view: View): View? {
        if (view.javaClass.simpleName == "HorsePhoneView") return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findHero(view.getChildAt(i))?.let { return it }
        return null
    }

    private fun snapshot(inst: android.app.Instrumentation, view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        inst.runOnMainSync { view.draw(Canvas(bitmap)) }
        return bitmap
    }
}
