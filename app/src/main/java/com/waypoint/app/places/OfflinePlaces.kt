package com.waypoint.app.places

import android.content.Context
import android.util.AtomicFile
import com.waypoint.app.Coordinate
import com.waypoint.app.Place
import com.waypoint.app.PlaceMatcher
import com.waypoint.app.PlaceSearchProvider
import com.waypoint.app.distance
import com.waypoint.app.isValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

private const val MAX_OFFLINE_PLACE_PACK_BYTES = 10 * 1024 * 1024
private const val MAX_OFFLINE_PLACE_PACKS = 100
private const val MAX_OFFLINE_PLACES_PER_PACK = 5_000
private const val MAX_OFFLINE_PLACE_INDEX_BYTES = 256 * 1024
private const val MAX_TOTAL_OFFLINE_PLACE_BYTES = 250L * 1024L * 1024L
internal const val DOWNLOADED_PLACE_SOURCE_PREFIX = "Downloaded map places ·"

/** Keeps the open-place records intersecting a user-selected offline map area on this device. */
internal class OfflinePlacesStore(
    context: Context,
    private val beforeCommit: suspend () -> Unit = {},
) {
    private val directory = File(context.applicationContext.filesDir, "offline-map/places")
    private val indexFile = File(directory, "index.json")

    internal class SaveToken internal constructor(internal val identity: Any)

    private companion object {
        // Every store instance in the app shares this index and pack directory.
        private val storageLock = Any()
        private val activeSaveTokens = mutableMapOf<Long, SaveToken>()
        private val deletingRegions = mutableSetOf<Long>()
    }

    data class Bounds(val south: Double, val west: Double, val north: Double, val east: Double) {
        fun contains(point: Coordinate) = point.lat in south..north && point.lon in west..east
        fun valid() = south.isFinite() && west.isFinite() && north.isFinite() && east.isFinite() &&
            south >= -90.0 && north <= 90.0 && west >= -180.0 && east <= 180.0 && south < north && west <= east
    }

    /** Begins one cancellable snapshot generation. A later generation invalidates this token. */
    fun beginSave(regionId: Long): SaveToken? = synchronized(storageLock) {
        require(regionId >= 0) { "This offline place area is invalid." }
        if (regionId in deletingRegions) return@synchronized null
        SaveToken(Any()).also { activeSaveTokens[regionId] = it }
    }

    /** Invalidates any save already in flight without blocking a later resume. */
    fun cancelSave(regionId: Long, token: SaveToken? = null) {
        synchronized(storageLock) {
            if (token == null || activeSaveTokens[regionId] === token) activeSaveTokens.remove(regionId)
        }
    }

    /** Invalidates writes before the asynchronous map-region deletion begins. */
    fun beginDelete(regionId: Long) {
        synchronized(storageLock) {
            deletingRegions += regionId
            activeSaveTokens.remove(regionId)
        }
    }

    /** A failed map-region deletion leaves the saved area available for a later resume. */
    fun deleteFailed(regionId: Long) {
        synchronized(storageLock) { deletingRegions.remove(regionId) }
    }

    suspend fun save(regionId: Long, token: SaveToken, bounds: Bounds, places: List<NearbyPlace>): Long = try {
        withContext(Dispatchers.IO) {
            require(regionId >= 0 && bounds.valid()) { "This offline place area is invalid." }
            require(places.size <= MAX_OFFLINE_PLACES_PER_PACK) { "This area contains too many places to save." }
            val safePlaces = places.asSequence().filter { it.point.isValid() && bounds.contains(it.point) && it.name.isNotBlank() }
                .distinctBy { it.id }.take(MAX_OFFLINE_PLACES_PER_PACK + 1).toList()
            require(safePlaces.size <= MAX_OFFLINE_PLACES_PER_PACK) { "This area contains too many places to save." }

            val payload = JSONObject()
                .put("version", 1)
                .put("bounds", boundsJson(bounds))
                .put("places", JSONArray().also { array -> safePlaces.forEach { array.put(placeJson(it)) } })
                .toString().toByteArray(Charsets.UTF_8)
            require(payload.size <= MAX_OFFLINE_PLACE_PACK_BYTES) { "Place data for this area is larger than 10 MB. Zoom in and try again." }
            beforeCommit()
            synchronized(storageLock) {
                if (regionId in deletingRegions || activeSaveTokens[regionId] !== token) {
                    throw CancellationException("This offline place snapshot was cancelled.")
                }
                if (!directory.isDirectory) check(directory.mkdirs()) { "Waypoint couldn't prepare storage for offline places." }

                val entries = readIndex().filterNot { it.first == regionId }.toMutableList()
                require(entries.size < MAX_OFFLINE_PLACE_PACKS) { "Remove an offline area before saving another place pack." }
                val usedBytes = entries.sumOf { packFile(it.first).length() }
                require(payload.size.toLong() <= MAX_TOTAL_OFFLINE_PLACE_BYTES - usedBytes) {
                    "Offline place storage is full. Remove an area and try again."
                }

                val target = packFile(regionId)
                writeAtomically(target, payload)
                try {
                    entries += regionId to bounds
                    writeIndex(entries)
                } catch (error: Throwable) {
                    target.delete()
                    throw error
                }
                payload.size.toLong()
            }
        }
    } finally {
        synchronized(storageLock) {
            if (activeSaveTokens[regionId] === token) activeSaveTokens.remove(regionId)
        }
    }

    fun remove(regionId: Long) {
        synchronized(storageLock) {
            activeSaveTokens.remove(regionId)
            packFile(regionId).delete()
            runCatching { writeIndex(readIndex().filterNot { it.first == regionId }) }
            deletingRegions.remove(regionId)
        }
    }

    fun contains(regionId: Long): Boolean = synchronized(storageLock) {
        regionId !in deletingRegions && readIndex().any { it.first == regionId }
    }

    fun bytes(regionId: Long): Long = synchronized(storageLock) {
        if (regionId !in deletingRegions && readIndex().any { it.first == regionId }) packFile(regionId).length() else 0L
    }

    /** Null means the point is not covered by a saved place pack; an empty list means it is. */
    fun nearby(center: Coordinate, category: NearbyCategory, want: Int = 36): List<NearbyPlace>? = synchronized(storageLock) {
        if (!center.isValid()) return@synchronized null
        val packs = readIndex().filter { it.first !in deletingRegions && it.second.contains(center) }
        if (packs.isEmpty()) return@synchronized null
        val records = packs.flatMap { (id, _) -> readPack(id) }
        records.asSequence().filter { category.matches(it) && distance(center, it.point) <= 6_000.0 }
            .distinctBy { it.id }
            .sortedBy { distance(center, it.point) }
            .take(want.coerceIn(1, 100))
            .toList()
    }

    /** Search every downloaded place pack locally; location only affects result ordering. */
    fun search(query: String, near: Coordinate? = null): List<Place> = synchronized(storageLock) {
        if (query.isBlank()) return@synchronized emptyList()
        val packs = readIndex().filterNot { it.first in deletingRegions }
        if (packs.isEmpty()) return@synchronized emptyList()
        val rankingCenter = near?.takeIf { it.isValid() }
        var bestMatches = emptyList<Place>()
        for ((id, _) in packs) {
            // Keep memory bounded to one decoded pack plus the twelve global results.
            val packMatches = readPack(id).map { place ->
                place.asWaypointPlace().copy(source = "$DOWNLOADED_PLACE_SOURCE_PREFIX ${place.origin}")
            }
            bestMatches = PlaceMatcher.ranked(query, rankingCenter, bestMatches + packMatches)
        }
        bestMatches
    }

    fun covers(point: Coordinate): Boolean = synchronized(storageLock) {
        point.isValid() && readIndex().any { it.first !in deletingRegions && it.second.contains(point) }
    }

    private fun readPack(id: Long): List<NearbyPlace> = runCatching {
        val file = packFile(id)
        if (!file.isFile || file.length() !in 1..MAX_OFFLINE_PLACE_PACK_BYTES.toLong()) return emptyList()
        val json = JSONObject(file.readBytes().toString(Charsets.UTF_8))
        if (json.optInt("version") != 1) return emptyList()
        val items = json.optJSONArray("places") ?: return emptyList()
        if (items.length() > MAX_OFFLINE_PLACES_PER_PACK) return emptyList()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val point = Coordinate(item.optDouble("lat", Double.NaN), item.optDouble("lon", Double.NaN))
                val name = item.optString("name").take(160)
                if (name.isBlank() || !point.isValid()) continue
                add(
                    NearbyPlace(
                        id = item.optString("id").take(256).ifBlank { "offline:$id:$i" },
                        name = name,
                        point = point,
                        kind = item.optString("kind").take(80).ifBlank { "Place" },
                        group = item.optString("group").take(40).ifBlank { "default" },
                        address = item.optString("address").take(240),
                        phone = item.optString("phone").take(80).takeIf(String::isNotBlank),
                        website = item.optString("website").take(300).takeIf(String::isNotBlank),
                        hours = item.optString("hours").take(240).takeIf(String::isNotBlank),
                        origin = item.optString("origin").take(40).ifBlank { "OpenStreetMap" },
                        prominence = item.optDouble("prominence", 0.0).takeIf(Double::isFinite) ?: 0.0,
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun readIndex(): List<Pair<Long, Bounds>> = runCatching {
        if (!indexFile.isFile || indexFile.length() !in 1..MAX_OFFLINE_PLACE_INDEX_BYTES.toLong()) return emptyList()
        val root = JSONObject(indexFile.readBytes().toString(Charsets.UTF_8))
        if (root.optInt("version") != 1) return emptyList()
        val items = root.optJSONArray("packs") ?: return emptyList()
        if (items.length() > MAX_OFFLINE_PLACE_PACKS) return emptyList()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optLong("id", -1L)
                val bounds = Bounds(
                    item.optDouble("south", Double.NaN), item.optDouble("west", Double.NaN),
                    item.optDouble("north", Double.NaN), item.optDouble("east", Double.NaN),
                )
                if (id >= 0L && bounds.valid() && packFile(id).isFile) add(id to bounds)
            }
        }
    }.getOrDefault(emptyList())

    private fun writeIndex(entries: List<Pair<Long, Bounds>>) {
        val payload = JSONObject().put("version", 1).put("packs", JSONArray().also { array ->
            entries.take(MAX_OFFLINE_PLACE_PACKS).forEach { (id, bounds) ->
                array.put(boundsJson(bounds).put("id", id))
            }
        }).toString().toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_OFFLINE_PLACE_INDEX_BYTES) { "Waypoint couldn't update its offline place index." }
        writeAtomically(indexFile, payload)
    }

    private fun boundsJson(bounds: Bounds) = JSONObject()
        .put("south", bounds.south).put("west", bounds.west).put("north", bounds.north).put("east", bounds.east)

    private fun placeJson(place: NearbyPlace) = JSONObject()
        .put("id", place.id.take(256)).put("name", place.name.take(160))
        .put("lat", place.point.lat).put("lon", place.point.lon)
        .put("kind", place.kind.take(80)).put("group", place.group.take(40))
        .put("address", place.address.take(240)).put("phone", place.phone?.take(80))
        .put("website", place.website?.take(300)).put("hours", place.hours?.take(240))
        .put("origin", place.origin.take(40)).put("prominence", place.prominence)

    private fun packFile(id: Long) = File(directory, "$id.json")

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }
}

