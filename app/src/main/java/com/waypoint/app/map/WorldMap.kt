package com.waypoint.app

import android.animation.ValueAnimator
import android.os.SystemClock
import kotlinx.coroutines.delay
import android.view.animation.LinearInterpolator
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.waypoint.app.places.NearbyPlace
import com.waypoint.app.offline.OfflineMapArea
import com.waypoint.app.offline.OfflineMapStyleFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.*
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.layers.*
import org.maplibre.android.style.layers.PropertyFactory.*
import java.util.Locale
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.*
import org.maplibre.android.style.expressions.Expression.*

/** Physical pixels, measured from the native map's edges to the uncovered viewport. */
data class MapViewportInsets(val left:Int,val top:Int,val right:Int,val bottom:Int)

@Composable
internal fun WorldMap(state: NavState, onPin: (Place)->Unit, onMapError: (String?)->Unit, modifier: Modifier=Modifier,active:Boolean=true,routeInsets:MapViewportInsets?=null,onBearing:(Float)->Unit={},cameraZoomOverride:Double?=null,cameraTiltOverride:Double?=null,styleFileOverride:String?=null,cameraCenterOverride:Coordinate?=null,onAwayFromUser:(Boolean)->Unit={},onSelectRoute:(Int)->Unit={},showScaleBar:Boolean=false,scaleBarInsets:MapViewportInsets?=null,nearbyPlaces:List<NearbyPlace> = emptyList(),onVisibleMapArea:(OfflineMapArea?)->Unit={},onBrowseMapCamera:(Coordinate,Double)->Unit={_,_->},onNearbyPlaceClick:(NearbyPlace)->Unit={place->onPin(place.asWaypointPlace())}) {
    val context=LocalContext.current
    val density=LocalDensity.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableIntStateOf(0) }
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    val rawDisplayPosition=if(state.navigating)navigationDisplayPosition(state.fix,state.progress,state.offRoute,state.accuracy) else state.fix
    val routeBearing=if(state.navigating && !state.offRoute)state.route?.let { route ->
        state.progress?.takeIf { it.offRoute < 30.0 }?.let { progress -> routeBearingAt(route.points,route.meters,progress.traveled) }
    } else null
    val markerDirectionReliable=state.headingReliable || routeBearing!=null
    val roadSnapBearing=when {
        state.navigating && routeBearing!=null -> routeBearing
        state.headingSource==HeadingSource.COURSE && state.headingReliable -> state.bearing
        else -> null
    }
    val stationaryPositionFilter=remember { StationaryPositionFilter() }
    var filteredDisplayPosition by remember { mutableStateOf(rawDisplayPosition) }
    LaunchedEffect(map,styleReady,active,rawDisplayPosition,state.navigating,state.mode,state.speed,state.accuracy,roadSnapBearing) {
        val visibleRoadPosition=if(active && styleReady>0 && rawDisplayPosition!=null)
            map?.snapToVisibleRoad(rawDisplayPosition,state.mode,roadSnapBearing,state.accuracy) ?: rawDisplayPosition
        else rawDisplayPosition
        filteredDisplayPosition=stationaryPositionFilter.update(visibleRoadPosition,state.mode,state.speed,state.accuracy)
    }
    val displayPosition=filteredDisplayPosition ?: rawDisplayPosition
    val selectRoute by rememberUpdatedState(onSelectRoute)
    val owner=LocalLifecycleOwner.current
    val lifecycleState by owner.lifecycle.currentStateAsState()
    val nightTheme=LocalNightMapTheme.current
    val mapNight=effectiveNightMapTheme(state.world,nightTheme) && styleFileOverride==null
    val availableStyles=remember(context) { mapThemeStyleFiles(context) }
    val requestedStyleFile=styleFileOverride ?: resolveMapThemeStyleFile(state.world,mapNight,state.tilted,availableStyles)
    val currentState by rememberUpdatedState(state)
    val currentActive by rememberUpdatedState(active)
    val currentPin by rememberUpdatedState(onPin)
    val currentNearbyPlaceClick by rememberUpdatedState(onNearbyPlaceClick)
    val currentError by rememberUpdatedState(onMapError)
    val currentBearing by rememberUpdatedState(onBearing)
    val currentVisibleMapArea by rememberUpdatedState(onVisibleMapArea)
    val currentBrowseMapCamera by rememberUpdatedState(onBrowseMapCamera)
    val mapView=remember { MapLibre.getInstance(context); MapView(context).apply { onCreate(null) } }
    var loadedStyleFile by remember { mutableStateOf<String?>(null) }
    val followPolicy=remember{MapFollowPolicy()}
    var followTick by remember { mutableIntStateOf(0) }
    val cameraMemory=remember { MapCameraMemory() }
    val cameraGestureStart=remember { arrayOfNulls<CameraPosition>(1) }
    val cameraGate=remember{CameraRequestGate()}
    val tiltRequestSeen=remember{intArrayOf(state.cameraRequest)}
    val orientationRequestSeen=remember{intArrayOf(state.orientationRequest)}
    val reportAway by rememberUpdatedState(onAwayFromUser)
    var mapBoundsOnScreen by remember { mutableStateOf(Rect.Zero) }
    var scaleLineBounds by remember { mutableStateOf(Rect.Zero) }
    var scaleTick by remember { mutableStateOf<MapScaleTick?>(null) }
    val currentMapBounds by rememberUpdatedState(mapBoundsOnScreen)
    val currentScaleLineBounds by rememberUpdatedState(scaleLineBounds)
    val currentScaleImperial by rememberUpdatedState(state.imperial)
    val currentScaleEnabled by rememberUpdatedState(showScaleBar && active)
    val lastScaleUpdate=remember { longArrayOf(0L) }
    fun refreshScale(m:MapLibreMap, force:Boolean=false) {
        if(!currentScaleEnabled) { scaleTick=null;return }
        val now=SystemClock.elapsedRealtime()
        if(!force && now-lastScaleUpdate[0]<120L)return
        lastScaleUpdate[0]=now
        val outer=currentMapBounds
        val line=currentScaleLineBounds
        if(outer.width<=0f || line.width<=0f) { scaleTick=null;return }
        val y=line.center.y-outer.top
        val maxMeters=runCatching {
            val left=m.projection.fromScreenLocation(PointF(line.left-outer.left,y))
            val right=m.projection.fromScreenLocation(PointF(line.right-outer.left,y))
            distance(Coordinate(left.latitude,left.longitude),Coordinate(right.latitude,right.longitude))
        }.getOrNull()
        val next=maxMeters?.let { chooseMapScale(it,currentScaleImperial) }
        if(next==null || scaleTick?.label!=next.label ||
            kotlin.math.abs((scaleTick?.widthFraction ?: -1f)-next.widthFraction)>.01f)scaleTick=next
    }
    fun reportVisibleMapArea(m:MapLibreMap) {
        val bounds=runCatching { m.projection.visibleRegion.latLngBounds }.getOrNull() ?: return
        m.cameraPosition.target?.let { target ->
            currentBrowseMapCamera(Coordinate(target.latitude,target.longitude),m.cameraPosition.zoom)
        }
        currentVisibleMapArea(OfflineMapArea(
            south=bounds.latitudeSouth,
            west=bounds.longitudeWest,
            north=bounds.latitudeNorth,
            east=bounds.longitudeEast,
            zoom=m.cameraPosition.zoom,
        ))
    }
    fun navigationPose(m:MapLibreMap, nav:NavState, headingOverride:Float?=null):NavigationCameraPose {
        val topInset=scaleBarInsets?.top?.takeIf { it>0 }?.toDouble()
            ?: if(landscape)0.0 else m.height*.20
        val wantedTilt=mapCameraTilt(nav.world,nav.tilted,nav.navigating,cameraTiltOverride)
        return navigationCameraPose(nav,m.cameraPosition.bearing.toFloat(),m.height.toInt(),
            density.density.toDouble(),topInset,wantedTilt,headingOverride)
    }
    val motion=remember { MapMotion() }
    val renderedBearing=remember{floatArrayOf(0f)}
    val positionFrames=remember{arrayOfNulls<MapMotionFrames>(1)}
    val globeAnimator=remember(mapView){arrayOfNulls<ValueAnimator>(1)}
    val focusCameraHoldUntil=remember{longArrayOf(0L)}
    val styleEpoch=remember{intArrayOf(0)}
    val renderLifecycle=remember(mapView){MapRenderLifecycle(mapView)}
    DisposableEffect(mapView,owner) {
        val observer=LifecycleEventObserver { _,_ -> renderLifecycle.update(owner.lifecycle.currentState,currentActive) }
        owner.lifecycle.addObserver(observer)
        renderLifecycle.update(owner.lifecycle.currentState,currentActive)
        onDispose {styleEpoch[0]++;positionFrames[0]?.cancel();owner.lifecycle.removeObserver(observer);renderLifecycle.destroy()}
    }
    LaunchedEffect(active) {renderLifecycle.update(owner.lifecycle.currentState,active)}
    DisposableEffect(map,owner,active,styleReady,cameraZoomOverride,cameraTiltOverride,cameraCenterOverride) {
        fun syncGlobeRotation() {
            val targetMap=map
            val shouldRotate=targetMap!=null && active && styleReady>0 && cameraZoomOverride!=null &&
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && ValueAnimator.areAnimatorsEnabled()
            if(!shouldRotate) {
                globeAnimator[0]?.cancel();globeAnimator[0]=null
                return
            }
            if(globeAnimator[0]?.isRunning==true)return
            val current=requireNotNull(targetMap)
            val camera=current.cameraPosition
            val target=cameraCenterOverride?.latLng() ?: camera.target ?: PreviewCenter.latLng()
            val latitude=target.latitude
            val longitude=target.longitude
            globeAnimator[0]=ValueAnimator.ofFloat(0f,360f).apply {
                duration=72000;repeatCount=ValueAnimator.INFINITE;interpolator=LinearInterpolator()
                addUpdateListener { frame ->
                    if(!active || styleReady==0)return@addUpdateListener
                    val rotated=((longitude+(frame.animatedValue as Float)+180.0)%360.0+360.0)%360.0-180.0
                    val position=CameraPosition.Builder(current.cameraPosition)
                        .target(LatLng(latitude,rotated))
                        .zoom(cameraZoomOverride ?: current.cameraPosition.zoom)
                        .tilt(cameraTiltOverride ?: current.cameraPosition.tilt)
                        .bearing(0.0)
                        .build()
                    current.moveCamera(CameraUpdateFactory.newCameraPosition(position))
                }
                start()
            }
        }
        val observer=LifecycleEventObserver { _,_ -> syncGlobeRotation() }
        owner.lifecycle.addObserver(observer)
        syncGlobeRotation()
        onDispose { owner.lifecycle.removeObserver(observer);globeAnimator[0]?.cancel();globeAnimator[0]=null }
    }
    Box(modifier.onGloballyPositioned { mapBoundsOnScreen=it.boundsInRoot() }) {
    AndroidView(factory={mapView.apply {
        addOnDidFailLoadingMapListener { currentError("Map couldn't load. Check your internet connection.") }
        getMapAsync { m ->
            map=m; m.uiSettings.apply {
                isCompassEnabled=false; isLogoEnabled=false; isAttributionEnabled=false
                setAllGesturesEnabled(true)
            }
            val initialZoom=cameraZoomOverride ?: if(currentState.navigating)NAV_ZOOM else 15.2
            val initialTilt=mapCameraTilt(currentState.world,currentState.tilted,currentState.navigating,cameraTiltOverride)
            m.cameraPosition=CameraPosition.Builder().target((cameraCenterOverride ?: PreviewCenter).latLng()).zoom(initialZoom).tilt(if(currentState.navigating)navigationTiltAtZoom(initialZoom,initialTilt) else initialTilt).bearing(-22.0).build()
            m.addOnCameraMoveListener { currentBearing(m.cameraPosition.bearing.toFloat());refreshScale(m) }
            m.addOnCameraMoveStartedListener { reason -> if(reason==MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                cameraGestureStart[0]=m.cameraPosition
                focusCameraHoldUntil[0]=0L;cameraGate.userMovedCamera();followPolicy.gestureStarted(SystemClock.elapsedRealtime());followTick++
            } }
            m.addOnCameraIdleListener {
                val now=SystemClock.elapsedRealtime()
                val start=cameraGestureStart[0];cameraGestureStart[0]=null
                val end=m.cameraPosition
                val centerShift=start?.target?.let { before -> end.target?.let { after ->
                    distance(Coordinate(before.latitude,before.longitude),Coordinate(after.latitude,after.longitude))
                } } ?: Double.MAX_VALUE
                val metersPerPixel=m.projection.getMetersPerPixelAtLatitude(end.target?.latitude ?: 0.0)
                val zoomOnly=start?.let { zoomGestureKeepsFollowing(it.zoom,end.zoom,centerShift,metersPerPixel) } ?: false
                if(followPolicy.gestureEnded(now,keepFollowing=zoomOnly))followTick++
                cameraMemory.zoom=m.cameraPosition.zoom
                reportAway(m.awayFromUser(currentState,followPolicy))
                refreshScale(m,true)
                reportVisibleMapArea(m)
            }
            m.addOnMapLongClickListener { point ->
                if(!currentState.navigating) currentPin(Place("pin:${point.latitude},${point.longitude}","Dropped pin","%.5f, %.5f".format(point.latitude,point.longitude),Coordinate(point.latitude,point.longitude)))
                true
            }
            m.addOnMapClickListener { point ->
                if((!currentState.navigating && currentState.routeOptions.size>1) || (currentState.navigating && currentState.driveAlternatives.isNotEmpty())) {
                    val at=m.projection.toScreenLocation(point);val reach=36f
                    val alternate=m.queryRenderedFeatures(android.graphics.RectF(at.x-reach,at.y-reach,at.x+reach,at.y+reach),"route-alt-line","route-alt-casing","route-alt-label")
                        .firstNotNullOfOrNull { f -> runCatching { f.getNumberProperty("index").toInt() }.getOrNull() }
                    if(alternate!=null) { selectRoute(alternate);return@addOnMapClickListener true }
                }
                val screen = m.projection.toScreenLocation(point)
                val hitRadius = 26f
                val nearby = m.queryRenderedFeatures(
                    android.graphics.RectF(screen.x-hitRadius,screen.y-hitRadius,screen.x+hitRadius,screen.y+hitRadius),
                    "nearby-place-dot","nearby-place-marker",
                ).mapNotNull { feature ->
                    val id = runCatching { feature.getStringProperty("nearby_id") }.getOrNull() ?: return@mapNotNull null
                    val pointFeature = feature.geometry() as? Point ?: return@mapNotNull null
                    val markerScreen = m.projection.toScreenLocation(LatLng(pointFeature.latitude(),pointFeature.longitude()))
                    val dx = (markerScreen.x-screen.x).toFloat()
                    val dy = (markerScreen.y-screen.y).toFloat()
                    (dx*dx+dy*dy).takeIf { it <= hitRadius*hitRadius }?.let { it to feature }
                }.minByOrNull { it.first }?.second
                if (nearby != null && !currentState.navigating) {
                    val id = runCatching { nearby.getStringProperty("nearby_id") }.getOrNull()
                    val selected = nearbyPlaces.firstOrNull { it.id == id }
                    if (selected != null) {
                        currentNearbyPlaceClick(selected)
                        return@addOnMapClickListener true
                    }
                }
                val features=m.queryRenderedFeatures(m.projection.toScreenLocation(point))
                val named=features.firstOrNull { f -> f.geometry() is Point && safeMapFeatureText(runCatching { f.getStringProperty("name") }.getOrNull()) != null }
                if(named!=null && !currentState.navigating) {
                    val p=named.geometry() as Point
                    val name=safeMapFeatureText(runCatching { named.getStringProperty("name") }.getOrNull()) ?: return@addOnMapClickListener false
                    val rawKind=when {
                        named.hasProperty("subclass") -> runCatching { named.getStringProperty("subclass") }.getOrNull()
                        named.hasProperty("class") -> runCatching { named.getStringProperty("class") }.getOrNull()
                        else -> null
                    }
                    val kind=safeMapFeatureText(rawKind,48) ?: "place"
                    currentPin(Place("map:${p.latitude()},${p.longitude()}",name,"Selected on map",Coordinate(p.latitude(),p.longitude()),kind))
                    true
                } else false
            }
        }
    }},modifier=Modifier.fillMaxSize(),update={it.visibility=if(active)android.view.View.VISIBLE else android.view.View.INVISIBLE})
    if(showScaleBar && active) {
        val insets=scaleBarInsets ?: MapViewportInsets(0,0,0,0)
        val leftInset=with(density){insets.left.toDp()}
        val rightInset=with(density){insets.right.toDp()}
        val topInset=with(density){insets.top.toDp()}
        val bottomInset=with(density){insets.bottom.toDp()}
        val placement=if(state.selected!=null && !state.navigating && !landscape)
            Modifier.align(Alignment.TopStart)
                .padding(start=leftInset+8.dp,top=topInset+8.dp)
        else if(landscape)Modifier.align(Alignment.BottomStart)
            .padding(start=leftInset+16.dp,bottom=bottomInset+18.dp)
        else Modifier.align(Alignment.BottomCenter)
            .offset(x=(leftInset-rightInset)/2)
            .padding(bottom=bottomInset+18.dp)
        MapScaleBar(state.world,scaleTick,landscape,placement,
            onLineBounds={scaleLineBounds=it})
    }
    }
    LaunchedEffect(map,styleReady,active,showScaleBar,state.imperial,mapBoundsOnScreen,scaleLineBounds) {
        val m=map
        if(m==null || !active || !showScaleBar || styleReady==0)scaleTick=null
        else refreshScale(m,true)
    }
    LaunchedEffect(map,state.world,active,requestedStyleFile,mapNight) {
        val m=map ?: return@LaunchedEffect
        if(!active || styleReady>0 && loadedStyleFile==requestedStyleFile)return@LaunchedEffect
        // Cancel synchronously before setStyle destroys native layer handles.
        styleEpoch[0]++;positionFrames[0]?.cancel();positionFrames[0]=null
        styleReady=0; currentError(null)
        val requestedWorld=state.world
        val requestedNight=mapNight
        val requestedFile=requestedStyleFile
        val requestedEpoch=styleEpoch[0]
        val (json,images)=withContext(Dispatchers.Default) {
            val source=context.assets.open("styles/$requestedFile.json").bufferedReader().use{it.readText()}
            val poiCategories=MapPoiCategories.all(context)
            val prepared=HashMap<String,Bitmap>(poiCategories.size+12).apply {
                mapMaterial(context,requestedWorld,requestedNight)?.let { put("world-material",it) }
                putAll(mapTextureKit(context,requestedWorld))
                put("nav-arrow",mapPlayer(context,requestedWorld))
                put("vehicle-car",mapVehicle(requestedWorld,TravelMode.CAR))
                put("vehicle-bike",mapVehicle(requestedWorld,TravelMode.MOTORCYCLE))
                put("vehicle-bicycle",mapVehicle(requestedWorld,TravelMode.BICYCLE))
                put("position-uncertain",mapUnknownPosition(requestedWorld))
                put("destination-marker",mapDestination(context,requestedWorld,requestedNight))
                poiCategories.forEach { category ->
                    put("poi-${category.id}",mapPoi(context,category.id,requestedWorld,requestedNight))
                }
                put("pixel-grass",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_grass))
                put("pixel-stone",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_stone))
                put("pixel-water",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_water))
                put("paper-fibre",BitmapFactory.decodeResource(context.resources,R.drawable.paper_fibre))
            }
            source to prepared
        }
        if(styleEpoch[0]!=requestedEpoch || !active)return@LaunchedEffect
        val styleUri=withContext(Dispatchers.IO) { OfflineMapStyleFile.write(context,json) }
        m.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            if(currentState.world!=requestedWorld || styleEpoch[0]!=requestedEpoch)return@setStyle
            val world=requestedWorld
            val art=world.mapArt()
            style.addImages(images)
            style.raiseBuildingExtrusionsAboveRoads()
            reportVisibleMapArea(m)
            val poiLabel=step(zoom(),literal(""),stop(17.2,coalesce(get("name:en"),get("name_en"),get("name"))))
            val poiCategories=MapPoiCategories.all(context)
            fun matches(category:MapPoiCategory):org.maplibre.android.style.expressions.Expression {
                val clauses=(category.classes.map { eq(get("class"),literal(it)) }+
                    category.subclasses.map { eq(get("subclass"),literal(it)) }).distinct()
                return if(clauses.isEmpty()) literal(false) else any(*clauses.toTypedArray())
            }
            val iconCases=buildList {
                poiCategories.drop(1).forEach { category ->
                    add(matches(category));add(literal("poi-${category.id}"))
                }
                add(literal("poi-${poiCategories.first().id}"))
            }
            val poiIcon=switchCase(*iconCases.toTypedArray())
            fun groupPoi(group:String):org.maplibre.android.style.expressions.Expression =
                poiCategories.filter { it.group==group && it.classes.isNotEmpty() }.let { grouped ->
                    val clauses=grouped.map(::matches)
                    if(clauses.isEmpty())literal(false) else any(*clauses.toTypedArray())
                }
            val socialPoi=groupPoi("social")
            val essentialPoi=groupPoi("essential")
            val utilityPoi=groupPoi("utility")
            val discoveryPoi=groupPoi("discovery")
            val communityPoi=groupPoi("community")
            val supportPoi=groupPoi("support")
            val otherPoi=not(any(essentialPoi,utilityPoi,discoveryPoi,communityPoi,supportPoi,socialPoi))
            val poiRank=coalesce(get("rank"),literal(0))
            val readablePoiScale=art.poiScale.coerceAtLeast(1.1f)
            // Markers grow with the zoom so they stay readable close in and still visible when zoomed out.
            fun poiIconSize(scale:Float)=interpolate(linear(),zoom(),
                stop(12f,1.15f*scale),stop(14f,1.15f*scale),stop(16f,1.3f*scale),stop(18f,1.6f*scale),stop(20f,1.95f*scale),stop(22f,2.2f*scale))
            fun poiTextSize()=interpolate(linear(),zoom(),stop(12f,11f),stop(16f,12f),stop(18f,14f),stop(20f,16f))
            fun poiLayer(id:String, minZoom:Float, filter:org.maplibre.android.style.expressions.Expression, scale:Float)=
                SymbolLayer(id,"openmaptiles").withSourceLayer("poi").apply{this.minZoom=minZoom}.withFilter(filter).withProperties(
                    iconImage(poiIcon),iconSize(poiIconSize(scale)),iconAllowOverlap(false),iconPadding(5f),
                    textFont(arrayOf(world.mapFontName())),textField(poiLabel),textSize(poiTextSize()),textOptional(true),
                    textColor(if(art.lightLabels)"#EEEEDF" else "#282E26"),
                    textHaloColor(if(art.lightLabels)"#18201C" else "#E3DABF"),textHaloWidth(1.5f),textOffset(arrayOf(0f,2f)))
            // Show ranked landmarks sooner, then add detail without crowding streets.
            style.addLayer(poiLayer("waypoint-other-places",18.6f,
                otherPoi,readablePoiScale*.8f))
            style.addLayer(poiLayer("waypoint-discovery-places",14.2f,
                all(discoveryPoi,lte(poiRank,step(zoom(),literal(4),stop(15.5,literal(10)),stop(16.8,literal(24)),stop(17.8,literal(45)),stop(18.6,literal(999))))),readablePoiScale*.9f))
            style.addLayer(poiLayer("waypoint-utility-places",14f,
                all(utilityPoi,lte(poiRank,step(zoom(),literal(10),stop(15.5,literal(35)),stop(17.5,literal(120)),stop(18.6,literal(999))))),readablePoiScale*.9f))
            style.addLayer(poiLayer("waypoint-community-places",14.8f,
                all(communityPoi,lte(poiRank,step(zoom(),literal(3),stop(16f,literal(8)),stop(17.5,literal(25)),stop(18.6,literal(999))))),readablePoiScale*.9f))
            style.addLayer(poiLayer("waypoint-support-places",17.8f,
                all(supportPoi,lte(poiRank,step(zoom(),literal(20),stop(18.6,literal(999))))),readablePoiScale*.8f))
            style.addLayer(poiLayer("waypoint-social-places",14.8f,
                all(socialPoi,lte(poiRank,step(zoom(),literal(80),stop(17.5,literal(160)),stop(18.6,literal(999))))),readablePoiScale))
            style.addLayer(poiLayer("waypoint-essential-places",13.2f,
                all(essentialPoi,lte(poiRank,step(zoom(),literal(2),stop(14.5,literal(12)),stop(16f,literal(36)),stop(17.5,literal(100)),stop(18.6,literal(999))))),readablePoiScale))
            style.addSource(GeoJsonSource("route",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addRouteLayerBelowBuildings(LineLayer("route-casing","route").withProperties(lineColor(currentState.world.routeCasingColor(mapNight)),lineWidth(routeWidthAtZoom(art.routeWidth,4f)),lineDasharray(currentState.world.routeLineDash(currentState.mode,currentState.navigating)),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
            style.addRouteLayerBelowBuildings(LineLayer("route-line","route").withProperties(lineColor(currentState.world.routeColor),lineWidth(routeWidthAtZoom(art.routeWidth,0f)),lineDasharray(currentState.world.routeLineDash(currentState.mode,currentState.navigating)),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
            // Other ways to the same place: quieter lines under the chosen route, tap one to take it.
            style.addSource(GeoJsonSource("route-alt",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayerBelow(LineLayer("route-alt-casing","route-alt").withProperties(lineColor(currentState.world.routeCasingColor(mapNight)),lineWidth(routeWidthAtZoom(art.routeWidth,3f)),lineOpacity(.85f),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)),"route-casing")
            style.addLayerBelow(LineLayer("route-alt-line","route-alt").withProperties(lineColor(if(mapNight)"#8A949A" else "#7E8A92"),lineWidth(routeWidthAtZoom(art.routeWidth,-0.5f)),lineOpacity(.95f),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)),"route-casing")
            style.addSource(GeoJsonSource("route-alt-labels",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(SymbolLayer("route-alt-label","route-alt-labels").withProperties(textField(get("label")),textFont(arrayOf(world.mapFontName())),textSize(13f),
                textColor(if(art.lightLabels)"#EEEEDF" else "#282E26"),textHaloColor(if(art.lightLabels)"#18201C" else "#E3DABF"),textHaloWidth(2f),textAllowOverlap(true),textIgnorePlacement(true)))
            style.addSource(GeoJsonSource("destination",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(SymbolLayer("destination-dot","destination").withProperties(iconImage("destination-marker"),iconSize(.65f),iconAllowOverlap(true),iconIgnorePlacement(true)))
            style.addSource(GeoJsonSource("nearby-results",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(CircleLayer("nearby-place-dot","nearby-results").withProperties(
                circleRadius(4.5f),circleColor(currentState.world.routeColor),
                circleStrokeColor(if(mapNight)"#10151C" else "#F3F0E8"),circleStrokeWidth(1.5f),
            ))
            style.addLayer(SymbolLayer("nearby-place-marker","nearby-results").withProperties(
                iconImage(get("poi_icon")),iconSize(.34f),iconAllowOverlap(false),iconIgnorePlacement(false),
                textField(get("name")),textFont(arrayOf(world.mapFontName())),textSize(11f),textOptional(true),
                textOffset(arrayOf(0f,1.15f)),textColor(if(art.lightLabels)"#EEEEDF" else "#282E26"),
                textHaloColor(if(art.lightLabels)"#18201C" else "#E3DABF"),textHaloWidth(1.5f),
            ))
            style.addSource(GeoJsonSource("position",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addSource(GeoJsonSource("accuracy",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(FillLayer("position-accuracy","accuracy").withProperties(fillColor("#9CCBD8"),fillOpacity(.10f),fillOutlineColor("#9CCBD8")))
            style.addLayer(SymbolLayer("position-arrow","position").withProperties(iconImage("nav-arrow"),iconSize(playerIconSize(0.55f)),iconAnchor("center"),iconOffset(arrayOf(0f,0f)),iconAllowOverlap(true),iconIgnorePlacement(true),iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),iconRotate(renderedBearing[0])))
            loadedStyleFile=requestedFile;styleReady=requestedEpoch
        }
    }
    LaunchedEffect(active,styleReady,state.mode,state.navigating,state.offRoute,state.routing,mapNight) {
        if(!active || styleReady==0)return@LaunchedEffect
        val style=map?.style ?: return@LaunchedEffect
        // World-specific line patterns carry the theme; walk and active guidance
        // retain their established dotted variants.
        val dotted=state.world.routeGrammar(state.mode,state.navigating)==RouteGrammar.DOTS
        val routeColor=if(state.world==World.SAINTS && state.navigating)"#86D955" else state.world.routeColor
        val routeDash=state.world.routeLineDash(state.mode,state.navigating)
        style.getLayerAs<LineLayer>("route-line")?.setProperties(lineColor(routeColor),lineOpacity(if(state.offRoute || state.routing && state.navigating).45f else 1f),lineDasharray(routeDash))
        style.getLayerAs<LineLayer>("route-casing")?.setProperties(lineColor(state.world.routeCasingColor(mapNight)),lineDasharray(routeDash),lineOpacity(if(dotted)0f else 1f))
    }
    LaunchedEffect(active,styleReady,state.routeOptions,state.driveAlternatives,state.route,state.navigating) {
        if(!active || styleReady==0) return@LaunchedEffect
        val style=map?.style ?: return@LaunchedEffect
        val chosen=state.route
        // While guiding the alternatives are fresh routes from the car; labels compare with the time left.
        val others=when {
            chosen==null->emptyList()
            state.navigating->state.driveAlternatives.withIndex().toList()
            state.routeOptions.size<2->emptyList()
            else->state.routeOptions.withIndex().filter{it.value!==chosen}
        }
        val baseSeconds=if(state.navigating)(state.progress?.seconds ?: chosen?.seconds) else chosen?.seconds
        style.getSourceAs<GeoJsonSource>("route-alt")?.setGeoJson(FeatureCollection.fromFeatures(others.map { (index,route) ->
            Feature.fromGeometry(LineString.fromLngLats(route.points.map{p->Point.fromLngLat(p.lon,p.lat)})).also { it.addNumberProperty("index",index) }
        }))
        style.getSourceAs<GeoJsonSource>("route-alt-labels")?.setGeoJson(FeatureCollection.fromFeatures(others.mapIndexedNotNull { rank,(index,route) ->
            // Spread the labels along their own routes so they never sit on top of each other.
            val mid=route.points.getOrNull((route.points.size*(0.35+0.2*rank)).toInt().coerceIn(0,route.points.lastIndex)) ?: return@mapIndexedNotNull null
            val gap=route.seconds-(baseSeconds ?: route.seconds)
            // Rounded up, as the route chips are, so the two never disagree by a minute.
            val delta=if(gap>=0)kotlin.math.ceil(gap/60.0).toInt() else -kotlin.math.ceil(-gap/60.0).toInt()
            val label=when { delta>0->"+$delta min"; delta<0->"$delta min".replace("-","\u2212"); else->"same time" }
            Feature.fromGeometry(Point.fromLngLat(mid.lon,mid.lat)).also { it.addNumberProperty("index",index);it.addStringProperty("label",label) }
        }))
    }
    LaunchedEffect(active,styleReady,state.route,state.selected,state.navigating,state.progress?.traveled) {
        if(!active || styleReady==0) return@LaunchedEffect
        val style=map?.style ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>("route")?.setFeature(state.route?.let{Feature.fromGeometry(LineString.fromLngLats((if(state.navigating)it.remainingPoints(state.progress).let{ahead->state.progress?.snapped?.let{at->trimAhead(ahead,at)} ?: ahead} else it.points).map{p->Point.fromLngLat(p.lon,p.lat)}))})
        style.getSourceAs<GeoJsonSource>("destination")?.setFeature(state.selected?.let{Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat))})
    }
    LaunchedEffect(active,styleReady,nearbyPlaces,state.navigating) {
        if (!active || styleReady == 0) return@LaunchedEffect
        val source = map?.style?.getSourceAs<GeoJsonSource>("nearby-results") ?: return@LaunchedEffect
        val features = if (state.navigating) emptyList() else nearbyPlaces.take(36).map { place ->
            Feature.fromGeometry(Point.fromLngLat(place.point.lon, place.point.lat)).also {
                it.addStringProperty("nearby_id", place.id)
                it.addStringProperty("name", place.name)
                it.addStringProperty("kind", place.kind)
                it.addStringProperty("poi_icon", "poi-${nearbyPoiIcon(place)}")
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
    LaunchedEffect(active,lifecycleState,styleReady,state.fix,displayPosition,routeBearing,state.bearing,state.headingReliable,state.headingSource,state.mode,state.navigating,state.keepMapNorthUp,state.progress?.offRoute,state.offRoute,state.accuracy) {
        positionFrames[0]?.cancel();positionFrames[0]=null
        if(!active || styleReady==0 || !lifecycleState.isAtLeast(Lifecycle.State.RESUMED))return@LaunchedEffect
        val m=map ?: return@LaunchedEffect
        val style=m.style ?: return@LaunchedEffect
        val layer=style.getLayerAs<SymbolLayer>("position-arrow") ?: return@LaunchedEffect
        val source=style.getSourceAs<GeoJsonSource>("position") ?: return@LaunchedEffect
        val gpsCourse=state.navigating && state.headingReliable && state.headingSource==HeadingSource.COURSE
        val heading=when {
            gpsCourse -> state.bearing
            routeBearing!=null -> routeBearing
            state.headingReliable -> state.bearing
            else -> renderedBearing[0]
        }
        val travelDirection=state.navigating && (gpsCourse || routeBearing!=null)
        val baseCameraBearing=mapCameraBearing(state,m.cameraPosition.bearing.toFloat())
        val cameraBearing=if(state.navigating && displayPosition!=null)
            navigationPose(m,state.copy(fix=displayPosition),baseCameraBearing).bearing.toFloat()
        else baseCameraBearing
        motion.update(displayPosition,heading,SystemClock.elapsedRealtime(),travelDirection,ValueAnimator.areAnimatorsEnabled(),
            cameraBearing=cameraBearing,
            currentCameraBearing=m.cameraPosition.bearing.toFloat())
        layer.setProperties(iconImage(playerMarkerImage(state.world,state.mode,markerDirectionReliable)),
            iconPitchAlignment(if(state.world==World.NIGHT_CITY)Property.ICON_PITCH_ALIGNMENT_VIEWPORT
                else if(state.mode==TravelMode.WALK)Property.ICON_PITCH_ALIGNMENT_MAP
                else Property.ICON_PITCH_ALIGNMENT_VIEWPORT))
        val epoch=styleEpoch[0]
        // Sensor/accuracy emissions cannot reset the accepted fix's timeline.
        // This bookkeeping is deliberately not Compose state: a GL frame should
        // not recompose the whole map, HUD and its effects.
        positionFrames[0]=MapMotionFrames { now ->
            if(epoch!=styleEpoch[0] || !currentActive || styleReady==0)return@MapMotionFrames false
            val pose=motion.sample(now)
            renderedBearing[0]=pose.bearing
            source.setFeature(pose.point?.let { Feature.fromGeometry(Point.fromLngLat(it.lon,it.lat)) })
            layer.setProperties(iconRotate(pose.bearing))
            val point=pose.point
            val followsPosition=followPolicy.shouldFollow(currentState.navigating,currentState.selected!=null,now)
            val followsHeadingOnly=!currentState.navigating && currentState.selected!=null &&
                !currentState.keepMapNorthUp && currentState.headingReliable && followPolicy.shouldFollowHeading(false,now)
            val cameraTarget=when {
                point==null -> null
                followsPosition -> point.latLng()
                followsHeadingOnly -> m.cameraPosition.target
                else -> null
            }
            if(cameraTarget!=null && now>=focusCameraHoldUntil[0]) {
                val cameraBearing=when {
                    currentState.navigating && currentState.keepMapNorthUp -> 0.0
                    currentState.navigating -> pose.cameraBearing.toDouble()
                    mapCameraBearingIsControlled(currentState) -> pose.cameraBearing.toDouble()
                    else -> m.cameraPosition.bearing
                }
                val camera=CameraPosition.Builder(m.cameraPosition).target(cameraTarget).bearing(cameraBearing)
                if(followsPosition)camera.padding(drivePadding(m,currentState.navigating))
                m.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()))
                if(followsPosition && point!=null)cameraMemory.fix=point
            }
            motion.remainingMillis(now)>0
        }.also { it.start() }
    }
    LaunchedEffect(active,styleReady,state.fix,state.accuracy) {
        if(!active || styleReady==0)return@LaunchedEffect
        val polygon=state.fix?.takeIf{state.accuracy>=25f}?.let{accuracyBoundary(it,state.accuracy.toDouble())}.orEmpty()
        map?.style?.getSourceAs<GeoJsonSource>("accuracy")?.setFeature(if(polygon.isEmpty())null else Feature.fromGeometry(Polygon.fromLngLats(listOf(polygon.map{Point.fromLngLat(it.lon,it.lat)}))))
    }
    // A timer also restores following while stationary, without waiting for a sensor event.
    LaunchedEffect(active,state.navigating) {
        while(active && state.navigating) { delay(1000);followTick++ }
    }
    // Like Google Maps, guidance sits the vehicle low on the screen so most of it shows the road ahead.
    LaunchedEffect(active,styleReady,state.navigating) {
        if(!active || styleReady==0)return@LaunchedEffect
        val m=map ?: return@LaunchedEffect
        val camera=CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).padding(drivePadding(m,state.navigating)).build())
        if(state.navigating)m.easeCamera(camera,600) else m.moveCamera(camera)
    }
    LaunchedEffect(active,styleReady,state.fix,displayPosition,routeBearing,state.bearing,state.headingReliable,state.headingSource,state.navigating,state.keepMapNorthUp,state.tilted,state.selected,state.progress,state.route,state.speed,state.routing,state.offRoute,scaleBarInsets,followTick) {
        if(!active || styleReady==0)return@LaunchedEffect
        val m=map ?: return@LaunchedEffect
        val following=followPolicy.shouldFollow(state.navigating,state.selected!=null,SystemClock.elapsedRealtime())
        // A panned-away map offers an explicit way back to the user.
        reportAway(m.awayFromUser(state,followPolicy))
        val cameraState=if(state.navigating && displayPosition!=null)state.copy(fix=displayPosition) else state
        val navPose=if(state.navigating)navigationPose(m,cameraState) else null
        val desiredCameraZoom=navPose?.zoom ?: m.cameraPosition.zoom
        val desiredTilt=navPose?.tilt ?: mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)
        val motionRemaining=motion.remainingMillis(SystemClock.elapsedRealtime())
        // The position-frame interpolator normally owns course rotation. If a map gesture or a
        // camera hold prevented those frames from applying it, recheck once the short interpolation
        // window has elapsed and let the follow camera correct the bearing without fighting it.
        if(motionRemaining>0)delay(motionRemaining+48L)
        val desiredBearing=navPose?.bearing ?: m.cameraPosition.bearing
        val zoomChanged=state.navigating && (cameraMemory.zoom.isNaN() || kotlin.math.abs(cameraMemory.zoom-desiredCameraZoom)>.2)
        val bearingChanged=state.navigating && kotlin.math.abs(headingDelta(m.cameraPosition.bearing.toFloat(),desiredBearing.toFloat()))>2f
        if(SystemClock.elapsedRealtime()>=focusCameraHoldUntil[0] && following && displayPosition!=null &&
            (cameraMemory.fix==null || distance(cameraMemory.fix!!,displayPosition)>1.5 ||
            distance(Coordinate(m.cameraPosition.target!!.latitude,m.cameraPosition.target!!.longitude),displayPosition)>20 ||
            kotlin.math.abs(m.cameraPosition.tilt-desiredTilt)>1.0 || zoomChanged || bearingChanged)) {
            cameraMemory.fix=displayPosition;cameraMemory.zoom=desiredCameraZoom
            focusCameraHoldUntil[0]=SystemClock.elapsedRealtime()+500L
            m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).padding(drivePadding(m,state.navigating)).target(displayPosition.latLng())
                .zoom(desiredCameraZoom).bearing(if(state.navigating)desiredBearing else m.cameraPosition.bearing)
                .tilt(desiredTilt).build()),500)
        }
    }
    // A compass tap rotates the view in place; it never recentres or changes zoom.
    LaunchedEffect(active,styleReady,state.orientationRequest) {
        val m=map ?: return@LaunchedEffect
        if(!active || styleReady==0 || orientationRequestSeen[0]==state.orientationRequest)return@LaunchedEffect
        orientationRequestSeen[0]=state.orientationRequest
        val following=followPolicy.shouldFollow(state.navigating,state.selected!=null,SystemClock.elapsedRealtime())
        // Browsing the live position is still a following camera. The compass must rotate it
        // without changing its centre or zoom; only a moving navigation camera is left to the
        // position-frame interpolator to avoid two competing camera animations.
        if(!state.navigating || !following || state.fix==null) {
            val bearing=mapCameraBearing(state,m.cameraPosition.bearing.toFloat())
            m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).bearing(bearing.toDouble()).build()),350)
        }
    }
    // The 2D/3D control changes pitch alone: same target, zoom and bearing wherever the user is looking.
    LaunchedEffect(active,styleReady,state.tilted,state.world,cameraTiltOverride,state.cameraRequest) {
        val requestChanged=tiltRequestSeen[0]!=state.cameraRequest
        tiltRequestSeen[0]=state.cameraRequest
        val m=map ?: return@LaunchedEffect
        // A new camera request already carries the tilt in its own animation.
        if(!active || styleReady==0 || requestChanged) return@LaunchedEffect
        val wantedTilt=mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)
        val targetTilt=if(state.navigating)navigationTiltAtZoom(m.cameraPosition.zoom,wantedTilt) else wantedTilt
        val tilt=pitchChange(m.cameraPosition.tilt,targetTilt) ?: return@LaunchedEffect
        m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).tilt(tilt).build()),400)
    }
    LaunchedEffect(active,state.cameraRequest,styleReady,routeInsets,cameraZoomOverride,cameraTiltOverride,cameraCenterOverride,scaleBarInsets,displayPosition) {
        val m=map ?: return@LaunchedEffect
        if(!active || styleReady==0) return@LaunchedEffect
        // A style reload or new insets replays this effect; only a fresh request may move a map the user has taken over.
        if(!cameraGate.accept(state.cameraRequest,passive=state.cameraRequest==state.passiveCameraRequest))return@LaunchedEffect
        val focusUser=displayPosition!=null && state.cameraRequest==state.focusUserCameraRequest
        focusCameraHoldUntil[0]=SystemClock.elapsedRealtime()+if(focusUser)1100L else 750L
        followPolicy.resume();cameraMemory.fix=null
        // A follow tick would launch a second camera animation at the old zoom.
        if(!focusUser)followTick++
        if(focusUser) {
            val fix=requireNotNull(displayPosition)
            val pose=if(state.navigating)navigationPose(m,state) else null
            val zoom=pose?.zoom ?: maxOf(LOCATE_ZOOM,state.world.mapArt().zoom)
            val tilt=pose?.tilt ?: mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)
            val bearing=pose?.bearing ?: mapCameraBearing(state,m.cameraPosition.bearing.toFloat()).toDouble()
            cameraMemory.zoom=zoom
            m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .padding(drivePadding(m,state.navigating)).target(fix.latLng()).zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build()),700)
        } else if(state.route!=null && !state.navigating) {
            val insets=routeInsets ?: return@LaunchedEffect
            val points=(if(state.navigating)state.route.points else state.routeOptions.flatMap{it.points}.ifEmpty{state.route.points}).map{it.latLng()}+listOfNotNull(state.fix?.latLng(),state.selected?.point?.latLng())
            val bounds=LatLngBounds.Builder().includes(points).build()
            // An overview is north-up and flat. Inheriting a recreated map's preview tilt
            // produces an invalid fit with the narrow landscape viewport.
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,0.0,0.0,insets.left,insets.top,insets.right,insets.bottom),700)
        } else {
            val center=if(state.navigating) displayPosition else state.selected?.point ?: state.fix
            if(center!=null) {
                val pose=if(state.navigating)navigationPose(m,state) else null
                val zoom=pose?.zoom ?: cameraZoomOverride ?: state.world.mapArt().zoom
                val bearing=pose?.bearing ?: mapCameraBearing(state,m.cameraPosition.bearing.toFloat()).toDouble()
                val wantedTilt=mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)
                val tilt=pose?.tilt ?: wantedTilt
                cameraMemory.zoom=zoom
                m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().padding(drivePadding(m,state.navigating)).target(center.latLng()).zoom(zoom).bearing(bearing).tilt(tilt).build()),700)
            }
            else m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target((cameraCenterOverride ?: PreviewCenter).latLng()).zoom(cameraZoomOverride ?: state.world.mapArt().zoom).bearing(0.0).tilt(mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)).build()))
        }
    }
}

