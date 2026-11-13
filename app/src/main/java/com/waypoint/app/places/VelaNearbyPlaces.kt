/*
 * Nearby-place archive reader adapted from PimpinPumpkin/Vela, chiefly:
 * app/src/main/java/app/vela/offline/PlacesArchiveSearch.kt.
 * Copyright (C) 2026 Vela contributors.
 * Distributed under the GNU General Public License, version 3 or later; see LICENSE.
 * The place data has separate source licenses; see docs/wiki/Sources-and-Licensing.md.
 */
package com.waypoint.app.places

import com.waypoint.app.Coordinate
import com.waypoint.app.JsonSafety
import com.waypoint.app.Place
import com.waypoint.app.PlaceMatcher
import com.waypoint.app.distance
import com.waypoint.app.isValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

private const val MAX_PLACE_MANIFEST_BYTES = 2 * 1024 * 1024
private const val MAX_COMMONS_PLACE_RESPONSE_BYTES = 2 * 1024 * 1024
private const val VELA_PLACES_RELEASE_PREFIX = "/PimpinPumpkin/Vela/releases/download/places-overlays/"

internal enum class NearbyCategory(val title: String, val group: String? = null) {
    ALL("Explore"), FOOD("Food", "food"), COFFEE("Coffee"), PARKS("Parks", "park"),
    CULTURE("Culture", "culture"), SHOPPING("Shopping", "shop"), FUEL("Fuel", "fuel"),
    STAYS("Stays", "lodging"), ESSENTIALS("Essentials");

    fun matches(place: NearbyPlace): Boolean {
        val kind = place.kind.lowercase()
        return when (this) {
            ALL -> place.group != "default"
            FOOD -> place.group == "food"
            COFFEE -> place.group == "food" && listOf("coffee", "cafe", "tea", "bakery").any(kind::contains)
            PARKS -> place.group == "park"
            CULTURE -> place.group == "culture"
            SHOPPING -> place.group == "shop"
            FUEL -> place.group == "fuel"
            STAYS -> place.group == "lodging"
            ESSENTIALS -> place.group in setOf("health", "parking", "civic", "fuel")
        }
    }
}

internal data class NearbyPlace(
    val id: String,
    val name: String,
    val point: Coordinate,
    val kind: String,
    val group: String,
    val address: String,
    val phone: String?,
    val website: String?,
    val hours: String?,
    val origin: String,
    val prominence: Double,
) {
    fun asWaypointPlace(): Place = Place(
        id = id,
        name = name,
        address = address,
        point = point,
        kind = kind,
        source = "Vela open places · $origin",
    )
}