/** Searches downloaded place names before network providers so saved regions work without signal. */
internal class OfflineFirstPlaceSearchProvider(
    context: Context,
    private val upstream: PlaceSearchProvider,
) : PlaceSearchProvider {
    private val offline = OfflinePlacesStore(context)
    private val vela = VelaNearbyPlacesRepository(
        client = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build(),
    )

    override suspend fun find(query: String, near: Coordinate): List<Place> = withContext(Dispatchers.IO) {
        val local = offline.search(query, near)
        if (local.isNotEmpty()) return@withContext local
        suspend fun findSafely(provider: PlaceSearchProvider): Result<List<Place>> = try {
            Result.success(provider.find(query, near).take(40))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
        val (standard, openPlaces) = coroutineScope {
            val standardSearch = async { findSafely(upstream) }
            val velaSearch = async { findSafely(PlaceSearchProvider(vela::search)) }
            standardSearch.await() to velaSearch.await()
        }
        if (standard.isFailure && openPlaces.isFailure) {
            throw standard.exceptionOrNull() ?: openPlaces.exceptionOrNull() ?: IllegalStateException("Place search failed.")
        }
        (standard.getOrDefault(emptyList()) + openPlaces.getOrDefault(emptyList()))
            .distinctBy { it.id }
            .take(40)
    }
}
