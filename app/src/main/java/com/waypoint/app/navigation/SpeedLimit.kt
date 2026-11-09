package com.waypoint.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val MPH_TO_KMH = 1.609344
private const val MPS_TO_KMH = 3.6

/**
 * Reads an OpenStreetMap `maxspeed` value as km/h. Understands "50", "50 km/h" and "30 mph"; returns null
 * for values that carry no number ("signals", "none", "walk", "IN:urban") so the HUD shows nothing
 * rather than a guess.
 */
internal fun parseMaxSpeedKmh(tag: String?): Int? {
    val text = tag?.trim()?.lowercase() ?: return null
    val match = Regex("^(\\d{1,3})(?:\\s*(km/h|kmh|kph|mph))?$").matchEntire(text) ?: return null
    val value = match.groupValues[1].toInt()
    if (value !in 5..200) return null
    val kmh = if (match.groupValues[2] == "mph") (value * MPH_TO_KMH).roundToInt() else value
    return kmh.takeIf { it in 5..200 }
}

/** The limit shown to the driver, in the unit they chose in Settings. */
internal fun speedLimitForDisplay(limitKmh: Int, imperial: Boolean): Int =
    if (imperial) (limitKmh / MPH_TO_KMH / 5.0).roundToInt() * 5 else limitKmh

internal fun speedKmh(metersPerSecond: Float): Float = metersPerSecond.coerceAtLeast(0f) * MPS_TO_KMH.toFloat()

/**
 * Decides when the driver is over the limit. Fires once when the car goes more than [toleranceKmh] over and
 * stays over until the speed is back at or under the limit, so a speed hovering at the edge does not flicker
 * or nag. Spoken alerts are further spaced by [alertGapMillis].
 */
internal class SpeedWarningTracker(private val toleranceKmh: Int = 3, private val alertGapMillis: Long = 60_000L) {
    var over = false
        private set
    private var lastAlertAt = Long.MIN_VALUE

    /** True only when a new spoken alert should be given now. */
    fun update(speedMetersPerSecond: Float, limitKmh: Int?, nowMillis: Long): Boolean {
        if (limitKmh == null) { over = false; return false }
        val kmh = speedKmh(speedMetersPerSecond)
        val wasOver = over
        over = if (wasOver) kmh > limitKmh else kmh > limitKmh + toleranceKmh
        val entered = over && !wasOver
        if (entered && (lastAlertAt == Long.MIN_VALUE || nowMillis - lastAlertAt >= alertGapMillis)) {
            lastAlertAt = nowMillis
            return true
        }
        return false
    }

    fun reset() { over = false }
}

/** Source of the posted limit at a point. Implementations may be slow; callers never block the UI on them. */
fun interface SpeedLimitSource {
    suspend fun limitAt(point: Coordinate): Int?
}

/** Picks the nearest road with a readable `maxspeed` from an Overpass `out tags geom` response. */
internal fun nearestSpeedLimitKmh(response: JSONObject, point: Coordinate, maxDistanceMeters: Double = 30.0): Int? {
    val elements = response.optJSONArray("elements") ?: return null
    var best: Pair<Double, Int>? = null
    for (i in 0 until minOf(elements.length(), 20)) {
        val way = elements.optJSONObject(i) ?: continue
        val limit = parseMaxSpeedKmh(way.optJSONObject("tags")?.optString("maxspeed")) ?: continue
        val geometry = way.optJSONArray("geometry") ?: continue
        val line = (0 until minOf(geometry.length(), 2000)).mapNotNull { index ->
            val node = geometry.optJSONObject(index) ?: return@mapNotNull null
            Coordinate(node.optDouble("lat"), node.optDouble("lon")).takeIf { it.isValid() }
        }
        val gap = distanceToPolylineMeters(point, line)
        if (gap <= maxDistanceMeters && (best == null || gap < best.first)) best = gap to limit
    }
    return best?.second
}

internal fun distanceToPolylineMeters(point: Coordinate, line: List<Coordinate>): Double {
    if (line.isEmpty()) return Double.MAX_VALUE
    if (line.size == 1) return distance(point, line[0])
    val metersPerDegreeLat = 111_320.0
    val metersPerDegreeLon = metersPerDegreeLat * cos(Math.toRadians(point.lat))
    fun x(c: Coordinate) = (c.lon - point.lon) * metersPerDegreeLon
    fun y(c: Coordinate) = (c.lat - point.lat) * metersPerDegreeLat
    var best = Double.MAX_VALUE
    for (i in 0 until line.size - 1) {
        val ax = x(line[i]); val ay = y(line[i]); val bx = x(line[i + 1]); val by = y(line[i + 1])
        val dx = bx - ax; val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else ((-ax * dx - ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
        best = minOf(best, hypot(ax + t * dx, ay + t * dy))
    }
    return best
}

/** Looks the posted limit up from OpenStreetMap through the public Overpass API. Sends only the current position. */
internal class OverpassSpeedLimitSource(
    private val endpoint: String = "https://overpass-api.de/api/interpreter",
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).callTimeout(12, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build(),
) : SpeedLimitSource {
    override suspend fun limitAt(point: Coordinate): Int? = withContext(Dispatchers.IO) {
        require(point.isValid())
        val query = "[out:json][timeout:8];way(around:25,${"%.5f".format(java.util.Locale.US, point.lat)},${"%.5f".format(java.util.Locale.US, point.lon)})[highway][maxspeed];out tags geom 8;"
        val request = Request.Builder().url(endpoint)
            .header("User-Agent", "WaypointAndroid/${BuildConfig.VERSION_NAME} (https://github.com/the-geeky-couldron/waypoint)")
            .post(FormBody.Builder().add("data", query).build()).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            if (body.contentLength() > 512 * 1024) return@withContext null
            val text = body.string()
            if (text.length > 512 * 1024) return@withContext null
            JsonSafety.checkDepth(text)
            nearestSpeedLimitKmh(JSONObject(text), point)
        }
    }
}