/** Streams Vela's regional PMTiles by byte range and snapshots selected place tiles for offline use. */
internal class VelaNearbyPlacesRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build(),
    private val offlinePlaces: OfflinePlacesStore? = null,
) {
    suspend fun nearby(center: Coordinate, category: NearbyCategory): List<NearbyPlace> = withContext(Dispatchers.IO) {
        require(center.isValid()) { "Waiting for a valid location." }
        offlinePlaces?.nearby(center, category)?.let { return@withContext it }
        val region = VelaPlacesManifest.regions(client).asSequence()
            .filter { center.lat in it.south..it.north && center.lon in it.west..it.east }
            .minByOrNull { (it.north - it.south) * (it.east - it.west) }
            ?: error("Vela has no open-place archive for this area yet.")
        require(isTrustedVelaArchiveUrl(region.url)) {
            "The place archive address is not trusted. Try again later."
        }
        PmtilesReader.Archive.http(client, region.url, maxBytesRead = MAX_PLACE_ARCHIVE_READ_BYTES).use { archive ->
            VelaPlacesArchiveSearch.search(archive, center, category, want = 36, maxRings = 8)
        }
    }

    /** Searches the open archive locally; the query itself never leaves the device. */
    suspend fun search(query: String, center: Coordinate): List<Place> = withContext(Dispatchers.IO) {
        val term = query.trim().take(256)
        require(term.length >= 2 && center.isValid()) { "Enter a place name and wait for a valid location." }
        val region = VelaPlacesManifest.regions(client).asSequence()
            .filter { center.lat in it.south..it.north && center.lon in it.west..it.east }
            .minByOrNull { (it.north - it.south) * (it.east - it.west) }
            ?: return@withContext emptyList()
        require(isTrustedVelaArchiveUrl(region.url)) { "The place archive address is not trusted. Try again later." }
        PmtilesReader.Archive.http(client, region.url, maxBytesRead = MAX_PLACE_ARCHIVE_READ_BYTES).use { archive ->
            VelaPlacesArchiveSearch.searchByName(archive, center, term, want = 12, maxRings = 8)
                .map(NearbyPlace::asWaypointPlace)
        }
    }

    /** Fetches only the Vela place tiles that intersect an explicitly selected offline map area. */
    suspend fun downloadArea(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        onProgress: suspend (Int, Int) -> Unit,
    ): List<NearbyPlace> = withContext(Dispatchers.IO) {
        val bounds = OfflinePlacesStore.Bounds(south, west, north, east)
        require(bounds.valid()) { "This offline place area is invalid." }
        val region = VelaPlacesManifest.regions(client).asSequence()
            .filter { south >= it.south && west >= it.west && north <= it.north && east <= it.east }
            .minByOrNull { (it.north - it.south) * (it.east - it.west) }
            ?: error("Open place data does not cover this whole area yet. Choose a smaller area inside one region.")
        require(isTrustedVelaArchiveUrl(region.url)) { "The place archive address is not trusted. Try again later." }

        PmtilesReader.Archive.http(client, region.url, maxBytesRead = MAX_PLACE_ARCHIVE_READ_BYTES).use { archive ->
            val zoom = archive.header?.maxZoom ?: error("The open-place archive could not be read. Try again later.")
            val (x0, y0) = PmtilesReader.tileOf(north, west, zoom)
            val (x1, y1) = PmtilesReader.tileOf(south, east, zoom)
            val tileCount = (x1 - x0 + 1L) * (y1 - y0 + 1L)
            require(tileCount in 1L..MAX_OFFLINE_PLACE_TILES) {
                "This area needs $tileCount place tiles. Zoom in until it is under $MAX_OFFLINE_PLACE_TILES tiles."
            }
            val coordinates = buildList(tileCount.toInt()) {
                for (x in x0..x1) for (y in y0..y1) add(x to y)
            }
            val batches = coordinates.chunked(128)
            val places = LinkedHashMap<String, NearbyPlace>()
            for ((index, batch) in batches.withIndex()) {
                for ((tile, bytes) in archive.tiles(zoom, batch)) {
                    for (place in VelaPlacesArchiveSearch.decodePlaces(bytes, zoom, tile.first, tile.second)) {
                        if (place.point.lat !in south..north || place.point.lon !in west..east) continue
                        places.putIfAbsent(place.id, place)
                        require(places.size <= MAX_OFFLINE_PLACE_COUNT) {
                            "This area has too many places to save. Zoom in and try again."
                        }
                    }
                }
                check(!archive.readBudgetExceeded) {
                    "This place snapshot exceeded its 64 MB download limit. Zoom in and try again."
                }
                check(!archive.rangeReadFailed) { "Place data could not be downloaded completely. Check your connection and retry." }
                onProgress(index + 1, batches.size)
            }
            places.values.toList()
        }
    }
}

private const val MAX_OFFLINE_PLACE_TILES = 4_096L
private const val MAX_OFFLINE_PLACE_COUNT = 5_000
private const val MAX_PLACE_ARCHIVE_READ_BYTES = 64L * 1024L * 1024L

private object VelaPlacesManifest {
    private const val URL = "https://github.com/PimpinPumpkin/Vela/releases/download/places-overlays/places-overlay-manifest.json"
    @Volatile private var cached: Pair<Long, List<Region>>? = null

