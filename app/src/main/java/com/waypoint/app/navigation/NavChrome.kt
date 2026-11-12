package com.waypoint.app

import android.content.Context
import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Turn-by-turn chrome in the familiar navigation-app layout: a maneuver banner with the distance to the
 * turn and a "Then" tab for the following turn, a bottom trip sheet (end, time, distance, arrival, steps),
 * a compact side rail and a speed badge. All artwork is drawn here; the world skin supplies colours,
 * shape and fonts through the existing hud helpers.
 */

/** Chunky maneuver arrow drawn on a 48 unit grid. A null step means "continue straight". */
@Composable
internal fun ManeuverGlyph(step: Maneuver?, modifier: Modifier = Modifier, color: Color = Color.White, arriving: Boolean = false,
                           blocky: Boolean = false) {
    Canvas(modifier) { drawManeuver(step, arriving, color, blocky) }
}

/** Pixel, terminal and retro worlds get square-cornered arrows; every other world gets rounded joins. */
internal fun World.blockyArrows(): Boolean =
    this in setOf(World.PIXEL, World.GREEN_TERMINAL, World.DOS_ATLAS, World.RETRO_QUEST, World.DEFCON, World.COUNTER_STRIKE)

private fun DrawScope.drawManeuver(step: Maneuver?, arriving: Boolean, color: Color, blocky: Boolean) {
    val u = size.minDimension / 48f
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    fun arrow(points: List<Offset>, alpha: Float = 1f) {
        val tip = points.last()
        val before = points[points.size - 2]
        val len = (tip - before).getDistance().coerceAtLeast(0.001f)
        val dir = (tip - before) / len
        val perp = Offset(-dir.y, dir.x)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size - 1) lineTo(points[i].x, points[i].y)
            val stop = tip - dir * (6f * u)
            lineTo(stop.x, stop.y)
        }
        drawPath(path, color.copy(alpha = alpha), style = Stroke(width = 6.5f * u, cap = StrokeCap.Butt, join = if (blocky) StrokeJoin.Miter else StrokeJoin.Round))
        val base = tip - dir * (10f * u)
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo((base + perp * (8.5f * u)).x, (base + perp * (8.5f * u)).y)
            lineTo((base - perp * (8.5f * u)).x, (base - perp * (8.5f * u)).y)
            close()
        }
        drawPath(head, color.copy(alpha = alpha))
    }

    val type = step?.type.orEmpty()
    val mod = step?.modifier.orEmpty()
    val sign = when { mod.contains("left") -> -1f; mod.contains("right") -> 1f; else -> 0f }
    when {
        arriving || type == "arrive" -> {
            drawCircle(color, 12f * u, pt(24f, 19f), style = Stroke(width = 5f * u))
            drawCircle(color, 4.6f * u, pt(24f, 19f))
            val tail = Path().apply { moveTo((24f - 6f) * u, 30f * u); lineTo(24f * u, 43f * u); lineTo((24f + 6f) * u, 30f * u); close() }
            drawPath(tail, color)
        }
        type.contains("roundabout") || type.contains("rotary") -> {
            drawCircle(color, 9f * u, pt(24f, 20f), style = Stroke(width = 5f * u))
            drawLine(color, pt(24f, 44f), pt(24f, 29f), strokeWidth = 6.5f * u)
            val end = when { sign < 0 -> pt(5f, 20f); sign > 0 -> pt(43f, 20f); else -> pt(24f, 3f) }
            val from = when { sign < 0 -> pt(15f, 20f); sign > 0 -> pt(33f, 20f); else -> pt(24f, 11f) }
            arrow(listOf(from, end))
        }
        mod == "uturn" -> {
            val flip = step?.uTurnRight == true
            fun x(v: Float) = if (flip) 48f - v else v
            arrow(listOf(pt(x(33f), 44f), pt(x(33f), 17f), pt(x(31f), 11f), pt(x(25f), 8f), pt(x(19f), 11f), pt(x(17f), 17f), pt(x(17f), 36f)))
        }
        type == "fork" || type == "merge" || type.contains("ramp") -> {
            arrow(listOf(pt(24f, 44f), pt(24f, 29f), pt(24f + (if (sign == 0f) 0f else sign) * 12f, 8f)))
            if (sign != 0f) arrow(listOf(pt(24f, 30f), pt(24f - sign * 12f, 9f)), alpha = 0.35f)
        }
        else -> {
            val slight = mod.contains("slight")
            val sharp = mod.contains("sharp")
            when {
                sign == 0f -> arrow(listOf(pt(24f, 44f), pt(24f, 6f)))
                slight -> arrow(listOf(pt(24f, 44f), pt(24f, 28f), pt(24f + sign * 13f, 10f)))
                sharp -> arrow(listOf(pt(24f, 44f), pt(24f, 18f), pt(24f + sign * 15f, 33f)))
                else -> arrow(listOf(pt(24f, 44f), pt(24f, 26f), pt(24f + sign * 19f, 26f)))
            }
        }
    }
}

