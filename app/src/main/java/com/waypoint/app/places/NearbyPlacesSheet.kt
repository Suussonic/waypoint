package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import com.waypoint.app.places.CommonsPlacePhoto
import com.waypoint.app.places.CommonsPlacePhotoRepository
import com.waypoint.app.places.GoogleMapPhotoFetcher
import com.waypoint.app.places.GooglePlaceDetails
import com.waypoint.app.places.GooglePlaceLookup
import com.waypoint.app.places.NearbyCategory
import com.waypoint.app.places.NearbyPlace
import com.waypoint.app.places.OfflinePlacesStore
import com.waypoint.app.places.VelaNearbyPlacesRepository
import com.waypoint.app.places.readBoundedBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.net.URI
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NearbyPlacesSheet(
    state: NavState,
    center: Coordinate?,
    googleEnabled: Boolean,
    commonsPhotosEnabled: Boolean,
    savedIds: Set<String>,
    onGoogleEnabledChange: (Boolean) -> Unit,
    onCommonsPhotosEnabledChange: (Boolean) -> Unit,
    onPlacesChanged: (List<NearbyPlace>) -> Unit,
    onSelect: (NearbyPlace) -> Unit,
    onSave: (NearbyPlace) -> Unit,
    onLocate: () -> Unit,
    onClose: () -> Unit,
    initialSelection: NearbyPlace? = null,
    onSelectionDismissed: () -> Unit = {},
) {
    val world = state.world
    val palette = hudPalette(world)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offlinePlaces = remember(context.applicationContext) { OfflinePlacesStore(context.applicationContext) }
    val placesRepository = remember(offlinePlaces) { VelaNearbyPlacesRepository(offlinePlaces = offlinePlaces) }
    val commonsPhotos = remember { CommonsPlacePhotoRepository() }
    val googleLookup = remember { GooglePlaceLookup() }
    val googlePhotos = remember(context) { GoogleMapPhotoFetcher(context) }
    var category by remember { mutableStateOf(NearbyCategory.ALL) }
    var results by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<NearbyPlace?>(null) }
    var googleDetails by remember { mutableStateOf<GooglePlaceDetails?>(null) }
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }
    var gallery by remember { mutableStateOf<List<String>>(emptyList()) }
    var galleryLoading by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(initialSelection?.id) {
        selected = initialSelection
    }

    LaunchedEffect(center, category, refresh) {
        if (center == null) {
            results = emptyList()
            onPlacesChanged(emptyList())
            error = null
            loading = false
        } else {
            loading = true
            error = null
            try {
                val found = placesRepository.nearby(center, category)
                results = found
                onPlacesChanged(found)
                if (found.isEmpty()) error = "No ${category.title.lowercase()} found within 6 km. Try another category."
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                results = emptyList()
                onPlacesChanged(emptyList())
                error = e.message?.take(160) ?: "Nearby places could not be loaded. Check your connection and try again."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selected?.id, googleEnabled) {
        val place = selected
        googleDetails = null
        gallery = emptyList()
        googleError = null
        galleryLoading = false
        if (place == null || !googleEnabled) {
            googleLoading = false
            return@LaunchedEffect
        }
        googleLoading = true
        try {
            googleDetails = googleLookup.details(place)
            if (googleDetails == null) googleError = "Google did not return a confidently matching listing. OpenStreetMap place data is still available."
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            googleError = e.message?.take(160) ?: "Google details could not be loaded. Try again later."
        } finally {
            googleLoading = false
        }
    }

    LaunchedEffect(selected?.id, googleDetails?.featureId, googleEnabled) {
        val details = googleDetails ?: return@LaunchedEffect
        if (!googleEnabled) return@LaunchedEffect
        galleryLoading = true
        try {
            gallery = googlePhotos.photos(details)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            gallery = details.previewPhotos
        } finally {
            galleryLoading = false
        }
    }

    DisposableEffect(googlePhotos) {
        onDispose { scope.launch { googlePhotos.close() } }
    }

    ModalBottomSheet(
        onDismissRequest = { onSelectionDismissed(); onClose() },
        containerColor = palette.panel,
        contentColor = palette.foreground,
        scrimColor = Color.Black.copy(alpha = .58f),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 2.dp).size(width = 36.dp, height = 4.dp)
                .clip(CircleShape).background(palette.muted.copy(alpha = .55f)))
        },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 760.dp).navigationBarsPadding()) {
            if (selected == null) {
                NearbyHeader(state, center, palette, onClose)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NearbyCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.title, fontFamily = hudFont(world), maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent,
                                selectedLabelColor = palette.panel,
                                containerColor = palette.panel.copy(alpha = .7f),
                                labelColor = palette.foreground,
                            ),
                        )
                    }
                }
                PlacePhotoPrivacyRow(
                    enabled = commonsPhotosEnabled,
                    p = palette,
                    onEnabledChange = onCommonsPhotosEnabledChange,
                )
                if (googleEnabled) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = palette.accent)
                        Text(
                            "Suggestions run in Search while you type. Here, Google loads details and photos only when you open a place, and receives that place’s name and coordinates.",
                            fontSize = 12.sp,
                            color = palette.muted,
                        )
                    }
                }
                when {
                    center == null -> NearbyEmpty(
                        world, palette, Icons.Default.Place, "Map area unavailable",
                        "Move the map to an area, or use your location to explore nearby places.",
                        onAction = onLocate,
                    )
                    loading -> NearbyLoadingRows(world, palette)
                    error != null && results.isEmpty() -> NearbyEmpty(
                        world, palette, if (category == NearbyCategory.ALL) Icons.Default.CloudOff else Icons.Default.Explore,
                        if (error?.startsWith("No ") == true) "Nothing nearby yet" else "Nearby places are unavailable",
                        error ?: "Try again.",
                        onAction = if (error?.startsWith("No ") == true) null else ({ refresh++ }),
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                    ) {
                        items(results, key = { it.id }) { place ->
                            NearbyPlaceRow(
                                place = place,
                                center = center,
                                state = state,
                                photoRepository = commonsPhotos,
                                photoEnabled = commonsPhotosEnabled,
                                saved = place.id in savedIds,
                                onOpen = { selected = place },
                                onRoute = { onSelect(place) },
                                onSave = { onSave(place) },
                            )
                        }
                    }
                }
                Text(
                    "Places: Overture · AllThePlaces · OpenStreetMap. Optional Wikimedia Commons photos are credited individually when enabled.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp,
                )
            } else {
                NearbyPlaceDetail(
                    place = selected!!,
                    center = center,
                    state = state,
                    googleEnabled = googleEnabled,
                    commonsPhotosEnabled = commonsPhotosEnabled,
                    details = googleDetails,
                    loading = googleLoading,
                    error = googleError,
                    photos = gallery,
                    photosLoading = galleryLoading,
                    saved = selected!!.id in savedIds,
                    palette = palette,
                    photoRepository = commonsPhotos,
                    onCommonsPhotosEnabledChange = onCommonsPhotosEnabledChange,
                    onBack = { selected = null; onSelectionDismissed() },
                    onGoogleEnabledChange = onGoogleEnabledChange,
                    onRoute = { onSelect(selected!!) },
                    onSave = { onSave(selected!!) },
                    onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
                )
            }
        }
    }
}

