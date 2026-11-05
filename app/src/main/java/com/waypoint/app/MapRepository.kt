package com.waypoint.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind
import uniffi.ferrostar.createRouteFromOsrmRoute
import uniffi.ferrostar.Route

interface NavigationRepository {
    suspend fun search(query: String, near: Coordinate): List<Place>
    suspend fun reverse(point: Coordinate): String
    suspend fun reversePlace(point: Coordinate): Place? = null
    suspend fun route(from: Coordinate, to: Coordinate, mode: TravelMode = TravelMode.CAR): RoadRoute
}

internal fun decodePolyline6(encoded: String): List<Coordinate> {
    var cursor=0
    var lat=0L
    var lon=0L
    fun value(): Long {
        var result=0L
        var shift=0
        while(true) {
            require(cursor<encoded.length && shift<=35) { "The route geometry is invalid. Try again." }
            val chunk=encoded[cursor++].code-63
            require(chunk in 0..63) { "The route geometry is invalid. Try again." }
            result=result or ((chunk and 31).toLong() shl shift)
            if(chunk<32)break
            shift+=5
        }
        return if((result and 1L)==0L) result shr 1 else -(result shr 1)-1
    }
    return buildList {
        while(cursor<encoded.length) {
            require(size<50_000) { "This route is too short or too complex. Choose a closer destination." }
            lat+=value();lon+=value()
            add(Coordinate(lat/1_000_000.0,lon/1_000_000.0).also {
                require(it.isValid()) { "The map service returned invalid coordinates. Try again." }
            })
        }
    }
}

internal fun ferrostarRouteFromOsrm(route: JSONObject, from: Coordinate, to: Coordinate): Route =
    createRouteFromOsrmRoute(route.toString().toByteArray(Charsets.UTF_8),listOf(
        Waypoint(GeographicCoordinate(from.lat,from.lon),WaypointKind.BREAK,null),
        Waypoint(GeographicCoordinate(to.lat,to.lon),WaypointKind.BREAK,null)),6u)

