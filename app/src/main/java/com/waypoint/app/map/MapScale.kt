package com.waypoint.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class MapScaleTick(val meters: Double, val label: String, val widthFraction: Float)

/** Pick the largest 1–2–5 distance that fits between the projected endpoints. */
internal fun chooseMapScale(maxMeters: Double, imperial: Boolean): MapScaleTick? {
    if (!maxMeters.isFinite() || maxMeters <= 0.0) return null
    val mile = 1609.344
    val foot = 0.3048
    val (metersPerUnit, unit) = when {
        imperial && maxMeters >= mile -> mile to "mi"
        imperial -> foot to "ft"
        maxMeters >= 1000.0 -> 1000.0 to "km"
        else -> 1.0 to "m"
    }
    val maxUnits = maxMeters / metersPerUnit
    val power = 10.0.pow(floor(log10(maxUnits)))
    val mantissa = maxUnits / power
    val step = when {
        mantissa >= 5.0 -> 5.0
        mantissa >= 2.0 -> 2.0
        else -> 1.0
    }
    val units = step * power
    val value = if (units >= 1.0) units.toLong().toString()
        else String.format(Locale.US, "%.2f", units).trimEnd('0').trimEnd('.')
    val meters = units * metersPerUnit
    return MapScaleTick(meters, "$value $unit", (meters / maxMeters).toFloat().coerceIn(0f, 1f))
}

/** The canvas bounds are reported so the native map can project the actual bar endpoints. */
@Composable
internal fun MapScaleBar(
    world: World,
    tick: MapScaleTick?,
    landscape: Boolean,
    modifier: Modifier = Modifier,
    onLineBounds: (Rect) -> Unit,
) {
    val palette = hudPalette(world)
    Surface(
        modifier = modifier.graphicsLayer { alpha = if (tick == null) 0f else 1f }
            .semantics { if (tick != null) contentDescription = "Map scale: ${tick.label}" },
        color = palette.panel,
        contentColor = palette.foreground,
        shape = hudShape(world),
        border = hudBorder(world),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(tick?.label ?: "Scale", color = palette.foreground, fontFamily = hudFont(world),
                fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Canvas(Modifier.width(if (landscape) 120.dp else 136.dp).height(9.dp)
                .onGloballyPositioned { onLineBounds(it.boundsInRoot()) }) {
                val end = size.width * (tick?.widthFraction ?: 1f)
                val y = size.height * .5f
                val stroke = 2.dp.toPx()
                drawLine(palette.foreground, Offset(0f, y), Offset(end, y), stroke, cap = StrokeCap.Square)
                drawLine(palette.foreground, Offset(0f, 0f), Offset(0f, size.height), stroke)
                drawLine(palette.foreground, Offset(end, 0f), Offset(end, size.height), stroke)
            }
        }
    }
}
