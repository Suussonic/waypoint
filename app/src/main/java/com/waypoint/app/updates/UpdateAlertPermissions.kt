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
import androidx.compose.ui.res.stringResource
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
        title={Text(stringResource(if(batteryStep)R.string.update_permission_battery_title else R.string.update_permission_title), fontWeight=FontWeight.SemiBold)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if(batteryStep)R.string.update_permission_battery_description else R.string.update_permission_description))
            Surface(color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.6f), shape=RoundedCornerShape(12.dp)) {
                Text(stringResource(if(batteryStep)R.string.update_permission_battery_detail else R.string.update_permission_detail),
                    style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(12.dp))
            }
        }},
        confirmButton={TextButton(onClick={
            if(batteryStep) { finish();alerts.openBatterySettings() }
            else if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { finish();alerts.openNotificationSettings() }
        },modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(if(batteryStep)R.string.update_battery_settings else R.string.update_allow_notifications)) }},
        dismissButton={TextButton(onClick=::finish,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.update_not_now)) }})
}

@Composable internal fun UpdateAlertStatus(alerts:UpdateAlerts) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val notifications=remember(lifecycle){alerts.allowed()}
    val unrestricted=remember(lifecycle){alerts.batteryUnrestricted()}
    Surface(color=Panel.copy(alpha=.72f),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.update_alerts_title),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                    Text(stringResource(if(alerts.scheduled())R.string.update_alerts_scheduled else R.string.update_alerts_unavailable),
                        style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(if(alerts.scheduled())R.string.update_alerts_info else R.string.update_alerts_foreground),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(if(notifications)R.string.update_notifications_allowed else R.string.update_notifications_off),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick=alerts::openNotificationSettings,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.update_notification_settings)) }
            if(!unrestricted) {
                Text(stringResource(R.string.update_battery_info),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick=alerts::openBatterySettings,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.update_battery_settings)) }
            }
        }
    }
}
