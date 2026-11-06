package com.waypoint.app

import android.app.Application
import android.location.Location
import com.stadiamaps.ferrostar.core.AlternativeRouteProcessor
import com.stadiamaps.ferrostar.core.CustomRouteProvider
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider.Companion.toOkHttpClientProvider
import com.stadiamaps.ferrostar.core.location.NavigationLocationProviding
import com.stadiamaps.ferrostar.core.location.NavigationLocationProvider
import com.stadiamaps.ferrostar.core.location.SimulatedLocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.WaypointAdvanceMode
import uniffi.ferrostar.stepAdvanceDistanceEntryAndExit
import uniffi.ferrostar.stepAdvanceDistanceToEndOfStep

/** Feeds only Waypoint's validated GPS fixes to Ferrostar's navigation session. */
internal class WaypointFerrostarLocation : NavigationLocationProviding {
    private val updates=MutableSharedFlow<Location>(replay=1,extraBufferCapacity=8,onBufferOverflow=BufferOverflow.DROP_OLDEST)
    @Volatile private var latest: Location?=null
    fun accept(location: Location) {
        val copy=Location(location)
        latest=copy
        updates.tryEmit(copy)
    }
    override suspend fun lastLocation(): Location?=latest?.let(::Location)
    override fun locationUpdates(intervalMillis: Long): Flow<Location> = updates
}

private fun ferrostarConfig(mode: TravelMode)=NavigationControllerConfig(
    WaypointAdvanceMode.WaypointWithinRange(if(mode==TravelMode.WALK)25.0 else 45.0),
    stepAdvanceDistanceEntryAndExit(30u,5u,32u),
    stepAdvanceDistanceToEndOfStep(if(mode==TravelMode.WALK)15u else 25u,32u),
    RouteDeviationTracking.StaticThreshold(if(mode==TravelMode.WALK)25u else 35u,if(mode==TravelMode.WALK)25.0 else 50.0),
    CourseFiltering.SNAP_TO_ROUTE,
)

/** One Ferrostar trip shared by the phone HUD, voice guidance and Android Auto. */
internal class FerrostarGuidance(
    app: Application,
    repository: NavigationRepository,
    private val onState: (NavigationState)->Unit,
    private val onAlternateRoute: (RoadRoute, Coordinate)->Unit,
    private val onAlternateError: (String)->Unit,
    private val onVoiceStatus: (GuidanceVoiceStatus)->Unit,
) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val locationProvider=WaypointFerrostarLocation()
    private val simulatedLocationProvider=SimulatedLocationProvider(scope=scope,warpFactor=2u)
    private val navigationLocationProvider=NavigationLocationProvider(locationProvider,simulatedLocationProvider)
    private var autoDriveEnabled=false
    private var mode=TravelMode.CAR
    private data class Alternate(val session: Long, val destination: Coordinate, val route: RoadRoute)
    private val sessions=GuidanceSessionGate()
    private var activeDestination: Coordinate?=null
    private var pendingAlternate: Alternate?=null
    private fun isCurrent(ticket: Long)=sessions.read { session -> ticket==session && activeDestination!=null }
    private val voice=androidGuidanceVoice(app,world={
        val preferences=app.getSharedPreferences("waypoint",0)
        if(!preferences.getBoolean("gameVoice",true))null
        else World.entries.firstOrNull { it.name==preferences.getString("world",null) } ?: World.LOS_SANTOS
    }) { status -> scope.launch { onVoiceStatus(status) } }
    private val core=FerrostarCore(
        customRouteProvider=CustomRouteProvider { user, waypoints ->
            val target=waypoints.lastOrNull()?.coordinate ?: return@CustomRouteProvider emptyList()
            val destination=Coordinate(target.lat,target.lng)
            val request=sessions.read { session ->
                if(activeDestination?.let { distance(it,destination)<=5.0 }==true) session to mode else null
            } ?: return@CustomRouteProvider emptyList()
            val (ticket,requestedMode)=request
            val origin=user.coordinates
            val replacement=try {
                repository.route(Coordinate(origin.lat,origin.lng),destination,requestedMode)
            } catch(error: CancellationException) { throw error }
            catch(error: Exception) {
                scope.launch { if(isCurrent(ticket))onAlternateError(error.message ?: "Couldn't update the route. Check your connection.") }
                throw error
            }
            val parsed=requireNotNull(replacement.ferrostarRoute) { "Ferrostar could not read the new route." }
            sessions.read { session ->
                if(ticket!=session || activeDestination?.let { distance(it,destination)<=5.0 }!=true)
                    throw CancellationException("Navigation session changed")
                pendingAlternate=Alternate(ticket,destination,replacement)
            }
            listOf(parsed)
        },
        httpClient=OkHttpClient().toOkHttpClientProvider(),
        locationProvider=navigationLocationProvider,
        navigationControllerConfig=ferrostarConfig(TravelMode.CAR),
    )
    init {
        core.spokenInstructionObserver=voice
        voice.prepare()
        core.alternativeRouteProcessor=AlternativeRouteProcessor { running, routes ->
            val selected=routes.firstOrNull()
            val replacement=sessions.read { session ->
                val candidate=pendingAlternate
                pendingAlternate=null
                candidate?.takeIf { it.session==session && selected!=null && it.route.ferrostarRoute==selected }
            }
            if(replacement!=null && selected!=null) {
                scope.launch {
                    if(!isCurrent(replacement.session))return@launch
                    try {
                        withContext(Dispatchers.IO) {
                            // Validate on the mutation dispatcher, under the same gate as start/stop.
                            // A check before withContext cannot protect a later shared-core mutation.
                            sessions.applyIfCurrent(replacement.session) { running.replaceRoute(selected) }
                        }
                        if(isCurrent(replacement.session))onAlternateRoute(replacement.route,replacement.destination)
                    } catch(error: CancellationException) { throw error }
                    catch(error: Exception) {
                        if(isCurrent(replacement.session))onAlternateError(error.message ?: "Couldn't update the route. Check your connection.")
                    }
                }
            }
        }
        scope.launch { core.state.collect { onState(it) } }
    }
    fun start(route: RoadRoute, acceptedLocation: Location?, voiceEnabled: Boolean, destination: Coordinate) {
        sessions.transition {
            activeDestination=destination;pendingAlternate=null;mode=route.mode
            acceptedLocation?.let(locationProvider::accept)
            voice.stopAndClearQueue()
            voice.setMuted(!voiceEnabled)
            if(autoDriveEnabled) navigationLocationProvider.enableSimulationOn(requireNotNull(route.ferrostarRoute))
            core.startNavigation(requireNotNull(route.ferrostarRoute) { "Ferrostar could not read this route." },ferrostarConfig(route.mode))
        }
    }
    fun enableAutoDrive(route: RoadRoute?) {
        autoDriveEnabled=true
        route?.ferrostarRoute?.let(navigationLocationProvider::enableSimulationOn)
    }
    fun isAutoDriving()=autoDriveEnabled
    fun acceptLocation(location: Location) { locationProvider.accept(location) }
    fun mute(muted: Boolean) { voice.setMuted(muted) }
    fun voiceStatus(): GuidanceVoiceStatus=voice.status
    fun stop() {
        sessions.transition {
            activeDestination=null;pendingAlternate=null
            navigationLocationProvider.disableSimulation();core.stopNavigation()
        }
    }
    fun leaveCarSession() { navigationLocationProvider.disableSimulation();autoDriveEnabled=false }
    fun shutdown() { stop();voice.shutdown();scope.cancel() }
}
