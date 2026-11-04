package com.waypoint.app

import androidx.compose.animation.core.tween

import androidx.compose.ui.semantics.contentDescription

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
internal fun QuestTheme(world: World, adaptSystemBars: Boolean = false, content: @Composable () -> Unit) {
    val p = hudPalette(world)
    val base = if (p.panel.luminance() > .5f) lightColorScheme() else darkColorScheme()
    val surface = p.panel.copy(alpha = 1f)
    val accent = questInk(p.accent, surface)
    val container = accent.copy(alpha = .14f).compositeOver(surface)
    val view = LocalView.current
    val pageSystemBars = LocalPageSystemBarSurface.current
    DisposableEffect(view, world, adaptSystemBars) {
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<android.app.Activity>().firstOrNull()
        val bars = if (adaptSystemBars && activity != null) WindowCompat.getInsetsController(activity.window, view) else null
        val previousStatus = bars?.isAppearanceLightStatusBars
        val previousNavigation = bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars = surface.luminance() > .5f
        bars?.isAppearanceLightNavigationBars = surface.luminance() > .5f
        if (adaptSystemBars) pageSystemBars(surface)
        onDispose {
            if (adaptSystemBars) pageSystemBars(null)
            if (previousStatus != null) bars?.isAppearanceLightStatusBars = previousStatus
            if (previousNavigation != null) bars?.isAppearanceLightNavigationBars = previousNavigation
        }
    }
    MaterialTheme(colorScheme = base.copy(primary = accent, onPrimary = questInk(surface, accent),
        primaryContainer = container, onPrimaryContainer = questInk(p.foreground, container), surfaceTint = accent,
        surface = surface, background = surface, onBackground = questInk(p.foreground, surface), onSurface = questInk(p.foreground, surface),
        onSurfaceVariant = questInk(p.muted, surface), surfaceVariant = p.foreground.copy(alpha = .08f).compositeOver(surface), secondary = accent,
        secondaryContainer = container, onSecondaryContainer = questInk(p.foreground, container),
        outline = questInk(p.muted, surface, 3f)), typography = MaterialTheme.typography.copy(
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = worldFont(world)),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = hudFont(world)))) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestsScreen(world: World, controller: QuestController, navigation: NavigationController, nav: NavState,
                 openId: String?, seedPlace: Place?, entry: Int, onNavigate: (Place) -> Unit, onClose: () -> Unit,
                 onEntryConsumed: () -> Unit = {}) {
    val snapshot by controller.state.collectAsStateWithLifecycle()
    val compactHeight = LocalConfiguration.current.screenHeightDp < 500
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var profile by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var template by rememberSaveable { mutableStateOf("Everyday task") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var seedName by rememberSaveable { mutableStateOf<String?>(null) }
    var seedAddress by rememberSaveable { mutableStateOf("") }
    var seedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var seedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    LaunchedEffect(entry) {
        if (openId != null) detail = openId
        if (seedPlace != null) {
            seedName = seedPlace.name; seedAddress = seedPlace.address
            seedLatitude = seedPlace.point.lat; seedLongitude = seedPlace.point.lon
            creating = true; template = "Visit a place"
        }
        onEntryConsumed()
    }
    fun endEdit() { creating = false; editing = null; seedName = null; seedLatitude = null; seedLongitude = null }
    fun finish(id: String) { controller.complete(id) {
        detail = null
        if (!snapshot.profile.showProgress) scope.launch { if (snackbar.showSnackbar("Quest completed", "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) controller.reopen(id) }
    } }
    QuestTheme(world, adaptSystemBars = true) {
        BackHandler(detail != null || profile) { detail = null; profile = false }
        when {
            creating || editing != null -> QuestEditor(world, snapshot.quests.find { it.id == editing },
                if (creating && seedName != null && seedLatitude != null && seedLongitude != null) Place("quest-seed", seedName!!, seedAddress, Coordinate(seedLatitude!!, seedLongitude!!)) else null,
                template, controller, navigation, nav, onSaved = ::endEdit, onClose = ::endEdit)
            profile -> QuestProfileScreen(world, snapshot, controller) { profile = false }
            detail != null -> {
                val quest = snapshot.quests.find { it.id == detail }
                if (quest != null) QuestDetailScreen(world, quest, snapshot.focus, controller, onNavigate,
                    onEdit = { editing = quest.id }, onComplete = { finish(quest.id) }, onBack = { detail = null })
                else if (!snapshot.loading) LaunchedEffect(detail) { detail = null }
            }
            else -> Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) },
                topBar = { TopAppBar(title = { Text("Quests", style = MaterialTheme.typography.headlineMedium) },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to map") } },
                    actions = { IconButton(onClick = { profile = true }, modifier = Modifier.semantics { contentDescription = "Your rewards and quest settings" }) {
                        QuestPortrait(QuestRecord(title = "Your profile"), Modifier.size(32.dp), snapshot.profile.frame.takeIf { it in snapshot.progress.frames } ?: "Simple")
                    } }) },
                bottomBar = { Surface {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Button(onClick = { template = "Everyday task"; creating = true }, shape = hudShape(world),
                            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(min = 52.dp).semantics { contentDescription = "Add a quest" }) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add a quest")
                        }
                    }
                } }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (!compactHeight) item { Text("Your tasks, with rewards", style = MaterialTheme.typography.titleLarge) }
                        if (!compactHeight && snapshot.profile.showProgress && snapshot.progress.completed > 0) item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Level ${snapshot.progress.level}", fontWeight = FontWeight.Bold)
                                    Text("${snapshot.progress.xp} XP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(progress = { snapshot.progress.levelProgress }, modifier = Modifier.fillMaxWidth())
                                Text(nextLevelText(snapshot.progress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item {
                            ScrollableTabRow(selectedTabIndex = if (completed) 1 else 0, edgePadding = 0.dp) {
                                Tab(selected = !completed, onClick = { completed = false }, text = { Text("Active", maxLines = 1) })
                                Tab(selected = completed, onClick = { completed = true }, text = { Text("Completed", maxLines = 1) })
                            }
                        }
                        if (snapshot.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        val quests = snapshot.quests.filter { (it.completedAt != null) == completed }
                        if (!snapshot.loading && quests.isEmpty()) item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(if (completed) Icons.Default.TaskAlt else Icons.Default.Assignment, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(if (completed) "Small wins belong here" else "Start with something simple", style = MaterialTheme.typography.headlineSmall)
                                Text(if (completed) "Complete a quest and it will appear here. There is no deadline to get started." else "Turn everyday tasks into quests. Complete them to earn rewards. A title is all you need.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!completed) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Everyday task", "Visit a place", "Focus session").forEach { name ->
                                        OutlinedButton(onClick = { template = name; creating = true }) { Text(name) }
                                    }
                                }
                            }
                        }
                        items(quests, key = { it.id }) { quest ->
                            Column(Modifier.animateItem(fadeInSpec=tween(150),placementSpec=tween(220),fadeOutSpec=tween(100))) {
                            ListItem(headlineContent = { Text(quest.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(questSummary(quest), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { QuestPortrait(quest, Modifier.size(48.dp)) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, "View quest") },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth().clickable { detail = quest.id })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
                            }
                        }
                    }
                }
            }
        }
        snapshot.error?.let { message -> AlertDialog(onDismissRequest = controller::dismissError,
            title = { Text("Please try again") }, text = { Text(message) }, confirmButton = { TextButton(onClick = controller::dismissError) { Text("OK") } }) }
    }
}

private fun nextLevelText(progress: QuestProgress): String = "Complete ${progress.nextLevelTasks} more ${if (progress.nextLevelTasks == 1) "quest" else "quests"} to reach Level ${progress.level + 1}."
private fun questSummary(quest: QuestRecord): String = when {
    quest.completedAt != null -> "Completed · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(quest.completedAt))}"
    quest.reminderAt != null -> "${if (quest.reminderAt < System.currentTimeMillis()) "Ready when you are" else "Reminder"} · ${questDate(quest.reminderAt)}"
    quest.placeName != null -> quest.placeName
    quest.hasFocus -> "${quest.focusMinutes}-minute focus session"
    quest.notes.isNotBlank() -> quest.notes
    else -> "Ready when you are"
}
private fun questDate(value: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))

