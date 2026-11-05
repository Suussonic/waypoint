package com.waypoint.app

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.GnssStatus
import android.os.Handler
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.TripState
import com.stadiamaps.ferrostar.core.NavigationState

val PreviewCenter = Coordinate(22.5726,88.3639)

data class NavState(
    val world: World = World.LOS_SANTOS,
    val mode: TravelMode = TravelMode.CAR,
    val fix: Coordinate? = null,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val headingStatus: String = "Waiting for heading",
    val headingReliable: Boolean = false,
    val headingSource: HeadingSource = HeadingSource.NONE,
    val sensorDescription: String = "Checking phone sensors",
    val locationProvider: String = "Not connected",
    val satellitesUsed: Int? = null,
    val satellitesVisible: Int? = null,
    val gpsStatus: String = "GPS not connected",
    val gpsReady: Boolean = false,
    val city: String = "Kolkata · map preview",
    val selected: Place? = null,
    val route: RoadRoute? = null,
    val progress: RouteProgress? = null,
    val navigating: Boolean = false,
    val carConnected: Boolean = false,
    val arrived: Boolean = false,
    val routing: Boolean = false,
    val routeError: String? = null,
    val offRoute: Boolean = false,
    val query: String = "",
    val searchResults: List<Place> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    val searchNote: String? = null,
    val voice: Boolean = true,
    val gameVoice: Boolean = true,
    val imperial: Boolean = false,
    val saved: List<Place> = emptyList(),
    val recent: List<Place> = emptyList(),
    val message: String? = null,
    val pendingSharedDestination: String? = null,
    val pendingSharedDestinationId: Long = 0L,
    val pendingSharedCoordinates: String? = null,
    val resolvingSharedDestination: Boolean = false,
    val sharedImportError: Boolean = false,
    val cameraRequest: Int = 0,
    val focusUserCameraRequest: Int = -1,
    val tilted: Boolean = true,
)

class WaypointApplication : Application() {
    var updatePromptDismissedAtForegroundEntry: Int = -1
    private val appPrefs by lazy { getSharedPreferences("waypoint", Context.MODE_PRIVATE) }
    override fun onCreate() {
        super.onCreate()
        // An empty preference store means a fresh install; updates retain the previous store.
        if (appPrefs.all.isEmpty()) appPrefs.edit().putInt("whatsNewShownVersion", BuildConfig.VERSION_CODE).apply()
        MapGlyphs.install(this)
        updateAlerts.schedule()
    }
    // Older builds did not record which notes were read; show their full published history once.
    private fun lastNotesVersion(): Int = appPrefs.getInt("whatsNewShownVersion", 0)
    fun shouldShowWhatsNew(): Boolean = lastNotesVersion() < BuildConfig.VERSION_CODE
    fun markWhatsNewSeen() { appPrefs.edit().putInt("whatsNewShownVersion", BuildConfig.VERSION_CODE).apply() }
    fun bundledReleaseNotes(): String = releaseNotesAfter(lastNotesVersion(), BuildConfig.VERSION_CODE)
        .joinToString("\n\n") { entry -> assets.open(entry.asset).bufferedReader().use { it.readText().trim() } }
    val navigation by lazy { NavigationController(this) }
    val updater by lazy { AppUpdater(this) }
    val updateAlerts by lazy { UpdateAlerts(this) }
    val quests by lazy { QuestController(this) }
}