internal fun formatTripTime(seconds: Double): String {
    val minutes = ceil(seconds / 60).toInt().coerceAtLeast(1)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} hr ${minutes % 60} min"
}

internal fun formatArrivalTime(context: Context, secondsUntilArrival: Double): String {
    val seconds = secondsUntilArrival.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    val arrival = Date(System.currentTimeMillis() + (seconds * 1000).toLong())
    return AndroidDateFormat.getTimeFormat(context).format(arrival)
}

private fun navigationDistanceLabel(state: NavState): String = when {
    !state.gpsReady -> "Finding GPS"
    state.routing -> "Rerouting…"
    state.offRoute -> "Off route"
    state.progress == null -> "Starting"
    else -> formatDistance(state.progress.toTurn, state.imperial)
}

private fun navigationInstructionLabel(state: NavState, guidance: RouteGuidance): String = when {
    !state.gpsReady -> "Waiting for a fresh, accurate location"
    state.routing -> "Updating the route from your position"
    state.offRoute -> if (state.routeError != null) "Route unavailable · check connection and keep moving"
        else "Checking your position for a new route"
    else -> guidance.instruction
}

/** The banner: maneuver arrow, distance to the turn, instruction, and a "Then" tab for the next turn. */
@Composable
internal fun NavBanner(state: NavState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = hudPalette(state.world)
    val guidance = routeGuidance(state.route, state.progress)
    val distanceText = navigationDistanceLabel(state)
    val instructionText = navigationInstructionLabel(state, guidance)
    val announceStatus = !state.gpsReady || state.routing || state.offRoute
    val steps = state.route?.steps.orEmpty()
    val index = state.progress?.nextStep ?: 0
    val after = steps.getOrNull(index + 1)?.takeIf { !guidance.continuingToArrival && guidance.maneuver?.type != "arrive" && state.gpsReady && !state.offRoute }
    val shape = hudShape(state.world)
    Column(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick, color = p.panel, contentColor = p.foreground, shape = shape, border = hudBorder(state.world),
            modifier = modifier.fillMaxWidth().semantics {
                contentDescription = "$distanceText. $instructionText. Show route steps"
                if (announceStatus) liveRegion = LiveRegionMode.Polite
            },
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ManeuverGlyph(if (guidance.continuingToArrival) null else guidance.maneuver, Modifier.size(56.dp), p.foreground,
                    arriving = guidance.maneuver?.type == "arrive" && !guidance.continuingToArrival, blocky = state.world.blockyArrows())
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(distanceText, fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold, fontSize = 32.sp,
                        lineHeight = 36.sp, color = p.foreground, maxLines = 1)
                    Text(instructionText, fontFamily = hudFont(state.world), fontSize = 18.sp, lineHeight = 23.sp, color = p.foreground, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (after != null) {
            Surface(color = p.panel, contentColor = p.foreground, shape = shape, border = hudBorder(state.world),
                modifier = Modifier.padding(start = 12.dp).semantics { contentDescription = "Then ${after.instruction}" }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Then", fontFamily = hudFont(state.world), fontSize = 16.sp, color = p.muted)
                    ManeuverGlyph(after, Modifier.size(26.dp), p.foreground, blocky = state.world.blockyArrows())
                }
            }
        }
    }
}

