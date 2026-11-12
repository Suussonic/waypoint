package com.waypoint.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/** One explanation, after onboarding, never over guidance or another permission flow. */
@Composable internal fun UpdateAlertPermissions(alerts:UpdateAlerts,ready:Boolean) {
    val context=LocalContext.current
    var pending by rememberSaveable { mutableStateOf(!alerts.explained) }
    var batteryStep by rememberSaveable { mutableStateOf(alerts.allowed()) }
    fun finish() { alerts.markExplained();pending=false }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted)batteryStep=true else finish()
    }
    if(!ready || !pending)return
    if(batteryStep && alerts.batteryUnrestricted()) {
        LaunchedEffect(Unit) { finish() }
        return
    }
    AlertDialog(onDismissRequest=::finish,
        title={Text(if(batteryStep)"Keep update alerts timely" else "Know when Waypoint is updated", fontWeight=FontWeight.SemiBold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(if(batteryStep)
                "Waypoint checks for new releases about twice a day while the app is closed. Restricted battery access can delay checks."
                else "Waypoint checks for stable releases automatically. Allow notifications to hear about new versions; alerts wait while you are driving.")
            Surface(color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.6f), shape=RoundedCornerShape(12.dp)) {
                Text(if(batteryStep)
                    "Android's battery setting can reduce delays, but may use more battery. Checks are automatic; downloads and installation are always your choice. You can skip this and keep using the app."
                    else "An alert opens update details. Nothing is downloaded or installed until you choose.",
                    style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(12.dp))
            }
        }},
        confirmButton={TextButton(onClick={
            if(batteryStep) { finish();alerts.openBatterySettings() }
            else if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { finish();alerts.openNotificationSettings() }
        },modifier=Modifier.heightIn(min=48.dp)) { Text(if(batteryStep)"Review battery access" else "Allow notifications") }},
        dismissButton={TextButton(onClick=::finish,modifier=Modifier.heightIn(min=48.dp)) { Text("Not now") }})
}

@Composable internal fun UpdateAlertStatus(alerts:UpdateAlerts) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val notifications=remember(lifecycle){alerts.allowed()}
    val unrestricted=remember(lifecycle){alerts.batteryUnrestricted()}
    Surface(color=Panel.copy(alpha=.72f),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("Automatic update alerts",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                    Text(if(alerts.scheduled())"Checks run about twice a day" else "Background checks unavailable",
                        style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                }
            }
            Text(if(alerts.scheduled())"Waypoint checks when connected and the battery is not low. Android may delay checks. Downloads and installation are always your choice."
                else "Waypoint still checks for updates when you open the app.",style=MaterialTheme.typography.bodySmall)
            Text(if(notifications)"Notifications are allowed." else "Notifications are off. Allow App updates in Android to receive new-release alerts.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick=alerts::openNotificationSettings,modifier=Modifier.heightIn(min=48.dp)) { Text("Notification settings") }
            if(!unrestricted) {
                Text("Battery restrictions may delay alerts. Unrestricted background use can use more battery and is optional.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick=alerts::openBatterySettings,modifier=Modifier.heightIn(min=48.dp)) { Text("Review battery access") }
            }
        }
    }
}
