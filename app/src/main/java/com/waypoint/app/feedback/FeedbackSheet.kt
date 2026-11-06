package com.waypoint.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

private const val MAX_FEEDBACK_TITLE = 120
private const val MAX_FEEDBACK_DETAILS = 1_500

@Composable
internal fun FeedbackSettingsRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.RateReview, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text("Send feedback", style = MaterialTheme.typography.titleMedium)
            Text("Open a GitHub issue in your words", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp), tint = Muted)
    }
}

@Composable
internal fun FeedbackSheet(world: World, onBack: () -> Unit, onLeaveApp: () -> Unit) {
    val context = LocalContext.current
    var kindName by rememberSaveable { mutableStateOf(FeedbackKind.BUG.name) }
    var title by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var browserError by remember { mutableStateOf(false) }
    val kind = FeedbackKind.entries.firstOrNull { it.name == kindName } ?: FeedbackKind.BUG
    val draftUrl = feedbackIssueUrl(kind, title, details, world.title, BuildConfig.VERSION_NAME)
    val tooLong = title.isNotBlank() && details.isNotBlank() && draftUrl == null

    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to settings")
            }
            Text("Send feedback", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "Tell us what happened or what you would like to see. Your report opens on GitHub for you to review and submit.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedbackKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kindName = option.name },
                    label = { Text(option.label) },
                )
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(MAX_FEEDBACK_TITLE) },
            label = { Text("Issue title") },
            supportingText = { Text("${title.length}/$MAX_FEEDBACK_TITLE") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = details,
            onValueChange = { details = it.take(MAX_FEEDBACK_DETAILS) },
            label = { Text("Your experience or idea") },
            supportingText = { Text("${details.length}/$MAX_FEEDBACK_DETAILS") },
            minLines = 5,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Only your words, the selected world and the app version are added. Your location and routes are not attached.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        if (tooLong) Text(
            "This report is too long for a GitHub link. Shorten it, then try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        if (browserError) Text(
            "No app can open GitHub. Install a browser and try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = {
                if (draftUrl != null) {
                    browserError = !openGitHub(context, draftUrl)
                    if (!browserError) onLeaveApp()
                }
            },
            enabled = draftUrl != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Review issue on GitHub")
        }
        TextButton(
            onClick = {
                browserError = !openGitHub(context, WAYPOINT_ISSUES_URL)
                if (!browserError) onLeaveApp()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Browse existing issues") }
        Spacer(Modifier.height(16.dp))
    }
}

internal fun openGitHub(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
