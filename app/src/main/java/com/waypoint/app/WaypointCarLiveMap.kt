package com.waypoint.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.os.SystemClock
import android.view.Choreographer
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.renderer.MapRenderer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.abs

/** One GPU-backed MapLibre view owns the car map for the life of the projected Surface. */
internal class WaypointCarLiveMap(context: Context, private val overlay: WaypointCarOverlay): FrameLayout(context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mapView=MapView(context).apply {
        onCreate(null)
        setMaximumFps(60)
        setRenderingRefreshMode(MapRenderer.RenderingRefreshMode.WHEN_DIRTY)
    }
    private val hud=object: View(context) {
        override fun onDraw(canvas: Canvas) { this@WaypointCarLiveMap.overlay.drawHud(canvas,state,visibleArea,showCredit) }
    }
    private var map: MapLibreMap?=null
    private var loadedWorld: World?=null
    private var requestedWorld: World?=null
    private var styleEpoch=0
    private var styleReady=false
    private var state=NavState()
    private var camera=CameraPosition.Builder().target(LatLng(PreviewCenter.lat,PreviewCenter.lon)).zoom(15.2).build()
    private var visibleArea=Rect()
    private var showCredit=false
    private var following=true
    private var lastCameraKey: List<Any?>?=null
    private var lastRoute: RoadRoute?=null
    private var lastRouteProgress=-1.0
    private var lastRouteStyleKey: List<Any?>?=null
    private var lastDestination: Coordinate?=null
    private var renderedPosition: Coordinate?=null
    private var renderedBearing=0f
    private val motion=MapMotion()
    private var cameraFrom=camera
    private var cameraStart=0L
    private var cameraDuration=0L
    private var framePosted=false
    private val frames=Choreographer.getInstance()
    private val frameCallback=object:Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos:Long) {
            framePosted=false
            if(destroyed || !started || !styleReady)return
            val now=SystemClock.elapsedRealtime()
            renderMotion(now)
            if(motion.remainingMillis(now)>0 || now<cameraStart+cameraDuration)requestFrame()
        }
    }
    private var started=false
    private var destroyed=false

    init {
        MapLibre.getInstance(context)
        addView(mapView,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
        addView(hud,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
        mapView.getMapAsync { m ->
            if(destroyed)return@getMapAsync
            map=m
            m.uiSettings.apply {
                isCompassEnabled=false
                isLogoEnabled=false
                isAttributionEnabled=false
                setAllGesturesEnabled(false)
            }
            m.cameraPosition=camera
            requestStyle(state.world)
        }
    }

    fun start() {
        if(started)return
        started=true
        mapView.onStart()
        mapView.onResume()
        if(styleReady)requestFrame()
    }

    fun update(newState: NavState,newCamera: CameraPosition,visible: Rect,credit: Boolean,follow: Boolean) {
        state=newState
        camera=newCamera
        visibleArea=Rect(visible)
        showCredit=credit
        following=follow
        hud.postInvalidateOnAnimation()
        if(map==null)return
        if(newState.world!=loadedWorld) {
            requestStyle(newState.world)
            return
        }
        if(!styleReady)return
        syncRoute()
        syncDestination()
        syncMotion()
    }

    private fun requestStyle(world: World) {
        val m=map ?: return
        if(requestedWorld==world)return
        requestedWorld=world
        styleReady=false
        val epoch=++styleEpoch
        frames.removeFrameCallback(frameCallback);framePosted=false
        scope.launch {
            val (json,images)=withContext(Dispatchers.Default) {
                val source=context.assets.open("styles/${world.file}.json").bufferedReader().use { it.readText() }
                val bitmaps=hashMapOf(
                    "nav-arrow" to mapPlayer(world),
                    "vehicle-car" to mapVehicle(world,TravelMode.CAR),
                    "vehicle-bike" to mapVehicle(world,TravelMode.MOTORCYCLE),
                    "vehicle-bicycle" to mapVehicle(world,TravelMode.BICYCLE),
                    "position-uncertain" to mapUnknownPosition(world),
                    "destination-marker" to mapDestination(world),
                    "pixel-grass" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_grass),
                    "pixel-stone" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_stone),
                    "pixel-water" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_water),
                    "paper-fibre" to BitmapFactory.decodeResource(context.resources,R.drawable.paper_fibre)
                )
                // Generated markers have logical pixel dimensions. A bitmap's
                // default density belongs to the phone, not the projected display.
                listOf("nav-arrow","position-uncertain","destination-marker").forEach {
                    bitmaps.getValue(it).density=DisplayMetrics.DENSITY_DEFAULT
                }
                source to bitmaps
            }
            if(epoch!=styleEpoch)return@launch
            m.setStyle(Style.Builder().fromJson(json)) { style ->
                if(epoch!=styleEpoch)return@setStyle
                style.addImages(images)
                val art=world.mapArt()
                style.addSource(GeoJsonSource("car-route",Feature.fromGeometry(LineString.fromLngLats(emptyList()))))
                style.addLayer(LineLayer("car-route-casing","car-route").withProperties(
                    lineColor("#161716"),lineWidth(art.routeWidth+3f),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
                style.addLayer(LineLayer("car-route-line","car-route").withProperties(
                    lineColor(world.routeColor),lineWidth(art.routeWidth),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
                style.addSource(GeoJsonSource("car-destination",Feature.fromGeometry(Point.fromLngLat(0.0,0.0))))
                style.addLayer(SymbolLayer("car-destination-marker","car-destination").withProperties(
                    iconImage("destination-marker"),iconSize(.65f),iconAllowOverlap(true),iconIgnorePlacement(true)))
                style.addSource(GeoJsonSource("car-position",Feature.fromGeometry(Point.fromLngLat(0.0,0.0))))
                style.addLayer(SymbolLayer("car-position-marker","car-position").withProperties(
                    iconImage("nav-arrow"),iconSize(.55f),iconAllowOverlap(true),iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT)))
                loadedWorld=world
                styleReady=true
                lastRoute=null;lastRouteProgress=-1.0;lastRouteStyleKey=null
                lastDestination=null;lastCameraKey=null
                // A new style owns fresh sources even when the driver has not moved.
                renderedPosition=null
                syncRoute();syncDestination();syncMotion()
            }
        }
    }

    private fun syncRoute() {
        val style=map?.style ?: return
        val route=state.route
        val traveled=if(state.navigating)state.progress?.traveled ?: 0.0 else 0.0
        val styleKey=listOf(state.navigating,state.mode,state.offRoute,state.routing)
        if(route===lastRoute && abs(traveled-lastRouteProgress)<2.0 && styleKey==lastRouteStyleKey)return
        lastRoute=route;lastRouteProgress=traveled
        lastRouteStyleKey=styleKey
        val points=route?.let { if(state.navigating)it.remainingPoints(state.progress) else it.points }.orEmpty()
        style.getSourceAs<GeoJsonSource>("car-route")?.setFeature(if(points.size<2)null else
            Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lon,it.lat) })))
        val dotted=(state.world==World.NIGHT_CITY && state.mode==TravelMode.WALK) ||
            (state.world==World.SAINTS && state.navigating)
        style.getLayerAs<LineLayer>("car-route-line")?.setProperties(
            lineColor(if(state.world==World.SAINTS && state.navigating)"#86D955" else state.world.routeColor),
            lineOpacity(if(state.offRoute || state.routing && state.navigating).45f else 1f),
            lineDasharray(if(dotted)arrayOf(.3f,1.8f) else arrayOf(1f,0f)))
        style.getLayerAs<LineLayer>("car-route-casing")?.setProperties(lineOpacity(if(dotted)0f else 1f))
    }

    private fun syncDestination() {
        val selected=state.selected?.point
        if(selected==lastDestination)return
        lastDestination=selected
        map?.style?.getSourceAs<GeoJsonSource>("car-destination")?.setFeature(selected?.let {
            Feature.fromGeometry(Point.fromLngLat(it.lon,it.lat))
        })
    }

    private fun syncMotion() {
        val m=map ?: return
        val headingKnown=state.headingReliable || state.navigating && state.route!=null
        m.style?.getLayerAs<SymbolLayer>("car-position-marker")?.setProperties(
            iconImage(playerMarkerImage(state.mode,headingKnown)),iconSize(if(state.mode==TravelMode.WALK).55f else .72f))
        val now=SystemClock.elapsedRealtime()
        val bearing=if(state.navigating)carTravelBearing(state).toFloat() else if(state.headingReliable)state.bearing else 0f
        motion.update(state.fix,bearing,now,state.navigating,ValueAnimator.areAnimatorsEnabled())
        val manualTarget=if(following)null else camera.target
        val key=listOf(state.world,System.identityHashCode(state.route),state.selected?.id,
            state.navigating,following,manualTarget?.latitude,manualTarget?.longitude,
            camera.zoom,camera.tilt,camera.padding?.toList())
        if(key!=lastCameraKey) {
            cameraFrom=m.cameraPosition
            cameraStart=now
            cameraDuration=if(lastCameraKey!=null && ValueAnimator.areAnimatorsEnabled())450L else 0L
            lastCameraKey=key
            // One frame owner moves the camera, including zoom and follow recovery.
            // A native ease must never compete with the location interpolation.
            m.cancelTransitions()
        }
        requestFrame()
    }

    private fun requestFrame() {
        if(framePosted || destroyed || !started)return
        framePosted=true
        frames.postFrameCallback(frameCallback)
    }

    private fun renderMotion(now:Long) {
        val m=map ?: return
        val style=m.style ?: return
        val pose=motion.sample(now)
        renderedPosition=pose.point;renderedBearing=pose.bearing
        style.getSourceAs<GeoJsonSource>("car-position")?.setFeature(pose.point?.let {
            Feature.fromGeometry(Point.fromLngLat(it.lon,it.lat))
        })
        style.getLayerAs<SymbolLayer>("car-position-marker")?.setProperties(iconRotate(pose.bearing))
        val progress=if(cameraDuration==0L)1.0 else ((now-cameraStart).toDouble()/cameraDuration).coerceIn(0.0,1.0)
        // Decelerate deliberate camera changes. GPS movement itself stays linear.
        val eased=1.0-(1.0-progress)*(1.0-progress)*(1.0-progress)
        val followPosition=following && (state.selected==null || state.navigating) && pose.point!=null
        val target=if(followPosition)LatLng(pose.point!!.lat,pose.point.lon) else camera.target ?: return
        val start=cameraFrom.target ?: target
        val paddingFrom=cameraFrom.padding ?: doubleArrayOf(0.0,0.0,0.0,0.0)
        val paddingTo=camera.padding ?: doubleArrayOf(0.0,0.0,0.0,0.0)
        val padding=DoubleArray(4) { paddingFrom[it]+(paddingTo[it]-paddingFrom[it])*eased }
        val bearing=if(followPosition && state.navigating)pose.bearing.toDouble()
            else cameraFrom.bearing+headingDelta(cameraFrom.bearing.toFloat(),camera.bearing.toFloat())*eased
        m.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(start.latitude+(target.latitude-start.latitude)*eased,
                wrappedLongitude(start.longitude+shortestLongitudeDelta(start.longitude,target.longitude)*eased)))
            .bearing(bearing)
            .zoom(cameraFrom.zoom+(camera.zoom-cameraFrom.zoom)*eased)
            .tilt(cameraFrom.tilt+(camera.tilt-cameraFrom.tilt)*eased)
            .padding(padding[0],padding[1],padding[2],padding[3]).build()))
    }

    fun destroy() {
        destroyed=true
        styleEpoch++
        frames.removeFrameCallback(frameCallback);framePosted=false
        scope.cancel()
        if(started) { mapView.onPause();mapView.onStop() }
        mapView.onDestroy()
        map=null
    }
}

private fun GeoJsonSource.setFeature(feature: Feature?) {
    if(feature==null) setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>()))
    else setGeoJson(feature)
}
