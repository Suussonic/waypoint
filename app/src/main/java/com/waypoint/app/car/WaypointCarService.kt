package com.waypoint.app

import android.content.Intent
import android.content.Context
import android.app.Presentation
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.DisplayMetrics
import android.view.Surface
import android.os.SystemClock
import androidx.car.app.*
import androidx.car.app.model.*
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver as CarManeuver
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.tan
import kotlin.math.log2
import kotlin.math.atan
import kotlin.math.sinh
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2

/** Projected Android Auto UI. Uses the same routing and GPS engine as the phone. */
class WaypointCarService : CarAppService() {
    // Android Auto hosts are accepted by package *and* signing certificate. Never use
    // ALLOW_ALL_HOSTS_VALIDATOR on an exported service that controls navigation state.
    override fun createHostValidator(): HostValidator = waypointHostValidator(this)
    override fun onCreateSession(): Session = WaypointCarSession()
}

/** Connection lifetime belongs to the host session, not a map hidden by a menu. */
internal class WaypointCarSession : Session() {
    private val controller get() = (carContext.applicationContext as WaypointApplication).navigation
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) { controller.attachCarSession(this@WaypointCarSession) }
            override fun onResume(owner: LifecycleOwner) { controller.refreshCarLocation() }
            override fun onDestroy(owner: LifecycleOwner) { controller.detachCarSession(this@WaypointCarSession) }
        })
    }
    override fun onCreateScreen(intent: Intent): Screen {
        controller.handleNavigationIntent(intent)
        return WaypointCarScreen(carContext)
    }
    override fun onNewIntent(intent: Intent) {
        if (controller.handleNavigationIntent(intent)) {
            // The confirmation belongs on the map, even if Search or Settings is open.
            carContext.getCarService(ScreenManager::class.java).popToRoot()
        }
    }
}

internal fun waypointHostValidator(context: Context): HostValidator = HostValidator.Builder(context)
    .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
    // Allow the Play-signed Android Auto host alongside AndroidX's sample allowlist.
    .addAllowedHost("com.google.android.projection.gearhead", "1ca8dcc0bed3cbd872d2cb791200c0292ca9975768a82d676b8b424fb65b5295")
    .build()