/** The trip sheet: end, trip time, distance and arrival, steps. Pull up or tap to open directions. */
@Composable
internal fun NavBottomBar(state: NavState, onSteps: () -> Unit, onStop: () -> Unit) {
    val p = hudPalette(state.world)
    val seconds = state.progress?.seconds ?: state.route?.seconds ?: 0.0
    val meters = state.progress?.remaining ?: state.route?.meters ?: 0.0
    val arrival = formatArrivalTime(LocalContext.current, seconds)
    Surface(color = p.panel, contentColor = p.foreground, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectVerticalDragGestures { _, drag -> if (drag < -18f) onSteps() }
        }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 8.dp).width(40.dp).height(4.dp).background(p.muted.copy(alpha = .55f), RoundedCornerShape(2.dp)))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavRoundButton(Icons.Default.Close, "End navigation", onStop)
                Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = onSteps).heightIn(min = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(formatTripTime(seconds), fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold, fontSize = 30.sp,
                        lineHeight = 34.sp, color = p.accent, maxLines = 1)
                    Text("${formatDistance(meters, state.imperial)} • $arrival", fontFamily = hudFont(state.world), fontSize = 16.sp, lineHeight = 21.sp, color = p.muted, maxLines = 1)
                }
                NavRoundButton(Icons.Default.Route, "Route steps", onSteps)
            }
        }
    }
}