class MapRepository(private val photonBase:String="https://photon.komoot.io", private val routingBase:String="https://router.project-osrm.org", private val valhallaBase:String="https://valhalla1.openstreetmap.de",
    private val ferrostarParser:(JSONObject,Coordinate,Coordinate)->Route?=::ferrostarRouteFromOsrm) : NavigationRepository {
    private val routeMutex=Mutex()
    private var lastRouteCall=0L
    private val client = OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private suspend fun json(url: String, maxBytes: Int = 2 * 1024 * 1024): JSONObject = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).header("User-Agent","WaypointAndroid/${BuildConfig.VERSION_NAME} (https://github.com/the-geeky-couldron/waypoint)").build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = response.use {
                        check(it.isSuccessful) { if (it.code == 429) "Service is busy. Try again shortly." else "Map service unavailable (${it.code}). Try again." }
                        val body = it.body ?: error("Empty response")
                        require(body.contentLength() <= maxBytes) { "The map response is too large. Try a shorter route." }
                        val output = ByteArrayOutputStream()
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                if (!continuation.isActive) throw IOException("Request cancelled")
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= maxBytes) { "The map response is too large. Try a shorter route." }
                                output.write(buffer, 0, count)
                            }
                        }
                        val text = output.toString("UTF-8")
                        JsonSafety.checkDepth(text)
                        JSONObject(text)
                    }
                    if (continuation.isActive) continuation.resumeWith(Result.success(result))
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }
            }
        })
    }
    private fun JSONArray.point(): Coordinate = Coordinate(getDouble(1),getDouble(0)).also {
        require(it.isValid()) { "The map service returned invalid coordinates. Try again." }
    }
    private fun text(value: String, limit: Int = 200) = value.take(limit).map { if (it.isISOControl()) ' ' else it }.joinToString("").trim()
    private fun number(value: Double, maximum: Double): Double = value.also {
        require(it.isFinite() && it >= 0 && it <= maximum) { "The route contains invalid distances or times. Try again." }
    }
    override suspend fun search(query: String, near: Coordinate): List<Place> = withContext(Dispatchers.IO) {
        require(query.length <= 256) { "Use a shorter place name or address (up to 256 characters)." }
        require(near.isValid()) { "Waiting for a valid location." }
        val features = json("${photonBase.trimEnd('/')}/api/?q=${URLEncoder.encode(query,"UTF-8")}&lat=${near.lat}&lon=${near.lon}&limit=8&lang=en&zoom=12&location_bias_scale=0.2").getJSONArray("features")
        require(features.length() <= 8) { "The search response has too many places. Try again." }
        (0 until features.length()).map { i ->
            val f = features.getJSONObject(i); val p = f.getJSONObject("properties"); val c = f.getJSONObject("geometry").getJSONArray("coordinates")
            Place(if(p.has("osm_id"))"${text(p.optString("osm_type"),8)}${p.optLong("osm_id")}" else "photon:${c.getDouble(1)},${c.getDouble(0)}:${text(p.optString("name"))}",text(p.optString("name").ifBlank { p.optString("street","Dropped pin") }),
                listOf(p.optString("housenumber"),p.optString("street"),p.optString("district"),p.optString("city"),p.optString("county"),p.optString("country")).map{ text(it,120) }.filter{it.isNotBlank()}.distinct().joinToString(", "),
                c.point(),text(p.optString("osm_value","place"),60),"OpenStreetMap · Photon")
        }.distinctBy { it.id }
    }
    override suspend fun reverse(point: Coordinate): String = withContext(Dispatchers.IO) {
        require(point.isValid()) { "Waiting for a valid location." }
        val p = json("${photonBase.trimEnd('/')}/reverse?lat=${point.lat}&lon=${point.lon}&limit=1&lang=en").getJSONArray("features").optJSONObject(0)?.optJSONObject("properties")
        text(p?.optString("city")?.ifBlank { p.optString("county") }?.ifBlank { "Your location" } ?: "Your location")
    }
    override suspend fun reversePlace(point: Coordinate): Place? = withContext(Dispatchers.IO) {
        require(point.isValid()) { "Choose a destination with valid coordinates." }
        val features=json("${photonBase.trimEnd('/')}/reverse?lat=${point.lat}&lon=${point.lon}&limit=3&lang=en").getJSONArray("features")
        require(features.length()<=3) { "The place response is too large. Try again." }
        (0 until features.length()).mapNotNull { index ->
            val feature=features.optJSONObject(index) ?: return@mapNotNull null
            val properties=feature.optJSONObject("properties") ?: return@mapNotNull null
            val coordinate=feature.optJSONObject("geometry")?.optJSONArray("coordinates")?.let { runCatching { it.point() }.getOrNull() }
                ?: return@mapNotNull null
            val nearby=distance(point,coordinate)
            val landmark=text(properties.optString("name"),120).takeIf { nearby<=200 }
            val street=text(properties.optString("street"),120).takeIf { nearby<=500 }
            val area=text(properties.optString("district"),120).ifBlank { text(properties.optString("city"),120) }
            val name=landmark?.takeIf { it.isNotBlank() } ?: street?.takeIf { it.isNotBlank() }
                ?: area.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val address=listOf(properties.optString("housenumber"),properties.optString("street"),
                properties.optString("district"),properties.optString("city"),properties.optString("county"),
                properties.optString("country")).map { text(it,120) }.filter { it.isNotBlank() }.distinct().joinToString(", ")
            Place("reverse:${point.lat},${point.lon}",name,address,point,"place","OpenStreetMap · Photon")
        }.firstOrNull()
    }
    override suspend fun route(from: Coordinate, to: Coordinate,mode:TravelMode): RoadRoute = withContext(Dispatchers.IO) {
        require(from.isValid() && to.isValid()) { "Choose a destination with valid coordinates." }
        val obj = if(mode==TravelMode.CAR) json("${routingBase.trimEnd('/')}/route/v1/driving/${from.lon},${from.lat};${to.lon},${to.lat}?overview=full&geometries=polyline6&steps=true",8*1024*1024)
        else routeMutex.withLock {
            val elapsed=(System.nanoTime()-lastRouteCall)/1_000_000
            if(elapsed<1100)delay(1100-elapsed)
            lastRouteCall=System.nanoTime()
            val payload=JSONObject().put("locations",JSONArray().put(JSONObject().put("lat",from.lat).put("lon",from.lon)).put(JSONObject().put("lat",to.lat).put("lon",to.lon)))
                .put("costing",mode.costing).put("format","osrm").put("shape_format","polyline6")
                .put("units","kilometers").put("language","en-US")
            json("${valhallaBase.trimEnd('/')}/route?json=${URLEncoder.encode(payload.toString(),"UTF-8")}",8*1024*1024)
        }
        check(obj.optString("code") == "Ok") { "No ${mode.routeDescription} route found. Choose another destination." }
        val r = obj.getJSONArray("routes").getJSONObject(0)
        val points=decodePolyline6(r.getString("geometry"))
        require(points.size in 2..50_000) { "This route is too short or too complex. Choose a closer destination." }
        val legs = r.getJSONArray("legs")
        require(legs.length() in 1..64) { "This route has too many sections. Choose a closer destination." }
        val steps = buildList {
            for (i in 0 until legs.length()) {
                val s = legs.getJSONObject(i).getJSONArray("steps")
                require(size + s.length() <= 2000 && (size.toLong()+s.length())*points.size <= 10_000_000) { "This route is too complex. Choose a closer destination." }
                for (j in 0 until s.length()) {
                    val step = s.getJSONObject(j); val m = step.getJSONObject("maneuver"); val c = m.getJSONArray("location")
                    add(Maneuver(c.point(),text(m.getString("type"),40),text(m.optString("modifier"),40),text(step.optString("name").ifBlank { step.optString("ref") }),number(step.getDouble("distance"),40_000_000.0),number(step.getDouble("duration"),31*86400.0),m.optInt("exit").coerceIn(0,99),
                        isRightUTurn(m.optDouble("bearing_before").takeIf { it.isFinite() },
                            m.optDouble("bearing_after").takeIf { it.isFinite() },step.optString("driving_side"))))
                }
            }
        }
        val meters=number(r.getDouble("distance"),40_000_000.0)
        val seconds=number(r.getDouble("duration"),31*86400.0)
        check(steps.isNotEmpty() && meters>0) { "That destination is too close. Choose another place." }
        // The public routing endpoints omit voice and banner prompts. Add them to
        // their OSRM response so Ferrostar owns instruction timing and de-duplication.
        val sourceSteps=buildList<JSONObject> {
            for(i in 0 until legs.length()) {
                val items=legs.getJSONObject(i).getJSONArray("steps")
                for(j in 0 until items.length())add(items.getJSONObject(j))
            }
        }
        sourceSteps.forEachIndexed { index, source ->
            val upcoming=steps.getOrNull(index+1) ?: return@forEachIndexed
            val segment=steps[index].distance
            if((source.optJSONArray("voiceInstructions")?.length() ?: 0) == 0) {
                val prompts=JSONArray()
                if(segment>210)prompts.put(JSONObject().put("announcement","In 180 meters, ${upcoming.instruction}").put("distanceAlongGeometry",180.0))
                if(segment>45)prompts.put(JSONObject().put("announcement",upcoming.instruction).put("distanceAlongGeometry",35.0))
                if(prompts.length()>0)source.put("voiceInstructions",prompts)
            }
            if((source.optJSONArray("bannerInstructions")?.length() ?: 0) == 0) {
                source.put("bannerInstructions",JSONArray().put(JSONObject()
                    .put("distanceAlongGeometry",segment.coerceAtMost(300.0))
                    .put("primary",JSONObject().put("text",upcoming.instruction)
                        .put("components",JSONArray().put(JSONObject().put("type","text").put("text",upcoming.instruction))))))
            } else {
                val banners=source.getJSONArray("bannerInstructions")
                for(bannerIndex in 0 until banners.length()) {
                    val banner=banners.getJSONObject(bannerIndex)
                    for(key in listOf("primary","secondary","sub"))
                        banner.optJSONObject(key)?.let { if(!it.has("components"))it.put("components",JSONArray()) }
                }
            }
        }
        check(distance(from,points.first()) < 1500 && distance(to,points.last()) < 1500) { "No nearby road access found for this location." }
        val ferrostar=ferrostarParser(r,from,to)
        if(ferrostar!=null)require(ferrostar.geometry.size==points.size && ferrostar.steps.size==steps.size) {
            "The navigation route could not be matched to the map. Try again."
        }
        RoadRoute(points,steps,meters,seconds,mode,ferrostar)
    }
}
