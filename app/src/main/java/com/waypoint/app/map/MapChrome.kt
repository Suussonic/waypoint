package com.waypoint.app

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Explore
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.Dp

/**
 * Shared idle-map layout. Worlds supply their skin; the phone map preferences adjust
 * floating control size and the side of the tool rail.
 *
 *   top start   world pill (emblem, name, place) opens Worlds
 *   top end     Search, Settings
 *   right rail  map key (MapLegendControl, centre), then Tilt and Locate at the bottom
 *   bottom start  compass orientation toggle; GPS chip only when the fix needs attention
 *
 * Saved and Quests live in the bottom bar, so they are not repeated on the map.
 */
@Composable
internal fun MapChrome(state: NavState, heading: Float, actions: AtlasActions, modifier: Modifier = Modifier) {
    val world = state.world
    val size = state.hudSize
    val toolsOnRight = state.hudSide != HudSide.LEFT
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scaleClearance = if (landscape) 64.dp else 0.dp
    val gutter = when (size) { HudSize.COMPACT -> 12.dp; HudSize.STANDARD -> 16.dp; HudSize.LARGE -> 20.dp }
    val gap = if (size == HudSize.COMPACT) 8.dp else 12.dp
    val buttonSize = hudToolTargetSize(size)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compactTopKey = maxHeight < 420.dp && maxWidth >= 600.dp
        val keySpace = if (compactTopKey) buttonSize + gap else 0.dp
        val pillMax = (maxWidth - gutter * 2 - buttonSize * 3 - gap * 2 - keySpace - 12.dp).coerceAtLeast(120.dp)
        WorldPill(state, actions.huds,
            Modifier.align(Alignment.TopStart).padding(start = gutter, top = 12.dp).widthIn(max = pillMax))
        Row(Modifier.align(Alignment.TopEnd).padding(end = gutter, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(gap)) {
            ChromeButton(world, Icons.Default.Search, "Set a destination", actions.search, hudSize = size)
            ChromeButton(world, Icons.Default.Explore, "Explore nearby places", { actions.nearby("") }, hudSize = size)
            ChromeButton(world, Icons.Default.Tune, "Settings", actions.settings, hudSize = size)
        }
        val toolPlacement = Modifier.align(if (toolsOnRight) Alignment.BottomEnd else Alignment.BottomStart)
            .padding(start = if (toolsOnRight) 0.dp else gutter,
                end = if (toolsOnRight) gutter else 0.dp,
                bottom = gutter + if (toolsOnRight) 0.dp else scaleClearance)
        val mapTools: @Composable () -> Unit = {
            ChromeButton(world, Icons.Default.ViewInAr, "Toggle 2D and 3D", actions.tilt, hudSize = size)
            // Recenter remains separate from the compass orientation toggle.
            ChromeButton(world, Icons.Default.MyLocation, "Use my location", actions.locate,
                highlighted = !actions.awayFromUser && state.gpsReady,
                stateText = if (actions.awayFromUser) "Map is away from you" else if (state.gpsReady) "Centred on you" else null,
                hudSize = size)
        }
        if (landscape) Row(toolPlacement, horizontalArrangement = Arrangement.spacedBy(gap)) {
            mapTools()
        } else Column(toolPlacement, verticalArrangement = Arrangement.spacedBy(gap)) {
            mapTools()
        }
        val showFix = !state.gpsReady || state.accuracy > 20 || !state.headingReliable
        Column(Modifier.align(if (toolsOnRight) Alignment.BottomStart else Alignment.BottomEnd)
            .padding(start = if (toolsOnRight) gutter else 0.dp,
                end = if (toolsOnRight) 0.dp else gutter,
                bottom = gutter + if (toolsOnRight) scaleClearance else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompassRose(heading, world, onRecenter = actions.locate, hudSize = size,
                northUp = state.keepMapNorthUp, onToggleOrientation = actions.toggleMapOrientation)
            if (showFix) FixLabel(state, Modifier.clip(hudShape(world)))
        }
    }
}

internal fun hudToolTargetSize(size: HudSize): Dp = if (size == HudSize.LARGE) 56.dp else 48.dp
internal fun hudToolVisualSize(size: HudSize): Dp = if (size == HudSize.COMPACT) 40.dp else hudToolTargetSize(size)

/** Icon-only 48 dp control. The accessible label is the content description. */
@Composable
internal fun ChromeButton(world: World, icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, highlighted: Boolean = false, stateText: String? = null, hudSize: HudSize = HudSize.STANDARD) {
    val p = hudPalette(world)
    Box(modifier.size(hudToolTargetSize(hudSize))
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label; stateText?.let { stateDescription = it } },
        contentAlignment = Alignment.Center) {
        Surface(color = p.panel, contentColor = p.foreground, shape = hudShape(world),
            border = hudBorder(world), modifier = Modifier.size(hudToolVisualSize(hudSize))) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(when (hudSize) {
                    HudSize.COMPACT -> 19.dp; HudSize.STANDARD -> 22.dp; HudSize.LARGE -> 25.dp
                }), tint = if (highlighted) p.accent else LocalContentColor.current)
            }
        }
    }
}

/** World name and current place in one tappable pill; replaces the title block and the Worlds buttons. */
@Composable
internal fun WorldPill(state: NavState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val world = state.world
    val p = hudPalette(world)
    val size = state.hudSize
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = hudToolTargetSize(size)).semantics { contentDescription = "Change world, ${world.title}" },
        color = p.panel,
        contentColor = p.foreground,
        shape = hudShape(world),
        border = hudBorder(world),
    ) {
        Row(Modifier.padding(horizontal = if (size == HudSize.COMPACT) 7.dp else 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (size == HudSize.COMPACT) 6.dp else 8.dp)) {
            Image(painterResource(worldEmblemFor(world)), null, Modifier.size(when (size) {
                HudSize.COMPACT -> 26.dp; HudSize.STANDARD -> 32.dp; HudSize.LARGE -> 36.dp
            }), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f, fill = false)) {
                Text(world.title, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    fontFamily = worldFont(world), fontWeight = FontWeight.Bold,
                    fontSize = when (size) {
                        HudSize.COMPACT -> 15.sp
                        HudSize.STANDARD -> if (world.title.length >= 13) TypeScale.Body else 18.sp
                        HudSize.LARGE -> 20.sp
                    }, lineHeight = if (size == HudSize.LARGE) 24.sp else 22.sp,
                    maxLines = 1, overflow = TextOverflow.Clip, color = p.foreground)
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