    data class Region(val south: Double, val west: Double, val north: Double, val east: Double, val url: String)

    fun regions(client: OkHttpClient): List<Region> {
        val now = System.currentTimeMillis()
        cached?.takeIf { now - it.first < 6 * 60 * 60 * 1000L }?.let { return it.second }
        val request = Request.Builder().url(URL)
            .header("User-Agent", "WaypointAndroid Vela places reader")
            .header("Accept", "application/json").build()
        val regions = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Open-place catalog unavailable (${response.code}). Try again." }
            val body = response.body?.readBoundedBytes(MAX_PLACE_MANIFEST_BYTES) ?: error("Open-place catalog is empty.")
            val text = body.toString(Charsets.UTF_8)
            JsonSafety.checkDepth(text)
            val root = JSONObject(text)
            val items = root.optJSONArray("regions") ?: error("Open-place catalog has no regions.")
            require(items.length() in 1..1000) { "Open-place catalog is invalid." }
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val bbox = item.optJSONArray("bbox") ?: continue
                    if (bbox.length() != 4) continue
                    val bounds = (0..3).map { bbox.optDouble(it, Double.NaN) }
                    val url = item.optString("url")
                    if (bounds.any { !it.isFinite() } || bounds[0] !in -90.0..90.0 || bounds[2] !in -90.0..90.0 ||
                        bounds[1] !in -180.0..180.0 || bounds[3] !in -180.0..180.0 || bounds[0] >= bounds[2] || bounds[1] > bounds[3] ||
                        !isTrustedVelaArchiveUrl(url)) continue
                    add(Region(bounds[0], bounds[1], bounds[2], bounds[3], url))
                }
            }
        }
        check(regions.isNotEmpty()) { "Open-place catalog contains no usable regions." }
        cached = now to regions
        return regions
    }
}

/** Nearby search and point-feature decoding for Vela's `places` vector-tile layer. */
private object VelaPlacesArchiveSearch {
    private const val MIN_RINGS = 2
    private const val MAX_DISTANCE_METERS = 6_000.0
    private const val MAX_LAYER_FEATURES = 4_096
    private const val MAX_LAYER_DICTIONARY_ENTRIES = 4_096
    private const val MAX_LAYER_DICTIONARY_BYTES = 256 * 1024
    private const val MAX_LAYER_NAME_BYTES = 64
    private const val MAX_DICTIONARY_ENTRY_BYTES = 4 * 1024
    private const val MAX_FEATURE_TAG_VALUES = 256
    private const val MAX_LAYER_TAG_VALUES = 65_536
    private const val MAX_FEATURE_GEOMETRY_COMMANDS = 1_024
    private const val MAX_LAYER_GEOMETRY_COMMANDS = 65_536
    private const val MAX_NEARBY_CANDIDATES = 512
    private data class Feature(val properties: Map<String, String>, val point: Coordinate)
    private data class DecodeBudget(var tagValues: Int = 0, var geometryCommands: Int = 0)

