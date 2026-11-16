package com.waypoint.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuestEditor(world: World, initial: QuestRecord?, seedPlace: Place?, template: String,
                         controller: QuestController, navigation: NavigationController, nav: NavState,
                         onSaved: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val photoError by rememberUpdatedState(stringResource(R.string.quest_photo_error))
    val reminderError = stringResource(R.string.quest_reminder_error)
    val titleError = stringResource(R.string.quest_title_error)
    val timerError = stringResource(R.string.quest_timer_error)
    val scope = rememberCoroutineScope()
    val busy by controller.busy.collectAsStateWithLifecycle()
    var id by rememberSaveable { mutableStateOf(initial?.id ?: UUID.randomUUID().toString()) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var details by rememberSaveable { mutableStateOf(initial != null || template != "Everyday task") }
    var placeName by rememberSaveable { mutableStateOf(initial?.placeName ?: seedPlace?.name) }
    var placeAddress by rememberSaveable { mutableStateOf(initial?.placeAddress ?: seedPlace?.address) }
    var latitude by rememberSaveable { mutableStateOf(initial?.latitude ?: seedPlace?.point?.lat) }
    var longitude by rememberSaveable { mutableStateOf(initial?.longitude ?: seedPlace?.point?.lon) }
    var hasFocus by rememberSaveable { mutableStateOf(initial?.hasFocus ?: (template == "Focus session")) }
    var minutes by rememberSaveable { mutableStateOf((initial?.focusMinutes ?: 25).toString()) }
    var customMinutes by rememberSaveable { mutableStateOf(initial?.focusMinutes?.let { it !in listOf(15, 25, 45) } ?: false) }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminderAt) }
    var characterName by rememberSaveable { mutableStateOf(initial?.characterName.orEmpty()) }
    var portrait by rememberSaveable { mutableStateOf(initial?.portrait ?: "scout") }
    var imageName by rememberSaveable { mutableStateOf(initial?.imageName) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    var lookup by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val dirty = title != initial?.title.orEmpty() || notes != initial?.notes.orEmpty() || placeName != initial?.placeName ||
        hasFocus != (initial?.hasFocus ?: false) || reminder != initial?.reminderAt || imageName != initial?.imageName ||
        characterName != initial?.characterName.orEmpty() || portrait != (initial?.portrait ?: "scout") || minutes != (initial?.focusMinutes ?: 25).toString()
    fun discardDraft() {
        if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }
        pendingPhoto?.let { QuestImages.delete(context, it) }
        onClose()
    }
    fun close() { if (dirty) discard = true else discardDraft() }
    BackHandler { if (lookup) lookup = false else close() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            photoBusy = true
            try { pendingPhoto = QuestImages.import(context, uri) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: photoError }
            finally { photoBusy = false }
        }
    }
    fun chooseReminder() {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminder ?: (System.currentTimeMillis() + 60 * 60_000L) }
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) error = reminderError
                else reminder = calendar.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(context)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    if (lookup) {
        QuestPlacePicker(nav, navigation, onPick = { place ->
            placeName = place.name; placeAddress = place.address; latitude = place.point.lat; longitude = place.point.lon; lookup = false
        }, onBack = { lookup = false })
        return
    }
    val timerLabel = stringResource(R.string.quest_timer)
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(if (initial == null) R.string.quest_add else R.string.quest_edit)) },
        navigationIcon = { IconButton(onClick = ::close, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.quest_back)) } }) },
        bottomBar = {
            Surface {
                Box(Modifier.fillMaxWidth().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        val duration = minutes.toIntOrNull()
                        if (validQuestTitle(title) == null) error = titleError
                        else if (duration == null || duration !in 1..180) error = timerError
                        else controller.save(QuestRecord(id = id, title = title, notes = notes, placeName = placeName, placeAddress = placeAddress,
                            latitude = latitude, longitude = longitude, hasFocus = hasFocus, focusMinutes = duration, reminderAt = reminder,
                            characterName = characterName, portrait = portrait, imageName = imageName), onSaved)
                    }, enabled = !busy && !photoBusy && title.isNotBlank(), modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(min = 52.dp), shape = hudShape(world)) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(stringResource(if (initial == null) R.string.quest_add_short else R.string.quest_save))
                    }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.quest_question), style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(title, onValueChange = { if (it.length <= 160) title = it }, label = { Text(stringResource(R.string.quest_title_label)) },
                    placeholder = { Text(stringResource(when (template) { "Visit a place" -> R.string.quest_example_place; "Focus session" -> R.string.quest_example_focus; else -> R.string.quest_example_task })) },
                    modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3, supportingText = { Text(stringResource(R.string.quest_title_help)) })
                TextButton(onClick = { details = !details }) { Icon(if (details) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null); Text(stringResource(if (details) R.string.quest_hide_details else R.string.quest_add_details)) }
                if (details) {
                    OutlinedTextField(notes, onValueChange = { if (it.length <= 4000) notes = it }, label = { Text(stringResource(R.string.quest_notes_optional)) }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.quest_place), style = MaterialTheme.typography.titleLarge)
                        if (placeName != null) { Text(placeName!!); Text(placeAddress.orEmpty(), style = MaterialTheme.typography.bodySmall) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { lookup = true }) { Icon(Icons.Default.Place, null); Text(stringResource(if (placeName == null) R.string.quest_choose_place else R.string.quest_change_place)) }
                            if (placeName != null) TextButton(onClick = { placeName = null; placeAddress = null; latitude = null; longitude = null }) { Text(stringResource(R.string.quest_remove_place)) }
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.quest_reminder), style = MaterialTheme.typography.titleLarge)
                        if (reminder != null) Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(reminder!!)))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = ::chooseReminder) { Text(stringResource(if (reminder == null) R.string.quest_choose_date else R.string.quest_change_reminder)) }
                            if (reminder != null) TextButton(onClick = { reminder = null }) { Text(stringResource(R.string.quest_remove_reminder)) }
                        }
                        if (reminder != null) QuestAlertAccess(controller)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(timerLabel, style = MaterialTheme.typography.titleLarge); Text(stringResource(R.string.quest_timer_help), style = MaterialTheme.typography.bodySmall) }
                        Switch(hasFocus, onCheckedChange = { hasFocus = it }, modifier = Modifier.semantics { contentDescription = timerLabel })
                    }
                    if (hasFocus) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45).forEach { value -> FilterChip(selected = !customMinutes && minutes == value.toString(), onClick = { customMinutes = false; minutes = value.toString() }, label = { Text("$value min") }) }
                            FilterChip(selected = customMinutes, onClick = { customMinutes = true }, label = { Text(stringResource(R.string.quest_custom)) })
                        }
                        if (customMinutes) OutlinedTextField(minutes, onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) minutes = it }, label = { Text(stringResource(R.string.quest_minutes)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider()
                    Text(stringResource(R.string.quest_character_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.quest_character_help), style = MaterialTheme.typography.bodyMedium)
                    QuestPortrait(QuestRecord(title = title, characterName = characterName, portrait = portrait, imageName = imageName), Modifier.size(80.dp))
                    OutlinedTextField(characterName, onValueChange = { if (it.length <= 60) characterName = it }, label = { Text(stringResource(R.string.quest_character_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("scout", "guide", "companion").forEach { option -> FilterChip(selected = portrait == option && imageName == null, onClick = {
                            if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }; imageName = null; portrait = option
                        }, label = { Text(stringResource(when(option) { "scout" -> R.string.quest_portrait_scout; "guide" -> R.string.quest_portrait_guide; else -> R.string.quest_portrait_companion })) }) }
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !photoBusy) { Text(stringResource(if (photoBusy) R.string.quest_opening_photo else R.string.quest_choose_photo)) }
                    Text(stringResource(R.string.quest_photo_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (pendingPhoto != null) AlertDialog(onDismissRequest = { pendingPhoto?.let { QuestImages.delete(context, it) }; pendingPhoto = null },
        title = { Text(stringResource(R.string.quest_crop_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestPortrait(QuestRecord(title = "Photo preview", imageName = pendingPhoto), Modifier.fillMaxWidth().aspectRatio(1f))
            Text(stringResource(R.string.quest_crop_help))
        } }, confirmButton = { TextButton(onClick = {
            if (imageName != initial?.imageName) imageName?.let { QuestImages.delete(context, it) }
            imageName = pendingPhoto; pendingPhoto = null
        }) { Text(stringResource(R.string.quest_use_photo)) } }, dismissButton = { TextButton(onClick = { pendingPhoto?.let { QuestImages.delete(context, it) }; pendingPhoto = null }) { Text(stringResource(R.string.action_cancel)) } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.quest_discard_title)) }, text = { Text(stringResource(R.string.quest_discard_help)) },
        confirmButton = { TextButton(onClick = ::discardDraft) { Text(stringResource(R.string.quest_discard)) } }, dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.quest_keep_editing)) } })
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text(stringResource(R.string.quest_check_detail)) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestPlacePicker(nav: NavState, navigation: NavigationController, onPick: (Place) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { navigation.search("") }
    BackHandler(onBack = onBack)
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.quest_choose_place)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.quest_back_to_quest)) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(query, onValueChange = { query = it.take(256); navigation.search(query) }, label = { Text(stringResource(R.string.search_place_address)) }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (query.isBlank()) {
                item { Text(stringResource(R.string.quest_saved_places), style = MaterialTheme.typography.titleLarge) }
                if (nav.saved.isEmpty()) item { Text(stringResource(R.string.quest_no_saved_places)) }
                items(nav.saved, key = { it.id }) { place -> QuestPlaceRow(place, onPick) }
            } else {
                if (nav.searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                nav.searchError?.let { item { Text(stringResource(R.string.quest_search_unavailable)) } }
                if (!nav.searching && nav.searchResults.isEmpty() && query.length >= 2) item { Text(stringResource(R.string.quest_no_places)) }
                items(nav.searchResults, key = { it.id }) { place -> QuestPlaceRow(place, onPick) }
            }
        }
    }
}

@Composable private fun QuestPlaceRow(place: Place, onPick: (Place) -> Unit) {
    ListItem(headlineContent = { Text(place.name) }, supportingContent = { Text(place.address) },
        leadingContent = { Icon(Icons.Default.Place, null) },
        modifier = Modifier.fillMaxWidth().clickable { onPick(place) })
}