@Composable
private fun NearbyHeader(state: NavState, center: Coordinate?, p: HudPalette, onClose: () -> Unit) {
    val locationLabel = when {
        center == null -> "Map area unavailable"
        state.fix != null && distance(center, state.fix) <= 6_000.0 && state.city.isNotBlank() -> "Around ${state.city}"
        state.fix != null && distance(center, state.fix) <= 6_000.0 -> "Around your location"
        else -> "Around the map view"
    }
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = onClose, modifier = Modifier.size(48.dp).semantics { contentDescription = "Close nearby places" },
            color = p.panel.copy(alpha = .5f), contentColor = p.foreground, shape = hudShape(state.world), border = hudBorder(state.world)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        }
        Column(Modifier.weight(1f)) {
            Text("Explore nearby", fontFamily = worldFont(state.world), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall, color = p.foreground)
            Text(locationLabel,
                fontFamily = hudFont(state.world), color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("6 km", color = p.accent, fontFamily = hudFont(state.world), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun PlacePhotoPrivacyRow(
    enabled: Boolean,
    p: HudPalette,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp).heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Wikimedia Commons photos", color = p.foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${if (enabled) "On" else "Off by default"}. Photo searches send place coordinates and your network address to Wikimedia Commons.",
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Allow Wikimedia Commons photo searches; shares place coordinates and network address"
            },
        )
    }
}