    fun search(
        archive: PmtilesReader.Archive,
        center: Coordinate,
        category: NearbyCategory,
        want: Int,
        maxRings: Int,
    ): List<NearbyPlace> {
        val z = archive.header?.maxZoom ?: return emptyList()
        val (cx, cy) = PmtilesReader.tileOf(center.lat, center.lon, z)
        val n = 1 shl z
        fun ring(radius: Int) = (cx - radius..cx + radius).flatMap { x ->
            (cy - radius..cy + radius).map { y -> x to y }
        }.filter { (x, y) -> maxOf(kotlin.math.abs(x - cx), kotlin.math.abs(y - cy)) == radius && x in 0 until n && y in 0 until n }

        val found = LinkedHashMap<String, NearbyPlace>()
        var radius = 0
        while (radius <= maxRings) {
            val rings = if (radius == 0) (0..MIN_RINGS).toList() else listOf(radius)
            val coords = rings.flatMap(::ring)
            for ((tileCoord, tile) in archive.tiles(z, coords)) {
                for (feature in decode(tile, z, tileCoord.first, tileCoord.second)) {
                    val place = toPlace(feature) ?: continue
                    val meters = distance(center, place.point)
                    if (!meters.isFinite() || meters > MAX_DISTANCE_METERS || !category.matches(place)) continue
                    if (place.id !in found && found.size < MAX_NEARBY_CANDIDATES) {
                        found[place.id] = place
                    }
                }
            }
            radius = rings.last() + 1
            if (radius > MIN_RINGS && found.size >= want) break
        }
        return found.values.sortedWith(
            compareBy<NearbyPlace> { distance(center, it.point) - it.prominence.coerceIn(0.0, 9.5) * 12.0 }
                .thenBy { it.name.lowercase() },
        ).take(want)
    }

    fun searchByName(
        archive: PmtilesReader.Archive,
        center: Coordinate,
        query: String,
        want: Int,
        maxRings: Int,
    ): List<NearbyPlace> {
        val z = archive.header?.maxZoom ?: return emptyList()
        val (cx, cy) = PmtilesReader.tileOf(center.lat, center.lon, z)
        val n = 1 shl z
        fun ring(radius: Int) = (cx - radius..cx + radius).flatMap { x ->
            (cy - radius..cy + radius).map { y -> x to y }
        }.filter { (x, y) -> maxOf(kotlin.math.abs(x - cx), kotlin.math.abs(y - cy)) == radius && x in 0 until n && y in 0 until n }

        val coordinates = (0..maxRings).flatMap(::ring)
        val found = LinkedHashMap<String, NearbyPlace>()
        for ((tile, bytes) in archive.tiles(z, coordinates)) {
            for (place in decode(bytes, z, tile.first, tile.second).mapNotNull(::toPlace)) {
                if (distance(center, place.point) > MAX_DISTANCE_METERS) continue
                if (PlaceMatcher.score(query, place.asWaypointPlace()) < .52) continue
                found.putIfAbsent(place.id, place)
                if (found.size >= MAX_NEARBY_CANDIDATES) break
            }
            if (found.size >= MAX_NEARBY_CANDIDATES) break
        }
        return found.values.sortedWith(
            compareByDescending<NearbyPlace> { PlaceMatcher.score(query, it.asWaypointPlace()) }
                .thenBy { distance(center, it.point) - it.prominence.coerceIn(0.0, 9.5) * 12.0 },
        ).take(want.coerceIn(1, 40))
    }

    fun decodePlaces(tile: ByteArray, z: Int, x: Int, y: Int): List<NearbyPlace> = decode(tile, z, x, y).mapNotNull(::toPlace)

    private fun toPlace(feature: Feature): NearbyPlace? {
        val p = feature.properties
        val name = p["name"]?.takeIf(String::isNotBlank) ?: p["name_en"]?.takeIf(String::isNotBlank) ?: return null
        val group = p["group"]?.lowercase()?.takeIf(String::isNotBlank) ?: "default"
        val origin = when (p["origin"]?.lowercase()) {
            "osm" -> "OpenStreetMap"
            "atp" -> "AllThePlaces"
            else -> "Overture"
        }
        val address = listOf(p["addr"], p["loc"]).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct().joinToString(", ")
        return NearbyPlace(
            id = p["id"]?.takeIf(String::isNotBlank) ?: "vela:${name.hashCode()}:${feature.point.lat}:${feature.point.lon}",
            name = name.take(160), point = feature.point,
            kind = p["class"]?.take(80) ?: "Place", group = group,
            address = address.take(240), phone = p["phone"]?.take(80),
            website = p["website"]?.take(300), hours = p["hours"]?.take(240),
            origin = origin, prominence = p["prominence"]?.toDoubleOrNull()?.takeIf(Double::isFinite) ?: 0.0,
        )
    }

