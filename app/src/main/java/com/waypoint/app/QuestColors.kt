package com.waypoint.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalPageSystemBarSurface = staticCompositionLocalOf<(Color?) -> Unit> { {} }

internal fun questContrast(first: Color, second: Color): Float {
    val a = first.luminance(); val b = second.luminance()
    return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
}

/** Map accents can be decorative; task controls need readable ink in every world palette. */
internal fun questInk(color: Color, surface: Color, minimum: Float = 4.5f): Color {
    val ink = color.copy(alpha = 1f)
    if (questContrast(ink, surface) >= minimum) return ink
    val target = if (questContrast(Color.Black, surface) > questContrast(Color.White, surface)) Color.Black else Color.White
    for (step in 1..20) {
        val adjusted = lerp(ink, target, step / 20f)
        if (questContrast(adjusted, surface) >= minimum) return adjusted
    }
    return target
}