internal class WaypointCarScreen(context: CarContext): Screen(context), SurfaceCallback {
    private val controller=(context.applicationContext as WaypointApplication).navigation
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var surface: Surface?=null
    private var virtualDisplay: VirtualDisplay?=null
    private var presentation: Presentation?=null
    private var liveMap: WaypointCarLiveMap?=null
    private var width=800; private var height=480
    // Camera geometry must use the projected surface's density. CarContext's
    // resources describe the phone display and can be several times denser.
    private var surfaceDensity=1.0
    private var visibleArea=Rect(0,0,800,480)
    private var stableArea: Rect?=null
    private var mapFirstVisibleAt=0L
    private var mapCenter: LatLng?=null
    private var zoomOffset=0.0
    private var mapContext: Triple<World,String?,Boolean>?=null
    private var signaled=false
    private var lastTemplateKey: List<Any?>?=null
    private val overlay=WaypointCarOverlay(carContext)
    private val manager=carContext.getCarService(NavigationManager::class.java)
    private fun screenActive()=lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    init {
        MapLibre.getInstance(context)
        manager.setNavigationManagerCallback(object: NavigationManagerCallback {
            override fun onStopNavigation() { controller.stopNavigation() }
            override fun onAutoDriveEnabled() { controller.enableAutoDrive() }
        })
        lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                controller.refreshCarLocation()
                invalidate()
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(this@WaypointCarScreen)
            }
            override fun onDestroy(owner: LifecycleOwner) {
                if(signaled) { manager.navigationEnded();signaled=false }
                manager.clearNavigationManagerCallback()
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
                scope.cancel();closeSurface(true)
            }
        })
        scope.launch {
            controller.state.collect { s ->
                val contextKey=Triple(s.world,s.selected?.id,s.navigating)
                if(mapContext!=contextKey) { mapContext=contextKey;mapCenter=null;zoomOffset=0.0 }
                if(s.navigating && !signaled) { manager.navigationStarted();signaled=true }
                if(!s.navigating && signaled) { manager.navigationEnded();signaled=false }
                val key=templateKey(s)
                if(key!=lastTemplateKey) {
                    lastTemplateKey=key
                    if(s.navigating && s.progress!=null) updateTrip(s)
                    invalidate()
                }
                updateMap()
            }
        }
    }
    override fun onSurfaceAvailable(container: SurfaceContainer) {
        if(container.surface?.isValid!=true || !validCarSurfaceSize(container.width,container.height,container.dpi)) {
            if(container.surface === surface) closeSurface(true) else container.surface?.release()
            return
        }
        closeSurface(surface !== container.surface)
        surface=container.surface; width=container.width; height=container.height
        surfaceDensity=container.dpi.toDouble()/DisplayMetrics.DENSITY_DEFAULT
        mapFirstVisibleAt=SystemClock.elapsedRealtime()
        visibleArea=Rect(0,0,width,height)
        stableArea=null
        val display=try {
            carContext.getSystemService(DisplayManager::class.java)
                .createVirtualDisplay("Waypoint Android Auto map",width,height,container.dpi,
                    container.surface,DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        } catch(_: RuntimeException) { closeSurface(true);return }
        if(display==null) { closeSurface(true);return }
        virtualDisplay=display
        try {
            val window=Presentation(carContext,display.display)
            val view=WaypointCarLiveMap(window.context,overlay)
            liveMap=view
            presentation=window.apply {
                setContentView(view)
                show()
            }
            view.start()
            updateMap()
            invalidate()
            creditRefreshJob=scope.launch { delay(5000);updateMap() }
        } catch(_: RuntimeException) {
            closeSurface(true)
        }
    }
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea=clipToSurface(visibleArea) ?: Rect(0,0,width,height)
        updateMap()
        invalidate()
    }
    override fun onStableAreaChanged(stableArea: Rect) {
        // Stable bounds account for persistent host chrome and split-pane layouts.
        // An empty rectangle means the host has not provided a usable value yet.
        this.stableArea=clipToSurface(stableArea)
        updateMap()
        invalidate()
    }
    private fun clipToSurface(area: Rect): Rect? {
        val clipped=Rect(area)
        return clipped.takeIf { it.intersect(0,0,width,height) && !it.isEmpty }
    }
    private fun mapViewport(): Rect {
        val visible=clipToSurface(visibleArea) ?: Rect(0,0,width,height)
        val stable=stableArea?.let(::clipToSurface) ?: return visible
        return Rect(visible).apply {
            if(!intersect(stable) || isEmpty) set(visible)
        }
    }
    override fun onSurfaceDestroyed(container: SurfaceContainer) {
        if(container.surface !== surface)return
        closeSurface(true)
    }
    private var creditRefreshJob: kotlinx.coroutines.Job? = null
    private fun closeSurface(release: Boolean) {
        creditRefreshJob?.cancel();creditRefreshJob=null
        liveMap?.destroy();liveMap=null
        presentation?.dismiss();presentation=null
        virtualDisplay?.release();virtualDisplay=null
        if(release)surface?.release()
        surface=null
    }
    override fun onScroll(distanceX: Float, distanceY: Float) {
        if(!distanceX.isFinite() || !distanceY.isFinite())return
        val camera=adjustedCamera(controller.state.value)
        val center=mapCenter ?: camera.target ?: return
        val scale=512.0 * 2.0.pow(camera.zoom)
        val longitude=center.longitude + distanceX.coerceIn(-width*4f,width*4f) * 360.0 / scale
        val mercator=ln(tan(PI/4 + Math.toRadians(center.latitude)/2)) +
            distanceY.coerceIn(-height*4f,height*4f) * 2*PI/scale
        mapCenter=LatLng(Math.toDegrees(atan(sinh(mercator))).coerceIn(-85.0,85.0),
            ((longitude+540.0)%360.0)-180.0)
        updateMap()
    }
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if(scaleFactor.isFinite() && scaleFactor>0f) {
            zoomOffset=(zoomOffset+log2(scaleFactor.toDouble())).coerceIn(-5.0,5.0)
            updateMap()
        }
    }
    private fun recenter() { mapCenter=null;zoomOffset=0.0;updateMap() }
    private fun zoomBy(amount: Double) { zoomOffset=(zoomOffset+amount).coerceIn(-5.0,5.0);updateMap() }
    private fun mapActionStrip() = ActionStrip.Builder()
        .addAction(Action.PAN)
        .addAction(mapIconAction(R.drawable.ic_car_recenter) { recenter() })
        .addAction(mapIconAction(R.drawable.ic_car_plus) { zoomBy(1.0) })
        .addAction(mapIconAction(R.drawable.ic_car_minus) { zoomBy(-1.0) })
        .build()
    private fun mapIconAction(icon: Int, click: () -> Unit) = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext,icon)).build())
        .setOnClickListener { if(screenActive())click() }.build()
    /**
     * Keep the compact split-screen action row recognizable without losing its label.
     * Android Auto selects the shortest CarText variant that fits its current action slot.
     */
    private fun settingsAction(onClick: () -> Unit) = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext,R.drawable.ic_car_settings)).build())
        .setTitle("Settings")
        .setOnClickListener { if(screenActive())onClick() }.build()
    private fun adjustedCamera(s: NavState,viewport: Rect=mapViewport()): CameraPosition {
        val camera=carCamera(s,width,height,CarMapViewport(viewport.left,viewport.top,
            viewport.right,viewport.bottom),surfaceDensity)
        return CameraPosition.Builder(camera)
            .target(mapCenter ?: camera.target)
            .zoom((camera.zoom+zoomOffset).coerceIn(2.0,19.0))
            .build()
    }
    private fun templateKey(s: NavState): List<Any?> {
        val permission=controller.hasLocationPermission()
        return when {
            s.pendingSharedDestination!=null -> listOf(permission,"import",s.pendingSharedDestinationId,s.pendingSharedDestination)
            s.resolvingSharedDestination -> listOf(permission,"resolving")
            s.navigating -> listOf(permission,"navigation",s.world,s.voice,s.imperial,s.routing,s.offRoute,s.routeError,s.gpsReady,
                if(s.gpsReady)null else s.gpsStatus,s.progress?.nextStep,
                s.progress?.toTurn?.toInt()?.div(25),s.progress?.remaining?.toInt()?.div(25),
                s.progress?.seconds?.toInt()?.div(5),s.driveAlternatives.size)
            s.selected!=null -> listOf(permission,"preview",s.world,s.imperial,s.selected.id,s.selected.name,
                controller.destinationActionVersion,System.identityHashCode(s.route),
                s.routing,s.arrived,s.routeError,s.gpsReady,if(s.gpsReady)null else s.gpsStatus,
                s.route?.seconds?.toInt(),s.route?.meters?.toInt(),s.routeOptions.size)
            else -> listOf(permission,"idle",s.world)
        }
    }
    private fun header(title: CharSequence): Header = Header.Builder()
        .setTitle(title).setStartHeaderAction(Action.APP_ICON).build()
    private fun carCue(s: NavState): String = when {
        !s.gpsReady -> s.gpsStatus
        s.offRoute && s.routeError!=null -> "Route unavailable · keep moving"
        s.offRoute -> "Off route · finding a new route"
        else -> routeGuidance(s.route,s.progress).instruction
    }
    private fun carStep(maneuver: Maneuver?,cue: String): Step = Step.Builder(cue)
        .setManeuver(CarManeuver.Builder(carManeuverType(maneuver))
            .apply { if(maneuver?.type in setOf("roundabout","rotary","roundabout turn") && (maneuver?.exit ?: 0)>0)
                setRoundaboutExitNumber(maneuver?.exit ?: 0) }.build())
        .apply { maneuver?.road?.takeIf { it.isNotBlank() }?.let(::setRoad) }.build()
    private fun carEstimate(meters: Double,seconds: Long): TravelEstimate =
        TravelEstimate.Builder(carDistance(meters,controller.state.value.imperial),DateTimeWithZone.create(
            System.currentTimeMillis()+seconds*1000,TimeZone.getDefault()))
            .setRemainingTimeSeconds(seconds).build()
    private fun updateTrip(s: NavState) {
        val progress=s.progress ?: return
        val maneuver=s.route?.steps?.getOrNull(progress.nextStep)
        val remaining=progress.seconds.coerceAtLeast(0.0).toLong()
        val toTurnSeconds=if(progress.remaining>0) (progress.seconds*progress.toTurn/progress.remaining)
            .coerceAtLeast(0.0).toLong() else 0L
        val trip=Trip.Builder()
            .addDestination(Destination.Builder().setName(s.selected?.name ?: "Destination").build(),
                carEstimate(progress.remaining,remaining))
            .addStep(carStep(maneuver,carCue(s)),carEstimate(progress.toTurn,toTurnSeconds))
            .apply { maneuver?.road?.takeIf { it.isNotBlank() }?.let(::setCurrentRoad) }.build()
        manager.updateTrip(trip)
    }
    override fun onGetTemplate(): Template {
        val s=controller.state.value
        if(!controller.hasLocationPermission()) return MessageTemplate.Builder(carContext.getString(R.string.car_location_explanation))
            .setHeader(header("Waypoint"))
            .addAction(Action.Builder().setTitle(carContext.getString(R.string.car_allow_location))
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    if(screenActive()) carContext.requestPermissions(listOf(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )) { _, _ ->
                        if(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                            controller.refreshCarLocation()
                            invalidate()
                        }
                    }
                }).build())
            .addAction(Action.Builder().setTitle(carContext.getString(R.string.car_check_permission))
                .setOnClickListener { if(screenActive()) { controller.refreshCarLocation();invalidate() } }.build())
            .build()
        if(s.pendingSharedDestination!=null) {
            val id=s.pendingSharedDestinationId
            return MessageTemplate.Builder("Open ${s.pendingSharedDestination} as your destination?")
                .setHeader(header("New destination"))
                .addAction(Action.Builder().setTitle("Open").setOnClickListener{if(screenActive())controller.acceptPendingNavigationImport(id)}.build())
                .addAction(Action.Builder().setTitle("Dismiss").setOnClickListener{if(screenActive())controller.dismissPendingNavigationImport(id)}.build())
                .build()
        }
        if(s.resolvingSharedDestination) return MessageTemplate.Builder("Finding the place name…")
            .setHeader(header("Destination")).setLoading(true).build()
        if(s.navigating) {
            val tripId=controller.navigationSessionId
            val maneuver=s.route?.steps?.getOrNull(s.progress?.nextStep ?: 0)
            val step=carStep(maneuver,carCue(s))
            val info=if(s.routing && s.progress==null)RoutingInfo.Builder().setLoading(true).build() else
                RoutingInfo.Builder().setCurrentStep(step,carDistance(s.progress?.toTurn ?: 0.0,s.imperial))
                    .apply {
                        s.route?.steps?.getOrNull((s.progress?.nextStep ?: 0)+1)?.let { next ->
                            setNextStep(carStep(next,next.instruction))
                        }
                    }.build()
            val seconds=(s.progress?.seconds ?: 0.0).toLong()
            return NavigationTemplate.Builder().setNavigationInfo(info)
                .setDestinationTravelEstimate(carEstimate(s.progress?.remaining ?: 0.0,seconds))
                .setMapActionStrip(mapActionStrip())
                .setPanModeListener { }
                .setActionStrip(ActionStrip.Builder()
                    .addAction(Action.Builder().setTitle("End").setOnClickListener {
                        if(screenActive() && controller.acceptsNavigationSession(tripId))controller.stopNavigation()
                    }.build())
                    .addAction(Action.Builder().setTitle(if(s.voice)"Mute" else "Voice")
                        .setOnClickListener { if(screenActive() && controller.acceptsNavigationSession(tripId))controller.toggleVoice() }.build())
                    .addAction(settingsAction {
                        if(controller.acceptsNavigationSession(tripId))screenManager.push(WaypointCarSettingsScreen(carContext))
                    })
                    .apply {
                        // Fresh ways to the same place, offered while guiding as on the phone map.
                        if(s.driveAlternatives.isNotEmpty() && !s.routing && !s.offRoute)
                            addAction(Action.Builder()
                                .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext,R.drawable.ic_car_routes)).build())
                                .setOnClickListener {
                                    val current=controller.state.value
                                    if(screenActive() && controller.acceptsNavigationSession(tripId) && current.navigating &&
                                        !current.routing && current.driveAlternatives.isNotEmpty())
                                        screenManager.push(WaypointCarDriveAlternativesScreen(carContext))
                                }.build())
                    }.build()).build()
        }
        if(s.selected!=null) {
            val selectedId=s.selected.id
            val destinationVersion=controller.destinationActionVersion
            val previewRoute=s.route
            fun previewCurrent(): Boolean {
                val current=controller.state.value
                return screenActive() && controller.destinationActionVersion==destinationVersion &&
                    !current.navigating && current.selected?.id==selectedId && current.route===previewRoute
            }
            val text=when { s.arrived->"You have arrived at ${s.selected.name}";s.routing->"Finding a ${s.mode.label.lowercase()} route…";s.routeError!=null->s.routeError;!s.gpsReady->s.gpsStatus;s.route!=null->"${kotlin.math.ceil(s.route.seconds/60).toInt()} min · ${formatDistance(s.route.meters,s.imperial)}";else->"Waiting for your route" }
            val actions=ActionStrip.Builder()
            if(s.routeOptions.size>1 && s.route!=null && !s.routing && !s.arrived) {
                val destinationId=s.selected.id
                actions.addAction(Action.Builder().setTitle("Routes").setOnClickListener {
                    val current=controller.state.value
                    if(screenActive() && !current.navigating && !current.routing &&
                        current.selected?.id==destinationId && current.routeOptions.size>1)
                        screenManager.push(WaypointCarRouteOptionsScreen(carContext))
                }.build())
            }
            if(s.route!=null && s.gpsReady && !s.routing && !s.arrived)
                actions.addAction(Action.Builder().setTitle("Start").setOnClickListener{if(previewCurrent() && !controller.state.value.arrived)controller.startNavigation()}.build())
            if(s.routeError!=null)
                actions.addAction(Action.Builder().setTitle("Retry").setOnClickListener{
                    if(previewCurrent() && !controller.state.value.routing && controller.state.value.routeError!=null)
                        controller.requestRoute()
                }.build())
            actions.addAction(Action.Builder().setTitle("Cancel").setOnClickListener{if(previewCurrent())controller.clearDestination()}.build())
            return NavigationTemplate.Builder()
                .setNavigationInfo(MessageInfo.Builder(s.selected.name).setText(text).build())
                .setMapActionStrip(mapActionStrip())
                .setPanModeListener { }
                .setActionStrip(actions.build()).build()
        }
        return NavigationTemplate.Builder()
            .setMapActionStrip(mapActionStrip())
            .setPanModeListener { }
            .setActionStrip(ActionStrip.Builder()
                .addAction(Action.Builder().setTitle("Search")
                    .setFlags(Action.FLAG_IS_PERSISTENT)
                    .setOnClickListener{if(screenActive() && controller.state.value.selected==null)screenManager.push(WaypointCarSearchScreen(carContext))}.build())
                .addAction(Action.Builder().setTitle("Places")
                    .setOnClickListener{if(screenActive() && controller.state.value.selected==null)screenManager.push(WaypointCarDestinationsScreen(carContext))}.build())
                .addAction(settingsAction { screenManager.push(WaypointCarSettingsScreen(carContext)) })
                .build()).build()
    }
    private fun updateMap() {
        if(surface?.isValid!=true)return
        val s=controller.state.value
        val viewport=mapViewport()
        liveMap?.update(s,adjustedCamera(s,viewport),viewport,
            SystemClock.elapsedRealtime()-mapFirstVisibleAt<5000,mapCenter==null)
    }
    private fun carManeuverType(m: Maneuver?): Int = when {
        m?.type=="arrive" -> CarManeuver.TYPE_DESTINATION
        m?.type in setOf("roundabout","rotary","roundabout turn") ->
            if("left" in m?.modifier.orEmpty())CarManeuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW
            else CarManeuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW
        m?.type=="fork" && "left" in m.modifier -> CarManeuver.TYPE_FORK_LEFT
        m?.type=="fork" && "right" in m.modifier -> CarManeuver.TYPE_FORK_RIGHT
        m?.type=="merge" && "left" in m.modifier -> CarManeuver.TYPE_MERGE_LEFT
        m?.type=="merge" && "right" in m.modifier -> CarManeuver.TYPE_MERGE_RIGHT
        m?.type=="merge" -> CarManeuver.TYPE_MERGE_SIDE_UNSPECIFIED
        m?.type=="on ramp" && "left" in m.modifier -> CarManeuver.TYPE_ON_RAMP_NORMAL_LEFT
        m?.type=="on ramp" && "right" in m.modifier -> CarManeuver.TYPE_ON_RAMP_NORMAL_RIGHT
        m?.type=="off ramp" && "left" in m.modifier -> CarManeuver.TYPE_OFF_RAMP_NORMAL_LEFT
        m?.type=="off ramp" && "right" in m.modifier -> CarManeuver.TYPE_OFF_RAMP_NORMAL_RIGHT
        m?.modifier=="uturn" -> if(m.uTurnRight)CarManeuver.TYPE_U_TURN_RIGHT else CarManeuver.TYPE_U_TURN_LEFT
        m?.modifier=="slight left" -> CarManeuver.TYPE_TURN_SLIGHT_LEFT
        m?.modifier=="slight right" -> CarManeuver.TYPE_TURN_SLIGHT_RIGHT
        m?.modifier=="sharp left" -> CarManeuver.TYPE_TURN_SHARP_LEFT
        m?.modifier=="sharp right" -> CarManeuver.TYPE_TURN_SHARP_RIGHT
        m?.modifier=="left" -> CarManeuver.TYPE_TURN_NORMAL_LEFT
        m?.modifier=="right" -> CarManeuver.TYPE_TURN_NORMAL_RIGHT
        else -> CarManeuver.TYPE_STRAIGHT
    }
}

