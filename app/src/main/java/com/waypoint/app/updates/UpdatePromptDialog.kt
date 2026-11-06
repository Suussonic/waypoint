package com.waypoint.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun UpdatePromptDialog(
    release: ReleaseUpdate,
    updater: AppUpdater,
    navigating: Boolean,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    val status by updater.state.collectAsState()
    var installMessage by remember(release.version) { mutableStateOf<String?>(null) }
    val size = String.format(LocalConfiguration.current.locales[0], "%.1f", release.size / 1048576.0)

    AlertDialog(
        onDismissRequest = onLater,
        containerColor = Panel,
        title = { Text("Waypoint update available") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Version ${release.version} is ready. Review what's new, then download it. Android will ask before installation.", style = MaterialTheme.typography.bodyMedium)
                Text("$size MB · SHA-256 verified before installation", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (release.notes.isNotBlank()) ReleaseNotesContent(release.notes)
                when (val current = status) {
                    is UpdateStatus.Downloading -> {
                        LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(if (current.percent == 100) "Verifying the APK…" else "Downloading · ${current.percent}%", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = updater::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel download") }
                    }
                    is UpdateStatus.Ready -> Text("Verified and ready. Android will show its installation confirmation.", style = MaterialTheme.typography.bodySmall)
                    is UpdateStatus.Failed -> Text(current.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }
                installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            when (val current = status) {
                is UpdateStatus.Ready -> Button(
                    onClick = { installMessage = updater.install(context, current) },
                    enabled = !navigating,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.SystemUpdate, null)
                    Text("Install update")
                }
                is UpdateStatus.Downloading -> Unit
                else -> Button(
                    onClick = { updater.download(release) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Download, null)
                    Text(if (current is UpdateStatus.Failed) "Try download again" else "Download update")
                }
            }
        },
        dismissButton = {
            Row { TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) { Text("Later") } }
        },
    )
}