@Composable
private fun NearbyPlaceRow(
    place: NearbyPlace,
    center: Coordinate?,
    state: NavState,
    photoRepository: CommonsPlacePhotoRepository,
    photoEnabled: Boolean,
    saved: Boolean,
    onOpen: () -> Unit,
    onRoute: () -> Unit,
    onSave: () -> Unit,
) {
    val p = hudPalette(state.world)
    var photo by remember(place.id) { mutableStateOf<CommonsPlacePhoto?>(null) }
    LaunchedEffect(place.id, photoEnabled) {
        photo = null
        if (photoEnabled) photo = runCatching { photoRepository.photo(place) }.getOrNull()
    }
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)
        .semantics(mergeDescendants = true) { contentDescription = "${place.name}, ${place.kind}, ${place.address}" }
        .padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.width(98.dp).aspectRatio(1.08f).clip(hudShape(state.world))) {
            RemotePlaceImage(photo?.thumbnailUrl, place.name, Modifier.fillMaxSize()) {
                PlaceImageFallback(state.world, place.group, Modifier.fillMaxSize())
            }
            if (photo != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f))))
                    .padding(horizontal = 5.dp, vertical = 4.dp)) {
                    Text("${photo?.author?.ifBlank { "Commons" } ?: "Commons"} · ${photo?.license.orEmpty()}", color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(place.name, color = p.foreground, fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold,
                fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(place.kind, color = p.accent, fontFamily = hudFont(state.world), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.address.isNotBlank()) Text(place.address, color = p.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(center?.let { formatDistance(distance(it, place.point), state.imperial) } ?: "Nearby",
                    color = p.muted, fontSize = 11.sp)
                Text("·", color = p.muted)
                Text(place.origin, color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRoute, modifier = Modifier.heightIn(min = 44.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Directions, null, Modifier.size(16.dp), tint = p.accent)
                    Spacer(Modifier.width(4.dp)); Text("Route", color = p.accent, fontSize = 12.sp)
                }
                IconButton(onClick = onSave, modifier = Modifier.size(44.dp)) {
                    Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        if (saved) "Remove ${place.name} from saved places" else "Save ${place.name}", tint = if (saved) p.accent else p.muted)
                }
            }
        }
    }
    HorizontalDivider(color = p.muted.copy(alpha = .22f))
}