/** The current maneuver and world switch share one compact top edge in phone landscape. */
@Composable
internal fun CompactLandscapeNavBanner(
    state: NavState,
    onSteps: () -> Unit,
    onWorlds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = hudPalette(state.world)
    val guidance = routeGuidance(state.route, state.progress)
    val distanceText = navigationDistanceLabel(state)
    val instructionText = navigationInstructionLabel(state, guidance)
    val announceStatus = !state.gpsReady || state.routing || state.offRoute
    val largeText = LocalDensity.current.fontScale >= 1.75f
    BoxWithConstraints(modifier) {
        val visibleInstruction = if (largeText && maxWidth < 280.dp) when {
            !state.gpsReady -> "GPS"
            state.routing -> "Rerouting"
            state.offRoute -> "Off route"
            else -> compactGuidanceInstruction(guidance)
        } else instructionText
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChromeButton(state.world, Icons.Default.Layers, "Worlds", onWorlds)
            Surface(onClick = onSteps, color = p.panel, contentColor = p.foreground, shape = hudShape(state.world),
                border = hudBorder(state.world), modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                    contentDescription = "$distanceText. $instructionText. Show route steps"
                if (announceStatus) liveRegion = LiveRegionMode.Polite
                }) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManeuverGlyph(if (guidance.continuingToArrival) null else guidance.maneuver,
                        Modifier.size(36.dp), p.foreground,
                        arriving = guidance.maneuver?.type == "arrive" && !guidance.continuingToArrival,
                        blocky = state.world.blockyArrows())
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(distanceText, fontFamily = hudFont(state.world), fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, lineHeight = 22.sp, color = p.foreground, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(visibleInstruction, fontFamily = hudFont(state.world), fontSize = TypeScale.Label,
                            lineHeight = 18.sp, color = p.foreground, maxLines = if (largeText) 1 else 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun compactGuidanceInstruction(guidance: RouteGuidance): String {
    if (guidance.continuingToArrival) return "Continue"
    val step = guidance.maneuver ?: return guidance.instruction.substringBefore(" onto ")
    return when (step.type) {
        "arrive" -> "Arrive"
        "roundabout", "rotary", "roundabout turn" -> if (step.exit > 0) "Exit ${step.exit}" else "Roundabout"
        "exit roundabout", "exit rotary", "off ramp" -> "Exit"
        "on ramp" -> "Ramp"
        "merge" -> "Merge"
        "fork" -> "Fork"
        else -> when (step.modifier) {
            "uturn" -> "U-turn"
            "left", "slight left", "sharp left" -> "Left"
            "right", "slight right", "sharp right" -> "Right"
            else -> "Straight"
        }
    }
}

/** Trip status and every guidance action in one low landscape strip. */
@Composable
internal fun CompactLandscapeNavFooter(
    state: NavState,
    onSteps: () -> Unit,
    onStop: () -> Unit,
    onMute: () -> Unit,
    onLocate: () -> Unit,
    onToggleMapOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seconds = state.progress?.seconds ?: state.route?.seconds ?: 0.0
    val meters = state.progress?.remaining ?: state.route?.meters ?: 0.0
    val arrival = formatArrivalTime(LocalContext.current, seconds)
    val time = formatTripTime(seconds)
    val remaining = formatDistance(meters, state.imperial)
    val largeText = LocalDensity.current.fontScale >= 1.75f
    BoxWithConstraints(modifier) {
        val stacked = largeText || maxWidth < 356.dp
        val actionsInTwoRows = maxWidth < 280.dp
        val splitTripText = largeText && maxWidth < 240.dp
        if (stacked) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactLandscapeTripSummary(state.world, time, remaining, arrival, onSteps,
                    largeText = largeText, splitTripText = splitTripText, modifier = Modifier.fillMaxWidth())
                CompactLandscapeNavActions(state, onSteps, onStop, onMute, onLocate, onToggleMapOrientation, actionsInTwoRows)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactLandscapeTripSummary(state.world, time, remaining, arrival, onSteps,
                    largeText = false, splitTripText = false, modifier = Modifier.weight(1f))
                CompactLandscapeNavActions(state, onSteps, onStop, onMute, onLocate, onToggleMapOrientation, actionsInTwoRows)
            }
        }
    }
}

@Composable
private fun CompactLandscapeTripSummary(
    world: World,
    time: String,
    remaining: String,
    arrival: String,
    onSteps: () -> Unit,
    largeText: Boolean,
    splitTripText: Boolean,
    modifier: Modifier,
) {
    val p = hudPalette(world)
    Surface(onClick = onSteps, color = p.panel, contentColor = p.foreground, shape = hudShape(world),
        border = hudBorder(world), modifier = modifier.heightIn(min = 48.dp).semantics {
            contentDescription = "$time remaining. $remaining to destination. Arrival $arrival. Show route steps"
        }) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center) {
            if (splitTripText) {
                Text(time, fontFamily = hudFont(world), fontWeight = FontWeight.Bold,
                    fontSize = TypeScale.Label, lineHeight = 18.sp, color = p.accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(remaining, fontFamily = hudFont(world), fontSize = TypeScale.Label,
                    lineHeight = 18.sp, color = p.foreground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (largeText) {
                Text("$time · $remaining", fontFamily = hudFont(world), fontWeight = FontWeight.Bold,
                    fontSize = TypeScale.Label, lineHeight = 20.sp, color = p.foreground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(time, fontFamily = hudFont(world), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, lineHeight = 22.sp, color = p.accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$remaining · $arrival", fontFamily = hudFont(world), fontSize = TypeScale.Caption,
                    lineHeight = 16.sp, color = p.foreground, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CompactLandscapeNavActions(
    state: NavState,
    onSteps: () -> Unit,
    onStop: () -> Unit,
    onMute: () -> Unit,
    onLocate: () -> Unit,
    onToggleMapOrientation: () -> Unit,
    twoRows: Boolean,
) {
    val controls = listOf(
        CompactNavAction(Icons.Default.Close, "End navigation", onStop),
        CompactNavAction(Icons.Default.Route, "Route steps", onSteps),
        CompactNavAction(if (state.voice) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            if (state.voice) "Mute guidance" else "Unmute guidance", onMute),
        CompactNavAction(Icons.Default.MyLocation, "Use my location", onLocate),
        CompactNavAction(Icons.Default.Explore, mapOrientationAction(state.keepMapNorthUp), onToggleMapOrientation,
            highlighted = state.keepMapNorthUp, stateText = mapOrientationState(state.keepMapNorthUp)),
    )
    if (twoRows) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            controls.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { action -> ChromeButton(state.world, action.icon, action.label, action.onClick,
                        highlighted = action.highlighted, stateText = action.stateText) }
                }
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            controls.forEach { action -> ChromeButton(state.world, action.icon, action.label, action.onClick,
                highlighted = action.highlighted, stateText = action.stateText) }
        }
    }
}

private data class CompactNavAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val highlighted: Boolean = false,
    val stateText: String? = null,
)

@Composable
private fun NavRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent, contentColor = LocalContentColorFor(),
        border = BorderStroke(1.5.dp, LocalContentColorFor().copy(alpha = .55f)),
        modifier = Modifier.size(56.dp).semantics { contentDescription = label }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(26.dp)) }
    }
}

