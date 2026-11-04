package com.waypoint.app

import androidx.car.app.HostInfo
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Maneuver as CarManeuver
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.SearchTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(AndroidJUnit4::class)
class CarCompatibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Before fun allowLocationForNavigationTemplates() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test fun signedAndroidAutoHostIsAllowedButOtherCallersAreRejected() {
        val validator = waypointHostValidator(context)
        val host = context.packageManager.getApplicationInfo("com.google.android.projection.gearhead", 0)
        val other = context.packageManager.getApplicationInfo(instrumentation.context.packageName, 0)
        assertTrue("Installed Android Auto host should pass package/signature validation",
            validator.isValidHost(HostInfo(host.packageName, host.uid)))
        assertFalse("Untrusted app must not be a car host",
            validator.isValidHost(HostInfo(other.packageName, other.uid)))
        assertThrows("Claiming Android Auto's package with a different UID must fail",
            IllegalStateException::class.java) {
            validator.isValidHost(HostInfo(host.packageName, other.uid))
        }
    }

    @Test fun simulatedCarConnectionShowsNavigationMapTemplate() {
        lateinit var template: NavigationTemplate
        lateinit var search: SearchTemplate
        instrumentation.runOnMainSync {
            val carContext = TestCarContext.createCarContext(context)
            val screen = WaypointCarScreen(carContext)
            val screenController = ScreenController(screen)
            screenController.moveToState(Lifecycle.State.RESUMED)
            // A saved destination is not required for the first car connection.
            template = screen.onGetTemplate() as NavigationTemplate
            search = WaypointCarSearchScreen(carContext).onGetTemplate() as SearchTemplate
            screenController.moveToState(Lifecycle.State.DESTROYED)
        }
        assertEquals((context.applicationContext as WaypointApplication).navigation.state.value.world.title,
            (template.navigationInfo as MessageInfo).title?.toString())
        assertEquals(listOf("Search", "Destinations", "Settings"),
            template.actionStrip!!.actions.map { it.title?.toString() })
        assertEquals("Search places", search.searchHint)
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun previewGuidanceImportAndGpsRecoveryHaveUsableCarTemplates() {
        val controller=(context.applicationContext as WaypointApplication).navigation
        val stateField=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }
        val state=stateField.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        val start=Coordinate(51.5000,-0.1200)
        val target=Coordinate(51.5050,-0.1150)
        val place=Place("mock","Museum of Navigation","Test address",target)
        val turn=Maneuver(start,"roundabout","left","Grand Road",100.0,20.0,exit=2)
        val arrive=Maneuver(target,"arrive","","",0.0,0.0)
        val route=RoadRoute(listOf(start,target),listOf(turn,arrive),900.0,110.0)
        val progress=RouteProgress(100.0,800.0,95.0,0.0,0,80.0,start)
        instrumentation.runOnMainSync {
            val carContext=TestCarContext.createCarContext(context)
            val screen=WaypointCarScreen(carContext)
            val screenController=ScreenController(screen)
            screenController.moveToState(Lifecycle.State.RESUMED)
            try {
                state.value=NavState(world=World.OLD_WEST,fix=start,gpsReady=true,
                    selected=place,route=route)
                val preview=screen.onGetTemplate() as NavigationTemplate
                assertEquals(place.name,(preview.navigationInfo as MessageInfo).title.toString())
                assertEquals(listOf("Start","Cancel"),preview.actionStrip!!.actions.map { it.title.toString() })

                state.value=state.value.copy(navigating=true,progress=progress,voice=false)
                val active=screen.onGetTemplate() as NavigationTemplate
                val routing=active.navigationInfo as RoutingInfo
                assertFalse(routing.isLoading)
                assertTrue(routing.currentStep!!.cue.toString().contains("roundabout"))
                assertEquals(CarManeuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW,routing.currentStep!!.maneuver!!.type)
                assertEquals(2,routing.currentStep!!.maneuver!!.roundaboutExitNumber)
                assertEquals(listOf("End","Voice","Settings"),active.actionStrip!!.actions.map { it.title.toString() })
                assertTrue(active.destinationTravelEstimate!!.remainingTimeSeconds>=0)

                state.value=state.value.copy(navigating=false,progress=null,route=null,
                    gpsReady=false,gpsStatus="GPS signal lost · waiting for a fix")
                val waiting=screen.onGetTemplate() as NavigationTemplate
                assertTrue((waiting.navigationInfo as MessageInfo).text.toString().contains("GPS signal lost"))
                assertEquals(listOf("Cancel"),waiting.actionStrip!!.actions.map { it.title.toString() })

                state.value=state.value.copy(selected=null,pendingSharedDestination="Museum of Navigation")
                val import=screen.onGetTemplate() as MessageTemplate
                assertTrue(import.message.toString().contains("Museum of Navigation"))
                assertEquals(listOf("Open","Dismiss"),import.actions.map { it.title.toString() })
            } finally {
                state.value=original
                screenController.moveToState(Lifecycle.State.DESTROYED)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun everyWorldKeepsTheCarDestinationAndGuidanceControls() {
        val controller=(context.applicationContext as WaypointApplication).navigation
        val stateField=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }
        val state=stateField.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        val start=Coordinate(35.6895,139.6917)
        val destination=Coordinate(35.6930,139.7020)
        val place=Place("test-tokyo","Tokyo Station","Tokyo, Japan",destination)
        val route=RoadRoute(listOf(start,destination),listOf(
            Maneuver(start,"depart","","Central Avenue",500.0,60.0),
            Maneuver(destination,"arrive","","",0.0,0.0)),900.0,120.0)
        val progress=RouteProgress(100.0,800.0,100.0,0.0,0,200.0,start)
        instrumentation.runOnMainSync {
            val carContext=TestCarContext.createCarContext(context)
            val screen=WaypointCarScreen(carContext)
            val lifecycle=ScreenController(screen)
            lifecycle.moveToState(Lifecycle.State.RESUMED)
            try {
                World.entries.forEach { world ->
                    state.value=NavState(world=world,fix=start,gpsReady=true,selected=place,route=route)
                    val preview=screen.onGetTemplate() as NavigationTemplate
                    assertEquals("${world.name} destination",place.name,(preview.navigationInfo as MessageInfo).title.toString())
                    assertEquals("${world.name} preview actions",listOf("Start","Cancel"),
                        preview.actionStrip!!.actions.map { it.title.toString() })
                    assertEquals("${world.name} map controls",4,preview.mapActionStrip!!.actions.size)
                    state.value=state.value.copy(navigating=true,progress=progress)
                    val active=screen.onGetTemplate() as NavigationTemplate
                    assertTrue("${world.name} guidance",active.navigationInfo is RoutingInfo)
                    assertEquals("${world.name} guidance actions",listOf("End","Mute","Settings"),
                        active.actionStrip!!.actions.map { it.title.toString() })
                    assertEquals("${world.name} map controls",4,active.mapActionStrip!!.actions.size)
                }
            } finally {
                state.value=original
                lifecycle.moveToState(Lifecycle.State.DESTROYED)
            }
        }
    }
}
