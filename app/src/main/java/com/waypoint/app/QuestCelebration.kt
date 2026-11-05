package com.waypoint.app

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/** Rewards remain pending during a trip and are announced only on a resumed phone screen. */
@Composable internal fun QuestCelebration(world: World, controller: QuestController, allowed: Boolean,
                                          onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val snapshot by controller.state.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val ready = allowed && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val pending = snapshot.rewards.filter { !it.presented }
    var batch by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var played by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val rewards = snapshot.rewards.filter { it.questId in batch }
    fun dismiss() { controller.presented(batch); batch = emptyList() }
    LaunchedEffect(ready, pending.map { it.questId }, snapshot.profile.showProgress) {
        if (ready && batch.isEmpty() && pending.isNotEmpty()) {
            if (snapshot.profile.showProgress) batch = pending.map { it.questId }
            else controller.presented(pending.map { it.questId })
        }
    }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(ready, batch) {
        if (ready && batch.isNotEmpty()) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(8000, containsText = true, containsControls = true) ?: 8000)
            dismiss()
        }
    }
    if (!ready || rewards.isEmpty() || !snapshot.profile.showProgress) return
    val quest = snapshot.quests.find { it.id == rewards.singleOrNull()?.questId }
    val context = LocalContext.current
    DisposableEffect(batch) {
        val player = if (snapshot.profile.sound && played != batch) completionSound(context, world) else null
        played = batch
        onDispose { player?.release() }
    }
    QuestTheme(world) {
        Surface(modifier.widthIn(max = 560.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            shape = hudShape(world), border = hudBorder(world), tonalElevation = 6.dp, shadowElevation = 4.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuestPortrait(quest ?: QuestRecord(title = "Your journey"), Modifier.size(44.dp), snapshot.profile.frame.takeIf { it in snapshot.progress.frames } ?: "Simple")
                    Column(Modifier.weight(1f)) {
                        Text(if (rewards.size == 1) "Quest complete" else "${rewards.size} quests completed", style = MaterialTheme.typography.titleLarge)
                        Text("+${rewards.sumOf { it.xp }} XP · Level ${snapshot.progress.level}", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = ::dismiss) { Icon(Icons.Default.Close, "Dismiss reward") }
                }
                Text(quest?.title ?: "Your rewards are ready. Take a moment to enjoy your progress.", maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rewards.size == 1 && quest != null) TextButton(onClick = { controller.reopen(quest.id); dismiss() }) { Text("Undo") }
                    TextButton(onClick = { dismiss(); onOpen() }) { Text("View quests") }
                }
            }
        }
    }
}

private fun completionSound(context: Context, world: World): MediaPlayer? {
    val audio = context.getSystemService(AudioManager::class.java)
    val notifications = context.getSystemService(NotificationManager::class.java)
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL || notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return null
    val player = MediaPlayer()
    return try {
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        context.assets.openFd("quest-sounds/${world.file}.wav").use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        player.setVolume(.35f, .35f)
        player.setOnPreparedListener { it.start() }
        player.setOnErrorListener { _, _, _ -> true }
        player.prepareAsync()
        player
    } catch (_: Exception) { player.release(); null }
}
