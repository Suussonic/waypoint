package com.waypoint.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.animation.LinearInterpolator
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
    private var positionAnimator: ValueAnimator?=null
    private var bearingAnimator: ValueAnimator?=null
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
        syncBearing()
        syncCamera()
        syncPosition()
    }

    private fun requestStyle(world: World) {
        val m=map ?: return
        if(requestedWorld==world)return
        requestedWorld=world
        styleReady=false
        val epoch=++styleEpoch
        positionAnimator?.cancel();bearingAnimator?.cancel()
        scope.launch {
            val (json,images)=withContext(Dispatchers.Default) {
                val source=context.assets.open("styles/${world.file}.json").bufferedReader().use { it.readText() }
                val bitmaps=hashMapOf(
                    "nav-arrow" to mapPlayer(world),
                    "position-uncertain" to mapUnknownPosition(world),
                    "destination-marker" to mapDestination(world),
                    "pixel-grass" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_grass),
                    "pixel-stone" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_stone),
                    "pixel-water" to BitmapFactory.decodeResource(context.resources,R.drawable.pixel_water),
                    "paper-fibre" to BitmapFactory.decodeResource(context.resources,R.drawable.paper_fibre)
                )
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
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)))
                loadedWorld=world
                styleReady=true
                lastRoute=null;lastRouteProgress=-1.0;lastRouteStyleKey=null
                lastDestination=null;lastCameraKey=null
                syncRoute();syncDestination();syncBearing();syncCamera();syncPosition()
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

    private fun syncBearing() {
        val layer=map?.style?.getLayerAs<SymbolLayer>("car-position-marker") ?: return
        layer.setProperties(iconImage(if(state.headingReliable)"nav-arrow" else "position-uncertain"))
        val target=if(state.headingReliable)state.bearing else 0f
        if(abs(headingDelta(renderedBearing,target))<.5f)return
        bearingAnimator?.cancel()
        val from=renderedBearing
        val delta=headingDelta(from,target)
        val epoch=styleEpoch
        bearingAnimator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=180
            interpolator=LinearInterpolator()
            addUpdateListener {
                if(epoch==styleEpoch) {
                    renderedBearing=(from+delta*(it.animatedValue as Float)+360f)%360f
                    layer.setProperties(iconRotate(renderedBearing))
                }
            }
            start()
        }
    }

    private fun syncCamera() {
        val m=map ?: return
        // GPS fixes drive the camera in the position animator. Reissuing an ease for
        // every fix fights that animator and makes the car map visibly stutter.
        val manualTarget=if(following)null else camera.target
        val key=listOf(state.world,System.identityHashCode(state.route),state.selected?.id,
            state.navigating,following,manualTarget?.latitude,manualTarget?.longitude,
            if(state.navigating && following)null else camera.zoom)
        if(key==lastCameraKey)return
        val previous=lastCameraKey
        lastCameraKey=key
        if(previous==null) m.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
        else m.easeCamera(CameraUpdateFactory.newCameraPosition(camera),450)
    }

    private fun syncPosition() {
        val target=state.fix
        val source=map?.style?.getSourceAs<GeoJsonSource>("car-position") ?: return
        if(target==null) {
            positionAnimator?.cancel();renderedPosition=null;source.setFeature(null)
            return
        }
        val from=renderedPosition
        if(from==target)return
        positionAnimator?.cancel()
        val epoch=styleEpoch
        val m=map
        val startBearing=m?.cameraPosition?.bearing?.toFloat() ?: 0f
        val bearingChange=headingDelta(startBearing,camera.bearing.toFloat())
        val startZoom=m?.cameraPosition?.zoom ?: camera.zoom
        val startTilt=m?.cameraPosition?.tilt ?: camera.tilt
        val targetZoom=camera.zoom
        val targetTilt=camera.tilt
        val startCenter=m?.cameraPosition?.target ?: LatLng(from?.lat ?: target.lat,from?.lon ?: target.lon)
        val targetCenter=camera.target ?: LatLng(target.lat,target.lon)
        fun render(point: Coordinate,fraction: Float) {
            if(epoch!=styleEpoch)return
            renderedPosition=point
            source.setFeature(Feature.fromGeometry(Point.fromLngLat(point.lon,point.lat)))
            if(following && (state.selected==null || state.navigating)) {
                val liveMap=map ?: return
                liveMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(liveMap.cameraPosition)
                    .target(LatLng(startCenter.latitude+(targetCenter.latitude-startCenter.latitude)*fraction,
                        wrappedLongitude(startCenter.longitude+
                            shortestLongitudeDelta(startCenter.longitude,targetCenter.longitude)*fraction)))
                    .bearing((startBearing+bearingChange*fraction).toDouble())
                    .zoom(startZoom+(targetZoom-startZoom)*fraction)
                    .tilt(startTilt+(targetTilt-startTilt)*fraction).build()))
            }
        }
        if(from!=null && distance(from,target)<150.0) {
            positionAnimator=ValueAnimator.ofFloat(0f,1f).apply {
                duration=500
                interpolator=LinearInterpolator()
                addUpdateListener { frame ->
                    val fraction=frame.animatedValue as Float
                    render(Coordinate(from.lat+(target.lat-from.lat)*fraction,
                        wrappedLongitude(from.lon+shortestLongitudeDelta(from.lon,target.lon)*fraction)),fraction)
                }
                start()
            }
        } else render(target,1f)
    }

    fun destroy() {
        destroyed=true
        styleEpoch++
        positionAnimator?.cancel();bearingAnimator?.cancel()
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