    private fun decode(tile: ByteArray, z: Int, x: Int, y: Int): List<Feature> {
        val reader = PmtilesReader.Varints(tile)
        while (reader.hasMore()) {
            val key = reader.next()
            when ((key and 7L).toInt()) {
                0 -> reader.next()
                1 -> reader.skip(8)
                2 -> {
                    val length = reader.next().toInt()
                    val start = reader.position()
                    if (length < 0 || length > tile.size - start) return emptyList()
                    val layer = tile.copyOfRange(reader.position(), reader.position() + length)
                    reader.skip(length)
                    if ((key shr 3).toInt() == 3) decodeLayer(layer, z, x, y)?.let { return it }
                }
                5 -> reader.skip(4)
                else -> return emptyList()
            }
        }
        if (!reader.isValid()) return emptyList()
        return emptyList()
    }

    private fun decodeLayer(bytes: ByteArray, z: Int, tileX: Int, tileY: Int): List<Feature>? {
        var name: String? = null
        var extent = 4096
        val keys = ArrayList<String>()
        val values = ArrayList<String?>()
        val features = ArrayList<ByteArray>()
        var dictionaryBytes = 0
        val reader = PmtilesReader.Varints(bytes)
        while (reader.hasMore()) {
            val key = reader.next()
            val field = (key shr 3).toInt()
            when ((key and 7L).toInt()) {
                0 -> { val value = reader.next(); if (field == 5) extent = value.toInt() }
                1 -> reader.skip(8)
                2 -> {
                    val length = reader.next().toInt()
                    val start = reader.position()
                    if (length < 0 || length > bytes.size - start) return null
                    when (field) {
                        1 -> if (length > MAX_LAYER_NAME_BYTES) return null
                        2 -> if (features.size >= MAX_LAYER_FEATURES) return null
                        3 -> if (keys.size >= MAX_LAYER_DICTIONARY_ENTRIES || length > MAX_DICTIONARY_ENTRY_BYTES) return null
                        4 -> if (values.size >= MAX_LAYER_DICTIONARY_ENTRIES || length > MAX_DICTIONARY_ENTRY_BYTES) return null
                    }
                    if (field in 1..4) {
                        if (dictionaryBytes > MAX_LAYER_DICTIONARY_BYTES - length) return null
                        dictionaryBytes += length
                    }
                    when (field) {
                        1 -> name = String(bytes, start, length)
                        2 -> features += bytes.copyOfRange(start, start + length)
                        3 -> keys += String(bytes, start, length)
                        4 -> values += decodeValue(bytes.copyOfRange(start, start + length))
                    }
                    reader.skip(length)
                }
                5 -> reader.skip(4)
                else -> return null
            }
        }
        if (!reader.isValid() || name != "places" || extent <= 0) return null
        val budget = DecodeBudget()
        val decoded = ArrayList<Feature>(features.size)
        for (feature in features) {
            decodeFeature(feature, keys, values, extent, z, tileX, tileY, budget)?.let(decoded::add)
        }
        return decoded
    }

    private fun decodeValue(bytes: ByteArray): String? {
        val reader = PmtilesReader.Varints(bytes)
        while (reader.hasMore()) {
            val key = reader.next()
            val field = (key shr 3).toInt()
            when ((key and 7L).toInt()) {
                0 -> {
                    val value = reader.next()
                    return when (field) {
                        6 -> ((value ushr 1) xor -(value and 1)).toString()
                        7 -> (value != 0L).toString()
                        else -> value.toString()
                    }
                }
                1 -> {
                    val at = reader.position(); reader.skip(8)
                    return java.lang.Double.longBitsToDouble(PmtilesReader.le64(bytes, at)).toString()
                }
                2 -> return reader.string(reader.next().toInt())
                5 -> {
                    val at = reader.position(); reader.skip(4)
                    val bits = (bytes[at].toInt() and 0xff) or ((bytes[at + 1].toInt() and 0xff) shl 8) or
                        ((bytes[at + 2].toInt() and 0xff) shl 16) or ((bytes[at + 3].toInt() and 0xff) shl 24)
                    return java.lang.Float.intBitsToFloat(bits).toString()
                }
                else -> return null
            }
        }
        return null
    }