/** Bound untrusted map-tile labels before they reach UI, storage or guidance. */
internal fun safeMapFeatureText(raw: String?, limit: Int = 120): String? {
    if (raw == null) return null
    val text = StringBuilder(limit)
    for (character in raw) {
        if (text.length >= limit) break
        if (!character.isISOControl()) text.append(character)
    }
    return text.toString().trim().ifEmpty { null }
}

/** Match Vela archive taxonomy to Waypoint's per-world POI sprites. */
private fun nearbyPoiIcon(place: NearbyPlace): String {
    val kind = place.kind.lowercase(Locale.ROOT)
    return when {
        listOf("coffee", "cafe", "café", "tea").any(kind::contains) -> "cafe"
        "bakery" in kind -> "bakery"
        listOf("fast food", "fast_food", "burger").any(kind::contains) -> "fast_food"
        listOf("restaurant", "food", "dining").any(kind::contains) -> "restaurant"
        listOf("hospital", "medical center", "medical centre").any(kind::contains) -> "hospital"
        listOf("pharmacy", "chemist").any(kind::contains) -> "pharmacy"
        listOf("clinic", "doctor", "dentist").any(kind::contains) -> "clinic"
        listOf("fuel", "petrol", "gas station").any(kind::contains) -> "fuel"
        listOf("charging", "ev station").any(kind::contains) -> "ev_charging"
        listOf("parking", "car park").any(kind::contains) -> "parking"
        listOf("hotel", "hostel", "lodging", "guest house").any(kind::contains) -> "lodging"
        listOf("park", "garden", "playground").any(kind::contains) -> "park"
        listOf("museum", "gallery").any(kind::contains) -> "museum"
        listOf("attraction", "monument", "landmark", "castle").any(kind::contains) -> "attraction"
        listOf("worship", "church", "mosque", "temple", "synagogue").any(kind::contains) -> "place_of_worship"
        listOf("grocery", "supermarket", "market").any(kind::contains) -> "grocery"
        listOf("mall", "shopping centre", "shopping center").any(kind::contains) -> "mall"
        "shop" in kind || place.group == "shop" -> "shop"
        else -> when (place.group) {
            "food" -> "restaurant"
            "park" -> "park"
            "culture" -> "attraction"
            "fuel" -> "fuel"
            "lodging" -> "lodging"
            "health" -> "pharmacy"
            "parking" -> "parking"
            "civic" -> "information"
            else -> "place"
        }
    }
}

