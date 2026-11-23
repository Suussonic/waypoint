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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
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

@Composable
private fun clock(epochSeconds: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochSeconds, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(epochSeconds))
    }
}

@Composable
private fun transitLabel(leg: TransitLeg): String = stringResource(when (transitModeLabel(leg.mode)) {
    "Bus" -> R.string.transit_bus
    "Tram" -> R.string.transit_tram
    "Metro" -> R.string.transit_metro
    "Train" -> R.string.transit_train
    "Ferry" -> R.string.transit_ferry
    "Flight" -> R.string.transit_flight
    else -> R.string.transit_other
})

/** Which bus, tram or train to take to the chosen place, from open timetable data. Shown on request, never automatically. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TransitSheet(place: Place, from: Coordinate?, world: World, planner: TransitPlanner, onDismiss: () -> Unit) {
    val palette = hudPalette(world)
    var attempt by remember { mutableIntStateOf(0) }
    var ui by remember { mutableStateOf<TransitUi>(TransitUi.Loading) }
    val waitingMessage = stringResource(R.string.transit_waiting)
    val failureMessage = stringResource(R.string.transit_unavailable)
    LaunchedEffect(place.id, from, attempt, waitingMessage, failureMessage) {
        if (from == null) { ui = TransitUi.Failed(waitingMessage); return@LaunchedEffect }
        ui = TransitUi.Loading
        ui = try {
            TransitUi.Ready(planner.plan(from, place.point))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TransitSheet", "Transit planning failed: ${e.javaClass.simpleName}")
            TransitUi.Failed(failureMessage)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.panel, contentColor = palette.foreground) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.transit_title), style = MaterialTheme.typography.headlineSmall, fontFamily = worldFont(world))
            Text(stringResource(R.string.transit_to, place.name), style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            when (val current = ui) {
                TransitUi.Loading -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.transit_loading), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                is TransitUi.Failed -> {
                    Text(current.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (from != null) Button(onClick = { attempt++ }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.transit_retry)) }
                }
                is TransitUi.Ready -> {
                    if (current.trips.isEmpty()) {
                        Text(stringResource(R.string.transit_empty),
                            style = MaterialTheme.typography.bodyMedium)
                    } else if (current.trips.none { it.rides.isNotEmpty() }) {
                        Text(stringResource(R.string.transit_walk_only, current.trips.first().minutes), style = MaterialTheme.typography.bodyMedium)
                    } else current.trips.forEach { TripCard(it, world) }
                }
            }
            Text(stringResource(R.string.transit_attribution),
                style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripCard(trip: TransitItinerary, world: World) {
    val palette = hudPalette(world)
    val description = stringResource(R.string.transit_trip_description, clock(trip.departEpochSeconds), clock(trip.arriveEpochSeconds), trip.minutes)
    Surface(color = palette.accent.copy(alpha = .08f), shape = hudShape(world), modifier = Modifier.fillMaxWidth()
        .semantics { contentDescription = description }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${clock(trip.departEpochSeconds)} → ${clock(trip.arriveEpochSeconds)}", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.accent, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.transit_minutes, trip.minutes), style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trip.legs.forEach { leg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (leg.isWalk) Icons.Default.DirectionsWalk else Icons.Default.DirectionsBus, null, Modifier.size(18.dp), tint = palette.muted)
                        Spacer(Modifier.width(4.dp))
                        Text(if (leg.isWalk) stringResource(R.string.transit_minutes, leg.minutes) else "${transitLabel(leg)} ${leg.line ?: ""}".trim(),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            trip.legs.forEach { leg ->
                if (leg.isWalk) {
                    if (leg.minutes >= 2) Text(
                        if (leg.meters != null) stringResource(R.string.transit_walk_distance, leg.minutes, leg.meters, leg.to)
                        else stringResource(R.string.transit_walk_to, leg.minutes, leg.to),
                        style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                } else {
                    val line = "${transitLabel(leg)} ${leg.line ?: ""}".trim()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (leg.headsign != null) stringResource(R.string.transit_take_toward, line, leg.headsign) else stringResource(R.string.transit_take, line), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(if (leg.liveTimes) R.string.transit_from_live else R.string.transit_from, leg.from, clock(leg.departEpochSeconds)),
                            style = MaterialTheme.typography.bodySmall)
                        val stops = leg.stops?.let { " · " + pluralStringResource(R.plurals.transit_stops, it, it) }.orEmpty()
                        Text(stringResource(R.string.transit_get_off, leg.to, clock(leg.arriveEpochSeconds)) + stops,
                            style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
            }
        }
    }
}
