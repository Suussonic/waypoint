package com.waypoint.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** One stretch of a public-transport trip: a walk, or a ride on a numbered line. */
data class TransitLeg(
    val mode: String,
    val line: String?,
    val headsign: String?,
    val from: String,
    val to: String,
    val departEpochSeconds: Long,
    val arriveEpochSeconds: Long,
    val minutes: Int,
    val meters: Int?,
    val stops: Int?,
    val liveTimes: Boolean,
    val agency: String?,
) {
    val isWalk: Boolean get() = mode == "WALK" || mode == "BIKE" || mode == "CAR"
    val modeLabel: String get() = transitModeLabel(mode)
}

data class TransitItinerary(
    val departEpochSeconds: Long,
    val arriveEpochSeconds: Long,
    val minutes: Int,
    val transfers: Int,
    val legs: List<TransitLeg>,
) {
    val rides: List<TransitLeg> get() = legs.filterNot { it.isWalk }
    val walkMinutes: Int get() = legs.filter { it.isWalk }.sumOf { it.minutes }
}

internal fun transitModeLabel(mode: String): String = when (mode.uppercase()) {
    "WALK" -> "Walk"
    "BUS", "COACH", "TROLLEYBUS", "NIGHT_BUS" -> "Bus"
    "TRAM", "CABLE_CAR", "FUNICULAR" -> "Tram"
    "SUBWAY", "METRO" -> "Metro"
    "RAIL", "HIGHSPEED_RAIL", "LONG_DISTANCE", "REGIONAL_RAIL", "REGIONAL_FAST_RAIL", "SUBURBAN", "NIGHT_RAIL" -> "Train"
    "FERRY" -> "Ferry"
    "AIRPLANE" -> "Flight"
    else -> "Transit"
}

private fun clean(value: String?, limit: Int = 80): String? =
    value?.take(limit)?.map { if (it.isISOControl()) ' ' else it }?.joinToString("")?.trim()?.takeIf { it.isNotEmpty() }

private fun epoch(value: String?): Long? = runCatching { Instant.parse(value ?: return null).epochSecond }.getOrNull()

/**
 * Turns a MOTIS (Transitous) plan response into trips. Keeps trips that ride at least one vehicle; a walk-only
 * answer is kept only when nothing else exists, so the sheet can say the place is close enough to walk.
 */
internal fun parseTransitPlan(response: JSONObject, limit: Int = 4): List<TransitItinerary> {
    val raw = response.optJSONArray("itineraries") ?: return emptyList()
    val all = buildList {
        for (i in 0 until minOf(raw.length(), 12)) {
            val it = raw.optJSONObject(i) ?: continue
            val start = epoch(it.optString("startTime")) ?: continue
            val end = epoch(it.optString("endTime")) ?: continue
            if (end < start) continue
            val legsJson = it.optJSONArray("legs") ?: continue
            val legs = buildList {
                for (j in 0 until minOf(legsJson.length(), 24)) {
                    val leg = legsJson.optJSONObject(j) ?: continue
                    val depart = epoch(leg.optString("startTime")) ?: continue
                    val arrive = epoch(leg.optString("endTime")) ?: continue
                    val mode = leg.optString("mode").uppercase().ifBlank { "TRANSIT" }
                    val line = clean(leg.optString("routeShortName").ifBlank { leg.optString("displayName") }, 24)
                    add(TransitLeg(
                        mode = mode, line = line, headsign = clean(leg.optString("headsign")),
                        from = clean(leg.optJSONObject("from")?.optString("name")) ?: "Start",
                        to = clean(leg.optJSONObject("to")?.optString("name")) ?: "Destination",
                        departEpochSeconds = depart, arriveEpochSeconds = arrive,
                        minutes = ceil(((arrive - depart).coerceAtLeast(0)) / 60.0).toInt(),
                        meters = leg.optDouble("distance").takeIf { d -> d.isFinite() && d >= 0 }?.toInt(),
                        stops = leg.optJSONArray("intermediateStops")?.length()?.plus(1),
                        liveTimes = leg.optBoolean("realTime", false),
                        agency = clean(leg.optString("agencyName"), 60),
                    ))
                }
            }
            if (legs.isEmpty()) continue
            add(TransitItinerary(start, end, ceil((end - start) / 60.0).toInt().coerceAtLeast(1), it.optInt("transfers", 0).coerceAtLeast(0), legs))
        }
    }
    val withRides = all.filter { trip -> trip.rides.isNotEmpty() }
    return (if (withRides.isNotEmpty()) withRides else all).take(limit)
}

interface TransitPlanner {
    suspend fun plan(from: Coordinate, to: Coordinate): List<TransitItinerary>
}

/** Public-transport trips from Transitous, the community MOTIS service that merges open timetable feeds. */
class TransitousPlanner(
    private val base: String = "https://api.transitous.org",
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build(),
) : TransitPlanner {
    override suspend fun plan(from: Coordinate, to: Coordinate): List<TransitItinerary> = withContext(Dispatchers.IO) {
        require(from.isValid() && to.isValid()) { "Choose a destination with valid coordinates." }
        val url = "${base.trimEnd('/')}/api/v1/plan".toHttpUrl().newBuilder()
            .addQueryParameter("fromPlace", "${from.lat},${from.lon}")
            .addQueryParameter("toPlace", "${to.lat},${to.lon}")
            .addQueryParameter("numItineraries", "4")
            .addQueryParameter("transitModes", "TRANSIT")
            .build()
        val request = Request.Builder().url(url)
            .header("User-Agent", "WaypointAndroid/${BuildConfig.VERSION_NAME} (https://github.com/the-geeky-couldron/waypoint)").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { if (response.code == 429) "Transit service is busy. Try again shortly." else "Transit service unavailable (${response.code}). Try again." }
            val body = response.body ?: error("Empty response")
            require(body.contentLength() <= 4 * 1024 * 1024) { "The transit response is too large. Try a closer destination." }
            val text = body.string()
            require(text.length <= 4 * 1024 * 1024) { "The transit response is too large. Try a closer destination." }
            JsonSafety.checkDepth(text)
            parseTransitPlan(JSONObject(text))
        }
    }
}
