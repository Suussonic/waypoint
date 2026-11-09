package com.waypoint.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Open invite to the Waypoint Discord: a private channel for each world, release notes and bug reports. */
const val DISCORD_INVITE_URL = "https://discord.gg/3ddSCASNUn"

/** Settings entry that hands the invite to the browser or the Discord app. Nothing is sent from the phone. */
@Composable
internal fun CommunitySettingsRow() {
    val uri = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { runCatching { uri.openUri(DISCORD_INVITE_URL) } }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Forum, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text("Join the Waypoint Discord", style = MaterialTheme.typography.titleMedium)
            Text("A channel for each world, release notes and bug reports", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp), tint = Muted)
    }
}
