package com.waypoint.app

import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import java.io.File
import kotlin.math.ceil

/** Approximate public settlement centres, paired across 25 countries. */
private data class GlobalPlace(val name:String,val country:String,val size:String,val point:Coordinate)
private val globalPlaces=listOf(
    GlobalPlace("Kolkata","India","metro",Coordinate(22.5726,88.3639)),
    GlobalPlace("Ziro","India","small town",Coordinate(27.5417,93.8161)),
    GlobalPlace("Tokyo","Japan","metro",Coordinate(35.6812,139.7671)),
    GlobalPlace("Shirakawa-go","Japan","village",Coordinate(36.2575,136.9064)),
    GlobalPlace("New York","United States","metro",Coordinate(40.7128,-74.0060)),
    GlobalPlace("Marfa","United States","small town",Coordinate(30.3095,-104.0206)),
    GlobalPlace("London","United Kingdom","metro",Coordinate(51.5074,-0.1278)),
    GlobalPlace("Bibury","United Kingdom","village",Coordinate(51.7587,-1.8387)),
    GlobalPlace("Paris","France","metro",Coordinate(48.8566,2.3522)),
    GlobalPlace("Gordes","France","village",Coordinate(43.9114,5.2000)),
    GlobalPlace("Berlin","Germany","metro",Coordinate(52.5200,13.4050)),
    GlobalPlace("Rothenburg","Germany","small town",Coordinate(49.3789,10.1797)),
    GlobalPlace("Rome","Italy","metro",Coordinate(41.9028,12.4964)),
    GlobalPlace("Civita di Bagnoregio","Italy","village",Coordinate(42.6279,12.1130)),
    GlobalPlace("Madrid","Spain","metro",Coordinate(40.4168,-3.7038)),
    GlobalPlace("Ronda","Spain","small town",Coordinate(36.7462,-5.1612)),
    GlobalPlace("Lisbon","Portugal","metro",Coordinate(38.7223,-9.1393)),
    GlobalPlace("Monsanto","Portugal","village",Coordinate(40.0388,-7.1142)),
    GlobalPlace("Amsterdam","Netherlands","metro",Coordinate(52.3676,4.9041)),
    GlobalPlace("Giethoorn","Netherlands","village",Coordinate(52.7400,6.0775)),
    GlobalPlace("Zurich","Switzerland","city",Coordinate(47.3769,8.5417)),
    GlobalPlace("Lauterbrunnen","Switzerland","village",Coordinate(46.5935,7.9080)),
    GlobalPlace("Oslo","Norway","city",Coordinate(59.9139,10.7522)),
    GlobalPlace("Reine","Norway","village",Coordinate(67.9324,13.0894)),
    GlobalPlace("Stockholm","Sweden","city",Coordinate(59.3293,18.0686)),
    GlobalPlace("Sigtuna","Sweden","small town",Coordinate(59.6167,17.7236)),
    GlobalPlace("Helsinki","Finland","city",Coordinate(60.1699,24.9384)),
    GlobalPlace("Porvoo","Finland","small town",Coordinate(60.3923,25.6651)),
    GlobalPlace("Reykjavik","Iceland","city",Coordinate(64.1466,-21.9426)),
    GlobalPlace("Vik","Iceland","village",Coordinate(63.4186,-19.0060)),
    GlobalPlace("Toronto","Canada","metro",Coordinate(43.6532,-79.3832)),
    GlobalPlace("Banff","Canada","small town",Coordinate(51.1784,-115.5708)),
    GlobalPlace("Mexico City","Mexico","metro",Coordinate(19.4326,-99.1332)),
    GlobalPlace("Tulum","Mexico","small town",Coordinate(20.2114,-87.4654)),
    GlobalPlace("Sao Paulo","Brazil","metro",Coordinate(-23.5505,-46.6333)),
    GlobalPlace("Paraty","Brazil","small town",Coordinate(-23.2178,-44.7131)),
    GlobalPlace("Buenos Aires","Argentina","metro",Coordinate(-34.6037,-58.3816)),
    GlobalPlace("El Chalten","Argentina","village",Coordinate(-49.3315,-72.8863)),
    GlobalPlace("Johannesburg","South Africa","metro",Coordinate(-26.2041,28.0473)),
    GlobalPlace("Clarens","South Africa","small town",Coordinate(-28.5153,28.4199)),
    GlobalPlace("Cairo","Egypt","metro",Coordinate(30.0444,31.2357)),
    GlobalPlace("Siwa","Egypt","small town",Coordinate(29.2032,25.5195)),
    GlobalPlace("Casablanca","Morocco","metro",Coordinate(33.5731,-7.5898)),
    GlobalPlace("Chefchaouen","Morocco","small town",Coordinate(35.1688,-5.2697)),
    GlobalPlace("Sydney","Australia","metro",Coordinate(-33.8688,151.2093)),
    GlobalPlace("Coober Pedy","Australia","small town",Coordinate(-29.0134,134.7549)),
    GlobalPlace("Auckland","New Zealand","metro",Coordinate(-36.8485,174.7633)),
    GlobalPlace("Arrowtown","New Zealand","small town",Coordinate(-44.9382,168.8357)),
    GlobalPlace("Dubai","United Arab Emirates","metro",Coordinate(25.2048,55.2708)),
    GlobalPlace("Hatta","United Arab Emirates","small town",Coordinate(24.8063,56.1175)),
)

