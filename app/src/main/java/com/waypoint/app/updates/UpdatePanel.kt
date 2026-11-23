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
import androidx.compose.ui.res.stringResource
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
                        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.update_installed,BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }

                when (val current = status) {
                    UpdateStatus.Idle -> {
                        Text(stringResource(R.string.update_intro),
                            style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.update_check))
                        }
                    }
                    UpdateStatus.Checking -> {
                        Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodyMedium)
                        val description=stringResource(R.string.update_checking_accessibility)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = description })
                    }
                    UpdateStatus.Current -> {
                        Text(stringResource(R.string.update_current), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.update_latest,BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = Muted)
                        OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.update_check_again))
                        }
                    }
                    is UpdateStatus.Available -> {
                        Text(stringResource(R.string.update_available,current.release.version), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.update_size_confirm,String.format(locale, "%.1f", current.release.size / 1048576.0)),
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        ScrollableUpdateNotes(current.release.notes)
                        Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(stringResource(R.string.update_download))
                        }
                    }
                    is UpdateStatus.Downloading -> {
                        UpdateDownloadProgress(
                            percent = current.percent,
                            phase = if (current.percent >= 100) stringResource(R.string.update_verifying) else stringResource(R.string.update_downloading,current.release.version),
                        )
                        Text(stringResource(R.string.update_size_verify,String.format(locale, "%.1f", current.release.size / 1048576.0)),
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                        ScrollableUpdateNotes(current.release.notes)
                        TextButton(onClick = updater::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.update_cancel)) }
                    }
                    is UpdateStatus.Ready -> {
                        Text(stringResource(R.string.update_verified,current.release.version), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(if (navigating) R.string.update_finish_navigation else R.string.update_integrity_passed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                        )
                        ScrollableUpdateNotes(current.release.notes)
                        Button(onClick = { installMessage = updater.install(context, current) }, enabled = !navigating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.SystemUpdate, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(stringResource(R.string.update_install))
                        }
                        installMessage?.let { UpdateInfoMessage(it) }
                    }
                    is UpdateStatus.Failed -> {
                        UpdateErrorMessage(current.message)
                        if (current.release != null) {
                            Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.update_retry)) }
                        } else {
                            OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.update_check_again))
                            }
                        }
                    }
                }
            }
        }
        UpdateAlertStatus((context.applicationContext as WaypointApplication).updateAlerts)
        TextButton(onClick = { uri.openUri(ReleaseUpdate.PAGE) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.update_release_notes))
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
        val description=stringResource(R.string.update_progress,phase,percent)
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = description })
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
