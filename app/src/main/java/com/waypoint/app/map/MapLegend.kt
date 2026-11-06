package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The key reuses this world's original POI sprites and night palette. */
@Composable
internal fun MapLegendControl(state: NavState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val world = state.world
    val night = LocalNightMapTheme.current
    val palette = hudPalette(world)
    val profile = remember(world) { WorldLegendProfiles.forWorld(world) }
    var expanded by remember(world) { mutableStateOf(false) }
    val entries = remember(context) { MapPoiCategories.all(context) }
    val sections = remember(entries, profile) {
        profile.groupOrder.mapNotNull { group ->
            entries.filter { it.group == group }.takeIf { it.isNotEmpty() }?.let { group to it }
        }.also { ordered -> check(ordered.sumOf { it.second.size } == entries.size) {
            "Every live place category must appear in the map key."
        } }
    }
    val icons = remember(context, world, night, entries) {
        runCatching {
            val suffix = if (night) "_night" else ""
            val atlas = context.assets.open("poi/${world.file}$suffix.png").use(BitmapFactory::decodeStream)
                ?: error("Missing POI atlas")
            require(atlas.width == MapPoiCategories.atlasWidth() && atlas.height >= MapPoiCategories.atlasHeight(entries.size))
            val tiles = entries.indices.map { index ->
                val tile = Bitmap.createBitmap(atlas,
                    (index % MapPoiCategories.columns()) * MapPoiCategories.cellSize(),
                    (index / MapPoiCategories.columns()) * MapPoiCategories.cellSize(),
                    MapPoiCategories.cellSize(), MapPoiCategories.cellSize())
                Bitmap.createScaledBitmap(tile, 64, 64, true).also { tile.recycle() }.asImageBitmap()
            }
            atlas.recycle()
            tiles
        }.getOrNull()
    }
    val positionIcon = remember(context, world, state.mode) {
        (if (state.mode == TravelMode.WALK) mapPlayer(world) else mapVehicle(world, state.mode)).asImageBitmap()
    }
    val destinationIcon = remember(context, world, night) {
        mapDestination(context, world, night).asImageBitmap()
    }

    Box(modifier.fillMaxSize()) {
        if (expanded && icons != null) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 72.dp)
                    .fillMaxWidth(.84f).widthIn(min = 220.dp, max = 280.dp),
                color = palette.panel,
                contentColor = palette.foreground,
                shape = hudShape(world),
                border = hudBorder(world),
                tonalElevation = 8.dp,
            ) {
                Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(world.title, fontFamily = hudFont(world), fontWeight = FontWeight.Bold,
                                color = palette.foreground, fontSize = 14.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(profile.title, fontFamily = hudFont(world),
                                color = palette.muted, fontSize = TypeScale.Caption, maxLines = 1)
                            Text("LIVE PLACE DATA · OSM", fontFamily = hudFont(world),
                                color = palette.muted, fontSize = TypeScale.Caption, maxLines = 1)
                        }
                        IconButton(onClick = { expanded = false }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close map key", tint = palette.foreground)
                        }
                    }
                    HorizontalDivider(color = palette.accent.copy(alpha = .72f))
                    Spacer(Modifier.height(6.dp))
                    Text("NAVIGATION MARKS", fontFamily = hudFont(world),
                        color = palette.accent, fontSize = TypeScale.Caption, letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                    MapLegendRouteRow(world, state.mode, state.navigating, profile.navigation.route)
                    MapLegendImageRow(profile.navigation.position, positionIcon, world)
                    MapLegendImageRow(profile.navigation.destination, destinationIcon, world)
                    Spacer(Modifier.height(5.dp))
                    HorizontalDivider(color = palette.accent.copy(alpha = .28f))
                    for ((sectionIndex, section) in sections.withIndex()) {
                        if (sectionIndex > 0) Spacer(Modifier.height(if (profile.layout == WorldLegendLayout.LEDGER) 9.dp else 6.dp))
                        if (profile.layout == WorldLegendLayout.TABLE) HorizontalDivider(color = palette.accent.copy(alpha = .28f))
                        Text(profile.groupTitles.getValue(section.first), fontFamily = hudFont(world),
                            color = palette.accent, fontSize = TypeScale.Caption, letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        val columnCount = if (profile.layout == WorldLegendLayout.GRID) 2 else 1
                        val rowHeight = when (profile.layout) {
                            WorldLegendLayout.GRID -> 35.dp
                            WorldLegendLayout.RADAR -> 31.dp
                            WorldLegendLayout.TABLE -> 34.dp
                            WorldLegendLayout.LEDGER -> 38.dp
                        }
                        for (row in section.second.chunked(columnCount)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                row.forEach { entry ->
                                    val entryIndex = entries.indexOfFirst { it.id == entry.id }
                                    MapLegendItem(entry, icons[entryIndex], world, profile,
                                        Modifier.weight(1f), rowHeight)
                                }
                                repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(48.dp)
                .semantics { contentDescription = if (expanded) "Close ${world.title} map key" else "Open ${world.title} map key" },
            color = palette.panel,
            contentColor = palette.foreground,
            shape = hudShape(world),
            border = hudBorder(world),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, tint = palette.accent)
            }
        }
    }
}

@Composable
private fun MapLegendRouteRow(world: World, mode: TravelMode, navigating: Boolean, label: String) {
    val routeColor = Color(android.graphics.Color.parseColor(
        if (world == World.SAINTS && navigating) "#86D955" else world.routeColor,
    ))
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 26.dp, height = 22.dp)) {
            val grammar = world.routeGrammar(mode, navigating)
            val lineWidth = 3.dp.toPx()
            val pattern = world.routeLineDash(mode, navigating)
            val effect = if (grammar == RouteGrammar.SOLID) null
                else PathEffect.dashPathEffect(pattern.map { it * lineWidth * 1.5f }.toFloatArray())
            drawLine(routeColor, Offset(1.dp.toPx(), size.height / 2),
                Offset(size.width - 1.dp.toPx(), size.height / 2), lineWidth,
                cap = StrokeCap.Round, pathEffect = effect)
        }
        Text(label, Modifier.padding(start = 4.dp), fontFamily = hudFont(world),
            color = hudPalette(world).foreground, fontSize = TypeScale.Caption, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MapLegendImageRow(label: String, icon: ImageBitmap, world: World) {
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically) {
        Image(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(label, Modifier.padding(start = 4.dp), fontFamily = hudFont(world),
            color = hudPalette(world).foreground, fontSize = TypeScale.Caption, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MapLegendItem(
    entry: MapPoiCategory,
    icon: ImageBitmap,
    world: World,
    profile: WorldLegendProfile,
    modifier: Modifier = Modifier,
    rowHeight: androidx.compose.ui.unit.Dp = 34.dp,
) {
    val alias = profile.aliases[entry.id]
    Row(modifier.heightIn(min = rowHeight).padding(end = 5.dp).semantics {
        contentDescription = if (alias == null) entry.label else "$alias · ${entry.label}"
    }, verticalAlignment = Alignment.CenterVertically) {
        Image(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 4.dp).weight(1f)) {
            Text(alias ?: entry.label, fontFamily = hudFont(world), color = hudPalette(world).foreground,
                fontSize = TypeScale.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (alias != null) Text(entry.label, fontFamily = hudFont(world), color = hudPalette(world).muted,
                fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