@Composable
private fun LocalContentColorFor(): Color = androidx.compose.material3.LocalContentColor.current

/** Speed and Worlds on the left; compass, mute and re-centre on the right. */
@Composable
internal fun NavSideControls(state: NavState, onMute: () -> Unit, onLocate: () -> Unit,
                             onWorlds: () -> Unit, onToggleMapOrientation: () -> Unit,
                             modifier: Modifier = Modifier) {
    val world = state.world
    val p = hudPalette(world)
    val heading = if (state.headingReliable) state.bearing else Float.NaN
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val over = state.overSpeedLimit && state.speedWarning
            val overColor = Color(0xFFFF5A4D)
            Surface(color = p.panel, contentColor = if (over) overColor else p.foreground, shape = CircleShape,
                border = if (over) androidx.compose.foundation.BorderStroke(3.dp, overColor) else hudBorder(world),
                modifier = Modifier.size(64.dp).semantics { contentDescription = if (over) "Speed, over the limit" else "Speed" }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.gpsReady) (state.speed * if (state.imperial) 2.23694 else 3.6).roundToInt().toString() else "—",
                        fontFamily = hudFont(world), fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp)
                    Text(if (state.imperial) "mph" else "km/h", fontFamily = hudFont(world), fontSize = TypeScale.Caption, lineHeight = 14.sp, color = if (over) overColor else p.muted)
                }
            }
            val limit = state.speedLimitKmh
            if (state.speedWarning && limit != null && state.navigating) SpeedLimitSign(speedLimitForDisplay(limit, state.imperial), over)
            ChromeButton(world, Icons.Default.Layers, "Worlds", onWorlds)
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CompassRose(heading, world, onRecenter = onLocate, northUp = state.keepMapNorthUp,
                onToggleOrientation = onToggleMapOrientation)
            ChromeButton(world, if (state.voice) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                if (state.voice) "Mute guidance" else "Unmute guidance", onMute)
            ChromeButton(world, Icons.Default.MyLocation, "Use my location", onLocate)
        }
    }
}

/** The posted limit as a round road sign; it grows a little and its ring brightens while the car is over it. */
@Composable
internal fun SpeedLimitSign(limit: Int, over: Boolean, modifier: Modifier = Modifier) {
    val scale = androidx.compose.animation.core.animateFloatAsState(if (over) 1.12f else 1f,
        androidx.compose.animation.core.tween(220), label = "speedLimitScale").value
    val ring = if (over) Color(0xFFFF2D1F) else Color(0xFFD81E14)
    Surface(color = Color.White, contentColor = Color(0xFF111111), shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(6.dp, ring),
        modifier = modifier.size(56.dp).scale(scale)
            .semantics { contentDescription = "Speed limit $limit" }) {
        Box(contentAlignment = Alignment.Center) {
            Text(limit.toString(), fontWeight = FontWeight.ExtraBold, fontSize = if (limit >= 100) 18.sp else 22.sp, lineHeight = 24.sp)
        }
    }
}

/** Shown while guiding once the map has been panned away from the car; one tap returns to following. */
@Composable
internal fun RecentrePill(world: World, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = hudPalette(world)
    Surface(onClick = onClick, color = p.panel, contentColor = p.foreground, shape = hudShape(world), border = hudBorder(world),
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = "Re-centre on my position" }) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.MyLocation, null, Modifier.size(20.dp))
            Text("Re-centre", fontFamily = hudFont(world), fontWeight = FontWeight.Bold, fontSize = TypeScale.Label)
        }
    }
}