internal fun carDistance(meters: Double, imperial: Boolean = false): Distance {
    val safe=meters.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    if(imperial) {
        val miles = safe / 1609.344
        return when {
            miles < 0.1 -> Distance.create(kotlin.math.round(safe * 3.280839895 / 10) * 10,Distance.UNIT_FEET)
            miles < 10 -> Distance.create(miles,Distance.UNIT_MILES_P1)
            else -> Distance.create(miles,Distance.UNIT_MILES)
        }
    }
    return when {
        safe < 1000 -> Distance.create((safe/10).toInt()*10.0,Distance.UNIT_METERS)
        safe < 10000 -> Distance.create(safe/1000.0,Distance.UNIT_KILOMETERS_P1)
        else -> Distance.create(safe/1000.0,Distance.UNIT_KILOMETERS)
    }
}

internal data class CarMapViewport(val left: Int,val top: Int,val right: Int,val bottom: Int)

/** Fixed Android Auto guidance pitch, matched to the forward perspective used by Google Maps. */
internal const val CAR_NAVIGATION_TILT=45.0

/** Frame the route and driver within the map area left by the car host's panels. */
internal fun carCamera(s: NavState, width: Int, height: Int,
    viewport: CarMapViewport=CarMapViewport(0,0,width,height),density: Double=1.0): CameraPosition {
    val world=s.world
    val builder=CameraPosition.Builder()
    val area=viewport.takeIf { it.left>=0 && it.top>=0 && it.right<=width && it.bottom<=height &&
        it.right-it.left>=160 && it.bottom-it.top>=110 } ?: CarMapViewport(0,0,width,height)
    val mapWidth=area.right-area.left
    val mapHeight=area.bottom-area.top
    builder.padding(area.left.toDouble(),area.top.toDouble(),
        (width-area.right).toDouble(),(height-area.bottom).toDouble())
    val route=s.route
    if(route!=null && !s.navigating && route.points.isNotEmpty()) {
        // Frame every route on offer, as the phone overview does, so no alternative is cut off.
        val points=(if(s.routeOptions.size>1)s.routeOptions.flatMap { it.points } else route.points)+route.points+listOfNotNull(s.fix,s.selected?.point)
        val south=points.minOf { it.lat }.coerceIn(-85.0,85.0)
        val north=points.maxOf { it.lat }.coerceIn(-85.0,85.0)
        val originLongitude=route.points.first().lon
        val longitudes=points.map { originLongitude+shortestLongitudeDelta(originLongitude,it.lon) }
        val west=longitudes.min()
        val east=longitudes.max()
        fun mercator(lat: Double)=ln(tan(PI/4+Math.toRadians(lat)/2))
        val xSpan=((east-west)/360.0).coerceAtLeast(0.00001)
        val ySpan=((mercator(north)-mercator(south))/(2*PI)).coerceAtLeast(0.00001)
        val availableWidth=(mapWidth-100).coerceAtLeast(160).toDouble()
        val availableHeight=(mapHeight-145).coerceAtLeast(110).toDouble()
        // MapLibre's camera zoom is based on a 512 px world tile. Using 256 here
        // doubled the preview scale and clipped both route endpoints on the car.
        val zoom=min(log2(availableWidth/(512*xSpan)),log2(availableHeight/(512*ySpan))).coerceIn(3.0,17.0)
        return builder.target(LatLng((south+north)/2,wrappedLongitude((west+east)/2))).zoom(zoom).tilt(0.0).build()
    }
    // Keep the vehicle at a stable screen anchor even when the road bends.
    // Moving the camera target up the route as well as padding it made the
    // vehicle drift toward the edge at close zoom levels.
    val center=s.fix ?: (if(s.navigating)carLookAhead(s) else null) ?: PreviewCenter
    if(s.navigating)builder.padding(area.left+mapWidth*.18,area.top+mapHeight*.50,
        (width-area.right).toDouble(),(height-area.bottom).toDouble())
    val travelBearing=if(s.navigating)carTravelBearing(s).toFloat() else 0f
    // In-car guidance keeps its pitched road view at a stop too. A saved flat
    // browse preference should not turn the live guidance camera into a plan view.
    val wantedTilt=if(s.navigating)CAR_NAVIGATION_TILT
        else mapCameraTilt(world,s.tilted,false)
    val pose=if(s.navigating)navigationCameraPose(
        state=s,
        currentBearing=travelBearing,
        mapHeightPx=mapHeight,
        density=density,
        // The turn card sits over the upper part of the projected map.
        chromeBottomPx=mapHeight*.30,
        wantedTilt=wantedTilt,
        headingOverride=travelBearing,
    ) else null
    // Android Auto can keep the map surface's original size while its stable/visible
    // rectangle narrows for a second app. Pull back with that pane so the road and
    // vehicle remain framed instead of cropping the route at the sides.
    val paneWidthRatio=(mapWidth.toDouble()/width.coerceAtLeast(1)).coerceIn(.35,1.0)
    val navigationZoom=pose?.zoom?.plus(if(s.navigating)log2(paneWidthRatio).coerceAtLeast(-1.5) else 0.0)
    // Keep the Android Auto guidance pitch fixed while zoom changes for speed,
    // turns, or a split-screen pane. Tying pitch to zoom made a stopped driver
    // and distant maneuver fall back toward a flat plan view.
    val navigationTilt=if(s.navigating)wantedTilt else null
    return builder.target(LatLng(center.lat,center.lon))
        .zoom(navigationZoom ?: world.mapArt().zoom)
        .tilt(navigationTilt ?: pose?.tilt ?: mapCameraTilt(world,s.tilted,false))
        .bearing(pose?.bearing ?: if(s.navigating)travelBearing.toDouble() else 0.0)
        .build()
}

