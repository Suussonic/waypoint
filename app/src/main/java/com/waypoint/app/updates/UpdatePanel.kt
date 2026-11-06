package com.waypoint.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun UpdatePanel(updater: AppUpdater, navigating: Boolean) {
    val status by updater.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var installMessage by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App updates", style = MaterialTheme.typography.titleLarge)
        Text("Waypoint ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = Muted)
        when (val current = status) {
            UpdateStatus.Idle -> Text("Waypoint checks GitHub for stable releases when you open the app. Downloads begin only when you ask.", style = MaterialTheme.typography.bodyMedium)
            UpdateStatus.Checking -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Checking the latest release…", style = MaterialTheme.typography.bodyMedium)
            }
            UpdateStatus.Current -> Text("You're up to date.", style = MaterialTheme.typography.bodyMedium)
            is UpdateStatus.Available -> {
                Text("Version ${current.release.version} is available", style = MaterialTheme.typography.titleMedium)
                Text("${String.format(Locale.getDefault(), "%.1f", current.release.size / 1048576.0)} MB · Android will ask before installing.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Download, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Download update")
                }
            }
            is UpdateStatus.Downloading -> {
                LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text(if (current.percent == 100) "Verifying update…" else "Downloading update · ${current.percent}%", style = MaterialTheme.typography.bodyMedium)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                TextButton(onClick = updater::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel download") }
            }
            is UpdateStatus.Ready -> {
                Text("Version ${current.release.version} is ready", style = MaterialTheme.typography.titleMedium)
                Text(if (navigating) "End navigation before installing. Your saved places will be kept." else "Download verified. Android may ask you to allow updates from Waypoint. Your saved places will be kept.", style = MaterialTheme.typography.bodyMedium)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                Button(onClick = { installMessage = updater.install(context, current) }, enabled = !navigating, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.SystemUpdate, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Install update")
                }
                installMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            }
            is UpdateStatus.Failed -> Text(current.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (status == UpdateStatus.Idle || status == UpdateStatus.Current || status is UpdateStatus.Failed) {
            OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (status is UpdateStatus.Failed) "Try again" else "Check for updates")
            }
        }
        UpdateAlertStatus((context.applicationContext as WaypointApplication).updateAlerts)
        TextButton(onClick = { uri.openUri(ReleaseUpdate.PAGE) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Release notes on GitHub") }
    }
}
