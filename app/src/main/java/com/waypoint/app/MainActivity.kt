package com.waypoint.app

import android.Manifest
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

val Ink=Color(0xFF151815)
val Panel=Color(0xFF20241F)
val Cream=Color(0xFFF1F2E8)
val Muted=Color(0xFFAFB5A7)
val DisplayFont=FontFamily(Font(R.font.barlow_condensed_bold,FontWeight.Bold))
val WesternFont=FontFamily(Font(R.font.kirsty_regular),Font(R.font.kirsty_bold,FontWeight.Bold))
val WesternDisplayFont=FontFamily(Font(R.font.chinese_rocks_regular))
val GtaDisplayFont=FontFamily(Font(R.font.pricedown_black))
val PixelFont=FontFamily(Font(R.font.monocraft))
val TerminalFont=FontFamily(Font(R.font.share_tech_mono))
val TechnicalFont=FontFamily(Font(R.font.rajdhani))
val AtlasFont=FontFamily(Font(R.font.cinzel))
val PipBoyFont=FontFamily(Font(R.font.roboto_condensed))
val ReadingSerifFont=FontFamily(Font(R.font.libre_baskerville))
val SansFont=FontFamily(Font(R.font.noto_sans))
val OrbitFont=FontFamily(Font(R.font.orbitron))
val ViceFont=FontFamily(Font(R.font.yellowtail))
fun worldFont(world: World)=when(world){World.VICE_COAST->ViceFont;World.LOS_SANTOS,World.SAN_ANDREAS,World.LIBERTY->GtaDisplayFont;World.LANDS_BETWEEN,World.HYRULE,World.ASSASSIN,World.HORIZON_WILDS,World.HOGWARTS,World.KINGDOM,World.MAFIA->AtlasFont;World.OLD_WEST->WesternDisplayFont;World.RIVIA->ReadingSerifFont;World.PIXEL->PixelFont;World.GREEN_TERMINAL->TerminalFont;World.NIGHT_CITY,World.BRIDGES,World.SPIDER,World.STARFIELD->TechnicalFont;else->DisplayFont}
private val AppTypography=Typography(
    displayLarge=Typography().displayLarge.copy(fontFamily=DisplayFont,fontWeight=FontWeight.Bold),
    displayMedium=Typography().displayMedium.copy(fontFamily=DisplayFont,fontWeight=FontWeight.Bold),
    headlineLarge=Typography().headlineLarge.copy(fontFamily=DisplayFont,fontWeight=FontWeight.Bold),
    headlineMedium=Typography().headlineMedium.copy(fontFamily=DisplayFont,fontWeight=FontWeight.Bold),
    titleLarge=Typography().titleLarge.copy(fontWeight=FontWeight.SemiBold)
)

