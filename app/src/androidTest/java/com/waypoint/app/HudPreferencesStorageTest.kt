package com.waypoint.app

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HudPreferencesStorageTest {
    @Test fun mapControlPresetsPersistAndOriginalSideResets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val prefs = app.getSharedPreferences("hud-presets-test", 0)
        prefs.edit().clear().commit()
        val repository = object : NavigationRepository {
            override suspend fun search(query: String, near: Coordinate): List<Place> = emptyList()
            override suspend fun reverse(point: Coordinate): String = "Example place"
            override suspend fun route(from: Coordinate, to: Coordinate, mode: TravelMode): RoadRoute =
                error("No route expected")
        }
        val controllers = mutableListOf<NavigationController>()
        fun newController(): NavigationController = NavigationController(app, repository, "hud-presets-test", null)
            .also { controllers += it }
        fun field(controller: NavigationController, name: String) = controller.javaClass
            .getDeclaredField(name).apply { isAccessible = true }.get(controller)
        try {
            instrumentation.runOnMainSync {
                val first = newController()
                assertEquals(HudSize.STANDARD, first.state.value.hudSize)
                assertNull(first.state.value.hudSide)
                first.setHudSize(HudSize.LARGE)
                first.setHudSide(HudSide.LEFT)
            }
            assertEquals("LARGE", prefs.getString("hudSize", null))
            assertEquals("LEFT", prefs.getString("hudSide", null))

            instrumentation.runOnMainSync {
                val restored = newController()
                assertEquals(HudSize.LARGE, restored.state.value.hudSize)
                assertEquals(HudSide.LEFT, restored.state.value.hudSide)
                restored.setHudSide(null)
            }
            assertNull(prefs.getString("hudSide", null))
        } finally {
            instrumentation.runOnMainSync {
                controllers.forEach { controller ->
                    (field(controller, "scope") as CoroutineScope).cancel()
                    (field(controller, "guidance") as FerrostarGuidance).shutdown()
                }
            }
            prefs.edit().clear().commit()
        }
    }
}
