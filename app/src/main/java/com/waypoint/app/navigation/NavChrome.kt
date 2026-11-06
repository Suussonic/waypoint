package com.waypoint.app

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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

/** The banner: maneuver arrow, distance to the turn, instruction, and a "Then" tab for the next turn. */
@Composable
internal fun NavBanner(state: NavState, onClick: () -> Unit) {
    val p = hudPalette(state.world)
    val guidance = routeGuidance(state.route, state.progress)
    val distanceText = when {
        !state.gpsReady -> "Finding GPS"
        state.routing -> "Rerouting…"
        state.offRoute -> "Off route"
        else -> formatDistance(state.progress?.toTurn ?: 0.0, state.imperial)
    }
    val instructionText = when {
        !state.gpsReady -> "Waiting for a fresh, accurate location"
        state.routing -> "Updating the route from your position"
        state.offRoute -> if (state.routeError != null) "Route unavailable · check connection and keep moving" else "Checking your position for a new route"
        else -> guidance.instruction
    }
    val steps = state.route?.steps.orEmpty()
    val index = state.progress?.nextStep ?: 0
    val after = steps.getOrNull(index + 1)?.takeIf { !guidance.continuingToArrival && guidance.maneuver?.type != "arrive" && state.gpsReady && !state.offRoute }
    val shape = hudShape(state.world)
    Column(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick, color = p.panel, contentColor = p.foreground, shape = shape, border = hudBorder(state.world),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "$distanceText. $instructionText. Show route steps"
                liveRegion = LiveRegionMode.Polite
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
    val arrival = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + (seconds * 1000).toLong()))
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

/** Speed on the left; compass, mute and re-centre on the right. Shown while navigating. */
@Composable
internal fun NavSideControls(state: NavState, onMute: () -> Unit, onLocate: () -> Unit, modifier: Modifier = Modifier) {
    val world = state.world
    val p = hudPalette(world)
    val heading = if (state.headingReliable) state.bearing else Float.NaN
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
        Surface(color = p.panel, contentColor = p.foreground, shape = CircleShape, border = hudBorder(world),
            modifier = Modifier.size(64.dp).semantics { contentDescription = "Speed" }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.gpsReady) (state.speed * if (state.imperial) 2.23694 else 3.6).roundToInt().toString() else "—",
                    fontFamily = hudFont(world), fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp)
                Text(if (state.imperial) "mph" else "km/h", fontFamily = hudFont(world), fontSize = TypeScale.Caption, lineHeight = 14.sp, color = p.muted)
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (heading.isFinite()) CompassRose(heading, world, onRecenter = onLocate)
            ChromeButton(world, if (state.voice) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                if (state.voice) "Mute guidance" else "Unmute guidance", onMute)
            ChromeButton(world, Icons.Default.MyLocation, "Use my location", onLocate)
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