class NavigationController(private val app: Application, private val repository: NavigationRepository = MapRepository(), preferencesName: String = "waypoint", fallbackSearch: PlaceSearchProvider? = AndroidPlaceSearch(app)) {
    private val prefs = app.getSharedPreferences(preferencesName,Context.MODE_PRIVATE)
    private val createPlaceSearch = { PlaceSearch(PlaceSearchProvider(repository::search),fallbackSearch,SearchAliases(readSearchAliases())) }
    private var placeSearch = createPlaceSearch()
    private fun readSearchAliases():Map<String,String> = runCatching {
        val raw=prefs.getString("searchAliases",null) ?: return emptyMap()
        if(raw.length>65536)return emptyMap()
        val json=JSONObject(raw)
        json.keys().asSequence().take(100).mapNotNull { key ->
            (json.opt(key) as? String)?.let{key to it}
        }.toMap()
    }.getOrDefault(emptyMap())
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val initialWorld=World.entries.find{it.name==prefs.getString("world",null)} ?: World.LOS_SANTOS
    private val _state = MutableStateFlow(NavState(
        world = initialWorld,
        mode = TravelMode.entries.find{it.name==prefs.getString("mode",null)} ?: TravelMode.CAR,
        voice = prefs.getBoolean("voice",true), gameVoice = prefs.getBoolean("gameVoice",true), imperial = prefs.getBoolean("imperial",false),
        saved = readPlaces("saved"), recent = readPlaces("recent"),
        tilted = prefs.getBoolean("tilted",initialWorld.mapArt().tilt>0.0)
    ))
    val state = _state.asStateFlow()
    private val manager = app.getSystemService(LocationManager::class.java)
    private var visible = false
    private var carVisible = false
    val isDriving: Boolean get() = carVisible || (_state.value.navigating && _state.value.mode != TravelMode.WALK)
    private var enabled = prefs.getBoolean("locationEnabled",false)
    private var listening = false
    private val fused = LocationServices.getFusedLocationProviderClient(app)
    private val headingArbiter=HeadingArbiter()
    private var headingUiVisible=true
    private fun applyHeading(reading:HeadingReading) {
        // Lists do not need ten sensor-driven recompositions each second. The arbiter
        // still receives every sample; opening the map publishes its current reading.
        if(!headingUiVisible && !carVisible)return
        _state.update {
            if(it.headingReliable==reading.reliable && it.headingSource==reading.source && it.headingStatus==reading.status &&
                kotlin.math.abs(headingDelta(it.bearing,reading.degrees))<.3f) it
            else it.copy(bearing=reading.degrees,headingReliable=reading.reliable,headingSource=reading.source,headingStatus=reading.status)
        }
    }
    fun setHeadingUiVisible(value:Boolean) {
        headingUiVisible=value
        if(value)applyHeading(headingArbiter.current(SystemClock.elapsedRealtime()))
    }
    private val heading = HeadingSensor(app) { degrees,reliable,status ->
        applyHeading(headingArbiter.compass(degrees,reliable,status,SystemClock.elapsedRealtime()))
    }
    private val fusedCallback=object:LocationCallback() {
        override fun onLocationResult(result:LocationResult) { if(listening) result.locations.sortedBy { it.elapsedRealtimeNanos }.forEach(::receiveLocation) }
    }
    private val gnssCallback=object:GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status:GnssStatus) {
            _state.update{it.copy(satellitesUsed=(0 until status.satelliteCount).count{index->status.usedInFix(index)},satellitesVisible=status.satelliteCount)}
        }
        override fun onStopped() { _state.update{it.copy(satellitesUsed=null,satellitesVisible=null)} }
    }
    private var lastLocationNanos=0L
    private var userFocusPending=false
    private var lastFixElapsed = 0L
    private var lastGpsElapsed = 0L
    private var trackingStarted = 0L
    private var platformListening = false
    private var lastPreciseFixElapsed = 0L
    private var routeGeneration = 0L
    private val navigationSessions = NavigationSessionGate()
    val navigationSessionId: String? get() = navigationSessions.current
    fun acceptsNavigationSession(token: String?) = navigationSessions.accepts(token)
    var destinationActionVersion = 0L
        private set
    private var routeJob: Job? = null
    private var searchJob: Job? = null
    private var importJob: Job? = null
    private val importGate = NavigationImportGate()
    private val importConsent = NavigationImportConsent()
    private var routeOrigin: Coordinate? = null
    private var lastAcceptedLocation: Location? = null
    private val guidance=FerrostarGuidance(app,repository,::onFerrostarState,::onFerrostarAlternateRoute,::onFerrostarAlternateError,::onVoiceStatus)
    private var cityLookupPending = false
    private var lastCityAttempt = -60000L
    private var cityPoint: Coordinate? = null
    private var lastSimulationSampleAt = 0L
    private var lastSimulationTraveled = 0.0
    private var simulationSpeed = 0f
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = receiveLocation(location)
        override fun onProviderDisabled(provider: String) { if(provider == LocationManager.GPS_PROVIDER) _state.update{it.copy(gpsReady=false,gpsStatus="Turn on device location")} }
        override fun onProviderEnabled(provider: String) { _state.update{it.copy(gpsStatus="Finding GPS…")} }
        @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }
    init {
        scope.launch {
            while(isActive) {
                delay(1000)
                if(listening)applyHeading(headingArbiter.current(SystemClock.elapsedRealtime()))
                // Recover a silent/coarse-only fused stream instead of freezing forever.
                if(listening && !platformListening && SystemClock.elapsedRealtime()-maxOf(trackingStarted,lastPreciseFixElapsed)>8000) startPlatformLocation()
                if(listening && !guidance.isAutoDriving() && lastFixElapsed>0 && SystemClock.elapsedRealtime()-lastFixElapsed>15000) {
                    _state.update{it.copy(gpsReady=false,gpsStatus="GPS signal lost · waiting for a fix",speed=0f,headingReliable=false)}
                }
            }
        }
    }
    fun setDisplayRotation(rotation:Int) { heading.displayRotation=rotation }
    fun hasLocationPermission() = ContextCompat.checkSelfPermission(app,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    fun setCarVisible(value: Boolean) { carVisible=value; _state.update { it.copy(carConnected=value) }; if(value) enabled=true; updateTracking() }
    private val carSessions = mutableSetOf<Any>()
    internal fun attachCarSession(owner: Any) {
        carSessions.add(owner)
        setCarVisible(true)
    }
    internal fun detachCarSession(owner: Any) {
        if(carSessions.remove(owner) && carSessions.isEmpty()) {
            setCarVisible(false)
            leaveCarSession()
        }
    }
    internal fun refreshCarLocation() {
        if(hasLocationPermission()) {
            if(carVisible) enabled=true
            updateTracking()
        } else {
            updateTracking()
            permissionDenied()
        }
    }
    fun enableAutoDrive() {
        lastSimulationSampleAt=0L;lastSimulationTraveled=0.0;simulationSpeed=0f
        guidance.enableAutoDrive(_state.value.route)
        _state.update { it.copy(message="Android Auto test drive is simulating this route.") }
    }
    fun leaveCarSession() { guidance.leaveCarSession();lastSimulationSampleAt=0L;simulationSpeed=0f }
    fun handleNavigationIntent(intent: Intent?): Boolean {
        val shared = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        if (shared != null) {
            invalidateImport()
            if (_state.value.navigating) {
                _state.update { it.copy(message="End your current navigation before opening a shared destination.") }
                return false
            }
            val parsed = NavigationSharing.parseText(shared)
            if (parsed != null) return offerNavigationImport(parsed)
            if (shared.length > 4096 || NavigationSharing.supportedUrl(NavigationSharing.sharedUrl(shared) ?: shared) == null) {
                sharedImportFailed()
                return true
            }
            val ticket = importGate.begin()
            _state.update { it.copy(resolvingSharedDestination = true) }
            importJob = scope.launch {
                val resolved = try { withTimeoutOrNull(10_000) { NavigationSharing.resolve(shared) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                ensureActive()
                if (!importGate.canApply(ticket, _state.value.navigating)) return@launch
                importJob = null
                _state.update { it.copy(resolvingSharedDestination = false) }
                if (resolved != null) offerNavigationImport(resolved) else sharedImportFailed()
            }
            return true
        }
        val proposal = NavigationImport.parse(intent?.action, intent?.dataString) ?: return false
        invalidateImport()
        return offerNavigationImport(proposal)
    }
    private fun sharedImportFailed() {
        _state.update { it.copy(sharedImportError = true, resolvingSharedDestination = false, message = "Couldn't read this place. Paste an address or search for it below.") }
    }
    fun dismissSharedImportError() { _state.update { it.copy(sharedImportError = false) } }
    private fun offerNavigationImport(proposal: NavigationImport): Boolean {
        val id = importConsent.offer(proposal, _state.value.navigating)
        if (id == null) {
            _state.update { it.copy(message="End your current navigation before opening a shared destination.") }
            return false
        }
        _state.update { it.copy(pendingSharedDestination=when (proposal) {
            is NavigationImport.Point -> proposal.label
            is NavigationImport.Search -> proposal.query
        },pendingSharedDestinationId=id,pendingSharedCoordinates=if (proposal is NavigationImport.Point)
            "${proposal.coordinate.lat}, ${proposal.coordinate.lon}" else null) }
        return true
    }
    fun hasPendingNavigationImport() = _state.value.pendingSharedDestination != null
    fun acceptPendingNavigationImport(id: Long) {
        if (_state.value.pendingSharedDestinationId != id) return
        if (_state.value.navigating) { invalidateImport(); return }
        val proposal = importConsent.accept(id, false) ?: return
        _state.update { it.copy(pendingSharedDestination=null,pendingSharedDestinationId=0L,pendingSharedCoordinates=null,
            resolvingSharedDestination=proposal is NavigationImport.Point) }
        when (proposal) {
            is NavigationImport.Point -> {
                val ticket=importGate.begin()
                importJob=scope.launch {
                    val resolved=try { withTimeoutOrNull(8000) { repository.reversePlace(proposal.coordinate) } }
                    catch(error: CancellationException) { throw error }
                    catch(_: Exception) { null }
                    ensureActive()
                    if(!importGate.canApply(ticket,_state.value.navigating))return@launch
                    importJob=null
                    val genericLabel=proposal.label==coordinateLabel(proposal.coordinate) ||
                        proposal.label.equals("Shared destination",true) || proposal.label.equals("Dropped pin",true)
                    val chosenName=if(genericLabel)
                        resolved?.name ?: proposal.label else proposal.label
                    select(Place("shared:${proposal.coordinate.lat},${proposal.coordinate.lon}",chosenName,
                        resolved?.address?.ifBlank { "Coordinates: ${proposal.coordinate.lat}, ${proposal.coordinate.lon}" }
                            ?: "Coordinates: ${proposal.coordinate.lat}, ${proposal.coordinate.lon}",proposal.coordinate,
                        source=resolved?.source.orEmpty()))
                }
            }
            is NavigationImport.Search -> {
                val ticket = importGate.begin()
                importJob = scope.launch {
                    try {
                        val place = placeSearch.search(proposal.query, _state.value.fix ?: PreviewCenter,expanded=true).places.firstOrNull()
                        ensureActive()
                        if (!importGate.canApply(ticket, _state.value.navigating)) return@launch
                        importJob = null
                        if (place != null) select(place)
                        else _state.update { it.copy(message="Couldn't find the shared destination. Search by address.") }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        ensureActive()
                        if (importGate.canApply(ticket, _state.value.navigating)) _state.update { it.copy(message="Couldn't find the shared destination. Search by address.") }
                    }
                }
            }
        }
    }
    fun dismissPendingNavigationImport(id: Long) {
        if (_state.value.pendingSharedDestinationId != id || !importConsent.dismiss(id)) return
        invalidateImport()
    }
    private fun invalidateImport() {
        importGate.invalidate(); importJob?.cancel(); importJob=null; importConsent.dismissAll()
        if (_state.value.pendingSharedDestination != null || _state.value.resolvingSharedDestination)
            _state.update { it.copy(pendingSharedDestination=null,pendingSharedDestinationId=0L,pendingSharedCoordinates=null,resolvingSharedDestination=false) }
    }
    fun clearHistory() {
        invalidateImport()
        searchJob?.cancel();searchJob=null
        placeSearch=createPlaceSearch()
        _state.update{it.copy(recent=emptyList(),saved=emptyList(),query="",searchResults=emptyList(),searching=false,searchError=null,searchNote=null)}
        prefs.edit().remove("recent").remove("saved").apply()
    }
    fun setVisible(value: Boolean) { visible=value; updateTracking() }
    fun enableLocation() { enabled=true; prefs.edit().putBoolean("locationEnabled",true).apply(); _state.update{it.copy(gpsStatus="Finding GPS…",cameraRequest=it.cameraRequest+1)}; updateTracking() }
    private fun updateTracking() {
        val should = hasLocationPermission() && enabled && (visible || carVisible || _state.value.navigating)
        if(should && !listening) {
            try {
                listening=true
                trackingStarted=SystemClock.elapsedRealtime()
                _state.update{it.copy(gpsStatus="Finding GPS…",sensorDescription=heading.description)}
                headingArbiter.reset()
                heading.start()
                manager.registerGnssStatusCallback(gnssCallback,Handler(app.mainLooper))
                if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(app)==ConnectionResult.SUCCESS) {
                    _state.update{it.copy(locationProvider="Fused location · GNSS / Wi-Fi / cell")}
                    val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000L)
                        .setMinUpdateIntervalMillis(500L).setMaxUpdateDelayMillis(0L)
                        .setWaitForAccurateLocation(false).build()
                    fused.requestLocationUpdates(request,fusedCallback,app.mainLooper)
                        .addOnSuccessListener{if(!listening)fused.removeLocationUpdates(fusedCallback)}
                        .addOnFailureListener{if(listening)startPlatformLocation()}
                } else startPlatformLocation()
                if(!androidx.core.location.LocationManagerCompat.isLocationEnabled(manager)) _state.update{it.copy(gpsReady=false,gpsStatus="Turn on device location")}
            } catch (_: SecurityException) { stopTracking();permissionDenied() }
        } else if(!should && listening) {
            stopTracking()
            _state.update{it.copy(gpsReady=false,headingReliable=false)}
        }
    }
    @android.annotation.SuppressLint("MissingPermission")
    private fun startPlatformLocation() {
        if(!hasLocationPermission()){stopTracking();permissionDenied();return}
        if(platformListening)return
        try {
            platformListening=true
            manager.removeUpdates(locationListener)
            for(provider in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER)) {
                if(manager.allProviders.contains(provider)) manager.requestLocationUpdates(provider,1000L,1f,locationListener,app.mainLooper)
            }
            _state.update{it.copy(locationProvider="Android GNSS + network fallback")}
        } catch(_:SecurityException){stopTracking();permissionDenied()}
    }
    private fun stopTracking() {
        listening=false
        platformListening=false
        manager.removeUpdates(locationListener)
        fused.removeLocationUpdates(fusedCallback)
        manager.unregisterGnssStatusCallback(gnssCallback)
        heading.stop()
    }
    fun permissionDenied() { userFocusPending=false;_state.update{it.copy(gpsReady=false,gpsStatus="Precise location required",message="Allow precise location in Android settings to navigate.")} }
    private fun onFerrostarState(state: NavigationState) {
        val active=state.tripState
        if(active is TripState.Complete && _state.value.navigating) {
            navigationSessions.end()
            _state.update { it.copy(navigating=false,arrived=true,offRoute=false,routing=false) }
            guidance.stop()
            app.stopService(Intent(app,NavigationService::class.java))
            updateTracking()
            return
        }
        if(active !is TripState.Navigating || !_state.value.navigating)return
        val route=_state.value.route ?: return
        val allSteps=route.ferrostarRoute?.steps ?: return
        val currentIndex=(allSteps.size-active.remainingSteps.size).coerceIn(0,allSteps.lastIndex)
        val nextIndex=(currentIndex+1).coerceAtMost(route.steps.lastIndex)
        val snapped=Coordinate(active.snappedUserLocation.coordinates.lat,active.snappedUserLocation.coordinates.lng)
        val remainingShape=buildList {
            add(snapped)
            active.remainingSteps.forEachIndexed { index,step ->
                val first=if(index==0) (active.currentStepGeometryIndex?.toInt() ?: 0).coerceIn(0,step.geometry.lastIndex.coerceAtLeast(0)) else 0
                step.geometry.drop(first).forEach { add(Coordinate(it.lat,it.lng)) }
            }
        }
        val p=active.progress
        val cue=active.visualInstruction?.primaryContent?.text?.takeIf { it.isNotBlank() }
            ?: route.steps.getOrNull(nextIndex)?.instruction ?: "Continue on your route"
        val guidanceText=if(route.steps.getOrNull(nextIndex)?.type=="arrive" && p.distanceToNextManeuver>150)
            active.remainingSteps.firstOrNull()?.roadName?.takeIf { it.isNotBlank() }?.let { "Continue on $it" } ?: "Continue toward your destination"
            else cue
        val offRoute=active.deviation is RouteDeviation.Deviation
        val progress=RouteProgress(
            traveled=(route.meters-p.distanceRemaining).coerceAtLeast(0.0),
            remaining=p.distanceRemaining.coerceAtLeast(0.0),
            seconds=p.durationRemaining.coerceAtLeast(0.0),
            offRoute=distance(_state.value.fix ?: snapped,snapped),
            nextStep=nextIndex,
            toTurn=p.distanceToNextManeuver.coerceAtLeast(0.0),
            snapped=snapped,
            remainingShape=remainingShape,
            guidanceText=guidanceText,
        )
        if(guidance.isAutoDriving()) {
            val now=SystemClock.elapsedRealtime()
            val interval=now-lastSimulationSampleAt
            if(lastSimulationSampleAt>0 && interval>=200) {
                val measured=((progress.traveled-lastSimulationTraveled)/(interval/1000.0))
                    .toFloat().takeIf { it.isFinite() && it>=0f }?.coerceAtMost(50f) ?: 0f
                val reported=active.snappedUserLocation.speed?.value?.toFloat()?.takeIf { it.isFinite() && it>0.2f }
                simulationSpeed=if(simulationSpeed==0f)reported ?: measured
                    else simulationSpeed*.45f+(reported ?: measured)*.55f
            }
            if(interval>=200 || lastSimulationSampleAt==0L) {
                lastSimulationSampleAt=now;lastSimulationTraveled=progress.traveled
            }
        }
        _state.update { if(it.navigating)it.copy(progress=progress,offRoute=offRoute,routing=state.isCalculatingNewRoute,
            fix=if(guidance.isAutoDriving()) snapped else it.fix,
            gpsReady=if(guidance.isAutoDriving()) true else it.gpsReady,
            gpsStatus=if(guidance.isAutoDriving()) "Simulated drive" else it.gpsStatus,
            speed=if(guidance.isAutoDriving()) simulationSpeed else it.speed) else it }
    }
    private fun onFerrostarAlternateRoute(route: RoadRoute, destination: Coordinate) {
        val current=_state.value
        if(!current.navigating || current.selected==null || route.mode!=current.mode ||
            distance(current.selected.point,destination)>5.0)return
        routeOrigin=_state.value.fix
        _state.update { it.copy(route=route,progress=null,offRoute=false,routing=false,routeError=null) }
    }
    private fun onFerrostarAlternateError(message: String) {
        if(_state.value.navigating)_state.update { it.copy(routing=false,routeError=message) }
    }
    private fun receiveLocation(location: Location) {
        if(!listening)return
        if(guidance.isAutoDriving() && _state.value.navigating)return
        val now = SystemClock.elapsedRealtime()
        val fix = Coordinate(location.latitude,location.longitude)
        if (!fix.isValid() || !location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy < 0f) return
        if(location.elapsedRealtimeNanos<=lastLocationNanos)return
        val age = (SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos)/1_000_000
        if(age !in -1000L..15000L) return
        if(location.provider==LocationManager.NETWORK_PROVIDER && now-lastGpsElapsed<10000) return
        if(location.provider==LocationManager.GPS_PROVIDER) lastGpsElapsed=now
        lastLocationNanos=location.elapsedRealtimeNanos
        heading.updateLocation(location)
        val first = _state.value.fix==null
        lastFixElapsed=now-age
        val reliable = location.hasAccuracy() && location.accuracy <= 60f
        val focusUserNow = userFocusPending && reliable
        if(focusUserNow) userFocusPending=false
        if(reliable)lastPreciseFixElapsed=now-age
        val speed = if(location.hasSpeed() && location.speed.isFinite() && location.speed >= 0f) location.speed else 0f
        _state.update{
            val nextCameraRequest=it.cameraRequest+if(first || focusUserNow) 1 else 0
            it.copy(fix=fix,accuracy=location.accuracy,gpsReady=reliable,
            gpsStatus=if(reliable) "GPS connected" else "Improving GPS accuracy…",
            speed=speed,
            city=if(first)"Current area" else it.city,
            cameraRequest=nextCameraRequest,
            focusUserCameraRequest=if(focusUserNow) nextCameraRequest else it.focusUserCameraRequest)
        }
        applyHeading(headingArbiter.location(
            bearing=if(location.hasBearing())location.bearing else null,
            speed=speed,
            bearingAccuracy=if(location.hasBearingAccuracy())location.bearingAccuracyDegrees else null,
            speedAccuracy=if(location.hasSpeedAccuracy())location.speedAccuracyMetersPerSecond else null,
            fixReliable=reliable,mode=_state.value.mode,now=now))
        if(reliable && _state.value.pendingSharedDestination==null && !cityLookupPending && now-lastCityAttempt>=60000 && (cityPoint==null || distance(cityPoint!!,fix)>5000)) {
            cityLookupPending=true;lastCityAttempt=now
            _state.update{it.copy(city="Current area")}
            scope.launch {
                try { val label=repository.reverse(fix);cityPoint=fix;_state.update{it.copy(city=label.ifBlank{"Current area"})} }
                catch(_: Exception) { /* Keep the geographic-neutral label; a later fix retries. */ }
                finally { cityLookupPending=false }
            }
        }
        if(!reliable) return
        lastAcceptedLocation=Location(location)
        guidance.acceptLocation(location)
        val s = _state.value
        if(!s.navigating && s.selected!=null && !s.routing && !s.arrived && s.routeError==null &&
            (s.route==null || (routeOrigin?.let{distance(it,fix)} ?: 0.0)>100.0)) requestRoute()
    }
    fun select(place: Place) {
        invalidateImport()
        if (!place.point.isValid()) { _state.update { it.copy(message="That destination has invalid coordinates. Choose another place.") }; return }
        destinationActionVersion++
        stopNavigation(clear=false)
        _state.update{it.copy(selected=place,route=null,progress=null,routeError=null,arrived=false,cameraRequest=it.cameraRequest+1)}
        if(_state.value.gpsReady) requestRoute()
    }
    fun clearDestination() { destinationActionVersion++;stopNavigation(); _state.update{it.copy(selected=null,route=null,progress=null,arrived=false,routeError=null)} }
    fun requestRoute(reroute: Boolean = false) {
        val s = _state.value; val origin = s.fix ?: return; val target = s.selected ?: return
        if(s.pendingSharedDestination!=null)return
        if(!s.gpsReady) { _state.update{it.copy(message="Waiting for an accurate GPS fix.")}; return }
        routeJob?.cancel()
        val generation=++routeGeneration
        // A preview route belongs to the origin where it was calculated. If the
        // phone moves and the replacement request fails, leaving the old line on
        // screen implies that driving from the new location is still possible.
        // Active guidance keeps its current line while Ferrostar reroutes.
        _state.update{it.copy(routing=true,routeError=null,
            route=if(it.navigating)it.route else null,
            progress=if(it.navigating)it.progress else null)}
        routeJob = scope.launch {
            try {
                val route = repository.route(origin,target.point,s.mode)
                ensureActive()
                if(generation!=routeGeneration || _state.value.selected?.id!=target.id || _state.value.mode!=s.mode)return@launch
                val latestFix=_state.value.fix ?: origin
                // A slow response can already be obsolete at the current position.
                if(distance(origin,latestFix)>100) {
                    _state.update{it.copy(routing=false)}
                    requestRoute(reroute);return@launch
                }
                routeOrigin=origin
                _state.update{it.copy(route=route,progress=null,routing=false,offRoute=false,routeError=null,cameraRequest=it.cameraRequest+if(reroute) 0 else 1)}
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { ensureActive(); _state.update{it.copy(routing=false,routeError=e.message ?: "Couldn't load the route. Check your connection.")} }
        }
    }
    fun startNavigation() {
        invalidateImport()
        val s = _state.value
        if(!s.gpsReady || s.fix==null || !hasLocationPermission()) { _state.update{it.copy(message="Enable precise GPS and wait for a location fix.")}; return }
        if(s.route==null || s.routing || s.route.mode!=s.mode) return
        if((routeOrigin?.let{distance(it,s.fix)} ?: 0.0)>100.0) { requestRoute(); _state.update{it.copy(message="Updating the route from your current position. Start when it's ready.")}; return }
        if(s.routeError!=null) { requestRoute(); return }
        if(s.route.ferrostarRoute==null) { _state.update{it.copy(message="Ferrostar could not read this route. Try another destination.")};return }
        val destination=s.selected?.point ?: return
        navigationSessions.begin()
        _state.update{it.copy(navigating=true,arrived=false,offRoute=false,progress=null,cameraRequest=it.cameraRequest+1)}
        try { ContextCompat.startForegroundService(app,Intent(app,NavigationService::class.java)) }
        catch(error: Exception) { navigationSessions.end();android.util.Log.e("WaypointNav","Foreground service failed",error);_state.update{it.copy(navigating=false,message="Android couldn't start navigation. Keep the app open and try again.")}; return }
        try { guidance.start(s.route,lastAcceptedLocation,s.voice,destination) }
        catch(error: Exception) {
            navigationSessions.end()
            runCatching { guidance.stop() }
            android.util.Log.e("WaypointNav","Ferrostar guidance failed",error)
            _state.update{it.copy(navigating=false,message="Couldn't start guidance. Try this route again.")}
            app.stopService(Intent(app,NavigationService::class.java));return
        }
        s.selected.let { place ->
            val recent = (listOf(place)+s.recent.filter{it.id!=place.id}).take(12)
            _state.update{it.copy(recent=recent)}; writePlaces("recent",recent)
        }
        onVoiceStatus(guidance.voiceStatus())
    }
    private fun onVoiceStatus(status: GuidanceVoiceStatus) {
        _state.update { current ->
            if (!current.navigating || !current.voice) current
            else if (status.problem != null) current.copy(message=status.problem)
            else if (status == GuidanceVoiceStatus.READY && GuidanceVoiceStatus.entries.any { it.problem != null && it.problem == current.message }) current.copy(message=null)
            else current
        }
    }
    fun stopNavigation(clear: Boolean = true) {
        navigationSessions.end()
        lastSimulationSampleAt=0L;simulationSpeed=0f
        invalidateImport()
        routeGeneration++
        routeJob?.cancel(); routeJob=null; guidance.stop()
        _state.update{it.copy(navigating=false,routing=false,offRoute=false,arrived=if(clear) false else it.arrived)}
        app.stopService(Intent(app,NavigationService::class.java)); updateTracking()
    }
    fun serviceStopped(sessionId: String?) {
        if(!navigationSessions.accepts(sessionId))return
        navigationSessions.end()
        if(_state.value.navigating) {
            guidance.stop()
            _state.update{it.copy(navigating=false,message="Navigation stopped.")}
            updateTracking()
        }
    }
    fun search(query: String) {
        if (query.trim().startsWith("https:") || query.trim().startsWith("geo:")) {
            searchJob?.cancel()
            _state.update { it.copy(query=query.take(4096), searching=false, searchResults=emptyList(), searchError=null, searchNote="Tap Search places to open this shared location.") }
        } else searchPlaces(query,false)
    }
    fun submitSearch() {
        val query = _state.value.query.trim()
        if (query.startsWith("https:") || query.startsWith("geo:")) handleNavigationIntent(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT,query))
        else searchPlaces(query,true)
    }
    private fun searchPlaces(query:String,expanded:Boolean) {
        invalidateImport()
        searchJob?.cancel(); _state.update{it.copy(query=query,searching=query.length>=2,searchResults=emptyList(),searchError=null,searchNote=null)}
        if(query.trim().length<2) { _state.update{it.copy(searching=false)}; return }
        searchJob=scope.launch {
            if(!expanded)delay(700)
            try {
                val snapshot=_state.value
                val result=placeSearch.search(query,snapshot.fix ?: PreviewCenter,snapshot.saved+snapshot.recent,expanded);ensureActive()
                _state.update { current ->
                    val savedNames=current.saved.associate { it.id to it.name }
                    current.copy(searchResults=result.places.map { place ->
                        savedNames[place.id]?.let { place.copy(name=it) } ?: place
                    },searchNote=result.note,searching=false)
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { ensureActive(); _state.update{it.copy(searching=false,searchError=e.message ?: "Search unavailable. Try again.")} }
        }
    }
    fun changeMode(mode:TravelMode) {
        if(_state.value.navigating || _state.value.mode==mode)return
        routeJob?.cancel()
        _state.update{it.copy(mode=mode,route=null,progress=null,routing=false,routeError=null)}
        prefs.edit().putString("mode",mode.name).apply()
        if(_state.value.selected!=null && _state.value.gpsReady)requestRoute()
    }
    fun changeWorld(world: World) { _state.update{it.copy(world=world,tilted=if(prefs.contains("tilted"))it.tilted else world.mapArt().tilt>0.0)}; prefs.edit().putString("world",world.name).apply() }
    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean(
        "onboardingCompleted",
        prefs.getBoolean("mapOpened", prefs.contains("world"))
    )
    // Setup is a first-run task. Release changes have their own What's New dialog.
    fun shouldShowLanding(): Boolean = !hasCompletedOnboarding()
    fun markMapOpened() {
        prefs.edit().putBoolean("mapOpened", true).putBoolean("onboardingCompleted", true).apply()
    }
    fun favoriteWorlds(): List<World> = prefs.getString("favoriteWorlds", null)
        ?.split(',')
        ?.mapNotNull { name -> World.entries.find { it.name == name } }
        ?.distinct()
        ?.take(World.entries.size)
        ?: listOf(_state.value.world)
    fun setFavoriteWorlds(worlds: List<World>) {
        prefs.edit().putString("favoriteWorlds", worlds.distinct().joinToString(",") { it.name }).apply()
    }
    fun completeOnboarding(favorites: List<World>) {
        val edit = prefs.edit().putBoolean("onboardingCompleted", true).putBoolean("mapOpened", true)
            .putInt("welcomeShownVersion", BuildConfig.VERSION_CODE)
        if (favorites.isNotEmpty()) edit.putString("favoriteWorlds", favorites.distinct().joinToString(",") { it.name })
        edit.apply()
    }
    fun toggleVoice() { _state.update{it.copy(voice=!it.voice)}; prefs.edit().putBoolean("voice",_state.value.voice).apply(); guidance.mute(!_state.value.voice) }
    fun toggleGameVoice() { _state.update { it.copy(gameVoice=!it.gameVoice) };prefs.edit().putBoolean("gameVoice",_state.value.gameVoice).apply() }
    fun toggleUnits() { _state.update{it.copy(imperial=!it.imperial)}; prefs.edit().putBoolean("imperial",_state.value.imperial).apply() }
    fun toggleTilt() { _state.update{it.copy(tilted=!it.tilted,cameraRequest=it.cameraRequest+1)};prefs.edit().putBoolean("tilted",_state.value.tilted).apply() }
    fun recenter() {
        if(_state.value.fix==null || !_state.value.gpsReady) { userFocusPending=true;return }
        userFocusPending=false
        _state.update{val next=it.cameraRequest+1;it.copy(cameraRequest=next,focusUserCameraRequest=next)}
    }
    fun dismissMessage() { _state.update{it.copy(message=null)} }
    fun toggleSaved(place: Place) {
        val existing=_state.value.saved
        val next=if(existing.any{it.id==place.id}) existing.filter{it.id!=place.id} else existing+place
        _state.update{it.copy(saved=next)}; writePlaces("saved",next)
    }
    /** A personal label changes only the name; coordinates, address and provenance stay intact. */
    fun renameSaved(id: String, label: String): Boolean {
        val name = savedPlaceName(label) ?: return false
        if (_state.value.saved.none { it.id == id }) return false
        _state.update { state -> state.copy(
            saved = state.saved.map { if (it.id == id) it.copy(name = name) else it },
            selected = state.selected?.let { if (it.id == id) it.copy(name = name) else it },
            recent = state.recent.map { if (it.id == id) it.copy(name = name) else it },
            searchResults = state.searchResults.map { if (it.id == id) it.copy(name = name) else it }
        ) }
        writePlaces("saved", _state.value.saved)
        writePlaces("recent", _state.value.recent)
        return true
    }
    private fun readPlaces(key: String): List<Place> = runCatching {
        val a=JSONArray(prefs.getString(key,"[]")); (0 until a.length()).map {
            val p=a.getJSONObject(it); Place(p.getString("id"),p.getString("name"),p.getString("address"),Coordinate(p.getDouble("lat"),p.getDouble("lon")),p.optString("kind","place"),p.optString("source"))
        }
    }.getOrDefault(emptyList())
    private fun writePlaces(key: String, places: List<Place>) {
        val a=JSONArray(); places.forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("address",it.address).put("lat",it.point.lat).put("lon",it.point.lon).put("kind",it.kind).put("source",it.source)) }
        prefs.edit().putString(key,a.toString()).apply()
    }
}