@Composable
internal fun QuestPortrait(quest: QuestRecord, modifier: Modifier = Modifier, frame: String = "Simple") {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, quest.imageName) {
        value = withContext(Dispatchers.IO) { quest.imageName?.let { QuestImages.file(context, it) }?.takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) } }
    }
    val border = when (frame) { "Wayfinder" -> 3.dp; "Explorer", "Scout" -> 2.dp; else -> 1.dp }
    val shape = when (frame) { "Scout" -> RoundedCornerShape(12.dp); "Explorer" -> CutCornerShape(12.dp); "Wayfinder" -> RoundedCornerShape(18.dp, 4.dp, 18.dp, 4.dp); else -> CircleShape }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.secondaryContainer)
        .border(border, MaterialTheme.colorScheme.primary, shape), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), quest.characterName.ifBlank { "Quest portrait" }, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(when (quest.portrait) { "guide" -> Icons.Default.Explore; "companion" -> Icons.Default.Pets; else -> Icons.Default.Person },
            quest.characterName.ifBlank { "${quest.portrait.replaceFirstChar { it.titlecase() }} portrait" }, Modifier.fillMaxSize(.56f), tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuestDetailScreen(world: World, quest: QuestRecord, focus: FocusRecord?, controller: QuestController,
                              onNavigate: (Place) -> Unit, onEdit: () -> Unit, onComplete: () -> Unit, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val busy by controller.busy.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Your quest") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to quests") } },
        actions = { TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                QuestPortrait(quest, Modifier.size(72.dp))
                if (quest.characterName.isNotBlank()) Text("From ${quest.characterName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(quest.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                if (quest.notes.isNotBlank()) Text(quest.notes, style = MaterialTheme.typography.bodyLarge)
                quest.place()?.let { place ->
                    ListItem(headlineContent = { Text(place.name) }, supportingContent = { Text(place.address) }, leadingContent = { Icon(Icons.Default.Place, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                    OutlinedButton(onClick = { onNavigate(place) }) { Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(8.dp)); Text("Navigate") }
                }
                if (quest.reminderAt != null) Text("Reminder · ${questDate(quest.reminderAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (quest.hasFocus && quest.completedAt == null) FocusPanel(quest, focus?.takeIf { it.questId == quest.id }, controller)
                if (quest.completedAt == null) Button(onClick = onComplete, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = hudShape(world)) {
                    Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Complete quest")
                } else {
                    Text("Completed · ${questDate(quest.completedAt)}", color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = { controller.reopen(quest.id) }, enabled = !busy) { Text("Reopen quest") }
                    Text("Reopening removes this quest’s XP and achievement credit until you complete it again.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Delete quest") }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this quest?") },
        text = { Text("Its notes and picture will be removed from this phone. Any progress already earned will stay.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; controller.delete(quest.id, onBack) }) { Text("Delete quest") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep quest") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusPanel(quest: QuestRecord, focus: FocusRecord?, controller: QuestController) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(AndroidQuestTime(context).now()) }
    var alerts by rememberSaveable { mutableStateOf(false) }
    val busy by controller.busy.collectAsStateWithLifecycle()
    LaunchedEffect(focus?.id, focus?.status) {
        while (focus?.status == "RUNNING") {
            now = AndroidQuestTime(context).now()
            if (focus.clock().remaining(now.elapsed, now.wall, now.boot) == 0L) { controller.refresh(); break }
            delay(500)
        }
    }
    val remaining = focus?.clock()?.remaining(now.elapsed, now.wall, now.boot) ?: quest.focusMinutes * 60_000L
    val seconds = (remaining + 999) / 1000
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text("Focus time", style = MaterialTheme.typography.titleLarge)
        if (focus?.status == "NEEDS_CONFIRMATION") {
            Text("Did you finish your focus time?", style = MaterialTheme.typography.headlineSmall)
            Text("Your timer ended while the phone was restarting. Confirm the session to count its focus reward.")
            OutlinedButton(onClick = { controller.focusAction(focus.id, "confirm") }, enabled = !busy) { Text("Yes, I finished") }
        } else if (focus?.status == "FINISHED") {
            Text("Time well spent", style = MaterialTheme.typography.headlineSmall)
            Text("Your timer is finished. Complete the quest when you’re ready.")
        } else {
            Text(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60), style = MaterialTheme.typography.displayMedium.copy(fontFamily = TerminalFont))
            if (focus?.status !in listOf("RUNNING", "PAUSED")) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Alert me when time is up", Modifier.weight(1f)); Switch(checked = alerts, onCheckedChange = { alerts = it }, modifier = Modifier.semantics { contentDescription = "Alert me when time is up" }) }
                if (alerts) QuestAlertAccess(controller)
                Button(onClick = { controller.startFocus(quest.id, alerts) }, enabled = !busy) { Icon(Icons.Default.PlayArrow, null); Text("Start focus") }
            } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { controller.focusAction(focus!!.id, if (focus.status == "RUNNING") "pause" else "resume") }, enabled = !busy) { Text(if (focus?.status == "RUNNING") "Pause" else "Resume") }
                OutlinedButton(onClick = { controller.focusAction(focus!!.id, "cancel") }, enabled = !busy) { Text("Finish early") }
            }
        }
    }
}

