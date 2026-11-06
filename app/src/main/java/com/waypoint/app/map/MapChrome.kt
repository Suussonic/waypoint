package com.waypoint.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One idle-map layout for every world. Position, size and spacing never change between worlds; the
 * world only supplies a skin (palette, shape, border, fonts, emblem) through the existing hud helpers.
 *
 *   top start   world pill (emblem, name, place) opens Worlds
 *   top end     Search, Settings
 *   right rail  map key (MapLegendControl, centre), then Tilt and Locate at the bottom
 *   bottom start  compass when the heading is known; GPS chip only when the fix needs attention
 *
 * Saved and Quests live in the bottom bar, so they are not repeated on the map.
 */
@Composable
internal fun MapChrome(state: NavState, heading: Float, actions: AtlasActions, modifier: Modifier = Modifier) {
    val world = state.world
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Leave room for the two top-end buttons (48 + 12 + 48) plus gutters.
        val pillMax = (maxWidth - ChromeGutter - ChromeButtonsWidth - 12.dp - ChromeGutter).coerceAtLeast(160.dp)
        WorldPill(state, actions.huds,
            Modifier.align(Alignment.TopStart).padding(start = ChromeGutter, top = 12.dp).widthIn(max = pillMax))
        Row(Modifier.align(Alignment.TopEnd).padding(end = ChromeGutter, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChromeButton(world, Icons.Default.Search, "Set a destination", actions.search)
            ChromeButton(world, Icons.Default.Tune, "Settings", actions.settings)
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = ChromeGutter, bottom = ChromeGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChromeButton(world, Icons.Default.ViewInAr, "Toggle 2D and 3D", actions.tilt)
            // Highlighted while the map is centred on the user and following; otherwise a tap flies back to them.
            ChromeButton(world, Icons.Default.MyLocation, "Use my location", actions.locate,
                highlighted = !actions.awayFromUser && state.gpsReady,
                stateText = if (actions.awayFromUser) "Map is away from you" else if (state.gpsReady) "Centred on you" else null)
        }
        val showFix = !state.gpsReady || state.accuracy > 20 || !state.headingReliable
        Column(Modifier.align(Alignment.BottomStart).padding(start = ChromeGutter, bottom = ChromeGutter),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (heading.isFinite()) CompassRose(heading, world, onRecenter = actions.locate)
            if (showFix) FixLabel(state, Modifier.clip(hudShape(world)))
        }
    }
}

private val ChromeGutter = 16.dp
private val ChromeButtonsWidth = 108.dp

/** Icon-only 48 dp control. The accessible label is the content description. */
@Composable
internal fun ChromeButton(world: World, icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, highlighted: Boolean = false, stateText: String? = null) {
    val p = hudPalette(world)
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).semantics { contentDescription = label; stateText?.let { stateDescription = it } },
        color = p.panel,
        contentColor = p.foreground,
        shape = hudShape(world),
        border = hudBorder(world),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (highlighted) p.accent else LocalContentColor.current)
        }
    }
}

/** World name and current place in one tappable pill; replaces the title block and the Worlds buttons. */
@Composable
internal fun WorldPill(state: NavState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val world = state.world
    val p = hudPalette(world)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = "Change world, ${world.title}" },
        color = p.panel,
        contentColor = p.foreground,
        shape = hudShape(world),
        border = hudBorder(world),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(worldEmblemFor(world)), null, Modifier.size(32.dp), contentScale = ContentScale.Fit)
            Column {
                Text(world.title, fontFamily = worldFont(world), fontWeight = FontWeight.Bold,
                    fontSize = if (world.title.length >= 13) TypeScale.Body else 18.sp, lineHeight = 22.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = p.foreground)
                Text(state.city, fontFamily = hudFont(world), fontSize = TypeScale.Caption, lineHeight = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = p.muted)
            }
        }
    }
}


/** Route preview header: Back and a one-row mode selector, 48 dp tall, instead of a title plus a 2x2 grid. */
@Composable
internal fun RoutePreviewHeader(state: NavState, onBack: () -> Unit, onMode: (TravelMode) -> Unit, modifier: Modifier = Modifier) {
    val world = state.world
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChromeButton(world, Icons.AutoMirrored.Filled.ArrowBack, "Close route preview", onBack)
        ModeSegments(state.mode, onMode, world, Modifier.weight(1f))
    }
}

@Composable
internal fun ModeSegments(selected: TravelMode, onSelect: (TravelMode) -> Unit, world: World, modifier: Modifier = Modifier) {
    val p = hudPalette(world)
    Surface(modifier.height(48.dp), color = p.panel, contentColor = p.foreground, shape = hudShape(world), border = hudBorder(world)) {
        Row(Modifier.fillMaxSize().selectableGroup()) {
            TravelMode.entries.forEach { mode ->
                val active = selected == mode
                androidx.compose.foundation.layout.Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(mode) })
                        .semantics { contentDescription = mode.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(modeIcon(mode), null, Modifier.size(22.dp), tint = if (active) p.accent else p.muted)
                        androidx.compose.foundation.layout.Box(Modifier.width(22.dp).height(2.dp)
                            .then(if (active) Modifier.background(p.accent) else Modifier))
                    }
                }
            }
        }
    }
}
