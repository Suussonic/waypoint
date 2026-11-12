package com.waypoint.app.offline

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waypoint.app.Ink
import com.waypoint.app.Muted
import com.waypoint.app.Panel
import com.waypoint.app.WaypointColors
import com.waypoint.app.places.OfflinePlacesStore
import com.waypoint.app.places.VelaNearbyPlacesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

private const val MAX_DOWNLOAD_TILES = 3_000L
private const val MAX_DOWNLOAD_BYTES = 50L * 1024L * 1024L
private const val MAX_STYLE_ZOOM = 14
private const val DEVICE_TILE_LIMIT = 30_000L
private const val WEB_MERCATOR_MAX_LAT = 85.05112878
private const val OPENFREEMAP_DOWNLOAD_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/** One visible map view, retained as a rectangular offline region. */
data class OfflineMapArea(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val zoom: Double,
) {
    val valid: Boolean
        get() = south.isFinite() && west.isFinite() && north.isFinite() && east.isFinite() &&
            zoom.isFinite() && south < north && west <= east &&
            south >= -WEB_MERCATOR_MAX_LAT && north <= WEB_MERCATOR_MAX_LAT &&
            west >= -180.0 && east <= 180.0

    val minimumZoom: Int
        get() = (floor(zoom) - 2.0).toInt().coerceIn(0, MAX_STYLE_ZOOM - 2)

    /** Exact OpenFreeMap vector tile count for this box at the zooms that will be saved. */
    val estimatedTiles: Long
        get() {
            if (!valid) return 0L
            return (minimumZoom..MAX_STYLE_ZOOM).sumOf { z ->
                val n = 1L shl z
                val x0 = longitudeTile(west, z)
                val x1 = longitudeTile(east, z)
                val y0 = latitudeTile(north, z)
                val y1 = latitudeTile(south, z)
                (x1 - x0 + 1L).coerceAtLeast(0L) * (y1 - y0 + 1L).coerceAtLeast(0L)
            }
        }

    val centerLabel: String
        get() = String.format(Locale.getDefault(), "%.4f, %.4f", (south + north) / 2.0, (west + east) / 2.0)

    fun bounds(): LatLngBounds = LatLngBounds.from(north, east, south, west)

    private fun longitudeTile(longitude: Double, zoom: Int): Long {
        val n = 1L shl zoom
        return floor((longitude + 180.0) / 360.0 * n).toLong().coerceIn(0L, n - 1L)
    }

    private fun latitudeTile(latitude: Double, zoom: Int): Long {
        val n = 1L shl zoom
        val radians = Math.toRadians(latitude.coerceIn(-WEB_MERCATOR_MAX_LAT, WEB_MERCATOR_MAX_LAT))
        val y = (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0 * n
        return floor(y).toLong().coerceIn(0L, n - 1L)
    }
}

/** The stable style URI shared by phone and projected-car maps and their offline regions. */
internal object OfflineMapStyleFile {
    private fun file(context: Context): File = File(context.filesDir, "offline-map/active-style.json")

    fun uri(context: Context): String = Uri.fromFile(file(context)).toString()

    fun write(context: Context, json: String): String {
        val target = file(context)
        val parent = requireNotNull(target.parentFile)
        if (!parent.isDirectory) check(parent.mkdirs()) { "Couldn't prepare the local map style." }
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
        return Uri.fromFile(target).toString()
    }
}

data class OfflineMapRegion(
    val id: Long,
    val name: String,
    val center: String,
    val complete: Boolean = false,
    val active: Boolean = false,
    val error: String? = null,
    val completedResources: Long = 0L,
    val requiredResources: Long = 0L,
    val progressIsPrecise: Boolean = false,
    val completedTiles: Long = 0L,
    val completedBytes: Long = 0L,
    val placesIncluded: Boolean = false,
    val placesComplete: Boolean = false,
    val placesActive: Boolean = false,
    val placesProgress: Float? = null,
    val placesError: String? = null,
    val placesBytes: Long = 0L,
) {
    val progress: Float?
        get() = if (complete) 1f else if (progressIsPrecise && requiredResources > 0L)
            (completedResources.toFloat() / requiredResources).coerceIn(0f, 1f) else null
}

data class OfflineMapsState(
    val regions: List<OfflineMapRegion> = emptyList(),
    val preparingName: String? = null,
    val notice: String? = null,
)

/** Owns MapLibre's persistent offline-region database for the life of the application process. */
class OfflineMapDownloads(context: Context) {
    private val appContext = context.applicationContext
    private val manager = OfflineManager.getInstance(appContext)
    private val mutableState = MutableStateFlow(OfflineMapsState())
    val state: StateFlow<OfflineMapsState> = mutableState.asStateFlow()
    private val regions = linkedMapOf<Long, OfflineRegion>()
    private val entries = linkedMapOf<Long, OfflineMapRegion>()
    private val sizeLimitStops = mutableSetOf<Long>()
    private val offlinePlaces = OfflinePlacesStore(appContext)
    private val placeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val placeJobs = linkedMapOf<Long, Job>()

    fun refresh() {
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val found = offlineRegions.orEmpty().toList()
                val ids = found.mapTo(hashSetOf()) { it.id }
                regions.keys.retainAll(ids)
                entries.keys.retainAll(ids)
                found.forEach { attach(it) }
                publish()
            }

            override fun onError(error: String) {
                mutableState.value = mutableState.value.copy(notice = "Saved map areas couldn't be loaded. Reopen Offline maps to try again.")
            }
        })
    }

    fun download(area: OfflineMapArea, name: String, includePlaces: Boolean = true) {
        val safeName = name.trim().take(48).ifBlank { "Saved map area" }
        if (!area.valid) {
            message("Move the map away from the date line and try again.")
            return
        }
        if (area.estimatedTiles !in 1L..MAX_DOWNLOAD_TILES) {
            message("This view needs ${area.estimatedTiles} tiles. Zoom in to keep each download within 3,000 tiles.")
            return
        }
        if (!File(appContext.filesDir, "offline-map/active-style.json").exists()) {
            message("Open the map once before saving an offline area.")
            return
        }
        val minZoom = area.minimumZoom.toDouble()
        val maxZoom = MAX_STYLE_ZOOM.toDouble()
        val bounds = area.bounds()
        val definition = OfflineTilePyramidRegionDefinition(
            // OfflineManager resolves its style URL through MapLibre's HTTP resource loader.
            // The phone and car maps render Waypoint's local styles, but their vector tiles and
            // fallback glyphs use these same OpenFreeMap endpoints. Passing our file:// style
            // URI here is not supported by that loader and silently leaves regions at 0 resources.
            OPENFREEMAP_DOWNLOAD_STYLE, bounds, minZoom, maxZoom, 1f,
        )
        val metadata = JSONObject()
            .put(KEY_NAME, safeName)
            .put(KEY_CENTER, area.centerLabel)
            .put(KEY_SOUTH, area.south).put(KEY_WEST, area.west)
            .put(KEY_NORTH, area.north).put(KEY_EAST, area.east)
            .put(KEY_PLACES, includePlaces)
            .toString()
            .toByteArray(Charsets.UTF_8)

        mutableState.value = mutableState.value.copy(preparingName = safeName, notice = null)
        manager.setOfflineMapboxTileCountLimit(DEVICE_TILE_LIMIT)
        manager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                mutableState.value = mutableState.value.copy(preparingName = null)
                attach(offlineRegion)
                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                if (includePlaces) startPlaceDownload(offlineRegion.id, area)
            }

            override fun onError(error: String) {
                mutableState.value = mutableState.value.copy(
                    preparingName = null,
                    notice = "Waypoint couldn't prepare this map area. Check your connection and try again.",
                )
            }
        })
    }

    fun pause(id: Long) {
        regions[id]?.setDownloadState(OfflineRegion.STATE_INACTIVE)
        offlinePlaces.cancelSave(id)
        placeJobs.remove(id)?.cancel()
        entries[id]?.let { entries[id] = it.copy(active = false, error = null, placesActive = false, placesProgress = null) }
        publish()
    }

    fun resume(id: Long) {
        val region = regions[id] ?: return
        entries[id]?.let { entries[id] = it.copy(active = true, error = null) }
        publish()
        attach(region)
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        if (metadataIncludesPlaces(region) && !offlinePlaces.contains(id)) readArea(region)?.let { startPlaceDownload(id, it) }
    }

    fun delete(id: Long) {
        val region = regions[id] ?: return
        offlinePlaces.beginDelete(id)
        placeJobs.remove(id)?.cancel()
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                regions.remove(id)
                entries.remove(id)
                sizeLimitStops.remove(id)
                offlinePlaces.remove(id)
                publish()
                packDatabase()
            }

            override fun onError(error: String) {
                offlinePlaces.deleteFailed(id)
                message("Waypoint couldn't remove this map area. Try again.")
            }
        })
    }

    fun clearNotice() {
        mutableState.value = mutableState.value.copy(notice = null)
    }

    private fun attach(region: OfflineRegion) {
        regions[region.id] = region
        val hasPlaces = offlinePlaces.contains(region.id)
        val includesPlaces = metadataIncludesPlaces(region)
        val current = entries[region.id] ?: OfflineMapRegion(
            id = region.id,
            name = metadataName(region),
            center = metadataCenter(region),
            active = false,
            placesIncluded = includesPlaces,
            placesComplete = hasPlaces,
            placesError = if (includesPlaces && !hasPlaces) "Place download interrupted. Resume this area to try again." else null,
            placesBytes = offlinePlaces.bytes(region.id),
        )
        entries[region.id] = current.copy(placesIncluded = includesPlaces, placesComplete = hasPlaces, placesBytes = offlinePlaces.bytes(region.id))
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if ((status.completedResourceSize > MAX_DOWNLOAD_BYTES || status.completedTileCount > MAX_DOWNLOAD_TILES) &&
                    !sizeLimitStops.contains(region.id)) {
                    sizeLimitStops += region.id
                    stopForSizeLimit(region)
                    return
                }
                entries[region.id] = entryFrom(region, status)
                publish()
            }

            override fun onError(error: OfflineRegionError) {
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                entries[region.id]?.let {
                    entries[region.id] = it.copy(active = false, error = "Download paused. Check your connection, then resume it.")
                }
                publish()
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                entries[region.id]?.let {
                    entries[region.id] = it.copy(active = false, error = "Device map storage is full. Remove an area and try again.")
                }
                publish()
            }
        })
        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
            override fun onStatus(status: OfflineRegionStatus?) {
                if (status == null) return
                entries[region.id] = entryFrom(region, status)
                publish()
            }

            override fun onError(error: String?) = Unit
        })
    }

    private fun entryFrom(region: OfflineRegion, status: OfflineRegionStatus): OfflineMapRegion {
        val previous = entries[region.id] ?: OfflineMapRegion(region.id, metadataName(region), metadataCenter(region))
        val placesComplete = offlinePlaces.contains(region.id)
        return previous.copy(
            name = metadataName(region), center = metadataCenter(region), complete = status.isComplete,
            active = status.downloadState == OfflineRegion.STATE_ACTIVE && !status.isComplete,
            completedResources = status.completedResourceCount, requiredResources = status.requiredResourceCount,
            progressIsPrecise = status.isRequiredResourceCountPrecise, completedTiles = status.completedTileCount,
            completedBytes = status.completedResourceSize, placesIncluded = metadataIncludesPlaces(region),
            placesComplete = placesComplete, placesBytes = offlinePlaces.bytes(region.id),
        )
    }

    private fun startPlaceDownload(id: Long, area: OfflineMapArea) {
        if (placeJobs[id]?.isActive == true || offlinePlaces.contains(id)) return
        val saveToken = offlinePlaces.beginSave(id) ?: return
        entries[id]?.let { entries[id] = it.copy(placesIncluded = true, placesActive = true, placesProgress = 0f, placesError = null) }
        publish()
        val job = placeScope.launch(start = CoroutineStart.LAZY) {
            val ownedJob = coroutineContext[Job]
            try {
                val places = VelaNearbyPlacesRepository().downloadArea(area.south, area.west, area.north, area.east) { completed, total ->
                    withContext(Dispatchers.Main.immediate) {
                        entries[id]?.let { entries[id] = it.copy(placesActive = true, placesProgress = (completed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)) }
                        publish()
                    }
                }
                val bytes = offlinePlaces.save(id, saveToken, OfflinePlacesStore.Bounds(area.south, area.west, area.north, area.east), places)
                entries[id]?.let {
                    entries[id] = it.copy(placesIncluded = true, placesComplete = true, placesActive = false, placesProgress = 1f, placesError = null, placesBytes = bytes)
                }
                publish()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                entries[id]?.let {
                    entries[id] = it.copy(placesIncluded = true, placesActive = false, placesProgress = null,
                        placesError = error.message?.take(180) ?: "Open places couldn't be saved. Resume the area to try again.")
                }
                publish()
            } finally {
                offlinePlaces.cancelSave(id, saveToken)
                if (placeJobs[id] === ownedJob) placeJobs.remove(id)
            }
        }
        placeJobs[id] = job
        job.start()
    }

    private fun readArea(region: OfflineRegion): OfflineMapArea? = runCatching {
        val json = JSONObject(String(region.metadata, Charsets.UTF_8))
        OfflineMapArea(
            south = json.optDouble(KEY_SOUTH, Double.NaN), west = json.optDouble(KEY_WEST, Double.NaN),
            north = json.optDouble(KEY_NORTH, Double.NaN), east = json.optDouble(KEY_EAST, Double.NaN),
            zoom = 14.0,
        ).takeIf { it.valid }
    }.getOrNull()

    private fun metadataIncludesPlaces(region: OfflineRegion): Boolean = runCatching {
        JSONObject(String(region.metadata, Charsets.UTF_8)).optBoolean(KEY_PLACES, false)
    }.getOrDefault(false)

    private fun stopForSizeLimit(region: OfflineRegion) {
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        offlinePlaces.beginDelete(region.id)
        offlinePlaces.cancelSave(region.id)
        placeJobs.remove(region.id)?.cancel()
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                regions.remove(region.id)
                entries.remove(region.id)
                publish()
                offlinePlaces.remove(region.id)
                mutableState.value = mutableState.value.copy(
                    notice = "This area exceeded OpenFreeMap's 3,000 tile or 50 MB download limit. Zoom in and try again.",
                )
                packDatabase()
            }

            override fun onError(error: String) {
                offlinePlaces.deleteFailed(region.id)
                entries[region.id]?.let {
                    entries[region.id] = it.copy(active = false, error = "This area exceeded the 3,000 tile or 50 MB limit. Remove it and choose a smaller view.")
                }
                publish()
            }
        })
    }

    private fun packDatabase() {
        runCatching {
            manager.packDatabase(object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit
                override fun onError(message: String) = Unit
            })
        }
    }

    private fun metadataName(region: OfflineRegion): String = runCatching {
        JSONObject(String(region.metadata, Charsets.UTF_8)).optString(KEY_NAME)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Saved map area"

    private fun metadataCenter(region: OfflineRegion): String = runCatching {
        JSONObject(String(region.metadata, Charsets.UTF_8)).optString(KEY_CENTER)
    }.getOrNull().orEmpty()

    private fun publish() {
        mutableState.value = mutableState.value.copy(regions = entries.values.sortedByDescending { it.id })
    }

    private fun message(text: String) {
        mutableState.value = mutableState.value.copy(notice = text)
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_CENTER = "center"
        const val KEY_SOUTH = "south"
        const val KEY_WEST = "west"
        const val KEY_NORTH = "north"
        const val KEY_EAST = "east"
        const val KEY_PLACES = "includePlaces"
    }
}

