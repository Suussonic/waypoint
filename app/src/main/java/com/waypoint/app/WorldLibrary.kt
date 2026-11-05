package com.waypoint.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Browsing never changes the current map. Applying a preview is the single commit point. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun HudLibrary(selected: World, onSelect: (World) -> Unit, onClose: () -> Unit) {
    val navigation = (LocalContext.current.applicationContext as WaypointApplication).navigation
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var previewName by rememberSaveable { mutableStateOf<String?>(null) }
    var favorites by rememberSaveable { mutableStateOf(navigation.favoriteWorlds().map { it.name }) }
    // The preview replaces the catalogue's composition. Own its scroll state
    // here so previewing/applying a world cannot discard the user's position.
    val listState=rememberLazyListState()
    fun favorite(world: World) {
        favorites = if (world.name in favorites) favorites - world.name else favorites + world.name
        navigation.setFavoriteWorlds(favorites.mapNotNull { name -> World.entries.find { it.name == name } })
    }
    val preview = World.entries.find { it.name == previewName }
    if (preview != null) {
        BackHandler { previewName = null }
        QuestTheme(preview, adaptSystemBars = true) {
            Scaffold(topBar = { TopAppBar(title = { Text("World preview") }, navigationIcon = { IconButton(onClick = { previewName = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to worlds") } },
                actions = { FavoriteWorldButton(preview, preview.name in favorites) { favorite(preview) } }) },
                bottomBar = { Surface { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = { previewName = null; onSelect(preview) }, modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 52.dp), shape = hudShape(preview)) { Text("Use this world") }
                } } }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 760.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        WorldPreviewArtwork(preview, Modifier.fillMaxWidth().aspectRatio(1.5f))
                        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(preview.title, style = MaterialTheme.typography.headlineMedium)
                            Text(preview.inspiration(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(preview.descriptionRes), style = MaterialTheme.typography.bodyLarge)
                            Text("Preview: ${preview.previewPlace().label}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Text("Your places, quests and progress stay with you.", style = MaterialTheme.typography.bodyMedium)
                            Text("Maps © OpenFreeMap · OpenMapTiles · OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
        return
    }
    val worlds = World.entries.filter { world -> world.matchesGameQuery(query) && when (filter) {
        "Favourites" -> world.name in favorites
        "New" -> world in newWorlds
        else -> true
    } }
    Scaffold( containerColor = Ink, topBar = {
        TopAppBar(title = { Text("Worlds") }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to map") } })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), state=listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Find your next world", style = MaterialTheme.typography.headlineMedium)
                        Text("${World.entries.size} worlds. Every one included. Preview a world, then make it yours.", color = Muted)
                        OutlinedTextField(query, onValueChange = { query = it.take(100) }, label = { Text("Find a world or game") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Favourites", "New").forEach { name -> FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) }) }
                        }
                    }
                }
                if (worlds.isEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (filter == "Favourites" && query.isBlank()) "Keep your favourites close" else "No worlds found", style = MaterialTheme.typography.titleLarge)
                        Text(if (filter == "Favourites" && query.isBlank()) "Tap a heart on any world to find it here." else "Try a different name or browse all worlds.", color = Muted)
                        TextButton(onClick = { filter = "All"; query = "" }) { Text("Browse all worlds") }
                    }
                }
                items(worlds, key = { it.name }) { world ->
                    Surface(onClick = { previewName = world.name }, modifier = Modifier.semantics { contentDescription = "Preview ${world.title} theme" },
                        color = Panel, shape = RoundedCornerShape(12.dp), border = if (selected == world) BorderStroke(2.dp, world.accent) else null) {
                        Column {
                            Box {
                                WorldPreviewArtwork(world, Modifier.fillMaxWidth().height(180.dp))
                                Surface(color = Ink, shape = RoundedCornerShape(24.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                    FavoriteWorldButton(world, world.name in favorites) { favorite(world) }
                                }
                                if (selected == world || world in newWorlds) Surface(color = Ink, shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                    Text(if (selected == world) "Current world" else "New", color = Cream, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(world.title, fontFamily = worldFont(world), style = MaterialTheme.typography.titleLarge, color = Cream)
                                    Text(world.inspiration(), style = MaterialTheme.typography.bodySmall, color = Muted)
                                    Text("Preview world", style = MaterialTheme.typography.labelLarge, color = world.accent, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Cream)
                            }
                        }
                    }
                }
                item { Text("Maps © OpenFreeMap · OpenMapTiles · OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall, color = Muted) }
            }
        }
    }
}

@Composable private fun FavoriteWorldButton(world: World, favorite: Boolean, onClick: () -> Unit) {
    IconToggleButton(checked = favorite, onCheckedChange = { onClick() }) {
        Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            if (favorite) "Remove ${world.title} from favourites" else "Add ${world.title} to favourites")
    }
}

/** Match the system-bar viewport before measuring restored lazy content. */
@Composable internal fun Modifier.withReadyWindowInsets(): Modifier {
    val view = LocalView.current
    val systemBars = WindowInsets.systemBars
    // A restored lazy list must not measure against the transient edge-to-edge
    // viewport before Android dispatches system bars: that clamps a bottom anchor
    // irreversibly. Read both inset sources during layout, without a timed delay.
    return this.layout { measurable, constraints ->
        val actual = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemBars())
        if (actual == null || systemBars.getTop(this) != actual.top || systemBars.getBottom(this) != actual.bottom) {
            layout(constraints.maxWidth, constraints.maxHeight) {}
        } else {
            val child = measurable.measure(constraints)
            layout(child.width, child.height) { child.placeRelative(0, 0) }
        }
    }
}