@Composable
private fun NearbyPlaceDetail(
    place: NearbyPlace,
    center: Coordinate?,
    state: NavState,
    googleEnabled: Boolean,
    commonsPhotosEnabled: Boolean,
    details: GooglePlaceDetails?,
    loading: Boolean,
    error: String?,
    photos: List<String>,
    photosLoading: Boolean,
    saved: Boolean,
    palette: HudPalette,
    photoRepository: CommonsPlacePhotoRepository,
    onCommonsPhotosEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoogleEnabledChange: (Boolean) -> Unit,
    onRoute: () -> Unit,
    onSave: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val p = palette
    val dist = center?.let { formatDistance(distance(it, place.point), state.imperial) }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp, top = 7.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = "Back to nearby places" },
                    color = p.panel, contentColor = p.foreground, shape = hudShape(state.world), border = hudBorder(state.world)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
                Spacer(Modifier.width(12.dp))
                Text("Place details", fontFamily = hudFont(state.world), fontSize = 16.sp, color = p.muted)
            }
        }
        item {
            val displayPhotos = when {
                photos.isNotEmpty() -> photos
                details?.previewPhotos?.isNotEmpty() == true -> details.previewPhotos
                else -> emptyList()
            }
            when {
                displayPhotos.isNotEmpty() -> LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayPhotos.take(8), key = { it }) { url ->
                        RemotePlaceImage(url, "${details?.name ?: place.name} photo", Modifier.width(260.dp).height(178.dp).clip(hudShape(state.world))) {
                            PlaceImageFallback(state.world, place.group, Modifier.fillMaxSize())
                        }
                    }
                }
                loading || photosLoading -> Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(178.dp)
                    .clip(hudShape(state.world)).background(p.foreground.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                else -> if (commonsPhotosEnabled) {
                    NearbyPlacePhotoFallback(place, state.world, photoRepository, p, onOpenUrl)
                } else {
                    PlaceImageFallback(state.world, place.group, Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(178.dp))
                }
            }
            if (details != null && displayPhotos.isNotEmpty()) {
                Text("Photos · Google Maps", modifier = Modifier.padding(start = 20.dp, top = 5.dp), color = p.muted, fontSize = 11.sp)
            }
            PlacePhotoPrivacyRow(
                enabled = commonsPhotosEnabled,
                p = p,
                onEnabledChange = onCommonsPhotosEnabledChange,
            )
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(details?.name ?: place.name, color = p.foreground, fontFamily = worldFont(state.world),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                Text(details?.category ?: place.kind, color = p.accent, fontFamily = hudFont(state.world), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    details?.rating?.let { rating ->
                        Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = p.accent)
                        Text(String.format(java.util.Locale.ROOT, "%.1f", rating), color = p.foreground, fontWeight = FontWeight.SemiBold)
                    }
                    details?.reviewCount?.let { count -> Text("· ${"%,d".format(count)} reviews", color = p.muted, fontSize = 13.sp) }
                    details?.price?.let { Text("· $it", color = p.muted, fontSize = 13.sp) }
                    dist?.let { Text("· $it", color = p.muted, fontSize = 13.sp) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRoute, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.panel), shape = hudShape(state.world)) {
                    Icon(Icons.Default.Directions, null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Directions", fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onSave, modifier = Modifier.heightIn(min = 52.dp), shape = hudShape(state.world),
                    border = androidx.compose.foundation.BorderStroke(1.dp, p.accent), colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent)) {
                    Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (saved) "Saved" else "Save place")
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                details?.status?.let { status ->
                    Text(status, color = if (status.contains("closed", true)) Color(0xFFFF9A86) else p.accent,
                        fontFamily = hudFont(state.world), fontWeight = FontWeight.SemiBold)
                }
                Text(details?.address?.ifBlank { null } ?: place.address.ifBlank { "Address not listed" }, color = p.foreground, style = MaterialTheme.typography.bodyMedium)
                details?.hours?.takeIf { it.isNotEmpty() }?.let { hours ->
                    Text("Hours", color = p.accent, fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold)
                    hours.forEach { Text(it, color = p.muted, fontSize = 13.sp, lineHeight = 18.sp) }
                } ?: place.hours?.takeIf(String::isNotBlank)?.let { hours -> Text("OpenStreetMap hours · $hours", color = p.muted, fontSize = 13.sp) }
                details?.phone?.let { phone ->
                    TextButton(onClick = { onOpenUrl("tel:${Uri.encode(phone)}") }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Default.Call, null, tint = p.accent); Spacer(Modifier.width(8.dp)); Text(phone, color = p.accent)
                    }
                } ?: place.phone?.let { Text("Phone · $it", color = p.muted, fontSize = 13.sp) }
                details?.website?.let { website ->
                    TextButton(onClick = { onOpenUrl(website) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Default.Language, null, tint = p.accent); Spacer(Modifier.width(8.dp)); Text("Website", color = p.accent)
                    }
                } ?: place.website?.let { Text("Website · $it", color = p.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text("Place record · ${place.origin}", color = p.muted, fontSize = 11.sp)
            }
        }
        item {
            HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = p.muted.copy(alpha = .25f))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Google search, details and photos", color = p.foreground, fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(if (googleEnabled) "Enabled for places you open" else "Off until you choose to enable it",
                            color = p.muted, fontSize = 12.sp)
                        Text("Google receives search text and a location bias while you type, then this place name and coordinates when opened. Google also receives your network address. No Places API key or Places API billing is used.",
                            color = p.muted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Switch(checked = googleEnabled, onCheckedChange = onGoogleEnabledChange,
                        modifier = Modifier.semantics { contentDescription = "Enable Google search, place details and photos" })
                }
                if (!googleEnabled) {
                    Text("Turn this on for name suggestions while typing, and to look up ratings, review counts, live status, weekly hours and photos when you open a place. These are undocumented Google web endpoints, not the supported Places API.", color = p.muted, fontSize = 12.sp, lineHeight = 17.sp)
                } else if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(min = 44.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = p.accent, strokeWidth = 2.dp)
                        Text("Looking up this place…", color = p.muted, fontSize = 12.sp)
                    }
                } else if (error != null) {
                    Text(error, color = p.muted, fontSize = 12.sp)
                } else if (details != null) {
                    Text("Live listing details from Google Maps.", color = p.muted, fontSize = 12.sp)
                    TextButton(onClick = { onOpenUrl(details.mapUrl) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Open in Google Maps", color = p.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyPlacePhotoFallback(place: NearbyPlace, world: World, repository: CommonsPlacePhotoRepository, p: HudPalette, onOpenUrl: (String) -> Unit) {
    var photo by remember(place.id) { mutableStateOf<CommonsPlacePhoto?>(null) }
    LaunchedEffect(place.id) { photo = runCatching { repository.photo(place) }.getOrNull() }
    if (photo != null) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            RemotePlaceImage(photo?.thumbnailUrl, place.name, Modifier.fillMaxWidth().height(178.dp).clip(hudShape(world))) {
                PlaceImageFallback(world, place.group, Modifier.fillMaxSize())
            }
            TextButton(onClick = { photo?.pageUrl?.let(onOpenUrl) }, modifier = Modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(start = 3.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
                Text("${photo?.author?.ifBlank { photo?.title } ?: "Wikimedia Commons"} · ${photo?.license} · Source", color = p.muted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    } else {
        PlaceImageFallback(world, place.group, Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(178.dp))
    }
}

@Composable
private fun NearbyEmpty(world: World, p: HudPalette, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, onAction: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(34.dp), tint = p.accent)
        Text(title, fontFamily = worldFont(world), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = p.foreground)
        Text(message, fontFamily = hudFont(world), color = p.muted, style = MaterialTheme.typography.bodyMedium)
        if (onAction != null) OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp), shape = hudShape(world)) {
            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(7.dp)); Text("Try again")
        }
    }
}

