package com.waypoint.app

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Surface
import androidx.car.app.*
import androidx.car.app.model.*
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import java.util.TimeZone

/** Projected Android Auto UI. Uses the same routing and GPS engine as the phone. */
class WaypointCarService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.Builder(this)
        .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample).build()
    override fun onCreateSession(): Session = object: Session() {
        override fun onCreateScreen(intent: Intent): Screen {
            (carContext.applicationContext as WaypointApplication).navigation.handleNavigationIntent(intent)
            return WaypointCarScreen(carContext)
        }
        override fun onNewIntent(intent: Intent) { (carContext.applicationContext as WaypointApplication).navigation.handleNavigationIntent(intent) }
    }
}
private class WaypointCarScreen(context: CarContext): Screen(context), SurfaceCallback {
    private val controller=(context.applicationContext as WaypointApplication).navigation
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var surface: Surface?=null
    private var width=800; private var height=480
    private var snapshotter: MapSnapshotter?=null
    private var snapshotWorld: World?=null
    private var busy=false
    private var signaled=false
    private val manager=carContext.getCarService(NavigationManager::class.java)
    init {
        MapLibre.getInstance(context)
        manager.setNavigationManagerCallback(object: NavigationManagerCallback { override fun onStopNavigation() { controller.stopNavigation() } })
        lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                controller.setCarVisible(true)
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(this@WaypointCarScreen)
            }
            override fun onStop(owner: LifecycleOwner) { controller.setCarVisible(false) }
            override fun onDestroy(owner: LifecycleOwner) { snapshotter?.cancel();scope.cancel();surface=null }
        })
        scope.launch {
            controller.state.collect { s ->
                if(s.navigating && !signaled) { manager.navigationStarted();signaled=true }
                if(!s.navigating && signaled) { manager.navigationEnded();signaled=false }
                invalidate()
            }
        }
        scope.launch { while(isActive) { render();delay(1500) } }
    }
    override fun onSurfaceAvailable(container: SurfaceContainer) {
        surface=container.surface; width=container.width; height=container.height
        snapshotter?.cancel();snapshotter=null;busy=false;render()
    }
    override fun onSurfaceDestroyed(container: SurfaceContainer) { snapshotter?.cancel();snapshotter=null;surface=null;busy=false }
    private fun header(title: CharSequence): Header = Header.Builder()
        .setTitle(title).setStartHeaderAction(Action.APP_ICON).build()
    override fun onGetTemplate(): Template {
        val s=controller.state.value
        if(!controller.hasLocationPermission()) return MessageTemplate.Builder("Allow precise location in Waypoint on your phone before using navigation.").setHeader(header("Waypoint")).build()
        if(s.navigating) {
            val maneuver=s.route?.steps?.getOrNull(s.progress?.nextStep ?: 0)
            val cue=if(!s.gpsReady)s.gpsStatus else maneuver?.instruction ?: "Continue on your route"
            val step=Step.Builder(cue).setManeuver(androidx.car.app.navigation.model.Maneuver.Builder(carManeuverType(maneuver)).build()).build()
            val info=if(s.routing)RoutingInfo.Builder().setLoading(true).build() else RoutingInfo.Builder().setCurrentStep(step,Distance.create(s.progress?.toTurn ?: 0.0,Distance.UNIT_METERS)).build()
            val seconds=(s.progress?.seconds ?: 0.0).toLong()
            return NavigationTemplate.Builder().setNavigationInfo(info)
                .setDestinationTravelEstimate(TravelEstimate.Builder(Distance.create(s.progress?.remaining ?: 0.0,Distance.UNIT_METERS),DateTimeWithZone.create(System.currentTimeMillis()+seconds*1000,TimeZone.getDefault())).setRemainingTimeSeconds(seconds).build())
                .setActionStrip(ActionStrip.Builder().addAction(Action.Builder().setTitle("End").setOnClickListener{controller.stopNavigation()}.build()).addAction(Action.Builder().setTitle(if(s.voice)"Mute" else "Voice").setOnClickListener{controller.toggleVoice()}.build()).build()).build()
        }
        if(s.selected!=null) {
            val text=when { s.arrived->"You have arrived at ${s.selected.name}";s.routing->"Finding a ${s.mode.label.lowercase()} route…";s.routeError!=null->s.routeError;!s.gpsReady->s.gpsStatus;s.route!=null->"${kotlin.math.ceil(s.route.seconds/60).toInt()} min · ${formatDistance(s.route.meters,s.imperial)}";else->"Waiting for your route" }
            val builder=MessageTemplate.Builder(text).setHeader(header(s.selected.name))
            if(s.route!=null && s.gpsReady && !s.routing && !s.arrived) builder.addAction(Action.Builder().setTitle("Start navigation").setOnClickListener{controller.startNavigation()}.build())
            if(s.routeError!=null) builder.addAction(Action.Builder().setTitle("Retry").setOnClickListener{controller.requestRoute()}.build())
            return builder.addAction(Action.Builder().setTitle("Destinations").setOnClickListener{controller.clearDestination()}.build()).build()
        }
        val places=(s.saved+s.recent).distinctBy{it.id}.take(6)
        if(places.isEmpty()) return MessageTemplate.Builder("Choose or save a destination on your phone while parked. It will appear here.").setHeader(header("Waypoint")).build()
        val list=ItemList.Builder()
        places.forEach { p->list.addItem(Row.Builder().setTitle(p.name).addText(p.address.ifBlank{"Saved destination"}).setOnClickListener{controller.select(p)}.build()) }
        return ListTemplate.Builder().setHeader(header("Your destinations")).setSingleList(list.build()).build()
    }
    private fun render() {
        val target=surface ?: return
        if(!target.isValid || busy || width<=0 || height<=0) return
        val s=controller.state.value
        val world=if(s.carNight || carContext.isDarkMode)s.carNightWorld else s.carDayWorld
        val center=s.fix ?: PreviewCenter
        val camera=CameraPosition.Builder().target(LatLng(center.lat,center.lon)).zoom(if(s.navigating)16.5 else 14.5).tilt(if(s.tilted)40.0 else 0.0).bearing(if(s.navigating)s.bearing.toDouble() else 0.0).build()
        if(snapshotter==null || world!=snapshotWorld) {
            snapshotter?.cancel();snapshotWorld=world
            val json=carContext.assets.open("styles/${world.file}.json").bufferedReader().use{it.readText()}
            snapshotter=MapSnapshotter(carContext,MapSnapshotter.Options(width,height).withStyleBuilder(Style.Builder().fromJson(json)).withCameraPosition(camera).withLogo(false)).apply {
                for((id,res) in listOf("pixel-grass" to R.drawable.pixel_grass,"pixel-stone" to R.drawable.pixel_stone,"pixel-water" to R.drawable.pixel_water,"paper-fibre" to R.drawable.paper_fibre))
                    addImage(id,android.graphics.BitmapFactory.decodeResource(carContext.resources,res),false)
            }
        } else snapshotter?.setCameraPosition(camera)
        busy=true
        snapshotter?.start({ snapshot ->
            busy=false
            if(surface!==target || !target.isValid) return@start
            val canvas=runCatching{target.lockCanvas(null)}.getOrNull() ?: return@start
            try {
                canvas.drawBitmap(snapshot.bitmap,0f,0f,null)
                drawRoute(canvas,snapshot,s,world)
            } finally { target.unlockCanvasAndPost(canvas) }
        }, { busy=false })
    }
    private fun drawRoute(canvas: Canvas, snapshot: MapSnapshot,s: NavState,world: World) {
        val path=Path()
        s.route?.points?.forEachIndexed { i,p->val pixel=snapshot.pixelForLatLng(LatLng(p.lat,p.lon));if(i==0)path.moveTo(pixel.x,pixel.y) else path.lineTo(pixel.x,pixel.y) }
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.parseColor(world.routeColor);style=Paint.Style.STROKE;strokeWidth=8f;strokeJoin=Paint.Join.ROUND;strokeCap=Paint.Cap.ROUND}
        canvas.drawPath(path,paint)
        s.fix?.let{ p->
            val pixel=snapshot.pixelForLatLng(LatLng(p.lat,p.lon));canvas.save();canvas.translate(pixel.x,pixel.y)
            val arrow=Path().apply{moveTo(0f,-23f);lineTo(17f,19f);lineTo(0f,10f);lineTo(-17f,19f);close()}
            paint.color=Color.BLACK;paint.strokeWidth=6f;canvas.drawPath(arrow,paint);paint.style=Paint.Style.FILL;paint.color=Color.WHITE;canvas.drawPath(arrow,paint);canvas.restore()
        }
        paint.style=Paint.Style.FILL;paint.color=Color.BLACK;canvas.drawRect(0f,height-29f,width.toFloat(),height.toFloat(),paint)
        paint.color=Color.WHITE;paint.textSize=16f;canvas.drawText("© OpenStreetMap · OpenFreeMap",12f,height-9f,paint)
    }
    private fun carManeuverType(m: Maneuver?): Int = when {
        m?.type=="arrive"->androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION
        m?.modifier=="left"->androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_LEFT
        m?.modifier=="right"->androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_RIGHT
        m?.modifier=="uturn"->androidx.car.app.navigation.model.Maneuver.TYPE_U_TURN_LEFT
        else->androidx.car.app.navigation.model.Maneuver.TYPE_STRAIGHT
    }
}
