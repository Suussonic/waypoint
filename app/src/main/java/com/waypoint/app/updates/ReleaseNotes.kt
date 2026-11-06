package com.waypoint.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

private fun plainNote(line: String): String = line
    .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
    .replace("**", "")
    .replace("`", "")

/** A small, readable renderer for the Markdown used by our bundled and GitHub release notes. */
@Composable
fun ReleaseNotesContent(notes: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notes.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            when {
                line.startsWith("# ") -> Text(
                    plainNote(line.removePrefix("# ")),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                line.startsWith("### ") -> Text(
                    plainNote(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                line.startsWith("- ") -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(plainNote(line.removePrefix("- ")), style = MaterialTheme.typography.bodySmall)
                }
                else -> Text(plainNote(line), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WhatsNewDialog(notes: String, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("What’s new in Waypoint") },
        text = {
            ReleaseNotesContent(
                notes,
                Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Explore map") } },
        dismissButton = { TextButton(onClick = { uri.openUri(ReleaseUpdate.PAGE) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("GitHub release") } },
    )
}
