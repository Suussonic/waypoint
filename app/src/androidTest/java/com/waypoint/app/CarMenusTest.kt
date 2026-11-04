package com.waypoint.app

import androidx.car.app.OnDoneCallback
import androidx.car.app.ScreenManager
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.serialization.Bundleable
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class CarMenusTest {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val context get() = inst.targetContext
    private val controller get() = (context.applicationContext as WaypointApplication).navigation
    private val done = object : OnDoneCallback {
        override fun onFailure(response: Bundleable) { fail("Car action failed") }
    }
    @Test fun allWorldsAreReachableWithoutTruncation() = inst.runOnMainSync {
        val car = TestCarContext.createCarContext(context)
        car.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        val screens = car.getCarService(ScreenManager::class.java)
        val root = WaypointCarWorldScreen(car)
        screens.push(root)
        val groups = (root.onGetTemplate() as ListTemplate).singleList!!.items.map { it as Row }
        assertEquals(6,groups.size)
        val titles = mutableListOf<String>()
        try {
            fun visit(row:Row) {
                if(!row.isBrowsable) { titles+=row.title.toString();return }
                val parent=screens.top
                row.onClickDelegate!!.sendClick(done)
                val list=(screens.top.onGetTemplate() as ListTemplate).singleList!!.items
                assertTrue(list.size<=6)
                list.forEach { visit(it as Row) }
                screens.pop()
                assertSame(parent,screens.top)
            }
            groups.forEach(::visit)
            assertEquals(World.entries.map { it.title }.toSet(), titles.toSet())
            assertEquals(World.entries.size,titles.size)
        } finally { car.lifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED }
    }

    @Test fun settingsReflectPhoneChangesAndStaleToggleCannotInvertThem() = inst.runOnMainSync {
        val car = TestCarContext.createCarContext(context)
        val screen = WaypointCarSettingsScreen(car)
        val lifecycle = ScreenController(screen)
        lifecycle.moveToState(Lifecycle.State.RESUMED)
        val originalVoice = controller.state.value.voice
        val originalUnits = controller.state.value.imperial
        try {
            val initial = (screen.onGetTemplate() as ListTemplate).singleList!!.items.map { it as Row }
            controller.toggleVoice();controller.toggleUnits()
            val updated = (screen.onGetTemplate() as ListTemplate).singleList!!.items.map { it as Row }
            assertEquals(!originalVoice,updated[1].toggle!!.isChecked)
            assertEquals(!originalUnits,updated[2].toggle!!.isChecked)
            initial[1].toggle!!.onCheckedChangeDelegate.sendCheckedChange(!originalVoice,done)
            assertEquals(!originalVoice,controller.state.value.voice)
            lifecycle.moveToState(Lifecycle.State.DESTROYED)
            updated[1].toggle!!.onCheckedChangeDelegate.sendCheckedChange(originalVoice,done)
            assertEquals("Destroyed screen cannot change settings",!originalVoice,controller.state.value.voice)
        } finally {
            if(controller.state.value.voice != originalVoice)controller.toggleVoice()
            if(controller.state.value.imperial != originalUnits)controller.toggleUnits()
            if(screen.lifecycle.currentState != Lifecycle.State.DESTROYED)lifecycle.moveToState(Lifecycle.State.DESTROYED)
        }
    }
}
