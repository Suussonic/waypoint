package com.waypoint.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Phone setup explains actual connection/permission state without promising launcher eligibility. */
@Composable
internal fun AndroidAutoSettingsPanel(connected: Boolean, preciseLocation: Boolean, navigating: Boolean,
    onLocation: () -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.car_settings_title), style = MaterialTheme.typography.titleLarge)
        }
        CarSetupStatus(if(connected)Icons.Default.CheckCircle else Icons.Default.Info,
            stringResource(if(connected)R.string.car_connected else R.string.car_ready_to_connect),
            stringResource(if(connected)R.string.car_sync_explanation else R.string.car_connect_explanation))
        CarSetupStatus(Icons.Default.LocationOn,
            stringResource(if(preciseLocation)R.string.car_location_ready else R.string.car_location_needed),
            stringResource(if(preciseLocation)R.string.car_location_ready_detail else R.string.car_location_needed_detail))
        if(!connected) Text(stringResource(R.string.car_distribution_explanation),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { if(!openAndroidAutoSettings(context))onUnavailable() },
            enabled = !navigating, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.car_open_settings))
        }
        if(!preciseLocation) TextButton(onClick = onLocation, enabled = !navigating,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.car_location_settings))
        }
    }
}

@Composable private fun CarSetupStatus(icon: ImageVector, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.padding(top = 2.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun openAndroidAutoSettings(context: Context): Boolean {
    val host = "com.google.android.projection.gearhead"
    val settings = Intent("com.google.android.projection.gearhead.SETTINGS").setPackage(host)
    val intent = if(settings.resolveActivity(context.packageManager) != null) settings else {
        // Some hosts don't expose their settings action. Android's app-info page is a safe fallback.
        try { context.packageManager.getApplicationInfo(host,0) }
        catch(_: android.content.pm.PackageManager.NameNotFoundException) { return false }
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$host"))
    }
    return try { context.startActivity(intent);true }
    catch(_: ActivityNotFoundException) { false }
    catch(_: SecurityException) { false }
}