class MainActivity : ComponentActivity() {
    companion object {
        private var foregroundEntryCounter=0
        private var configurationRestartPending=false
    }
    private val controller get()=(application as WaypointApplication).navigation
    private var destinationLaunch by mutableIntStateOf(0)
    private var foregroundEntry by mutableIntStateOf(0)
    private val displayListener=object:DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId:Int)=Unit
        override fun onDisplayRemoved(displayId:Int)=Unit
        override fun onDisplayChanged(displayId:Int){updateDisplayRotation()}
    }
    private fun updateDisplayRotation() {
        val activeDisplay = window.decorView.display
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
            else getSystemService(DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)
        activeDisplay?.rotation?.let(controller::setDisplayRotation)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        val restorePending = savedInstanceState?.getBoolean("waypoint.pendingSharedDestination") == true
        if((savedInstanceState==null || restorePending && !controller.hasPendingNavigationImport()) && controller.handleNavigationIntent(intent)) destinationLaunch++
        setContent { val state by controller.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.navigating) { if(state.navigating) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            WaypointApp(controller,state,destinationLaunch,foregroundEntry)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (controller.handleNavigationIntent(intent)) {
            // Keep the last validated proposal as the restore source while its dialog is open.
            setIntent(intent)
            destinationLaunch++
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("waypoint.pendingSharedDestination",controller.hasPendingNavigationImport())
        super.onSaveInstanceState(outState)
    }
    override fun onStart() { super.onStart();if(configurationRestartPending)configurationRestartPending=false else foregroundEntryCounter++;foregroundEntry=foregroundEntryCounter;getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener,null); updateDisplayRotation(); controller.setVisible(true);(application as WaypointApplication).updater.checkOnLaunch() }
    override fun onStop() { configurationRestartPending=isChangingConfigurations;getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener);controller.setVisible(false); super.onStop() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointApp(controller: NavigationController,state: NavState,destinationLaunch:Int=0,foregroundEntry:Int=0) {
    val context=LocalContext.current
    val application=context.applicationContext as WaypointApplication
    val updater=application.updater
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val worldsStateHolder=rememberSaveableStateHolder()
    var landing by rememberSaveable { mutableStateOf(controller.shouldShowLanding()) }
    val welcomeVisible=landing && !state.navigating
    var whatsNewVisible by rememberSaveable { mutableStateOf(application.shouldShowWhatsNew()) }
    val bundledReleaseNotes=remember { application.bundledReleaseNotes() }
    LaunchedEffect(destinationLaunch){if(destinationLaunch>0 && !controller.shouldShowLanding()){controller.markMapOpened();landing=false}}
    var search by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var feedbackOpen by rememberSaveable { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }
    var steps by rememberSaveable { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapBounds by remember { mutableStateOf(Rect.Zero) }
    var routeHeaderBounds by remember { mutableStateOf(Rect.Zero) }
    var routePanelBounds by remember { mutableStateOf(Rect.Zero) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    var updatePromptDismissed by remember(foregroundEntry) { mutableStateOf(application.updatePromptDismissedAtForegroundEntry==foregroundEntry) }
    var pendingRelease by remember { mutableStateOf<ReleaseUpdate?>(null) }
    val updateStatus by updater.state.collectAsStateWithLifecycle()
    LaunchedEffect(updateStatus) {
        when (val status=updateStatus) {
            is UpdateStatus.Available -> pendingRelease=status.release
            is UpdateStatus.Downloading -> pendingRelease=status.release
            is UpdateStatus.Ready -> pendingRelease=status.release
            UpdateStatus.Current -> pendingRelease=null
            else -> Unit
        }
    }
    val hasOnboardingLocationPermission=remember(permissionRevision,state.gpsStatus,foregroundEntry){controller.hasLocationPermission()}
    LaunchedEffect(welcomeVisible,tab,search,settings,steps) {
        controller.setHeadingUiVisible(settings || (!welcomeVisible && tab==0 && !search && !steps))
    }

    val snackbar=remember { SnackbarHostState() }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionDenied=!controller.hasLocationPermission()
        permissionRevision++
        if(controller.hasLocationPermission()) controller.enableLocation() else controller.permissionDenied()
    }
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun locate() {
        if(controller.hasLocationPermission()) {
            if(state.fix==null || !state.gpsReady) controller.enableLocation()
            controller.recenter()
        } else {
            controller.recenter()
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    fun choose(place: Place) { controller.markMapOpened();controller.select(place); search=false;tab=0;landing=false }
    val showWhatsNew=whatsNewVisible && !welcomeVisible && !state.navigating && state.selected==null && state.pendingSharedDestination==null &&
        tab==0 && !search && !settings && !steps
    val showUpdatePrompt=pendingRelease!=null && !updatePromptDismissed && !whatsNewVisible && !welcomeVisible && !state.navigating && state.selected==null && state.pendingSharedDestination==null &&
        tab==0 && !search && !settings && !steps
    MaterialTheme(colorScheme=darkColorScheme(primary=state.world.accent,onPrimary=Ink,primaryContainer=state.world.accent.copy(alpha=0.18f),onPrimaryContainer=state.world.accent,
        background=Ink,surface=Ink,surfaceVariant=Panel,onSurface=Cream,onSurfaceVariant=Muted,outline=Color(0xFF51594B),secondary=state.world.accent),typography=AppTypography) {
        BackHandler(search || settings || steps || tab!=0 || (!welcomeVisible && state.selected!=null)) {
            when { search->search=false; feedbackOpen->feedbackOpen=false; settings->settings=false; steps->steps=false; tab!=0->tab=0; state.selected!=null->controller.clearDestination() }
        }
        LaunchedEffect(state.message) { state.message?.let{snackbar.showSnackbar(it);controller.dismissMessage()} }
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val wide=maxWidth>=600.dp
            val compactHeight=maxHeight<500.dp
            val density=LocalDensity.current
            val edge=with(density){28.dp.toPx()}
            val systemBottom=WindowInsets.navigationBars.getBottom(density)
            val routeInsets=if(state.selected!=null && !state.navigating && !mapBounds.isEmpty && !routeHeaderBounds.isEmpty && !routePanelBounds.isEmpty) {
                val top=(routeHeaderBounds.bottom-mapBounds.top+edge).coerceAtLeast(edge)
                val right=if(compactHeight)(mapBounds.right-routePanelBounds.left+edge).coerceAtLeast(edge) else edge
                val bottom=if(compactHeight)systemBottom+edge else (mapBounds.bottom-routePanelBounds.top+edge).coerceAtLeast(edge)
                // Fit the clear middle in portrait, or the area below the left toolbar in landscape.
                if(top+bottom<mapBounds.height-32 && edge+right<mapBounds.width-32)
                    MapViewportInsets(edge.roundToInt(),top.roundToInt(),right.roundToInt(),bottom.roundToInt())
                else null
            } else null
            Row(Modifier.fillMaxSize()) {
                if(wide && !state.navigating && !welcomeVisible) NavigationRail(containerColor=Ink,modifier=Modifier.statusBarsPadding()) {
                    Spacer(Modifier.height(16.dp));Icon(Icons.Default.Navigation,"Waypoint",tint=state.world.accent);Spacer(Modifier.height(32.dp))
                    listOf("Map" to Icons.Default.Map,"Worlds" to Icons.Default.Layers,"Saved" to Icons.Default.BookmarkBorder).forEachIndexed { i,(name,icon)->NavigationRailItem(selected=tab==i,onClick={tab=i},icon={Icon(icon,name)},label={Text(name)}) }
                }
                Scaffold(modifier=Modifier.weight(1f),snackbarHost={SnackbarHost(snackbar)},contentWindowInsets=WindowInsets(0),bottomBar={
                    if(!wide && !state.navigating && tab!=0 && !welcomeVisible) HudNavigationBar(state.world,tab,{tab=it})
                }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        // Keep the native map alive when switching destinations; hide other screens above it.
                        if(!welcomeVisible) WorldMap(state,{choose(it)},{mapError=it},Modifier.fillMaxSize().onGloballyPositioned{mapBounds=it.boundsInRoot()},active=tab==0 && !search && !settings && !steps,routeInsets=routeInsets)
                        Box(Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha=0.7f)))
                        if(!welcomeVisible && (tab==0 || state.navigating)) {
                            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                                val atlasActions=AtlasActions({search=true},{tab=1},{tab=2},{settings=true},
                                    {locate()},controller::toggleTilt,controller::toggleVoice,
                                    {query->search=true;controller.search(query)})
                                if(!state.navigating && state.selected==null) {
                                    GameAtlasChrome(state,atlasActions,Modifier.padding(bottom=48.dp))
                                    if(mapError!=null)Notice(mapError!!,Icons.Default.CloudOff)
                                } else {
                                    AtlasFrame(state.world)
                                    Column(Modifier.align(Alignment.TopStart).fillMaxWidth(if(compactHeight)0.5f else 1f).onGloballyPositioned{routeHeaderBounds=it.boundsInRoot()}.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        if(state.navigating)ManeuverBanner(state,{steps=true}) else {
                                            Row(Modifier.background(hudPalette(state.world).panel,hudShape(state.world)),verticalAlignment=Alignment.CenterVertically) {
                                                AtlasTool(state.world,Icons.AutoMirrored.Filled.ArrowBack,"Close route preview",controller::clearDestination)
                                                AtlasText("ROUTE",state.world,Modifier.weight(1f).padding(horizontal=12.dp),22)
                                                AtlasTool(state.world,Icons.Default.Layers,"Worlds",{tab=1})
                                                AtlasTool(state.world,Icons.Default.Tune,"Settings",{settings=true})
                                            }
                                            TransportModes(state.mode,controller::changeMode,state.world)
                                        }
                                        if(mapError!=null)Notice(mapError!!,Icons.Default.CloudOff)
                                        if(state.navigating && !state.gpsReady)Notice(state.gpsStatus,Icons.Default.GpsOff)
                                        if(state.navigating && state.routeError!=null)Notice("Reroute unavailable. ${state.routeError}",Icons.Default.CloudOff)
                                    }
                                    Column(Modifier.align(if(compactHeight)Alignment.BottomEnd else Alignment.BottomCenter).fillMaxWidth(if(compactHeight)0.48f else 1f).onGloballyPositioned{routePanelBounds=it.boundsInRoot()}.padding(horizontal=16.dp).padding(bottom=56.dp).then(if(compactHeight)Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState()) else Modifier),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {
                                            FixLabel(state,Modifier.weight(1f))
                                            if(state.navigating) AtlasTool(state.world,Icons.Default.Layers,"Worlds",{tab=1})
                                            if(state.navigating) AtlasTool(state.world,if(state.voice)Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,if(state.voice)"Mute guidance" else "Unmute guidance",controller::toggleVoice)
                                            if(compactHeight && !state.navigating) {
                                                TextButton(onClick=atlasActions.locate,modifier=Modifier.heightIn(min=48.dp).background(hudPalette(state.world).panel,hudShape(state.world)).semantics{contentDescription="Use my location"}) {
                                                    Icon(Icons.Default.MyLocation,null,Modifier.size(18.dp),tint=hudPalette(state.world).foreground)
                                                    Spacer(Modifier.width(8.dp));Text("Locate",fontFamily=PipBoyFont,fontSize=12.sp,color=hudPalette(state.world).foreground)
                                                }
                                            } else AtlasTool(state.world,Icons.Default.MyLocation,"Use my location",atlasActions.locate)
                                        }
                                        if(state.navigating) NavigationFooter(state,{steps=true},{controller.stopNavigation()})
                                        else DestinationCard(state,{controller.toggleSaved(state.selected!!)},{controller.clearDestination()},{locate()},{controller.requestRoute()},{
                                            controller.startNavigation()
                                            if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },{steps=true},compact=compactHeight)
                                    }
                                }
                                Box(Modifier.align(Alignment.BottomEnd)){Attribution(state.world)}
                            }
                        }
                        if(welcomeVisible) ReferenceOnboarding(
                            initialWorld=state.world,
                            initialFavorites=if(controller.hasCompletedOnboarding()) controller.favoriteWorlds() else emptyList(),
                            hasLocationPermission=hasOnboardingLocationPermission,
                            permissionDenied=permissionDenied,
                            onComplete={chosen,favorites->controller.changeWorld(chosen);controller.completeOnboarding(favorites);landing=false},
                            onRequestLocation={
                                if(controller.hasLocationPermission()){controller.enableLocation();permissionRevision++}
                                else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            onOpenAppSettings={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}
                        )
                        if(tab==1) worldsStateHolder.SaveableStateProvider("worlds") {
                            WorldsScreen(state.world,{controller.changeWorld(it);tab=0},{tab=0})
                        }
                        if(tab==2 && !state.navigating) SavedScreen(state,::choose,{controller.toggleSaved(it)},{search=true})
                        if(search) SearchScreen(state,controller::search,::choose,{search=false},controller::changeMode,controller::submitSearch)
                    }
                }
            }
        }
        if(settings) ModalBottomSheet(onDismissRequest={feedbackOpen=false;settings=false},containerColor=Ink) {
            if(feedbackOpen) {
                FeedbackSheet(state.world,onBack={feedbackOpen=false},onLeaveApp={feedbackOpen=false;settings=false})
            } else {
            Column(Modifier.padding(horizontal=24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("Make it your own",style=MaterialTheme.typography.headlineLarge)
                UpdatePanel((context.applicationContext as WaypointApplication).updater, state.navigating)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                SettingRow("Voice guidance","Spoken turn instructions",state.voice,controller::toggleVoice)
                SettingRow("Miles & feet","Use imperial distances",state.imperial,controller::toggleUnits)
                SettingRow("3D perspective","Tilt the map in every world",state.tilted,controller::toggleTilt)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Text("Android Auto",style=MaterialTheme.typography.titleLarge)
                Text("Available on compatible car displays",style=MaterialTheme.typography.bodyMedium)
                Text("Allow precise location on your phone, then open Waypoint in Android Auto. Opening it makes Waypoint the current default for car navigation requests; Google Maps remains in the launcher. Saved and recent destinations appear on the car screen.",style=MaterialTheme.typography.bodySmall,color=Muted)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Text("Location & sensors",style=MaterialTheme.typography.titleMedium)
                Text(state.locationProvider,style=MaterialTheme.typography.bodyMedium)
                Text(state.sensorDescription,style=MaterialTheme.typography.bodySmall,color=Muted)
                Text(state.headingStatus,style=MaterialTheme.typography.bodySmall,color=if(state.headingReliable)state.world.accent else Color(0xFFFFCF89))
                if(state.fix!=null)Text("Position accuracy: ±${state.accuracy.roundToInt()} m",style=MaterialTheme.typography.bodySmall,color=Muted)
                state.satellitesVisible?.let{Text("Satellites: ${state.satellitesUsed ?: 0} used / $it visible",style=MaterialTheme.typography.bodySmall,color=Muted)}
                Text(state.gpsStatus,style=MaterialTheme.typography.bodyMedium,color=Muted)
                TextButton(onClick={context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))}) { Text("Open device location settings") }
                TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}) { Text("Manage location permissions") }
                TextButton(onClick={clearConfirm=true}) { Text("Clear saved places and history",color=MaterialTheme.colorScheme.error) }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                FeedbackSettingsRow(onClick={feedbackOpen=true})
                Text("WAYPOINT",style=MaterialTheme.typography.headlineMedium,color=state.world.accent)
                Text("Made in India · Open to the world",style=MaterialTheme.typography.bodySmall,color=Muted)
                Text("Open source. Every world included. No ads or subscriptions.",style=MaterialTheme.typography.bodyMedium)
                Text("Routes use OpenStreetMap with OSRM (car) and Valhalla (motorcycle/walk). Ferrostar provides guidance and rerouting. Routes don't include live traffic. Online maps and search require internet. Your location and destination are shared with routing and search providers when needed; saved places stay on this phone.",style=MaterialTheme.typography.bodySmall,color=Muted)
                Text("Original map styles inspired by games. Not affiliated with their publishers.",style=MaterialTheme.typography.bodySmall,color=Muted)
                Spacer(Modifier.height(24.dp))
            }
            }
        }
        if(showWhatsNew) WhatsNewDialog(bundledReleaseNotes) { application.markWhatsNewSeen();whatsNewVisible=false }
        if(showUpdatePrompt) UpdatePromptDialog(
            release=requireNotNull(pendingRelease),
            updater=updater,
            navigating=state.navigating,
                            onLater={updatePromptDismissed=true;application.updatePromptDismissedAtForegroundEntry=foregroundEntry},
        )
        val sharedDestination = state.pendingSharedDestination
        val sharedDestinationId = state.pendingSharedDestinationId
        if(sharedDestination!=null && !welcomeVisible && !state.navigating) AlertDialog(
            onDismissRequest={controller.dismissPendingNavigationImport(sharedDestinationId)},
            title={Text("Open shared destination?")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(sharedDestination.take(100),maxLines=2,overflow=TextOverflow.Ellipsis)
                state.pendingSharedCoordinates?.let { Text("Coordinates: $it",style=MaterialTheme.typography.bodySmall,color=Muted) }
                Text("Waypoint will use your location to look up this place and preview a route. Continue?",style=MaterialTheme.typography.bodyMedium)
            }},
            confirmButton={TextButton(onClick={controller.acceptPendingNavigationImport(sharedDestinationId)}){Text("Open destination")}},
            dismissButton={TextButton(onClick={controller.dismissPendingNavigationImport(sharedDestinationId)}){Text("Cancel")}},
        )
        if(clearConfirm) AlertDialog(onDismissRequest={clearConfirm=false},title={Text("Clear your places?")},text={Text("This removes your saved destinations and recent navigation history from this phone.")},confirmButton={TextButton(onClick={controller.clearHistory();clearConfirm=false}){Text("Clear places")}},dismissButton={TextButton(onClick={clearConfirm=false}){Text("Cancel")}})
        if(steps) ModalBottomSheet(onDismissRequest={steps=false},containerColor=Ink) {
            Text("Your route",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(horizontal=24.dp))
            LazyColumn(Modifier.fillMaxWidth().padding(vertical=16.dp)) {
                items(state.route?.steps.orEmpty().drop(if(state.navigating)state.progress?.nextStep ?: 0 else 0)) { step ->
                    ListItem(headlineContent={Text(step.instruction)},supportingContent={Text(formatDistance(step.distance,state.imperial))},leadingContent={Icon(maneuverIcon(step),null,tint=state.world.accent)},colors=ListItemDefaults.colors(containerColor=Ink))
                }
            }
        }
    }
}