@Composable
internal fun QuestAlertAccess(controller: QuestController) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { revision++; controller.refresh() }
    val alarmRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { revision++; controller.refresh() }
    val notifications = remember(revision) { controller.reminders.canNotify() }
    val exact = remember(revision) { controller.reminders.canAlertOnTime() }
    if (!notifications) {
        Text("Allow notifications to receive your reminders. Your quests work without them.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            if (Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            else alarmRequest.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Allow notifications") }
        TextButton(onClick = { alarmRequest.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Open notification settings") }
    } else if (!exact) {
        Text("Alerts may arrive late while your phone sleeps. Allow on-time alerts to hear when your timer finishes.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { if (Build.VERSION.SDK_INT >= 31) alarmRequest.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("Enable on-time alerts") }
    } else Text("Alerts are ready. Your phone’s sound and Do Not Disturb settings still apply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuestProfileScreen(world: World, snapshot: QuestSnapshot, controller: QuestController, onBack: () -> Unit) {
    val progress = snapshot.progress
    Scaffold(topBar = { TopAppBar(title = { Text("Your journey") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to quests") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text("A little progress, every day", style = MaterialTheme.typography.headlineMedium) }
            item { Text("Your tasks and rewards stay on this phone. Every world and navigation feature is available from the start.") }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Show levels and rewards", Modifier.weight(1f)); Switch(snapshot.profile.showProgress, onCheckedChange = controller::showProgress, modifier = Modifier.semantics { contentDescription = "Show levels and rewards" }) } }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("In-app completion sounds", Modifier.weight(1f)); Switch(snapshot.profile.sound, onCheckedChange = controller::sound, modifier = Modifier.semantics { contentDescription = "In-app completion sounds" }) } }
            if (snapshot.profile.showProgress) {
                item { Text("Level ${progress.level} · ${progress.xp} XP", style = MaterialTheme.typography.titleLarge); Text(nextLevelText(progress)) }
                item { QuestSectionTitle("Your badges") }
                item { Text(if (progress.badges.isEmpty()) "Complete your first quest to earn First step." else progress.badges.joinToString("\n"), style = MaterialTheme.typography.bodyLarge) }
                item {
                    QuestSectionTitle("Avatar frame")
                    QuestPortrait(QuestRecord(title = "Your profile"), Modifier.padding(vertical = 12.dp).size(72.dp), snapshot.profile.frame.takeIf { it in progress.frames } ?: "Simple")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { progress.frames.forEach { frame -> FilterChip(selected = snapshot.profile.frame == frame, onClick = { controller.frame(frame) }, label = { Text(frame) }) } }
                    Text("New frames at Levels 2, 5 and 10.", style = MaterialTheme.typography.bodySmall)
                }
                item { QuestSectionTitle("Your story") }
                items(journeyChapters) { chapter ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(chapter.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (progress.completed >= chapter.quests) chapter.text else "Opens after ${chapter.quests} completed quests. ${progress.completed} completed so far.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Text("Each quest earns 20 XP. Finishing its focus timer of at least 10 minutes adds 10 XP once. No streaks to maintain, and no points to lose for taking your time.", style = MaterialTheme.typography.bodySmall) }
            }
            item { HorizontalDivider(); Text("On this phone", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp)); Text("App updates preserve your quests. Uninstalling Waypoint removes them. There is no account or cloud backup.") }
        }
    }
}

@Composable private fun QuestSectionTitle(value: String) { Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