@RunWith(AndroidJUnit4::class)
class GlobalScenariosTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private val places get()=globalPlaces.take(InstrumentationRegistry.getArguments().getString("scenario_limit")?.toIntOrNull()?.coerceIn(1,50) ?: 50)
    private fun findMap(view:View):MapView? {
        if(view is MapView)return view
        if(view is ViewGroup)for(index in 0 until view.childCount)findMap(view.getChildAt(index))?.let{return it}
        return null
    }
    private fun onMain(test:()->Boolean):Boolean {
        var result=false
        inst.runOnMainSync {result=test()}
        return result
    }
    private fun await(timeoutMs:Long,test:()->Boolean):Boolean {
        val deadline=SystemClock.elapsedRealtime()+timeoutMs
        while(SystemClock.elapsedRealtime()<deadline) {
            if(onMain(test))return true
            SystemClock.sleep(120)
        }
        return onMain(test)
    }

    @Test fun fiftyPlacesRenderAndRecenterAcrossWorlds() {
        val output=File(inst.targetContext.getExternalFilesDir(null),"global-map-scenarios").apply{mkdirs()}
        File(output,"results.tsv").writeText("number\tplace\tcountry\tsize\tworld\tcamera_and_marker\tfully_rendered\troad_tiles\terror\n")
        val model=mutableStateOf(NavState(world=World.LOS_SANTOS,fix=globalPlaces[0].point,
            gpsReady=true,accuracy=5f,focusUserCameraRequest=1,cameraRequest=1))
        var map:MapLibreMap?=null
        var mapError:String?=null
        var fullyRenderedAt=0L
        lateinit var decor:View
        val failures=mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                (activity.application as WaypointApplication).navigation.setVisible(false)
                decor=activity.window.decorView
                activity.setContent {
                    WorldMap(model.value,{},{mapError=it},Modifier.fillMaxSize())
                }
            }
            assertTrue("MapView did not mount",await(10000){findMap(decor)!=null})
            scenario.onActivity {
                findMap(decor)!!.apply {
                    addOnDidFinishRenderingMapListener { complete ->
                        if(complete)fullyRenderedAt=SystemClock.elapsedRealtime()
                    }
                    getMapAsync { loaded -> map=loaded }
                }
            }
            assertTrue("MapLibre did not initialize",await(10000){map!=null})
            places.forEachIndexed { index,place ->
                val world=World.entries[index%World.entries.size]
                val sceneStarted=SystemClock.elapsedRealtime()
                inst.runOnMainSync {
                    mapError=null
                    model.value=model.value.copy(world=world,fix=place.point,
                        cameraRequest=index+2,focusUserCameraRequest=index+2)
                }
                val ready=await(10000) {
                    val native=map ?: return@await false
                    val style=native.style ?: return@await false
                    val marker=style.getSourceAs<GeoJsonSource>("position") ?: return@await false
                    val camera=native.cameraPosition
                    val center=camera.target ?: return@await false
                    val markerNear=marker.querySourceFeatures(null).any { feature ->
                        (feature.geometry() as? Point)?.let {
                            distance(place.point,Coordinate(it.latitude(),it.longitude()))<30
                        }==true
                    }
                    val routeColor=style.getLayerAs<LineLayer>("route-line")?.lineColorAsInt
                    markerNear && camera.zoom>=16.8 && routeColor==android.graphics.Color.parseColor(world.routeColor) &&
                        distance(place.point,Coordinate(center.latitude,center.longitude))<150 &&
                        style.getLayer("waypoint-essential-places")!=null
                }
                if(!ready) {
                    var detail="map missing"
                    inst.runOnMainSync {
                        map?.let { native ->
                            val camera=native.cameraPosition
                            val center=camera.target
                            val style=native.style
                            val marker=style?.getSourceAs<GeoJsonSource>("position")
                            val markerPoints=marker?.querySourceFeatures(null)?.mapNotNull{it.geometry() as? Point}
                            detail="zoom=${camera.zoom}, center=$center, marker=$markerPoints, routeColor=${style?.getLayerAs<LineLayer>("route-line")?.lineColor?.value}, expected=${world.routeColor}, layer=${style?.getLayer("waypoint-essential-places")!=null}"
                        }
                    }
                    failures+="${place.name}, ${place.country} (${world.name}): $detail; $mapError"
                }
                val complete=await(10000){fullyRenderedAt>sceneStarted}
                if(!complete)failures+="${place.name}, ${place.country} (${world.name}): map never finished rendering; $mapError"
                // The position source can be ready before vector tiles. Check actual rendered features.
                val tiles=await(8000) {
                    val native=map ?: return@await false
                    val width=device.displayWidth.toFloat()
                    val height=device.displayHeight.toFloat()
                    native.queryRenderedFeatures(RectF(width*.1f,height*.1f,width*.9f,height*.9f),
                        "road_minor","road_secondary_tertiary","road_trunk_primary","road_motorway",
                        "road_service_track","building").isNotEmpty()
                }
                if(!tiles)failures+="${place.name}, ${place.country} (${world.name}): no rendered map features; $mapError"
                SystemClock.sleep(250)
                device.takeScreenshot(File(output,"%02d-%s.png".format(index+1,place.name.lowercase().replace(Regex("[^a-z0-9]+"),"-"))))
                File(output,"results.tsv").appendText("${index+1}\t${place.name}\t${place.country}\t${place.size}\t${world.name}\t$ready\t$complete\t$tiles\t${mapError.orEmpty()}\n")
            }
        }
        assertTrue("Global map failures: ${failures.joinToString("; ")}",failures.isEmpty())
    }

    @Test fun fiftyPlacesFindRealRoadRoutes()=runBlocking<Unit> {
        val output=File(inst.targetContext.getExternalFilesDir(null),"global-route-scenarios").apply{mkdirs()}
        File(output,"results.tsv").writeText("number\tplace\tcountry\tsize\tmeters\tsteps\terror\n")
        val repository=MapRepository()
        val failures=mutableListOf<String>()
        places.forEachIndexed { index,place ->
            // A short journey close to the settlement centre, varied by latitude to cover
            // both hemispheres. The production repository parses and validates the response.
            val destination=Coordinate(place.point.lat+.003,place.point.lon+.003)
            val result=runCatching {repository.route(place.point,destination,TravelMode.CAR)}
            val route=result.getOrNull()
            val message=result.exceptionOrNull()?.message.orEmpty()
            if(route==null)failures+="${place.name}, ${place.country}: $message"
            else {
                val engine=NavigationEngine(route)
                val start=engine.update(route.points.first())
                // GPS arrives along the route. A direct jump to the endpoint is correctly
                // rejected by the engine's 500 m movement guard.
                var end=start
                for(segment in 0 until route.points.lastIndex) {
                    val from=route.points[segment]
                    val to=route.points[segment+1]
                    val samples=ceil(distance(from,to)/100.0).toInt().coerceAtLeast(1)
                    for(sample in 1..samples) {
                        val fraction=sample.toDouble()/samples
                        end=engine.update(Coordinate(from.lat+(to.lat-from.lat)*fraction,
                            from.lon+(to.lon-from.lon)*fraction))
                    }
                }
                if(route.meters<=0 || route.steps.isEmpty() || end.traveled<=start.traveled ||
                    !engine.hasArrived(route.points.last(),end))
                    failures+="${place.name}, ${place.country}: unusable parsed route"
            }
            File(output,"results.tsv").appendText("${index+1}\t${place.name}\t${place.country}\t${place.size}\t${route?.meters ?: "FAIL"}\t${route?.steps?.size ?: 0}\t$message\n")
            delay(1200)
        }
        assertTrue("Global route failures: ${failures.joinToString("; ")}",failures.isEmpty())
    }

    @Test fun fiftyPlacesAreSearchableNearTheirOwnMapPosition()=runBlocking<Unit> {
        val output=File(inst.targetContext.getExternalFilesDir(null),"global-search-scenarios").apply{mkdirs()}
        File(output,"results.tsv").writeText("number\tquery\tcountry\tsize\tmatches\tnearest_m\tnote\n")
        val repository=MapRepository()
        val search=PlaceSearch(PlaceSearchProvider(repository::search),AndroidPlaceSearch(inst.targetContext))
        val failures=mutableListOf<String>()
        places.forEachIndexed { index,place ->
            val result=search.search(place.name,place.point)
            val nearest=result.places.minOfOrNull{distance(place.point,it.point)}
            val limit=if(place.size=="metro")20_000.0 else 10_000.0
            if(nearest==null || nearest>limit)
                failures+="${place.name}, ${place.country}: nearest=$nearest; ${result.note}"
            File(output,"results.tsv").appendText("${index+1}\t${place.name}\t${place.country}\t${place.size}\t${result.places.take(3).joinToString(" | "){it.name}}\t${nearest ?: "NONE"}\t${result.note.orEmpty()}\n")
            delay(1200)
        }
        assertTrue("Global search failures: ${failures.joinToString("; ")}",failures.isEmpty())
    }

    @Test fun walkingAndMotorcycleRoutesCoverUrbanAndRuralPlaces()=runBlocking<Unit> {
        val output=File(inst.targetContext.getExternalFilesDir(null),"global-mode-scenarios").apply{mkdirs()}
        File(output,"results.tsv").writeText("place\tcountry\tsize\tmode\tmeters\tsteps\terror\n")
        val repository=MapRepository()
        val failures=mutableListOf<String>()
        val indices=listOf(0,1,2,3,4,7,8,19,34,37,40,41,44,45,48,49)
            .take(InstrumentationRegistry.getArguments().getString("scenario_limit")?.toIntOrNull()?.coerceIn(1,16) ?: 16)
        for(index in indices) {
            val place=globalPlaces[index]
            val destination=Coordinate(place.point.lat+.003,place.point.lon+.003)
            for(mode in listOf(TravelMode.WALK,TravelMode.MOTORCYCLE)) {
                val result=runCatching {repository.route(place.point,destination,mode)}
                val route=result.getOrNull()
                val message=result.exceptionOrNull()?.message.orEmpty()
                if(route==null || route.mode!=mode || route.meters<=0 || route.steps.isEmpty())
                    failures+="${place.name}, ${place.country}, $mode: $message"
                File(output,"results.tsv").appendText("${place.name}\t${place.country}\t${place.size}\t$mode\t${route?.meters ?: "FAIL"}\t${route?.steps?.size ?: 0}\t$message\n")
            }
        }
        assertTrue("Mode route failures: ${failures.joinToString("; ")}",failures.isEmpty())
    }

    @Test fun liveRoutesRenderInTheActualNavigationScreen()=runBlocking<Unit> {
        val cases=listOf(
            Triple(0,World.OLD_WEST,TravelMode.CAR),
            Triple(1,World.PIXEL,TravelMode.WALK),
            Triple(2,World.NIGHT_CITY,TravelMode.MOTORCYCLE),
            Triple(6,World.RIVIA,TravelMode.CAR),
            Triple(34,World.LOS_SANTOS,TravelMode.WALK),
            Triple(48,World.VICE_COAST,TravelMode.CAR),
        )
        val output=File(inst.targetContext.getExternalFilesDir(null),"global-navigation-ui").apply{mkdirs()}
        val repository=MapRepository()
        val routes=cases.map { (index,_,mode) ->
            val point=globalPlaces[index].point
            repository.route(point,Coordinate(point.lat+.003,point.lon+.003),mode)
        }
        inst.targetContext.getSharedPreferences("waypoint",0).edit()
            .putBoolean("onboardingCompleted",true).putBoolean("mapOpened",true)
            .putInt("welcomeShownVersion",BuildConfig.VERSION_CODE).commit()
        lateinit var flow:MutableStateFlow<NavState>
        lateinit var decor:View
        var map:MapLibreMap?=null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val controller=(activity.application as WaypointApplication).navigation
                controller.setVisible(false)
                val field=NavigationController::class.java.getDeclaredField("_state").apply{isAccessible=true}
                @Suppress("UNCHECKED_CAST")
                flow=field.get(controller) as MutableStateFlow<NavState>
                decor=activity.window.decorView
            }
            assertTrue("Actual navigation screen has no map",await(10000){findMap(decor)!=null})
            scenario.onActivity { findMap(decor)!!.getMapAsync{map=it} }
            assertTrue("Native map did not initialize",await(10000){map!=null})
            cases.forEachIndexed { number,(index,world,mode) ->
                val place=globalPlaces[index]
                val route=routes[number]
                val progress=NavigationEngine(route).update(route.points.first())
                inst.runOnMainSync {
                    flow.value=flow.value.copy(world=world,mode=mode,fix=place.point,
                        gpsReady=true,accuracy=5f,voice=false,city=place.name,
                        selected=Place("global-$index",place.name,place.country,route.points.last()),
                        route=route,progress=progress,navigating=true,cameraRequest=number+1,
                        focusUserCameraRequest=number+1)
                }
                assertTrue("Navigation footer missing in ${place.name}",
                    device.wait(Until.hasObject(By.desc("End navigation")),7000))
                assertTrue("Route did not render in ${place.name}",await(10000){
                    val native=map ?: return@await false
                    val style=native.style ?: return@await false
                    val center=native.cameraPosition.target ?: return@await false
                    style.getLayerAs<LineLayer>("route-line")?.lineColorAsInt==android.graphics.Color.parseColor(world.routeColor) &&
                        style.getSourceAs<GeoJsonSource>("route")?.querySourceFeatures(null)?.isNotEmpty()==true &&
                        distance(place.point,Coordinate(center.latitude,center.longitude))<150
                })
                SystemClock.sleep(650)
                val stem="${number+1}-${place.name.lowercase().replace(Regex("[^a-z0-9]+"),"-")}"
                assertTrue(device.takeScreenshot(File(output,"$stem.png")))
                device.dumpWindowHierarchy(File(output,"$stem.xml"))
            }
        }
    }
}
