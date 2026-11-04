package com.waypoint.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.car.app.ScreenManager
import androidx.car.app.model.MessageTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.navigation.TestNavigationManager
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarSessionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val navigation get() = (context.applicationContext as WaypointApplication).navigation

    @Before fun permissions() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test fun menusAndBackgroundDoNotDisconnectTheCar() = instrumentation.runOnMainSync {
        val car = TestCarContext.createCarContext(context)
        val session = CarSessionHarness(WaypointCarSession(), car, Intent())
        session.moveToState(Lifecycle.State.RESUMED)
        val screens = car.getCarService(ScreenManager::class.java)
        try {
            assertTrue(navigation.state.value.carConnected)
            assertTrue(navigation.isDriving)
            screens.push(WaypointCarSettingsScreen(car))
            assertTrue("Settings must retain the connection", navigation.state.value.carConnected)
            screens.push(WaypointCarWorldScreen(car))
            assertTrue("World selection must retain the connection", navigation.state.value.carConnected)
            screens.popToRoot()
            screens.push(WaypointCarSearchScreen(car))
            assertTrue("Search must retain the connection", navigation.state.value.carConnected)
            session.moveToState(Lifecycle.State.CREATED)
            assertTrue("A hidden session is still connected", navigation.state.value.carConnected)
            session.moveToState(Lifecycle.State.RESUMED)
        } finally { session.moveToState(Lifecycle.State.DESTROYED) }
        assertFalse(navigation.state.value.carConnected)
        assertNull(car.getCarService(TestNavigationManager::class.java).navigationManagerCallback)
    }

    @Test fun oldSessionDisconnectCannotClearAReconnectedSession() = instrumentation.runOnMainSync {
        val first = CarSessionHarness(WaypointCarSession(), TestCarContext.createCarContext(context), Intent())
        val second = CarSessionHarness(WaypointCarSession(), TestCarContext.createCarContext(context), Intent())
        first.moveToState(Lifecycle.State.RESUMED)
        second.moveToState(Lifecycle.State.RESUMED)
        try {
            first.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(navigation.state.value.carConnected)
        } finally { second.moveToState(Lifecycle.State.DESTROYED) }
        assertFalse(navigation.state.value.carConnected)
    }

    @Test fun incomingDestinationReturnsFromMenusToConfirmation() = instrumentation.runOnMainSync {
        val car = TestCarContext.createCarContext(context)
        val instance = WaypointCarSession()
        val session = CarSessionHarness(instance, car, Intent())
        session.moveToState(Lifecycle.State.RESUMED)
        val screens = car.getCarService(ScreenManager::class.java)
        val root = screens.top
        try {
            screens.push(WaypointCarSettingsScreen(car))
            instance.onNewIntent(Intent("androidx.car.app.action.NAVIGATE", Uri.parse("geo:0,0?q=51.5,-0.12(Test%20destination)")))
            assertSame(root, screens.top)
            assertNotNull(navigation.state.value.pendingSharedDestination)
            assertTrue(root.onGetTemplate() is MessageTemplate)
        } finally {
            navigation.dismissPendingNavigationImport(navigation.state.value.pendingSharedDestinationId)
            session.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test fun repeatedMapLifecycleDoesNotChangeConnectionOwnership() = instrumentation.runOnMainSync {
        val car = TestCarContext.createCarContext(context)
        val owner = Any()
        navigation.attachCarSession(owner)
        try {
            repeat(10) {
                val screen = ScreenController(WaypointCarScreen(car))
                screen.moveToState(Lifecycle.State.RESUMED)
                screen.moveToState(Lifecycle.State.CREATED)
                screen.moveToState(Lifecycle.State.RESUMED)
                screen.moveToState(Lifecycle.State.DESTROYED)
                assertTrue(navigation.state.value.carConnected)
            }
        } finally { navigation.detachCarSession(owner) }
        assertFalse(navigation.state.value.carConnected)
    }
}

/** Drive the context registry too; SessionController 1.8 only advances the public registry. */
private class CarSessionHarness(private val session: WaypointCarSession, private val car: TestCarContext, intent: Intent) {
    init {
        session.setCarContextInternal(car)
        session.setLifecycleRegistryInternal(car.lifecycleOwner.registry)
        car.lifecycleOwner.registry.currentState = Lifecycle.State.CREATED
        car.getCarService(ScreenManager::class.java).push(session.onCreateScreen(intent))
    }
    fun moveToState(state: Lifecycle.State) { car.lifecycleOwner.registry.currentState = state }
}
