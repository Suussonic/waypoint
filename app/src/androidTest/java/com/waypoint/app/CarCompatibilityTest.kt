package com.waypoint.app

import androidx.car.app.HostInfo
import androidx.car.app.ScreenManager
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Maneuver as CarManeuver
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.model.SearchTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
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
        assertFalse("Keep the keyboard closed until the driver opens search",search.isShowKeyboardByDefault)
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
        val alternate=route.copy(meters=1200.0,seconds=140.0)
        val progress=RouteProgress(100.0,800.0,95.0,0.0,0,80.0,start)
        instrumentation.runOnMainSync {
            val carContext=TestCarContext.createCarContext(context)
            val screen=WaypointCarScreen(carContext)
            val screenController=ScreenController(screen)
            screenController.moveToState(Lifecycle.State.RESUMED)
            try {
                state.value=NavState(world=World.OLD_WEST,fix=start,gpsReady=true,
                    selected=place,route=route,routeOptions=listOf(route,alternate))
                val preview=screen.onGetTemplate() as NavigationTemplate
                assertEquals(place.name,(preview.navigationInfo as MessageInfo).title.toString())
                assertEquals(listOf("Routes","Start","Cancel"),preview.actionStrip!!.actions.map { it.title.toString() })

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
    @Test fun carRoutePickerShowsAlternativesAndReturnsToTheMapAfterSelection() = instrumentation.runOnMainSync {
        val app=context.applicationContext as WaypointApplication
        val controller=app.navigation
        val stateField=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }
        val state=stateField.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        val start=Coordinate(51.5000,-0.1200)
        val destination=Coordinate(51.5050,-0.1150)
        val place=Place("route-picker-test","Museum of Navigation","Test address",destination)
        val primary=RoadRoute(listOf(start,destination),listOf(
            Maneuver(start,"depart","","Grand Road",900.0,90.0),
            Maneuver(destination,"arrive","","",0.0,0.0)),900.0,110.0)
        val alternative=RoadRoute(listOf(start,Coordinate(51.5020,-0.1170),destination),listOf(
            Maneuver(start,"depart","","Harbour Street",1100.0,120.0),
            Maneuver(destination,"arrive","","",0.0,0.0)),1100.0,130.0)
        val car=TestCarContext.createCarContext(context)
        car.lifecycleOwner.registry.currentState=Lifecycle.State.RESUMED
        val screens=car.getCarService(ScreenManager::class.java)
        val root=WaypointCarWorldScreen(car)
        screens.push(root)
        try {
            state.value=NavState(world=World.OLD_WEST,fix=start,gpsReady=true,
                selected=place,route=primary,routeOptions=listOf(primary,alternative))
            val picker=WaypointCarRouteOptionsScreen(car)
            screens.push(picker)
            val template=picker.onGetTemplate() as MapWithContentTemplate
            val rows=template.contentTemplate.let { it as ListTemplate }.singleList!!.items.map { it as Row }
            assertEquals(2,rows.size)
            assertTrue(rows[0].title.toString().contains("min"))
            assertTrue(rows[0].texts.any { it.toString().contains("Grand Road") })
            assertTrue(rows[0].texts.any { it.toString().contains("Selected") })
            rows[1].onClickDelegate!!.sendClick(object : androidx.car.app.OnDoneCallback {
                override fun onFailure(response: androidx.car.app.serialization.Bundleable) { fail("Route selection failed") }
            })
            assertSame(alternative,controller.state.value.route)
            assertSame(root,screens.top)
        } finally {
            state.value=original
            screens.popToRoot()
            car.lifecycleOwner.registry.currentState=Lifecycle.State.DESTROYED
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun staleCarRouteChoiceCannotReplaceANewerDestination() = instrumentation.runOnMainSync {
        val controller=(context.applicationContext as WaypointApplication).navigation
        val stateField=NavigationController::class.java.getDeclaredField("_state").apply { isAccessible=true }
        val state=stateField.get(controller) as MutableStateFlow<NavState>
        val original=state.value
        val start=Coordinate(51.5000,-0.1200)
        val first=Place("first-destination","First place","First address",Coordinate(51.5050,-0.1150))
        val second=Place("second-destination","Second place","Second address",Coordinate(51.5100,-0.1100))
        val route=RoadRoute(listOf(start,first.point),listOf(Maneuver(first.point,"arrive","","",0.0,0.0)),900.0,110.0)
        val alternative=route.copy(meters=1100.0,seconds=130.0)
        val replacement=RoadRoute(listOf(start,second.point),listOf(Maneuver(second.point,"arrive","","",0.0,0.0)),1500.0,180.0)
        val car=TestCarContext.createCarContext(context)
        car.lifecycleOwner.registry.currentState=Lifecycle.State.RESUMED
        val screens=car.getCarService(ScreenManager::class.java)
        val root=WaypointCarWorldScreen(car)
        screens.push(root)
        try {
            state.value=NavState(world=World.OLD_WEST,fix=start,gpsReady=true,
                selected=first,route=route,routeOptions=listOf(route,alternative))
            val picker=WaypointCarRouteOptionsScreen(car)
            screens.push(picker)
            val rows=(picker.onGetTemplate() as MapWithContentTemplate).contentTemplate
                .let { it as ListTemplate }.singleList!!.items.map { it as Row }
            state.value=state.value.copy(selected=second,route=replacement,routeOptions=listOf(replacement))
            rows[1].onClickDelegate!!.sendClick(object : androidx.car.app.OnDoneCallback {
                override fun onFailure(response: androidx.car.app.serialization.Bundleable) { fail("Stale route callback failed") }
            })
            assertSame(replacement,controller.state.value.route)
            assertSame(picker,screens.top)
        } finally {
            state.value=original
            screens.popToRoot()
            car.lifecycleOwner.registry.currentState=Lifecycle.State.DESTROYED
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