@Composable fun CircleControl(icon: ImageVector,label: String,onClick:()->Unit) { FilledIconButton(onClick=onClick,modifier=Modifier.size(48.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Ink,contentColor=Cream)) { Icon(icon,label,Modifier.size(22.dp)) } }
@Composable fun Notice(text: String,icon: ImageVector) { Surface(color=Panel,shape=RoundedCornerShape(12.dp)) { Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,Modifier.size(20.dp),tint=Color(0xFFFFCF89));Text(text,style=MaterialTheme.typography.bodySmall) } } }
@Composable fun Attribution(world:World) {
    val uri=LocalUriHandler.current
    var open by remember { mutableStateOf(false) }
    TextButton(onClick={open=true},modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription="Map information"}) {
        Row(Modifier.background(Ink.copy(alpha=.94f),RoundedCornerShape(4.dp)).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Icon(Icons.Default.Info,null,Modifier.size(14.dp),tint=Cream);Text("Map info",color=Cream,fontSize=11.sp)}
    }
    if(open) AlertDialog(onDismissRequest={open=false},title={Text("Map information")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={uri.openUri("https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap contributors")}
            TextButton(onClick={uri.openUri("https://openfreemap.org/")}){Text("Map tiles · OpenFreeMap")}
            if(world.hasTerrain())TextButton(onClick={uri.openUri("https://github.com/tilezen/joerd/blob/master/docs/attribution.md")}){Text("Terrain · Mapzen and data providers")}
        }
    },confirmButton={TextButton(onClick={open=false}){Text("Close")}})
}
@Composable fun SettingRow(title:String,subtitle:String,checked:Boolean,action:()->Unit) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Muted)};Switch(checked,onCheckedChange={action()}) } }
fun maneuverIcon(step: Maneuver?): ImageVector = when { step==null->Icons.Default.North; step.type=="arrive"->Icons.Default.Flag; step.type.contains("roundabout")||step.type.contains("rotary")->Icons.Default.RoundaboutRight; step.modifier=="uturn"->Icons.Default.UTurnLeft; step.modifier.contains("left")->Icons.Default.TurnLeft;step.modifier.contains("right")->Icons.Default.TurnRight;else->Icons.Default.Straight }

