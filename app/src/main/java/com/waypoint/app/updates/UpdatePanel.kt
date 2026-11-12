package com.waypoint.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UpdatePanel(updater: AppUpdater, navigating: Boolean) {
    val status by updater.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val uri = LocalUriHandler.current
    var installMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("App updates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Installed · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }

                when (val current = status) {
                    UpdateStatus.Idle -> {
                        Text("Waypoint checks for stable releases when you open the app. Downloads start only when you choose.",
                            style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Check for updates")
                        }
                    }
                    UpdateStatus.Checking -> {
                        Text("Checking for a stable release", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Checking for app updates" })
                    }
                    UpdateStatus.Current -> {
                        Text("You're up to date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Waypoint ${BuildConfig.VERSION_NAME} is the latest stable release.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                        OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Check again")
                        }
                    }
                    is UpdateStatus.Available -> {
                        Text("Version ${current.release.version} is ready to download", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${String.format(locale, "%.1f", current.release.size / 1048576.0)} MB · Android asks before installation.",
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        ScrollableUpdateNotes(current.release.notes)
                        Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Download update")
                        }
                    }
                    is UpdateStatus.Downloading -> {
                        UpdateDownloadProgress(
                            percent = current.percent,
                            phase = if (current.percent >= 100) "Verifying the update" else "Downloading version ${current.release.version}",
                        )
                        Text("${String.format(locale, "%.1f", current.release.size / 1048576.0)} MB · SHA-256 and app identity checked before install.",
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        ScrollableUpdateNotes(current.release.notes)
                        TextButton(onClick = updater::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel download") }
                    }
                    is UpdateStatus.Ready -> {
                        Text("Version ${current.release.version} is verified", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (navigating) "Finish navigation before installing. Your saved places will be kept."
                            else "The download passed its integrity checks. Android will show its installation confirmation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                        )
                        ScrollableUpdateNotes(current.release.notes)
                        Button(onClick = { installMessage = updater.install(context, current) }, enabled = !navigating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.SystemUpdate, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Install update")
                        }
                        installMessage?.let { UpdateInfoMessage(it) }
                    }
                    is UpdateStatus.Failed -> {
                        UpdateErrorMessage(current.message)
                        if (current.release != null) {
                            Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)) { Text("Retry download") }
                        } else {
                            OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Check again")
                            }
                        }
                    }
                }
            }
        }
        UpdateAlertStatus((context.applicationContext as WaypointApplication).updateAlerts)
        TextButton(onClick = { uri.openUri(ReleaseUpdate.PAGE) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Release notes on GitHub")
        }
    }
}

@Composable
internal fun UpdateDownloadProgress(percent: Int, phase: String, modifier: Modifier = Modifier) {
    val target = percent.coerceIn(0, 100) / 100f
    val animated by animateFloatAsState(targetValue = target,
        animationSpec = tween(240, easing = FastOutSlowInEasing), label = "app-update-progress")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Crossfade(phase, label = "app-update-phase") {
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = "$phase, $percent percent" })
        Text("$percent%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ScrollableUpdateNotes(notes: String) {
    if (notes.isBlank()) return
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ReleaseNotesContent(
            notes,
            Modifier.heightIn(max = 176.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
internal fun UpdateErrorMessage(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp))
    }
}

@Composable
internal fun UpdateInfoMessage(message: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp))
    }
}