private fun Coordinate.latLng()=LatLng(lat,lon)
// Native MapLibre cannot accept a Feature whose geometry is null.
private fun GeoJsonSource.setFeature(feature: Feature?) {
    if(feature==null) setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>())) else setGeoJson(feature)
}

/** Whether the recentre control has somewhere to go: guiding with follow paused, or browsing away from the fix. */
private fun MapLibreMap.awayFromUser(state: NavState, follow: MapFollowPolicy): Boolean {
    val target=cameraPosition.target
    val fix=state.fix
    val gap=if(target!=null && fix!=null) distance(Coordinate(target.latitude,target.longitude),fix) else null
    val paused=state.navigating && !follow.shouldFollow(true,state.selected!=null,SystemClock.elapsedRealtime())
    return cameraAwayFromUser(state.navigating,paused,gap,projection.getMetersPerPixelAtLatitude(target?.latitude ?: 0.0))
}

private class MapCameraMemory {
    var fix:Coordinate?=null
    var zoom=Double.NaN
}

/** Stop hidden GL rendering without losing the selected camera or native style. */
private class MapRenderLifecycle(private val view:MapView) {
    private var started=false
    private var resumed=false
    fun update(state:Lifecycle.State,active:Boolean) {
        val wantStart=active && state.isAtLeast(Lifecycle.State.STARTED)
        val wantResume=active && state.isAtLeast(Lifecycle.State.RESUMED)
        if(resumed && !wantResume){view.onPause();resumed=false}
        if(started && !wantStart){view.onStop();started=false}
        if(!started && wantStart){view.onStart();started=true}
        if(!resumed && wantResume){view.onResume();resumed=true}
    }
    fun destroy(){update(Lifecycle.State.DESTROYED,false);view.onDestroy()}
}