@Composable fun ManeuverBanner(state: NavState,onClick:()->Unit) = HudManeuver(state,onClick)
@Composable fun NavigationFooter(state: NavState,onSteps:()->Unit,onStop:()->Unit) = HudRouteFooter(state,onSteps,onStop)
private fun placeCategory(kind:String):Pair<ImageVector,String> = when(kind.lowercase(Locale.ROOT)) {
    "restaurant","fast_food","ice_cream","food" -> Icons.Default.Restaurant to "Food"
    "cafe","coffee_shop" -> Icons.Default.LocalCafe to "Café"
    "bar","beer","pub" -> Icons.Default.LocalBar to "Drinks & pubs"
    "lodging","hotel","hostel","guest_house" -> Icons.Default.Hotel to "Stay"
    "fuel" -> Icons.Default.LocalGasStation to "Fuel"
    "parking" -> Icons.Default.LocalParking to "Parking"
    "shop","grocery","alcohol_shop" -> Icons.Default.Store to "Shop"
    "attraction","art_gallery","castle","museum","library" -> Icons.Default.Explore to "Explore"
    "hospital","pharmacy" -> Icons.Default.LocalHospital to "Health"
    "park" -> Icons.Default.Park to "Park"
    "atm" -> Icons.Default.AttachMoney to "ATM"
    else -> Icons.Default.Place to "Place"
}
@Composable fun DestinationCard(state: NavState,onSave:()->Unit,onClose:()->Unit,onLocate:()->Unit,onRetry:()->Unit,onStart:()->Unit,onSteps:()->Unit,compact:Boolean=false) {
    val place=state.selected ?: return
    val palette=hudPalette(state.world)
    val (categoryIcon,categoryLabel)=placeCategory(place.kind)
    val detail=if(place.address=="Selected on map")"$categoryLabel · Selected on map" else place.address
    Surface(color=palette.panel,contentColor=palette.foreground,shape=hudShape(state.world),border=hudBorder(state.world),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if(compact)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact)8.dp else 12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(color=palette.accent.copy(alpha=.15f),shape=hudShape(state.world)) {Icon(categoryIcon,categoryLabel,Modifier.padding(10.dp).size(22.dp),tint=palette.accent)}
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(if(state.arrived)"YOU HAVE ARRIVED" else place.name,style=if(compact)MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(state.arrived)place.name else detail,style=MaterialTheme.typography.bodySmall,color=palette.muted,maxLines=if(compact)1 else 2,overflow=TextOverflow.Ellipsis) }
                TextButton(onClick=onSave,contentPadding=PaddingValues(2.dp),modifier=Modifier.width(64.dp).heightIn(min=56.dp).semantics{contentDescription="Save place"}) {Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(if(state.saved.any{it.id==place.id})Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null,tint=palette.accent);Text(if(state.saved.any{it.id==place.id})"Saved" else "Save",fontFamily=hudFont(state.world),fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,maxLines=1,softWrap=false,color=palette.accent)}}
            }
            when {
                state.arrived -> Button(onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text("Done") }
                state.routing -> { LinearProgressIndicator(modifier=Modifier.fillMaxWidth());Text("Finding a ${state.mode.routeDescription} route…",style=MaterialTheme.typography.bodySmall,color=palette.muted) }
                state.routeError!=null -> { Text(state.routeError,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error);Button(onClick=onRetry,modifier=Modifier.fillMaxWidth()){Text("Try route again")} }
                state.route!=null -> {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(modeIcon(state.mode),null,tint=palette.muted,modifier=Modifier.size(20.dp));Spacer(Modifier.width(8.dp))
                        Text("${ceil(state.route.seconds/60).toInt().coerceAtLeast(1)} min",style=MaterialTheme.typography.titleLarge,color=palette.accent);Spacer(Modifier.width(8.dp));Text("· ${formatDistance(state.route.meters,state.imperial)}",color=palette.muted,style=MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f));TextButton(onClick=onSteps){Text("Steps",color=palette.accent)}
                    }
                    Button(onClick=onStart,enabled=state.gpsReady,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=hudShape(state.world)) { Icon(Icons.Default.Navigation,null,Modifier.size(18.dp));Spacer(Modifier.width(10.dp));Text(if(state.gpsReady)"Start navigation" else "Waiting for GPS…",fontWeight=FontWeight.Bold) }
                }
                else -> { Text("Use your precise location to get ${state.mode.routeDescription} directions.",style=MaterialTheme.typography.bodySmall,color=palette.muted);Button(onClick=onLocate,modifier=Modifier.fillMaxWidth()){Text("Enable GPS for directions")} }
            }
        }
    }
}

