package com.waypoint.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
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
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Waypoint update", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Version ${release.version}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                // AlertDialog keeps its title outside the scrolling message area, so download
                // progress remains visible while release notes and details are being read.
                when (val current = status) {
                    is UpdateStatus.Downloading -> Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        UpdateDownloadProgress(
                            current.percent,
                            if (current.percent >= 100) "Verifying the update" else "Downloading version ${release.version}",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                    UpdateStatus.Checking -> Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Checking for a stable release", style = MaterialTheme.typography.labelLarge)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    else -> Unit
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                            androidx.compose.material3.Icon(Icons.Default.SystemUpdate, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(9.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Stable release", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("$size MB · Verified before install", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Review what's new, then download when you're ready. Android will ask you to confirm installation.",
                    style = MaterialTheme.typography.bodyMedium)
                when (val current = status) {
                    is UpdateStatus.Downloading, UpdateStatus.Checking -> Unit
                    is UpdateStatus.Ready -> UpdateInfoMessage("Verified and ready. Android will show its installation confirmation.")
                    is UpdateStatus.Failed -> UpdateErrorMessage(current.message)
                    else -> Unit
                }
                if (release.notes.isNotBlank()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ReleaseNotesContent(release.notes)
                }
                installMessage?.let { UpdateInfoMessage(it) }
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
                    Text("Install")
                }
                is UpdateStatus.Downloading -> TextButton(
                    onClick = updater::cancel,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Cancel download") }
                is UpdateStatus.Failed -> Button(
                    onClick = { updater.download(release) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Download, null)
                    Text("Retry download")
                }
                UpdateStatus.Checking -> TextButton(onClick = {}, enabled = false, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Checking…")
                }
                else -> Button(
                    onClick = { updater.download(release) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Download, null)
                    Text("Download update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) { Text("Later") }
        },
    )
}
