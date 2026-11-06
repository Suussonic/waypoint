package com.waypoint.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Shared local-time phase for the map's colors, patterns and HUD surfaces. */
internal val LocalNightMapTheme = compositionLocalOf { false }

/** The device's local clock drives a quiet switch at 06:00 and 19:00. */
@Composable
internal fun MapThemeClock(content: @Composable () -> Unit) {
    var isNight by remember { mutableStateOf(isNightAtLocalTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNight = isNightAtLocalTime()
            delay(60_000L)
        }
    }
    CompositionLocalProvider(LocalNightMapTheme provides isNight, content = content)
}

internal fun isNightAtLocalTime(time: LocalTime = LocalTime.now()): Boolean =
    time.hour >= 19 || time.hour < 6

internal fun mapThemeStyleFile(world: World, night: Boolean, tilted: Boolean): String = when {
    world == World.DIVISION && tilted -> if (night) "division_holo_night" else "division_holo"
    world == World.DIVISION -> if (night) "division_night" else "division"
    night -> "${world.file}_night"
    else -> world.file
}

internal fun resolveMapThemeStyleFile(
    world: World,
    night: Boolean,
    tilted: Boolean,
    availableFiles: Set<String>,
): String {
    val requested = mapThemeStyleFile(world, night, tilted)
    if (requested in availableFiles) return requested
    val flatStyle = world.file
    if (flatStyle in availableFiles) return flatStyle
    return flatStyle
}

internal fun mapThemeStyleFiles(context: Context): Set<String> =
    context.assets.list("styles").orEmpty()
        .filter { it.endsWith(".json") }
        .map { it.removeSuffix(".json") }
        .toSet()