/** Use the road ahead when the phone has no reliable driving course. */
internal fun carTravelBearing(state: NavState): Double {
    // HeadingArbiter owns the speed entry/exit thresholds and hysteresis. Rechecking
    // its entry threshold here made Android Auto fall back to the route tangent
    // while the arbiter was deliberately retaining a valid course during slowdown.
    if(state.headingReliable && state.headingSource==HeadingSource.COURSE)
        return state.bearing.toDouble()
    val ahead=state.route?.remainingPoints(state.progress)
    val origin=ahead?.firstOrNull()
    // Follow the current road tangent. A long look-ahead chord points the vehicle
    // into the next road before it has reached a tight bend or U-turn.
    val next=origin?.let { start -> ahead.drop(1).firstOrNull { distance(start,it)>=0.5 } }
    if(origin!=null && next!=null) {
        val lat1=Math.toRadians(origin.lat);val lat2=Math.toRadians(next.lat)
        val dLon=Math.toRadians(next.lon-origin.lon)
        val y=sin(dLon)*cos(lat2)
        val x=cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(dLon)
        return (Math.toDegrees(atan2(y,x))+360.0)%360.0
    }
    return if(state.headingReliable)state.bearing.toDouble() else 0.0
}

/** A point up the remaining route leaves more road visible in front of the driver. */
internal fun carLookAhead(state: NavState): Coordinate? {
    val points=state.route?.remainingPoints(state.progress) ?: return null
    if(points.size<2)return null
    var remaining=when {
        (state.progress?.toTurn ?: Double.POSITIVE_INFINITY)<200.0 -> 35.0
        state.speed>=25f -> 90.0
        state.speed>=12f -> 75.0
        else -> 70.0
    }
    for(index in 0 until points.lastIndex) {
        val from=points[index];val to=points[index+1]
        val segment=distance(from,to)
        if(segment>=remaining && segment>0.0) {
            val fraction=remaining/segment
            return Coordinate(from.lat+(to.lat-from.lat)*fraction,
                wrappedLongitude(from.lon+shortestLongitudeDelta(from.lon,to.lon)*fraction))
        }
        remaining-=segment
    }
    return points.last()
}