@Composable
private fun NearbyLoadingRows(world: World, p: HudPalette) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        repeat(4) { index ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(Modifier.width(98.dp).aspectRatio(1.08f).clip(hudShape(world)).background(p.foreground.copy(alpha = .08f)))
                Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(.7f).height(17.dp).clip(RoundedCornerShape(5.dp)).background(p.foreground.copy(alpha = .1f)))
                    Box(Modifier.fillMaxWidth(.4f).height(13.dp).clip(RoundedCornerShape(5.dp)).background(p.accent.copy(alpha = .18f)))
                    Box(Modifier.fillMaxWidth(.9f).height(13.dp).clip(RoundedCornerShape(5.dp)).background(p.foreground.copy(alpha = .07f)))
                    Text(if (index == 0) "Reading the regional place archive…" else " ", color = p.muted, fontSize = 11.sp)
                }
            }
            if (index < 3) HorizontalDivider(color = p.muted.copy(alpha = .18f))
        }
    }
}

@Composable
private fun PlaceImageFallback(world: World, group: String, modifier: Modifier = Modifier) {
    val p = hudPalette(world)
    val icon = when (group) {
        "food" -> Icons.Default.Restaurant
        "park" -> Icons.Default.Park
        "culture" -> Icons.Default.Museum
        "shop" -> Icons.Default.Storefront
        "fuel" -> Icons.Default.LocalGasStation
        "lodging" -> Icons.Default.Hotel
        else -> Icons.Default.Place
    }
    Box(modifier.background(Brush.linearGradient(listOf(p.panel, p.accent.copy(alpha = .22f)))), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(34.dp), tint = p.accent)
        Text("WAYPOINT", Modifier.align(Alignment.BottomEnd).padding(6.dp), fontFamily = hudFont(world),
            fontWeight = FontWeight.Bold, color = p.foreground.copy(alpha = .7f), fontSize = 9.sp)
    }
}

@Composable
private fun RemotePlaceImage(url: String?, description: String, modifier: Modifier = Modifier, fallback: @Composable () -> Unit) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = url?.let { remote -> loadRemotePlaceBitmap(remote) }
    }
    val loaded = bitmap
    if (loaded != null) Image(loaded.asImageBitmap(), contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
    else Box(modifier) { fallback() }
}

private const val MAX_PLACE_IMAGE_BYTES = 6 * 1024 * 1024
private val placeImageClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
    .callTimeout(12, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()

private fun isAllowedPlaceImageUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    val host = uri.host?.lowercase(Locale.ROOT) ?: return false
    uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null && uri.port in setOf(-1, 443) &&
        (host == "upload.wikimedia.org" || host == "googleusercontent.com" || host.endsWith(".googleusercontent.com"))
}.getOrDefault(false)

private suspend fun loadRemotePlaceBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (!isAllowedPlaceImageUrl(url)) return@withContext null
        val request = Request.Builder().url(url).header("User-Agent", "WaypointAndroid").build()
        placeImageClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val bytes = response.body?.readBoundedBytes(MAX_PLACE_IMAGE_BYTES) ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxDimension <= 0) return@withContext null
            var sample = 1
            while (maxDimension / sample > 900 && sample < (1 shl 30)) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()
}