@Composable
fun OfflineMapsPanel(
    state: OfflineMapsState,
    canChooseArea: Boolean,
    onBack: () -> Unit,
    onChooseArea: () -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var deleteRegion by remember { mutableStateOf<OfflineMapRegion?>(null) }
    val totalBytes = state.regions.sumOf { it.completedBytes + it.placesBytes }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to settings")
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Offline maps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${state.regions.size} saved areas · ${formatMapBytes(totalBytes)}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }

        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Take the map with you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Save map streets and open place names for areas you choose.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                FilledTonalButton(
                    onClick = onChooseArea,
                    enabled = canChooseArea,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(9.dp))
                    Text("Choose an area", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Text("Your saved areas", style = MaterialTheme.typography.titleMedium, color = WaypointColors.accent)
        AnimatedVisibility(
            visible = state.preparingName != null,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(100)),
        ) {
            Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Preparing ${state.preparingName.orEmpty()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Setting up the map download…", style = MaterialTheme.typography.bodySmall, color = Muted)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        AnimatedVisibility(
            visible = state.notice != null,
            enter = fadeIn(tween(160)) + expandVertically(tween(160)),
            exit = fadeOut(tween(90)) + shrinkVertically(tween(90)),
        ) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(state.notice.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
            }
        }
        if (state.regions.isEmpty() && state.preparingName == null) {
            Surface(color = Panel.copy(alpha = .72f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nothing saved yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Choose an area to keep its map streets and open place names on this device.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
        }
        state.regions.forEachIndexed { index, region ->
            OfflineRegionCard(region, onPause, onResume) { deleteRegion = region }
            if (index != state.regions.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
        if (state.regions.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Map downloads are limited to 3,000 tiles or 50 MB per area. Place snapshots use up to 10 MB per area and 250 MB total. Routes, photos and live business details still need internet.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        Spacer(Modifier.height(18.dp))
    }
    deleteRegion?.let { region ->
        AlertDialog(
            onDismissRequest = { deleteRegion = null },
            title = { Text("Remove ${region.name}?") },
            text = { Text("Its saved map tiles and open place names will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { onDelete(region.id); deleteRegion = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Remove")
                }
            },
            dismissButton = { TextButton(onClick = { deleteRegion = null }) { Text("Keep area") } },
        )
    }
}

@Composable
private fun OfflineRegionCard(
    region: OfflineMapRegion,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    val isWorking = region.active || region.placesActive
    val hasMoreToSave = !region.complete || region.placesIncluded && !region.placesComplete
    val ready = region.complete && (!region.placesIncluded || region.placesComplete)
    val status = when {
        region.error != null || region.placesError != null -> "Needs attention"
        ready -> "Ready offline"
        region.complete -> "Map ready"
        isWorking -> "Downloading"
        else -> "Paused"
    }
    val statusContainer = when {
        region.error != null || region.placesError != null -> MaterialTheme.colorScheme.errorContainer
        ready -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val statusInk = when {
        region.error != null || region.placesError != null -> MaterialTheme.colorScheme.onErrorContainer
        ready -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(region.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (region.center.isNotBlank()) {
                        Text(region.center, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(color = statusContainer, shape = RoundedCornerShape(50)) {
                    Text(status, color = statusInk, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
                }
                IconButton(onClick = onDelete, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Remove ${region.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!region.complete) {
                RegionProgressLine(
                    label = "Map data", progress = region.progress,
                    progressLabel = "${region.name} map download progress",
                    detail = "${region.completedTiles} tiles saved · ${formatMapBytes(region.completedBytes)}",
                )
            }
            if (region.placesIncluded) {
                when {
                    region.placesActive -> RegionProgressLine(
                        label = "Open place names", progress = region.placesProgress,
                        progressLabel = "${region.name} open place download progress",
                        detail = "Saving place names for offline search",
                    )
                    region.placesComplete -> Text(
                        "Open place names are ready for offline search · ${formatMapBytes(region.placesBytes)}",
                        style = MaterialTheme.typography.bodySmall, color = Muted,
                    )
                    region.placesError != null -> OfflineInlineError(region.placesError)
                    else -> Text("Open place names will download with the map.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            region.error?.let { OfflineInlineError(it) }
            if (hasMoreToSave) {
                FilledTonalButton(
                    onClick = { if (isWorking) onPause(region.id) else onResume(region.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(if (isWorking) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(when {
                        isWorking -> "Pause download"
                        region.placesError != null && region.complete -> "Retry place names"
                        region.error != null -> "Retry map download"
                        else -> "Resume download"
                    })
                }
            }
        }
    }
}

@Composable
private fun RegionProgressLine(label: String, progress: Float?, progressLabel: String, detail: String) {
    val target = progress?.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target ?: 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "offline-region-progress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            target?.let { Text("${(it * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        }
        if (target == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = progressLabel })
        } else {
            LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = progressLabel })
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OfflineInlineError(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp))
    }
}

@Composable
fun OfflineMapAreaPicker(
    area: OfflineMapArea?,
    name: String,
    includePlaces: Boolean,
    notice: String?,
    onNameChange: (String) -> Unit,
    onIncludePlacesChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileCount = area?.estimatedTiles ?: 0L
    val tooLarge = tileCount > MAX_DOWNLOAD_TILES
    val valid = area?.valid == true && tileCount in 1L..MAX_DOWNLOAD_TILES
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelMaxHeight = (maxHeight - 60.dp).coerceAtLeast(180.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Ink.copy(alpha = 0.96f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to offline maps")
                    }
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(9.dp).size(22.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Save an offline area", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Frame the map, then choose what to keep", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = Panel.copy(alpha = 0.98f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = panelMaxHeight),
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                Text("Area details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            when {
                                tooLarge -> "Zoom in to save this area"
                                area == null -> "Choose an area on the map"
                                !area.valid -> "Adjust the map bounds"
                                else -> "$tileCount map tiles estimated"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tooLarge || area?.valid == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            when {
                                tooLarge -> "Keep the selection under 3,000 tiles."
                                area == null -> "Pan or zoom until the map shows the streets you need."
                                !area.valid -> "Move the selection away from the date line and try again."
                                else -> "${area.centerLabel} · OpenFreeMap streets and labels"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it.take(48)) },
                    label = { Text("Area name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .toggleable(value = includePlaces, role = Role.Checkbox, onValueChange = onIncludePlacesChange)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = includePlaces, onCheckedChange = null)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("Include open place names", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Search these names offline across saved regions. Live Google suggestions need a connection and are not included in map downloads.",
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                AnimatedVisibility(
                    visible = notice != null,
                    enter = fadeIn(tween(160)) + expandVertically(tween(160)),
                    exit = fadeOut(tween(90)) + shrinkVertically(tween(90)),
                ) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(notice.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
                Text(
                    "Map limit: 3,000 tiles or 50 MB per area. Open place data saves up to 10 MB; live suggestions, photos and routes need internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    FilledTonalButton(
                        onClick = onSave,
                        enabled = valid && name.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(9.dp))
                        Text(if (valid) "Download area" else "Choose an area")
                    }
                }
            }
        }
    }
}
}

private fun formatMapBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
