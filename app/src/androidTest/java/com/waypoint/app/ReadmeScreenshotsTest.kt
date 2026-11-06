package com.waypoint.app

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Public documentation captures use an explicit London demo, never the device's position. */
@RunWith(AndroidJUnit4::class)
class ReadmeScreenshotsTest {
    @Test fun captureDemoWorldsAndUpdater() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val device = UiDevice.getInstance(inst)
        val output = File(context.getExternalFilesDir(null), "readme").apply { mkdirs() }
        val world = mutableStateOf(World.LOS_SANTOS)
        val screen = mutableStateOf("map")
        var mapError: String? = null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                (activity.application as WaypointApplication).navigation.setVisible(false)
                activity.setContent {
                    val selected = world.value
                    MaterialTheme(colorScheme = darkColorScheme(primary=selected.accent, onPrimary=Ink, background=Ink, surface=Ink, onSurface=Cream, onSurfaceVariant=Muted)) {
                        Box(Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()) {
                            when (screen.value) {
                                "welcome" -> ReferenceOnboarding(selected, emptyList(), false, false, { _, _ -> }, {}, {})
                                "library" -> androidx.compose.material3.Surface(color=Ink, contentColor=Cream) { HudLibrary(selected, {}, {}) }
                                "updates" -> androidx.compose.material3.Surface(color=Ink, contentColor=Cream) { Column(Modifier.fillMaxWidth().padding(24.dp)) { UpdatePanel((activity.application as WaypointApplication).updater, false) } }
                                else -> {
                                    // A public route point on the road, rather than the nearby square.
                                    val state = NavState(world=selected, fix=Coordinate(51.5077,-0.1270), accuracy=5f, bearing=28f,
                                        headingReliable=true, gpsReady=true, city="London · demonstration", tilted=false)
                                    WorldMap(state, {}, { mapError=it })
                                    GameAtlasChrome(state,AtlasActions({},{},{},{},{},{},{},{}),Modifier.padding(bottom=48.dp))
                                    Text("DEMONSTRATION · LONDON",color=Cream,style=MaterialTheme.typography.labelMedium,
                                        modifier=Modifier.align(Alignment.BottomCenter).background(Ink).padding(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            for (theme in if (InstrumentationRegistry.getArguments().getString("panelsOnly") == "true") emptyList() else listOf(World.LOS_SANTOS, World.OLD_WEST, World.NIGHT_CITY, World.RIVIA, World.HYRULE, World.PIXEL, World.STALKER)) {
                inst.runOnMainSync { world.value=theme }
                SystemClock.sleep(7000)
                assertNull("Map failed to render", mapError)
                device.takeScreenshot(File(output,"${theme.name.lowercase()}.png"))
            }
            inst.runOnMainSync { world.value=World.RIVIA;screen.value="welcome" }
            SystemClock.sleep(800);device.takeScreenshot(File(output,"welcome.png"))
            inst.runOnMainSync { screen.value="library" }
            SystemClock.sleep(800);device.takeScreenshot(File(output,"library.png"))
            inst.runOnMainSync { screen.value="updates" }
            SystemClock.sleep(800);device.takeScreenshot(File(output,"updates.png"))
        }
    }
}