@Composable fun SearchScreen(state: NavState,onQuery:(String)->Unit,onSelect:(Place)->Unit,onBack:()->Unit,onMode:(TravelMode)->Unit,onSubmit:()->Unit={onQuery(state.query)}) {
    val keyboard=LocalSoftwareKeyboardController.current
    fun submit(){keyboard?.hide();onSubmit()}
    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back to map")}
            OutlinedTextField(value=state.query,onValueChange={onQuery(it.take(256))},label={Text("Place or address")},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}),modifier=Modifier.weight(1f),shape=hudShape(state.world),trailingIcon={if(state.query.isNotEmpty())IconButton(onClick={onQuery("")}){Icon(Icons.Default.Close,"Clear search")}})
        }
        Button(onClick={submit()},enabled=state.query.trim().length>=2,shape=hudShape(state.world),modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
            Icon(Icons.Default.Search,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Search places")
        }
        Box(Modifier.padding(horizontal=16.dp)){TransportModes(state.mode,onMode,state.world)}
        Text(if(state.fix==null)"Search worldwide · preview near Kolkata" else "Search near your location",style=MaterialTheme.typography.bodySmall,color=Muted,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp))
        if(state.searchNote!=null)Text(state.searchNote,style=MaterialTheme.typography.bodySmall,color=Muted,modifier=Modifier.padding(horizontal=24.dp,vertical=8.dp))
        if(state.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=24.dp))
        if(state.searchError!=null) { Notice(state.searchError,Icons.Default.CloudOff);TextButton(onClick={submit()}){Text("Retry search")} }
        LazyColumn {
            if(state.query.length<2) {
                item { Text(if(state.recent.isEmpty())"Your next destination starts here." else "Recent destinations",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(24.dp)) }
                items(state.recent,key={it.id}){PlaceRow(it,state,onSelect)}
                if(state.recent.isEmpty()) item { Text("Find an address, a landmark, or your next coffee stop. You can also press and hold the map to drop a pin.",color=Muted,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(horizontal=24.dp)) }
            } else {
                items(state.searchResults,key={it.id}){PlaceRow(it,state,onSelect)}
                if(!state.searching && state.searchResults.isEmpty() && state.searchError==null && state.searchNote==null) item{Text("No places found. Try a street, city, or fuller address.",modifier=Modifier.padding(24.dp),color=Muted)}
            }
        }
    }
}
@Composable fun PlaceRow(place: Place,state: NavState,onSelect:(Place)->Unit) {
    ListItem(headlineContent={Text(place.name,maxLines=2,overflow=TextOverflow.Ellipsis)},supportingContent={Column { Text(place.address,maxLines=3,overflow=TextOverflow.Ellipsis);if(place.source.isNotEmpty())Text(place.source,style=MaterialTheme.typography.labelSmall,color=Muted) }},leadingContent={Icon(Icons.Default.Place,null,tint=state.world.accent)},trailingContent={state.fix?.let{Text(formatDistance(distance(it,place.point),state.imperial),style=MaterialTheme.typography.labelSmall,color=Muted)}},modifier=Modifier.clickable{onSelect(place)},colors=ListItemDefaults.colors(containerColor=Ink))
}
@Composable fun SavedScreen(state: NavState,onSelect:(Place)->Unit,onDelete:(Place)->Unit,onSearch:()->Unit) {
    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().padding(top=24.dp)) {
        Text("YOUR PLACES",style=MaterialTheme.typography.displayMedium,modifier=Modifier.padding(horizontal=24.dp))
        Text("Your bookmarked destinations.",style=MaterialTheme.typography.bodyMedium,color=Muted,modifier=Modifier.padding(horizontal=24.dp,vertical=8.dp))
        if(state.saved.isEmpty()) Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(36.dp));Icon(Icons.Default.Bookmarks,null,tint=state.world.accent,modifier=Modifier.size(48.dp));Text("No saved places yet",style=MaterialTheme.typography.titleLarge)
            Text("Search for a destination and tap the bookmark to keep it here.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Button(onClick=onSearch){Text("Find a place")}
        }
        LazyColumn { items(state.saved,key={it.id}) { place ->
            Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.weight(1f)){PlaceRow(place,state,onSelect)};IconButton(onClick={onDelete(place)}){Icon(Icons.Default.BookmarkRemove,"Remove ${place.name}",tint=Muted)} }
        } }
    }
}
@Composable fun WorldsScreen(selected: World,onSelect:(World)->Unit,onClose:()->Unit) = HudLibrary(selected,onSelect,onClose)
internal enum class WorldRouteStyle { URBAN, NEON, PIXEL, TRAIL, COAST }

