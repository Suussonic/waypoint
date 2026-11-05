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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.*
import org.maplibre.android.style.expressions.Expression.*

/** Physical pixels, measured from the native map's edges to the uncovered viewport. */
data class MapViewportInsets(val left:Int,val top:Int,val right:Int,val bottom:Int)

@Composable
fun WorldMap(state: NavState, onPin: (Place)->Unit, onMapError: (String?)->Unit, modifier: Modifier=Modifier,active:Boolean=true,routeInsets:MapViewportInsets?=null,onBearing:(Float)->Unit={},cameraZoomOverride:Double?=null,cameraTiltOverride:Double?=null,styleFileOverride:String?=null,cameraCenterOverride:Coordinate?=null) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val lifecycleState by owner.lifecycle.currentStateAsState()
    val currentState by rememberUpdatedState(state)
    val currentActive by rememberUpdatedState(active)
    val currentPin by rememberUpdatedState(onPin)
    val currentError by rememberUpdatedState(onMapError)
    val currentBearing by rememberUpdatedState(onBearing)
    val mapView=remember { MapLibre.getInstance(context); MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableIntStateOf(0) }
    var loadedWorld by remember { mutableStateOf<World?>(null) }
    val followPolicy=remember{MapFollowPolicy()}
    var followTick by remember { mutableIntStateOf(0) }
    val cameraMemory=remember { MapCameraMemory() }
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
    AndroidView(factory={mapView.apply {
        addOnDidFailLoadingMapListener { currentError("Map couldn't load. Check your internet connection.") }
        getMapAsync { m ->
            map=m; m.uiSettings.apply {
                isCompassEnabled=false; isLogoEnabled=false; isAttributionEnabled=false
                setAllGesturesEnabled(true)
            }
            m.cameraPosition=CameraPosition.Builder().target((cameraCenterOverride ?: PreviewCenter).latLng()).zoom(cameraZoomOverride ?: 15.2).tilt(mapCameraTilt(currentState.world,currentState.tilted,currentState.navigating,cameraTiltOverride)).bearing(-22.0).build()
            m.addOnCameraMoveListener { currentBearing(m.cameraPosition.bearing.toFloat()) }
            m.addOnCameraMoveStartedListener { reason -> if(reason==MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                focusCameraHoldUntil[0]=0L;followPolicy.gestureStarted(SystemClock.elapsedRealtime());followTick++
            } }
            m.addOnCameraIdleListener { if(followPolicy.gestureEnded(SystemClock.elapsedRealtime()))followTick++ }
            m.addOnMapLongClickListener { point ->
                if(!currentState.navigating) currentPin(Place("pin:${point.latitude},${point.longitude}","Dropped pin","%.5f, %.5f".format(point.latitude,point.longitude),Coordinate(point.latitude,point.longitude)))
                true
            }
            m.addOnMapClickListener { point ->
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
    }},modifier=modifier,update={it.visibility=if(active)android.view.View.VISIBLE else android.view.View.INVISIBLE})
    LaunchedEffect(map,state.world,active,styleFileOverride) {
        val m=map ?: return@LaunchedEffect
        if(!active || styleReady>0 && loadedWorld==state.world)return@LaunchedEffect
        // Cancel synchronously before setStyle destroys native layer handles.
        styleEpoch[0]++;positionFrames[0]?.cancel();positionFrames[0]=null
        styleReady=0; currentError(null)
        val requestedWorld=state.world
        val requestedEpoch=styleEpoch[0]
        val (json,images)=withContext(Dispatchers.Default) {
            val source=context.assets.open("styles/${styleFileOverride ?: requestedWorld.file}.json").bufferedReader().use{it.readText()}
            val prepared=HashMap<String,Bitmap>(20).apply {
                put("nav-arrow",mapPlayer(requestedWorld))
                put("vehicle-car",mapVehicle(requestedWorld,TravelMode.CAR))
                put("vehicle-bike",mapVehicle(requestedWorld,TravelMode.MOTORCYCLE))
                put("vehicle-bicycle",mapVehicle(requestedWorld,TravelMode.BICYCLE))
                put("position-uncertain",mapUnknownPosition(requestedWorld))
                put("destination-marker",mapDestination(requestedWorld))
                for(kind in listOf("place","food","cafe","pub","stay","fuel","parking","shop","sights","health","nature","cash","education"))
                    put("poi-$kind",mapPoi(context,kind,requestedWorld))
                put("pixel-grass",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_grass))
                put("pixel-stone",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_stone))
                put("pixel-water",BitmapFactory.decodeResource(context.resources,R.drawable.pixel_water))
                put("paper-fibre",BitmapFactory.decodeResource(context.resources,R.drawable.paper_fibre))
            }
            source to prepared
        }
        if(styleEpoch[0]!=requestedEpoch || !active)return@LaunchedEffect
        m.setStyle(Style.Builder().fromJson(json)) { style ->
            if(currentState.world!=requestedWorld || styleEpoch[0]!=requestedEpoch)return@setStyle
            val world=requestedWorld
            val art=world.mapArt()
            style.addImages(images)
            val poiLabel=step(zoom(),literal(""),stop(17.2,coalesce(get("name:en"),get("name_en"),get("name"))))
            val poiIcon=switchCase(
                any(eq(get("class"),literal("school")),eq(get("class"),literal("college")),eq(get("class"),literal("university")),eq(get("class"),literal("kindergarten"))),literal("poi-education"),
                any(eq(get("class"),literal("bar")),eq(get("class"),literal("beer")),eq(get("subclass"),literal("pub"))),literal("poi-pub"),
                eq(get("class"),literal("cafe")),literal("poi-cafe"),
                any(eq(get("class"),literal("restaurant")),eq(get("class"),literal("fast_food")),eq(get("class"),literal("ice_cream")),eq(get("class"),literal("bakery")),eq(get("class"),literal("butcher"))),literal("poi-food"),
                eq(get("class"),literal("lodging")),literal("poi-stay"),
                any(eq(get("class"),literal("fuel")),eq(get("class"),literal("charging_station")),eq(get("subclass"),literal("fuel"))),literal("poi-fuel"),
                eq(get("class"),literal("parking")),literal("poi-parking"),
                any(eq(get("class"),literal("hospital")),eq(get("class"),literal("pharmacy")),eq(get("class"),literal("clinic")),eq(get("subclass"),literal("pharmacy"))),literal("poi-health"),
                any(eq(get("class"),literal("attraction")),eq(get("class"),literal("art_gallery")),eq(get("class"),literal("castle")),eq(get("class"),literal("museum")),eq(get("class"),literal("library")),eq(get("class"),literal("monument")),eq(get("class"),literal("cinema")),eq(get("class"),literal("theatre"))),literal("poi-sights"),
                any(eq(get("class"),literal("park")),eq(get("class"),literal("garden")),eq(get("class"),literal("playground"))),literal("poi-nature"),
                any(eq(get("class"),literal("atm")),eq(get("class"),literal("bank"))),literal("poi-cash"),
                any(eq(get("class"),literal("shop")),eq(get("class"),literal("grocery")),eq(get("class"),literal("alcohol_shop")),eq(get("class"),literal("clothing_store")),eq(get("class"),literal("mall"))),literal("poi-shop"),
                literal("poi-place"))
            val socialPoi=any(eq(get("class"),literal("bar")),eq(get("class"),literal("beer")),eq(get("subclass"),literal("pub")))
            val essentialPoi=any(
                eq(get("class"),literal("restaurant")),eq(get("class"),literal("fast_food")),eq(get("class"),literal("ice_cream")),
                eq(get("class"),literal("bakery")),eq(get("class"),literal("butcher")),eq(get("class"),literal("cafe")),
                eq(get("class"),literal("lodging")),eq(get("class"),literal("hospital")),eq(get("class"),literal("clinic")),
                eq(get("class"),literal("pharmacy")))
            val utilityPoi=any(eq(get("class"),literal("fuel")),eq(get("class"),literal("charging_station")),eq(get("class"),literal("parking")))
            val discoveryPoi=any(
                eq(get("class"),literal("shop")),eq(get("class"),literal("grocery")),eq(get("class"),literal("alcohol_shop")),
                eq(get("class"),literal("clothing_store")),eq(get("class"),literal("mall")),eq(get("class"),literal("bank")),
                eq(get("class"),literal("attraction")),eq(get("class"),literal("art_gallery")),eq(get("class"),literal("castle")),
                eq(get("class"),literal("museum")),eq(get("class"),literal("park")),eq(get("class"),literal("garden")),
                eq(get("class"),literal("playground")),eq(get("class"),literal("library")),eq(get("class"),literal("monument")),
                eq(get("class"),literal("cinema")),eq(get("class"),literal("theatre")),eq(get("class"),literal("atm")))
            val communityPoi=any(
                eq(get("class"),literal("school")),eq(get("class"),literal("college")),eq(get("class"),literal("university")),
                eq(get("class"),literal("kindergarten")),eq(get("class"),literal("town_hall")),eq(get("class"),literal("post")),
                eq(get("class"),literal("police")),eq(get("class"),literal("fire_station")),
                eq(get("class"),literal("place_of_worship")),eq(get("class"),literal("stadium")),
                eq(get("class"),literal("sports_centre")),eq(get("class"),literal("railway")),
                eq(get("class"),literal("bus")),eq(get("class"),literal("ferry_terminal")))
            val supportPoi=any(
                eq(get("class"),literal("information")),eq(get("class"),literal("toilets")),
                eq(get("class"),literal("drinking_water")),eq(get("class"),literal("car")),
                eq(get("class"),literal("office")),eq(get("class"),literal("music")))
            val otherPoi=not(any(essentialPoi,utilityPoi,discoveryPoi,communityPoi,supportPoi,socialPoi))
            val poiRank=coalesce(get("rank"),literal(0))
            val readablePoiScale=art.poiScale.coerceAtLeast(1.1f)
            fun poiLayer(id:String, minZoom:Float, filter:org.maplibre.android.style.expressions.Expression, scale:Float)=
                SymbolLayer(id,"openmaptiles").withSourceLayer("poi").apply{this.minZoom=minZoom}.withFilter(filter).withProperties(
                    iconImage(poiIcon),iconSize(scale),iconAllowOverlap(false),iconPadding(5f),
                    textFont(arrayOf(world.mapFontName())),textField(poiLabel),textSize(10f),textOptional(true),
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
            style.addLayer(LineLayer("route-casing","route").withProperties(lineColor("#161716"),lineWidth(art.routeWidth+3f),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
            style.addLayer(LineLayer("route-line","route").withProperties(lineColor(currentState.world.routeColor),lineWidth(art.routeWidth),lineJoin(Property.LINE_JOIN_ROUND),lineCap(Property.LINE_CAP_ROUND)))
            style.addSource(GeoJsonSource("destination",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(SymbolLayer("destination-dot","destination").withProperties(iconImage("destination-marker"),iconSize(.65f),iconAllowOverlap(true),iconIgnorePlacement(true)))
            style.addSource(GeoJsonSource("position",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addSource(GeoJsonSource("accuracy",FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(FillLayer("position-accuracy","accuracy").withProperties(fillColor("#9CCBD8"),fillOpacity(.10f),fillOutlineColor("#9CCBD8")))
            style.addLayer(SymbolLayer("position-arrow","position").withProperties(iconImage("nav-arrow"),iconSize(0.55f),iconAllowOverlap(true),iconIgnorePlacement(true),iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),iconRotate(renderedBearing[0])))
            loadedWorld=requestedWorld;styleReady=requestedEpoch
        }
    }
    LaunchedEffect(active,styleReady,state.mode,state.navigating,state.offRoute,state.routing) {
        if(!active || styleReady==0)return@LaunchedEffect
        val style=map?.style ?: return@LaunchedEffect
        // Retail Cyberpunk walking radar uses yellow dotted guidance; driving is solid.
        val dotted=(state.world==World.NIGHT_CITY && state.mode==TravelMode.WALK) || (state.world==World.SAINTS && state.navigating)
        val routeColor=if(state.world==World.SAINTS && state.navigating)"#86D955" else state.world.routeColor
        style.getLayerAs<LineLayer>("route-line")?.setProperties(lineColor(routeColor),lineOpacity(if(state.offRoute || state.routing && state.navigating).45f else 1f),lineDasharray(if(dotted)arrayOf(.3f,1.8f) else arrayOf(1f,0f)))
        style.getLayerAs<LineLayer>("route-casing")?.setProperties(lineOpacity(if(dotted)0f else 1f))
    }
    LaunchedEffect(active,styleReady,state.route,state.selected,state.navigating,state.progress?.traveled) {
        if(!active || styleReady==0) return@LaunchedEffect
        val style=map?.style ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>("route")?.setFeature(state.route?.let{Feature.fromGeometry(LineString.fromLngLats((if(state.navigating)it.remainingPoints(state.progress) else it.points).map{p->Point.fromLngLat(p.lon,p.lat)}))})
        style.getSourceAs<GeoJsonSource>("destination")?.setFeature(state.selected?.let{Feature.fromGeometry(Point.fromLngLat(it.point.lon,it.point.lat))})
    }
    LaunchedEffect(active,lifecycleState,styleReady,state.fix,state.bearing,state.headingReliable,state.headingSource,state.mode,state.navigating) {
        positionFrames[0]?.cancel();positionFrames[0]=null
        if(!active || styleReady==0 || !lifecycleState.isAtLeast(Lifecycle.State.RESUMED))return@LaunchedEffect
        val m=map ?: return@LaunchedEffect
        val style=m.style ?: return@LaunchedEffect
        val layer=style.getLayerAs<SymbolLayer>("position-arrow") ?: return@LaunchedEffect
        val source=style.getSourceAs<GeoJsonSource>("position") ?: return@LaunchedEffect
        val course=state.navigating && state.headingSource==HeadingSource.COURSE
        val heading=if(state.headingReliable)state.bearing else renderedBearing[0]
        motion.update(state.fix,heading,SystemClock.elapsedRealtime(),course,ValueAnimator.areAnimatorsEnabled())
        layer.setProperties(iconImage(playerMarkerImage(state.mode,state.headingReliable)),
            iconPitchAlignment(if(state.mode==TravelMode.WALK)Property.ICON_PITCH_ALIGNMENT_MAP
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
            if(point!=null && now>=focusCameraHoldUntil[0] &&
                followPolicy.shouldFollow(currentState.navigating,currentState.selected!=null,now)) {
                m.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition)
                    .target(point.latLng()).bearing(if(course)pose.bearing.toDouble() else m.cameraPosition.bearing).build()))
                cameraMemory.fix=point;cameraMemory.bearing=m.cameraPosition.bearing.toFloat()
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
    LaunchedEffect(active,styleReady,state.fix,state.bearing,state.headingSource,state.navigating,state.tilted,state.selected,followTick) {
        if(!active || styleReady==0)return@LaunchedEffect
        val m=map ?: return@LaunchedEffect
        val following=followPolicy.shouldFollow(state.navigating,state.selected!=null,SystemClock.elapsedRealtime())
        val desiredCameraBearing=if(state.navigating && state.headingSource==HeadingSource.COURSE)state.bearing else m.cameraPosition.bearing.toFloat()
        val desiredTilt=mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)
        if(motion.remainingMillis(SystemClock.elapsedRealtime())>0)return@LaunchedEffect
        if(SystemClock.elapsedRealtime()>=focusCameraHoldUntil[0] && following && state.fix!=null &&
            (cameraMemory.fix==null || distance(cameraMemory.fix!!,state.fix)>1.5 ||
            distance(Coordinate(m.cameraPosition.target!!.latitude,m.cameraPosition.target!!.longitude),state.fix)>20 ||
            kotlin.math.abs(headingDelta(cameraMemory.bearing,desiredCameraBearing))>3f ||
            kotlin.math.abs(m.cameraPosition.tilt-desiredTilt)>1.0)) {
            cameraMemory.fix=state.fix;cameraMemory.bearing=desiredCameraBearing
            focusCameraHoldUntil[0]=SystemClock.elapsedRealtime()+500L
            m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).target(state.fix.latLng())
                .zoom(if(state.navigating)17.2 else m.cameraPosition.zoom).bearing(desiredCameraBearing.toDouble())
                .tilt(desiredTilt).build()),500)
        }
    }
    LaunchedEffect(active,state.cameraRequest,styleReady,routeInsets,cameraZoomOverride,cameraTiltOverride,cameraCenterOverride) {
        val m=map ?: return@LaunchedEffect
        if(!active || styleReady==0) return@LaunchedEffect
        val focusUser=state.fix!=null && state.cameraRequest==state.focusUserCameraRequest
        focusCameraHoldUntil[0]=SystemClock.elapsedRealtime()+if(focusUser)1100L else 750L
        followPolicy.resume();cameraMemory.fix=null
        // A follow tick would launch a second camera animation at the old zoom.
        if(!focusUser)followTick++
        if(focusUser) {
            val fix=requireNotNull(state.fix)
            val zoom=if(state.navigating)17.2 else maxOf(17.0,state.world.mapArt().zoom)
            m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(fix.latLng()).zoom(zoom)
                .bearing(if(state.navigating)state.bearing.toDouble() else 0.0)
                .tilt(mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride))
                .build()),700)
        } else if(state.route!=null && !state.navigating) {
            val insets=routeInsets ?: return@LaunchedEffect
            val points=state.route.points.map{it.latLng()}+listOfNotNull(state.fix?.latLng(),state.selected?.point?.latLng())
            val bounds=LatLngBounds.Builder().includes(points).build()
            // An overview is north-up and flat. Inheriting a recreated map's preview tilt
            // produces an invalid fit with the narrow landscape viewport.
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,0.0,0.0,insets.left,insets.top,insets.right,insets.bottom),700)
        } else {
            val center=if(state.navigating) state.fix else state.selected?.point ?: state.fix
            if(center!=null) m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(center.latLng()).zoom(if(state.navigating)17.2 else cameraZoomOverride ?: state.world.mapArt().zoom).bearing(if(state.navigating)state.bearing.toDouble() else 0.0).tilt(mapCameraTilt(state.world,state.tilted,state.navigating,cameraTiltOverride)).build()),700)
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
private fun Coordinate.latLng()=LatLng(lat,lon)
// Native MapLibre cannot accept a Feature whose geometry is null.
private fun GeoJsonSource.setFeature(feature: Feature?) {
    if(feature==null) setGeoJson(FeatureCollection.fromFeatures(emptyArray<Feature>())) else setGeoJson(feature)
}

private class MapCameraMemory {
    var fix:Coordinate?=null
    var bearing=0f
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
