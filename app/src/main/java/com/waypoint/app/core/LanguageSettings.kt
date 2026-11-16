package com.waypoint.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

@Composable
internal fun LanguageSettings() {
    var expanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val selected = if (configuration.locales[0].language == "fr") "fr" else "en"
    val languages = listOf("en" to stringResource(R.string.language_english), "fr" to stringResource(R.string.language_french))

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language_title),style=MaterialTheme.typography.titleMedium)
        Box {
            Surface(
                modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(role=Role.Button) { expanded=true },
                shape=RoundedCornerShape(12.dp),
                color=Panel,
            ) {
                Row(Modifier.padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Language,null,tint=WaypointColors.accent)
                    Spacer(Modifier.width(12.dp))
                    Text(languages.first { it.first==selected }.second,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Default.ExpandMore,null,tint=Muted)
                }
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},containerColor=Panel) {
                languages.forEach { (tag,label) ->
                    DropdownMenuItem(
                        text={Text(label)},
                        onClick={
                            expanded=false
                            if(AppCompatDelegate.getApplicationLocales()[0]?.language!=tag)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        },
                        trailingIcon={if(tag==selected)Icon(Icons.Default.Check,null,tint=WaypointColors.accent)},
                    )
                }
            }
        }
    }
}