    private fun decodeFeature(
        bytes: ByteArray,
        keys: List<String>,
        values: List<String?>,
        extent: Int,
        z: Int,
        x: Int,
        y: Int,
        budget: DecodeBudget,
    ): Feature? {
        val tags = ArrayList<Int>()
        val geometryPrefix = IntArray(3)
        var geometryCommandCount = 0
        var type = 0
        val reader = PmtilesReader.Varints(bytes)
        while (reader.hasMore()) {
            val key = reader.next()
            val field = (key shr 3).toInt()
            when ((key and 7L).toInt()) {
                0 -> { val value = reader.next(); if (field == 3) type = value.toInt() }
                1 -> reader.skip(8)
                2 -> {
                    val length = reader.next().toInt()
                    val start = reader.position()
                    if (length < 0 || length > bytes.size - start) return null
                    val end = start + length
                    if (field == 2 || field == 4) {
                        while (reader.position() < end) {
                            val value = reader.next().toInt()
                            if (!reader.isValid()) return null
                            if (field == 2) {
                                if (tags.size >= MAX_FEATURE_TAG_VALUES || budget.tagValues >= MAX_LAYER_TAG_VALUES) return null
                                tags += value
                                budget.tagValues++
                            } else {
                                if (geometryCommandCount >= MAX_FEATURE_GEOMETRY_COMMANDS ||
                                    budget.geometryCommands >= MAX_LAYER_GEOMETRY_COMMANDS) return null
                                if (geometryCommandCount < geometryPrefix.size) geometryPrefix[geometryCommandCount] = value
                                geometryCommandCount++
                                budget.geometryCommands++
                            }
                        }
                    } else reader.seek(end)
                }
                5 -> reader.skip(4)
                else -> return null
            }
        }
        if (!reader.isValid() || type != 1 || geometryCommandCount < geometryPrefix.size || (geometryPrefix[0] and 7) != 1) return null
        val px = (geometryPrefix[1] ushr 1) xor -(geometryPrefix[1] and 1)
        val py = (geometryPrefix[2] ushr 1) xor -(geometryPrefix[2] and 1)
        val n = (1 shl z).toDouble()
        val lon = (x + px.toDouble() / extent) / n * 360.0 - 180.0
        val lat = Math.toDegrees(atan(sinh(PI * (1 - 2 * (y + py.toDouble() / extent) / n))))
        val properties = HashMap<String, String>()
        var i = 0
        while (i + 1 < tags.size) {
            val property = keys.getOrNull(tags[i])
            val value = values.getOrNull(tags[i + 1])
            if (property != null && value != null && value != "null") properties[property] = value
            i += 2
        }
        return Feature(properties, Coordinate(lat, lon).takeIf(Coordinate::isValid) ?: return null)
    }
}

