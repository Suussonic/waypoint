package com.waypoint.app

import android.Manifest
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.MessageTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.serialization.Bundleable
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test

/** Run separately with both location permissions revoked before instrumentation starts. */
class CarPermissionRecoveryTest {
    @Test fun deniedLocationHasParkedRecoveryAndRefreshesAfterGrant() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val controller = (context.applicationContext as WaypointApplication).navigation
        assumeFalse("This test requires location denied at process start",controller.hasLocationPermission())
        lateinit var screen: WaypointCarScreen
        lateinit var lifecycle: ScreenController
        lateinit var message: MessageTemplate
        inst.runOnMainSync {
            screen = WaypointCarScreen(TestCarContext.createCarContext(context))
            lifecycle = ScreenController(screen)
            lifecycle.moveToState(Lifecycle.State.RESUMED)
            message = screen.onGetTemplate() as MessageTemplate
            assertEquals(listOf("Allow location","Check again"),message.actions.map { it.title.toString() })
            assertTrue(message.actions.first().onClickDelegate!!.isParkedOnly)
        }
        try {
            inst.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
            inst.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
            inst.runOnMainSync {
                message.actions.last().onClickDelegate!!.sendClick(object : OnDoneCallback {
                    override fun onFailure(response: Bundleable) { fail("Permission refresh callback failed") }
                })
                assertTrue(screen.onGetTemplate() is NavigationTemplate)
            }
        } finally { inst.runOnMainSync { lifecycle.moveToState(Lifecycle.State.DESTROYED) } }
    }
}
