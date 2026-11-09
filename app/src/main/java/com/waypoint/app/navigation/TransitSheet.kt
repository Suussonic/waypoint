package com.waypoint.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private sealed interface TransitUi {
    data object Loading : TransitUi
    data class Ready(val trips: List<TransitItinerary>) : TransitUi
    data class Failed(val message: String) : TransitUi
}

private val clockFormat: DateTimeFormatter by lazy { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()) }
private fun clock(epochSeconds: Long): String = clockFormat.format(Instant.ofEpochSecond(epochSeconds))

/** Which bus, tram or train to take to the chosen place, from open timetable data. Shown on request, never automatically. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TransitSheet(place: Place, from: Coordinate?, world: World, planner: TransitPlanner, onDismiss: () -> Unit) {
    val palette = hudPalette(world)
    var attempt by remember { mutableIntStateOf(0) }
    var ui by remember { mutableStateOf<TransitUi>(TransitUi.Loading) }
    LaunchedEffect(place.id, from, attempt) {
        if (from == null) { ui = TransitUi.Failed("Waiting for your location. Turn on GPS to plan a bus or train trip."); return@LaunchedEffect }
        ui = TransitUi.Loading
        ui = try {
            TransitUi.Ready(planner.plan(from, place.point))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            TransitUi.Failed(e.message?.takeIf { it.length < 140 } ?: "Public transport is unavailable right now. Try again.")
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.panel, contentColor = palette.foreground) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Bus & transit", style = MaterialTheme.typography.headlineSmall, fontFamily = worldFont(world))
            Text("To ${place.name}", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            when (val current = ui) {
                TransitUi.Loading -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Finding the next buses, trams and trains…", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                is TransitUi.Failed -> {
                    Text(current.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (from != null) Button(onClick = { attempt++ }, Modifier.heightIn(min = 48.dp)) { Text("Try again") }
                }
                is TransitUi.Ready -> {
                    if (current.trips.isEmpty()) {
                        Text("No public transport found for this trip. Timetables come from open data and coverage differs by city, so some areas have none yet.",
                            style = MaterialTheme.typography.bodyMedium)
                    } else if (current.trips.none { it.rides.isNotEmpty() }) {
                        Text("This is close enough to walk. About ${current.trips.first().minutes} min on foot.", style = MaterialTheme.typography.bodyMedium)
                    } else current.trips.forEach { TripCard(it, world) }
                }
            }
            Text("Timetables: Transitous, built on open transit data. Live times show where the operator supplies them. Planning a trip sends your position and this destination to api.transitous.org.",
                style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripCard(trip: TransitItinerary, world: World) {
    val palette = hudPalette(world)
    Surface(color = palette.accent.copy(alpha = .08f), shape = hudShape(world), modifier = Modifier.fillMaxWidth()
        .semantics { contentDescription = "Leave ${clock(trip.departEpochSeconds)}, arrive ${clock(trip.arriveEpochSeconds)}, ${trip.minutes} minutes" }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${clock(trip.departEpochSeconds)} → ${clock(trip.arriveEpochSeconds)}", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.accent, modifier = Modifier.weight(1f))
                Text("${trip.minutes} min", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trip.legs.forEach { leg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (leg.isWalk) Icons.Default.DirectionsWalk else Icons.Default.DirectionsBus, null, Modifier.size(18.dp), tint = palette.muted)
                        Spacer(Modifier.width(4.dp))
                        Text(if (leg.isWalk) "${leg.minutes} min" else "${leg.modeLabel} ${leg.line ?: ""}".trim(),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            trip.legs.forEach { leg ->
                if (leg.isWalk) {
                    if (leg.minutes >= 2) Text("Walk ${leg.minutes} min${leg.meters?.let { " (${it} m)" } ?: ""} to ${leg.to}", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                } else {
                    val line = "${leg.modeLabel} ${leg.line ?: ""}".trim()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Take $line${leg.headsign?.let { " toward $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("From ${leg.from} at ${clock(leg.departEpochSeconds)}${if (leg.liveTimes) " (live)" else ""}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Get off at ${leg.to} at ${clock(leg.arriveEpochSeconds)}${leg.stops?.let { " · $it stop${if (it == 1) "" else "s"}" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
            }
        }
    }
}
