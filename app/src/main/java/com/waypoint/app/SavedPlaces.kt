package com.waypoint.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal const val SAVED_PLACE_NAME_LIMIT = 80
internal fun savedPlaceName(value: String): String? = value.trim().takeIf {
    it.isNotBlank() && it.length <= SAVED_PLACE_NAME_LIMIT && it.none { c -> c.isISOControl() }
}

@Composable fun SavedScreen(state: NavState, onSelect: (Place) -> Unit, onDelete: (Place) -> Unit,
    onSearch: () -> Unit, onRename: (String, String) -> Boolean) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var label by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Ink).statusBarsPadding(), contentPadding = PaddingValues(vertical = 24.dp)) {
        item {
            Text("Your places", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Text("Your favourite destinations, with names you choose.",
                style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        if (state.saved.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(36.dp))
            Icon(Icons.Default.Bookmarks, null, tint = state.world.accent, modifier = Modifier.size(48.dp))
            Text("No saved places yet", style = MaterialTheme.typography.titleLarge)
            Text("Search for a destination and tap the bookmark to keep it here.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onSearch) { Text("Find a place") }
        } }
        items(state.saved, key = { it.id }) { place ->
            var menu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PlaceRow(place, state, onSelect) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options for ${place.name}", tint = Cream) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; label = place.name; error = false; editingId = place.id })
                        DropdownMenuItem(text = { Text("Remove from saved") }, leadingIcon = { Icon(Icons.Default.BookmarkRemove, null) },
                            onClick = { menu = false; onDelete(place) })
                    }
                }
            }
        }
    }
    state.saved.firstOrNull { it.id == editingId }?.let { place ->
        AlertDialog(onDismissRequest = { editingId = null }, title = { Text("Name this place") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(place.address, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(label, onValueChange = { label = it.take(SAVED_PLACE_NAME_LIMIT); error = false },
                    label = { Text("Place name") }, placeholder = { Text("For example, Work") }, singleLine = true,
                    isError = error, supportingText = { Text(if(error) "Enter a name for this place." else "Only the name changes. The location stays the same.") },
                    modifier = Modifier.fillMaxWidth())
            } },
            confirmButton = { TextButton(onClick = { if(onRename(place.id, label)) editingId = null else error = true },
                enabled = savedPlaceName(label) != null) { Text("Save name") } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("Cancel") } })
    }
}