/** Commons images are fetched only for visible nearby-place rows and are accepted only with a local match and license. */
internal class CommonsPlacePhotoRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(12, TimeUnit.SECONDS).build(),
) {
    suspend fun photo(place: NearbyPlace): CommonsPlacePhoto? = withContext(Dispatchers.IO) {
        val query = okhttp3.HttpUrl.Builder().scheme("https").host("commons.wikimedia.org").addPathSegments("w/api.php")
            .addQueryParameter("action", "query").addQueryParameter("generator", "geosearch")
            .addQueryParameter("ggscoord", "${place.point.lat}|${place.point.lon}")
            .addQueryParameter("ggsradius", "250").addQueryParameter("ggslimit", "12")
            .addQueryParameter("ggsnamespace", "6").addQueryParameter("prop", "coordinates|imageinfo")
            .addQueryParameter("iiprop", "url|extmetadata").addQueryParameter("iiurlwidth", "640")
            .addQueryParameter("format", "json").build()
        val request = Request.Builder().url(query).header("User-Agent", "WaypointAndroid/1.0 (Wikimedia Commons nearby place images)").build()
        val json = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Image lookup unavailable" }
            val bytes = response.body?.let { body ->
                runCatching { body.readBoundedBytes(MAX_COMMONS_PLACE_RESPONSE_BYTES) }.getOrNull()
            } ?: return@withContext null
            val text = bytes.toString(Charsets.UTF_8)
            JsonSafety.checkDepth(text)
            JSONObject(text)
        }
        val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return@withContext null
        val targetTokens = titleTokens(place.name)
        pages.keys().asSequence().mapNotNull { key -> pages.optJSONObject(key) }
            .mapNotNull { page ->
                val title = page.optString("title")
                val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: return@mapNotNull null
                val meta = imageInfo.optJSONObject("extmetadata") ?: return@mapNotNull null
                val imageUrl = imageInfo.optString("thumburl").takeIf(::isTrustedCommonsImageUrl) ?: return@mapNotNull null
                val license = meta.optJSONObject("LicenseShortName")?.optString("value")?.let(::plainText)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val coords = page.optJSONArray("coordinates")?.optJSONObject(0) ?: return@mapNotNull null
                val lat = coords.optDouble("lat", Double.NaN); val lon = coords.optDouble("lon", Double.NaN)
                if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
                val meters = distance(place.point, Coordinate(lat, lon))
                if (meters > 250.0) return@mapNotNull null
                val fileTokens = titleTokens(title.removePrefix("File:").substringBeforeLast('.'))
                val shared = targetTokens.intersect(fileTokens).size
                val needed = maxOf(1, kotlin.math.ceil(targetTokens.size * 0.5).toInt())
                if (targetTokens.isEmpty() || shared < needed) return@mapNotNull null
                val author = meta.optJSONObject("Artist")?.optString("value")?.let(::plainText)?.take(90).orEmpty()
                val pageUrl = "https://commons.wikimedia.org/wiki/${java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"
                (meters - shared * 40.0) to CommonsPlacePhoto(title, imageUrl, pageUrl, author, license)
            }.minByOrNull { it.first }?.second
    }

    private fun titleTokens(text: String) = text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .split(Regex("\\s+")).filter { it.length >= 3 && it !in setOf("the", "and", "for", "place", "file", "commons") }.toSet()

    private fun plainText(html: String): String = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
        .toString().replace(Regex("\\s+"), " ").trim().take(300)
}

private fun isTrustedCommonsImageUrl(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.host?.equals("upload.wikimedia.org", ignoreCase = true) == true &&
        uri.userInfo == null && uri.port in setOf(-1, 443)
}.getOrDefault(false)

private fun isTrustedVelaArchiveUrl(value: String): Boolean = runCatching {
    if (value.length !in 1..2_048) return false
    val uri = java.net.URI(value)
    val host = uri.host?.lowercase(java.util.Locale.ROOT) ?: return false
    val path = uri.path ?: return false
    val archiveName = path.removePrefix(VELA_PLACES_RELEASE_PREFIX)
    uri.scheme.equals("https", ignoreCase = true) && host == "github.com" &&
        uri.userInfo == null && uri.port in setOf(-1, 443) && uri.query == null && uri.fragment == null &&
        path.startsWith(VELA_PLACES_RELEASE_PREFIX) && archiveName.isNotBlank() &&
        archiveName.split('/').none { it == "." || it == ".." }
}.getOrDefault(false)

internal data class CommonsPlacePhoto(
    val title: String,
    val thumbnailUrl: String,
    val pageUrl: String,
    val author: String,
    val license: String,
)