internal fun World.routeStyle()=when {
    this==World.PIXEL -> WorldRouteStyle.PIXEL
    this in setOf(World.NIGHT_CITY,World.GREEN_TERMINAL,World.LIBERTY,World.SPIDER,World.SAINTS) -> WorldRouteStyle.NEON
    this in setOf(World.OLD_WEST,World.RIVIA,World.HYRULE,World.SKYRIM,World.LANDS_BETWEEN,World.ASSASSIN,World.FAR_CRY,World.GHOST,World.BRIDGES,World.HORIZON_WILDS,World.HOGWARTS,World.KINGDOM,World.JUST_CAUSE) -> WorldRouteStyle.TRAIL
    this in setOf(World.VICE_COAST,World.HORIZON,World.HEAT) -> WorldRouteStyle.COAST
    else -> WorldRouteStyle.URBAN
}

internal data class RouteGeometry(val path:Path,val stops:List<Offset>)

internal fun routeGeometry(world:World,width:Float,height:Float):RouteGeometry {
    fun p(x:Float,y:Float)=Offset(width*x,height*y)
    val style=world.routeStyle()
    val stops:List<Offset>
    val path=Path()
    when(style) {
        WorldRouteStyle.PIXEL -> {
            stops=listOf(p(.12f,.84f),p(.36f,.84f),p(.36f,.64f),p(.58f,.64f),p(.58f,.39f),p(.84f,.39f),p(.84f,.16f))
            path.moveTo(stops.first().x,stops.first().y);stops.drop(1).forEach{path.lineTo(it.x,it.y)}
        }
        WorldRouteStyle.NEON -> {
            stops=listOf(p(.12f,.84f),p(.27f,.7f),p(.27f,.52f),p(.48f,.52f),p(.48f,.36f),p(.72f,.36f),p(.72f,.17f),p(.88f,.17f))
            path.moveTo(stops.first().x,stops.first().y);stops.drop(1).forEach{path.lineTo(it.x,it.y)}
        }
        WorldRouteStyle.TRAIL -> {
            stops=listOf(p(.12f,.84f),p(.31f,.67f),p(.48f,.59f),p(.62f,.39f),p(.86f,.17f))
            path.moveTo(stops[0].x,stops[0].y)
            path.cubicTo(p(.2f,.8f).x,p(.2f,.8f).y,p(.17f,.65f).x,p(.17f,.65f).y,stops[1].x,stops[1].y)
            path.cubicTo(p(.42f,.7f).x,p(.42f,.7f).y,p(.38f,.46f).x,p(.38f,.46f).y,stops[2].x,stops[2].y)
            path.cubicTo(p(.62f,.61f).x,p(.62f,.61f).y,p(.49f,.42f).x,p(.49f,.42f).y,stops[3].x,stops[3].y)
            path.cubicTo(p(.78f,.39f).x,p(.78f,.39f).y,p(.66f,.22f).x,p(.66f,.22f).y,stops[4].x,stops[4].y)
        }
        WorldRouteStyle.COAST -> {
            stops=listOf(p(.1f,.77f),p(.32f,.64f),p(.51f,.48f),p(.7f,.3f),p(.9f,.19f))
            path.moveTo(stops[0].x,stops[0].y)
            path.cubicTo(p(.2f,.75f).x,p(.2f,.75f).y,p(.19f,.58f).x,p(.19f,.58f).y,stops[1].x,stops[1].y)
            path.cubicTo(p(.43f,.69f).x,p(.43f,.69f).y,p(.42f,.4f).x,p(.42f,.4f).y,stops[2].x,stops[2].y)
            path.cubicTo(p(.62f,.53f).x,p(.62f,.53f).y,p(.6f,.26f).x,p(.6f,.26f).y,stops[3].x,stops[3].y)
            path.cubicTo(p(.79f,.34f).x,p(.79f,.34f).y,p(.79f,.18f).x,p(.79f,.18f).y,stops[4].x,stops[4].y)
        }
        WorldRouteStyle.URBAN -> {
            stops=listOf(p(.12f,.84f),p(.32f,.67f),p(.5f,.61f),p(.68f,.39f),p(.88f,.16f))
            path.moveTo(stops[0].x,stops[0].y)
            path.cubicTo(p(.22f,.79f).x,p(.22f,.79f).y,p(.21f,.65f).x,p(.21f,.65f).y,stops[1].x,stops[1].y)
            path.cubicTo(p(.45f,.71f).x,p(.45f,.71f).y,p(.38f,.51f).x,p(.38f,.51f).y,stops[2].x,stops[2].y)
            path.cubicTo(p(.63f,.6f).x,p(.63f,.6f).y,p(.55f,.4f).x,p(.55f,.4f).y,stops[3].x,stops[3].y)
            path.cubicTo(p(.81f,.35f).x,p(.81f,.35f).y,p(.75f,.19f).x,p(.75f,.19f).y,stops[4].x,stops[4].y)
        }
    }
    return RouteGeometry(path,stops)
}

@Composable fun WorldDropdown(label: String, selected: World, onSelect: (World)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text("$label · ${selected.title}",modifier=Modifier.weight(1f));Icon(Icons.Default.ExpandMore,null) }
        DropdownMenu(expanded,onDismissRequest={expanded=false}) { World.entries.forEach { w -> DropdownMenuItem(text={Text(w.title)},onClick={onSelect(w);expanded=false}) } }
    }
}
