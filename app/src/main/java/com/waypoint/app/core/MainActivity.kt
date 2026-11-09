package com.waypoint.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
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
import kotlinx.coroutines.launch

val Ink=Color(0xFF151815)
val Panel=Color(0xFF20241F)
val Cream=Color(0xFFF1F2E8)
val Muted=Color(0xFFAFB5A7)
val DisplayFont=FontFamily(Font(R.font.barlow_condensed_bold,FontWeight.Bold))
val WesternFont=FontFamily(Font(R.font.kirsty_regular),Font(R.font.kirsty_bold,FontWeight.Bold))
val WesternDisplayFont=FontFamily(Font(R.font.chinese_rocks_regular))
val GtaDisplayFont=FontFamily(Font(R.font.pricedown_black))
val PixelFont=FontFamily(Font(R.font.minecraft_seven))
val RetroPixelFont=FontFamily(Font(R.font.monocraft))
val BordaFont=FontFamily(Font(R.font.borda_regular),Font(R.font.borda_bold,FontWeight.Bold))
val StratumFont=FontFamily(Font(R.font.stratum2_regular),Font(R.font.stratum2_bold,FontWeight.Bold))
val WindlassFont=FontFamily(Font(R.font.windlass))
val FuturaCondensedFont=FontFamily(Font(R.font.futura_condensed_medium))
val TerminalFont=FontFamily(Font(R.font.share_tech_mono))
val TechnicalFont=FontFamily(Font(R.font.rajdhani))
val AtlasFont=FontFamily(Font(R.font.cinzel))
val PipBoyFont=FontFamily(Font(R.font.roboto_condensed))
val ReadingSerifFont=FontFamily(Font(R.font.libre_baskerville))
val SansFont=FontFamily(Font(R.font.noto_sans))
val OrbitFont=FontFamily(Font(R.font.orbitron))
val ViceFont=FontFamily(Font(R.font.yellowtail))
fun worldFont(world: World)=world.titleTypeface().family
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
        private var openingAnimationPlayed=false
    }
    private val controller get()=(application as WaypointApplication).navigation
    private var openingAnimationVisible by mutableStateOf(false)
    private var destinationLaunch by mutableIntStateOf(0)
    private var foregroundEntry by mutableIntStateOf(0)
    private var questLaunch by mutableIntStateOf(0)
    private var updateLaunch by mutableIntStateOf(0)
    private var questId by mutableStateOf<String?>(null)
    private fun handleQuestIntent(intent: Intent?) {
        val uri = intent?.data
        if (intent?.action == "OPEN_QUEST" && uri?.scheme == "waypoint" && uri.host == "quest") {
            uri.lastPathSegment?.takeIf { runCatching { java.util.UUID.fromString(it).toString() == it }.getOrDefault(false) }?.let { questId = it; questLaunch++; setIntent(intent) }
        }
    }
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
        val openedFromLauncher = savedInstanceState == null &&
            intent?.action == Intent.ACTION_MAIN &&
            intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
        if (openedFromLauncher && !openingAnimationPlayed) {
            openingAnimationPlayed = true
            openingAnimationVisible = true
        }
        handleQuestIntent(intent)
        if(intent?.action==UpdateAlerts.OPEN_UPDATES)updateLaunch++
        val restorePending = savedInstanceState?.getBoolean("waypoint.pendingSharedDestination") == true
        if((savedInstanceState==null || restorePending && !controller.hasPendingNavigationImport()) && controller.handleNavigationIntent(intent)) destinationLaunch++
        setContent {
            MapThemeClock {
                val state by controller.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.navigating) { if(state.navigating) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                Box(Modifier.fillMaxSize()) {
                    WaypointApp(controller,state,destinationLaunch,foregroundEntry,questId,questLaunch,updateLaunch)
                    if (openingAnimationVisible) {
                        WaypointOpeningScreen(onFinished = { openingAnimationVisible = false })
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleQuestIntent(intent)
        if(intent.action==UpdateAlerts.OPEN_UPDATES) { updateLaunch++;setIntent(intent) }
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
    override fun onStart() { super.onStart();if(configurationRestartPending)configurationRestartPending=false else foregroundEntryCounter++;foregroundEntry=foregroundEntryCounter;getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener,null); updateDisplayRotation(); controller.setVisible(true);(application as WaypointApplication).updater.checkOnLaunch();(application as WaypointApplication).quests.refresh() }
    override fun onStop() { configurationRestartPending=isChangingConfigurations;getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener);controller.setVisible(false); super.onStop() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointApp(controller: NavigationController,state: NavState,destinationLaunch:Int=0,foregroundEntry:Int=0,openQuestId:String?=null,questLaunch:Int=0,updateLaunch:Int=0) {
    val context=LocalContext.current
    val carSettingsUnavailable=stringResource(R.string.car_settings_unavailable)
    val application=context.applicationContext as WaypointApplication
    val updater=application.updater
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val worldsStateHolder=rememberSaveableStateHolder()
    var questSeed by remember { mutableStateOf<Place?>(null) }
    var pendingQuestId by rememberSaveable { mutableStateOf<String?>(null) }
    var questEntry by rememberSaveable { mutableIntStateOf(0) }
    var externalPlace by remember { mutableStateOf<Place?>(null) }
    var transitFor by remember { mutableStateOf<Place?>(null) }
    val transitPlanner = remember { TransitousPlanner() }
    val uiScope = rememberCoroutineScope()
    var landing by rememberSaveable { mutableStateOf(controller.shouldShowLanding()) }
    val welcomeVisible=landing && !state.navigating
    var whatsNewVisible by rememberSaveable { mutableStateOf(application.shouldShowWhatsNew()) }
    val bundledReleaseNotes=remember { application.bundledReleaseNotes() }
    LaunchedEffect(destinationLaunch){if(destinationLaunch>0 && !controller.shouldShowLanding()){controller.markMapOpened();landing=false}}
    var search by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.carConnected,tab) { if (state.carConnected && tab == 3) tab = 0 }
    LaunchedEffect(questLaunch) { if (questLaunch > 0 && !controller.isDriving) { landing=false;tab=3;questEntry++;questSeed=null;pendingQuestId=openQuestId } }
    LaunchedEffect(state.sharedImportError) { if (state.sharedImportError) { landing=false;search=true;controller.dismissSharedImportError() } }
    var settings by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(updateLaunch) { if(updateLaunch>0 && !controller.isDriving) { landing=false;settings=true } }
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
    var pageSystemBarSurface by remember { mutableStateOf<Color?>(null) }
    val navigationScope=rememberCoroutineScope()
    fun selectTab(destination:Int) {
        if(destination==3 && controller.state.value.carConnected) {
            navigationScope.launch { snackbar.showSnackbar("Quests are available after Android Auto disconnects.") }
        } else tab=destination
    }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionDenied=!controller.hasLocationPermission()
        permissionRevision++
        if(controller.hasLocationPermission()) controller.enableLocation() else controller.permissionDenied()
    }
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // True when the map has been panned away from the user (or guidance follow is paused).
    var awayFromUser by remember { mutableStateOf(false) }
    fun locate() {
        when(locateAction(controller.hasLocationPermission(),state.fix!=null,state.gpsReady,awayFromUser)) {
            LocateAction.REQUEST_PERMISSION -> {
                controller.recenter()
                permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            LocateAction.ENABLE_GPS -> { controller.enableLocation();controller.recenter() }
            LocateAction.RECENTRE -> controller.recenter()
            LocateAction.NONE -> Unit
        }
    }
    fun choose(place: Place) { controller.markMapOpened();controller.select(place); search=false;tab=0;landing=false }
    val showWhatsNew=whatsNewVisible && !welcomeVisible && !state.navigating && state.selected==null && state.pendingSharedDestination==null &&
        tab==0 && !search && !settings && !steps
    val showUpdatePrompt=pendingRelease!=null && !updatePromptDismissed && !whatsNewVisible && !welcomeVisible && !state.navigating && state.selected==null && state.pendingSharedDestination==null &&
        tab==0 && !search && !settings && !steps
    MaterialTheme(colorScheme=darkColorScheme(primary=WaypointColors.accent,onPrimary=Ink,primaryContainer=WaypointColors.accent.copy(alpha=0.18f),onPrimaryContainer=WaypointColors.accent,
        background=Ink,surface=Ink,surfaceVariant=Panel,onSurface=Cream,onSurfaceVariant=Muted,outline=Color(0xFF51594B),secondary=WaypointColors.accent,
        secondaryContainer=WaypointColors.accent.copy(alpha=.18f),onSecondaryContainer=Cream),typography=AppTypography) {
        UpdateAlertPermissions(application.updateAlerts,ready=!welcomeVisible && !showWhatsNew && !showUpdatePrompt && !settings && !search && tab==0 && !state.navigating && !state.carConnected && state.pendingSharedDestination==null)
        BackHandler(search || settings || steps || tab!=0 || (!welcomeVisible && state.selected!=null)) {
            when { search->search=false; feedbackOpen->feedbackOpen=false; settings->settings=false; steps->steps=false; tab!=0->tab=0; state.selected!=null->controller.clearDestination() }
        }
        LaunchedEffect(state.message) { state.message?.let{snackbar.showSnackbar(it);controller.dismissMessage()} }
        CompositionLocalProvider(LocalPageSystemBarSurface provides { pageSystemBarSurface=it }) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val wide=maxWidth>=600.dp
            val landscape=maxWidth>maxHeight
            val compactHeight=maxHeight<500.dp
            val density=LocalDensity.current
            val largeText=density.fontScale>1.3f
            val adaptivePreview=state.selected!=null && !state.navigating && (compactHeight || landscape || largeText)
            val mapContentWidth=maxWidth-(if(wide && !state.navigating && !welcomeVisible)80.dp else 0.dp)
            // Leave at least 300 dp for the map after the navigation rail.
            val sidePreviewWidth=if(adaptivePreview && landscape && wide && mapContentWidth>=600.dp)
                minOf(if(density.fontScale>=1.75f)420.dp else 360.dp,mapContentWidth-300.dp) else null
            val sidePreview=sidePreviewWidth!=null
            val edge=with(density){28.dp.toPx()}
            val systemBottom=WindowInsets.navigationBars.getBottom(density)
            val routeInsets=if(sidePreview && !mapBounds.isEmpty && !routePanelBounds.isEmpty) {
                MapViewportInsets(edge.roundToInt(),edge.roundToInt(),
                    (mapBounds.right-routePanelBounds.left+edge).coerceAtLeast(edge).roundToInt(),(systemBottom+edge).roundToInt())
            } else if(!adaptivePreview && state.selected!=null && !state.navigating && !mapBounds.isEmpty && !routeHeaderBounds.isEmpty && !routePanelBounds.isEmpty) {
                val top=(routeHeaderBounds.bottom-mapBounds.top+edge).coerceAtLeast(edge)
                val right=if(compactHeight)(mapBounds.right-routePanelBounds.left+edge).coerceAtLeast(edge) else edge
                val bottom=if(compactHeight)systemBottom+edge else (mapBounds.bottom-routePanelBounds.top+edge).coerceAtLeast(edge)
                // Fit the clear middle of the portrait route preview.
                if(top+bottom<mapBounds.height-32 && edge+right<mapBounds.width-32)
                    MapViewportInsets(edge.roundToInt(),top.roundToInt(),right.roundToInt(),bottom.roundToInt())
                else null
            } else null
            val scaleInsets=when {
                routeInsets!=null -> routeInsets
                state.navigating && !landscape && !mapBounds.isEmpty &&
                    !routeHeaderBounds.isEmpty && !routePanelBounds.isEmpty -> {
                    val top=(routeHeaderBounds.bottom-mapBounds.top+edge).coerceAtLeast(0f)
                    val bottom=(mapBounds.bottom-routePanelBounds.top+edge).coerceAtLeast(0f)
                    if(top+bottom+with(density){80.dp.toPx()}<mapBounds.height)
                        MapViewportInsets(0,top.roundToInt(),0,bottom.roundToInt())
                    else null
                }
                else -> null
            }
            val scaleFits=scaleInsets==null || mapBounds.isEmpty ||
                (mapBounds.height-scaleInsets.top-scaleInsets.bottom>with(density){72.dp.toPx()} &&
                    mapBounds.width-scaleInsets.left-scaleInsets.right>
                        with(density){(if(compactHeight)160.dp else 180.dp).toPx()})
            val scaleVisible=tab==0 && !welcomeVisible && !search && !settings && !steps && scaleFits &&
                (!adaptivePreview || sidePreview) &&
                (state.selected==null || scaleInsets!=null || state.navigating && landscape)
            Row(Modifier.fillMaxSize()) {
                if(wide && !state.navigating && !welcomeVisible) NavigationRail(containerColor=Ink,modifier=Modifier.statusBarsPadding()) {
                    if(!compactHeight) { Spacer(Modifier.height(16.dp));Icon(Icons.Default.Navigation,"Waypoint",tint=WaypointColors.accent);Spacer(Modifier.height(32.dp)) }
                    listOf("Map" to Icons.Default.Map,"Worlds" to Icons.Default.Layers,"Saved" to Icons.Default.BookmarkBorder,"Quests" to Icons.Default.Assignment).forEachIndexed { i,(name,icon)->
                        val artworkIcon=when(i) { 1 -> R.drawable.waypoint_worlds; 2 -> R.drawable.waypoint_bookmark; else -> null }
                        NavigationRailItem(selected=tab==i,onClick={selectTab(i)},icon={
                            if(artworkIcon!=null) Icon(painterResource(artworkIcon),name) else Icon(icon,name)
                        },label={Text(name)})
                    }
                }
                Scaffold(modifier=Modifier.weight(1f).withReadyWindowInsets(),snackbarHost={SnackbarHost(snackbar)},contentWindowInsets=WindowInsets(0),bottomBar={
                    if(!wide && !state.navigating && !welcomeVisible) HudNavigationBar(state.world,tab,{selectTab(it)})
                }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                        // Keep the native map alive when switching destinations; hide other screens above it.
                        if(!welcomeVisible) WorldMap(state,{choose(it)},{mapError=it},Modifier.fillMaxSize().onGloballyPositioned{mapBounds=it.boundsInRoot()},active=tab==0 && !search && !settings && !steps,routeInsets=routeInsets,onAwayFromUser={awayFromUser=it},onSelectRoute={ index -> if(state.navigating)controller.switchToAlternate(index) else controller.selectRouteOption(index) },showScaleBar=scaleVisible,scaleBarInsets=scaleInsets)
                        Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=0.6f),Color.Transparent))))
                        if(!welcomeVisible && (tab==0 || state.navigating)) {
                            BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                                val overlayWidth=maxWidth
                                val overlayHeight=maxHeight
                                val atlasActions=AtlasActions({search=true},{tab=1},{tab=2},{settings=true},
                                    {locate()},controller::toggleTilt,controller::toggleVoice,
                                    {query->search=true;controller.search(query)},awayFromUser=awayFromUser)
                                if(!state.navigating && state.selected==null) {
                                    GameAtlasChrome(state,atlasActions,Modifier.padding(bottom=if(landscape)0.dp else 48.dp))
                                    if(mapError!=null)Notice(mapError!!,Icons.Default.CloudOff)
                                } else if(adaptivePreview) {
                                    AdaptiveRoutePreview(state,
                                        modifier=Modifier.align(if(sidePreview)Alignment.CenterEnd else Alignment.Center)
                                            .then(if(sidePreview)Modifier.width(sidePreviewWidth) else Modifier.fillMaxWidth())
                                            .fillMaxHeight()
                                            .onGloballyPositioned { routePanelBounds=it.boundsInRoot() },
                                        onBack=controller::clearDestination,onSettings={settings=true},onMode=controller::changeMode,
                                        onLocate=::locate,onSave={controller.toggleSaved(state.selected!!)},
                                        onRetry={controller.requestRoute()},onStart={
                                            controller.startNavigation()
                                            if(Build.VERSION.SDK_INT>=33)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },onSteps={steps=true},
                                        onSelectRoute=controller::selectRouteOption,
                                        onQuest={if(!state.carConnected){questSeed=state.selected;questEntry++};selectTab(3)},
                                        onShare={externalPlace=state.selected},mapError=mapError,
                                        compactLandscape=sidePreview,onTransit={transitFor=state.selected})
                                } else if(state.navigating && landscape) {
                                    AtlasFrame(state.world)
                                    Column(Modifier.align(Alignment.TopStart)
                                        .width(minOf(400.dp,overlayWidth/2-16.dp))
                                        .onGloballyPositioned { routeHeaderBounds=it.boundsInRoot() }
                                        .padding(start=16.dp,top=12.dp)
                                        .heightIn(max=overlayHeight*0.5f)
                                        .verticalScroll(rememberScrollState()),
                                        verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        CompactLandscapeNavBanner(state,{steps=true},{selectTab(1)})
                                        when {
                                            mapError!=null -> Notice(mapError!!,Icons.Default.CloudOff)
                                            !state.gpsReady -> Notice(state.gpsStatus,Icons.Default.GpsOff)
                                            state.routeError!=null -> Notice("Reroute unavailable. ${state.routeError}",Icons.Default.CloudOff)
                                        }
                                    }
                                    Box(Modifier.align(Alignment.BottomEnd)
                                        .width(minOf(384.dp,overlayWidth/2-32.dp))
                                        .onGloballyPositioned { routePanelBounds=it.boundsInRoot() }
                                        .padding(end=16.dp,bottom=12.dp)) {
                                        CompactLandscapeNavFooter(state,{steps=true},{controller.stopNavigation()},
                                            controller::toggleVoice,atlasActions.locate,Modifier.fillMaxWidth())
                                    }
                                } else {
                                    AtlasFrame(state.world)
                                    Column(Modifier.align(Alignment.TopStart).fillMaxWidth(if(compactHeight)0.5f else 1f).onGloballyPositioned{routeHeaderBounds=it.boundsInRoot()}.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        if(state.navigating)ManeuverBanner(state,{steps=true}) else {
                                            RoutePreviewHeader(state,controller::clearDestination,controller::changeMode)
                                        }
                                        if(mapError!=null)Notice(mapError!!,Icons.Default.CloudOff)
                                        if(state.navigating && !state.gpsReady)Notice(state.gpsStatus,Icons.Default.GpsOff)
                                        if(state.navigating && state.routeError!=null)Notice("Reroute unavailable. ${state.routeError}",Icons.Default.CloudOff)
                                    }
                                    Column(Modifier.align(if(compactHeight)Alignment.BottomEnd else Alignment.BottomCenter).fillMaxWidth(if(compactHeight)0.48f else 1f).onGloballyPositioned{routePanelBounds=it.boundsInRoot()}.padding(horizontal=if(state.navigating)0.dp else 16.dp).padding(bottom=if(state.navigating)0.dp else 56.dp).then(if(compactHeight)Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState()) else Modifier),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        if(state.navigating && awayFromUser) Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) { RecentrePill(state.world,atlasActions.locate) }
                                        if(state.navigating) NavSideControls(state,controller::toggleVoice,atlasActions.locate,{selectTab(1)})
                                        if(!state.navigating && !state.gpsReady) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {
                                            FixLabel(state,Modifier.weight(1f))
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
                                        },{steps=true},compact=compactHeight,
                                            onSelectRoute=controller::selectRouteOption,
                                            onQuest={if(!state.carConnected){questSeed=state.selected;questEntry++};selectTab(3)},onShare={externalPlace=state.selected},onTransit={transitFor=state.selected})
                                    }
                                }

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
                        AnimatedContent(targetState=tab,modifier=Modifier.fillMaxSize(),
                            transitionSpec={(fadeIn(tween(180,delayMillis=40)) togetherWith fadeOut(tween(100))).using(null)},
                            label="Main destinations") { destination ->
                            when(destination) {
                                1 -> worldsStateHolder.SaveableStateProvider("worlds") {
                                    WorldsScreen(state.world,{controller.changeWorld(it);tab=0},{tab=0})
                                }
                                2 -> if(!state.navigating) SavedScreen(state,::choose,{controller.toggleSaved(it)},{search=true},controller::renameSaved)
                                3 -> if(!state.navigating && !state.carConnected) QuestsScreen(state.world,application.quests,controller,state,pendingQuestId,questSeed,questEntry,::choose,{tab=0},{pendingQuestId=null;questSeed=null})
                                else -> Box(Modifier.fillMaxSize())
                            }
                        }
                        if(search) SearchScreen(state,controller::search,::choose,{search=false},controller::changeMode,controller::submitSearch)
                    }
                }
            }
            pageSystemBarSurface?.let { surface ->
                Box(Modifier.align(Alignment.TopStart).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(surface))
            }
        }
        }
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp), contentAlignment=Alignment.TopCenter) {
            QuestCelebration(state.world,application.quests,
                allowed=!welcomeVisible && !state.navigating && !state.carConnected && state.selected==null && !search && !settings && !showWhatsNew && state.pendingSharedDestination==null,
                onOpen={tab=3})
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
                SettingRow("World voice style","Give each world its own spoken wording and delivery on your device's speech voice. Turn it off for standard instructions.",state.gameVoice,controller::toggleGameVoice)
                SettingRow("Miles & feet","Use imperial distances",state.imperial,controller::toggleUnits)
                SettingRow("Speed limit warning","Show the posted limit and warn when you go over it while driving. Looks up the limit on OpenStreetMap through Overpass, sending your position only while guiding by car.",state.speedWarning,controller::toggleSpeedWarning)
                SettingRow("3D perspective","Tilt the map in every world",state.tilted,controller::toggleTilt)
                CursorModelSettings(state,controller)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Text("Map controls", style=MaterialTheme.typography.titleMedium)
                Text("These presets affect controls floating over the phone map.",
                    style=MaterialTheme.typography.bodySmall, color=Muted)
                Text("Size", style=MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    HudSize.entries.forEach { size ->
                        FilterChip(selected=state.hudSize==size,onClick={controller.setHudSize(size)},
                            label={Text(size.label)},modifier=Modifier.heightIn(min=48.dp))
                    }
                }
                Text("Side", style=MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf<Pair<HudSide?,String>>(null to "Original", HudSide.LEFT to "Left", HudSide.RIGHT to "Right")
                        .forEach { (side,label) ->
                            FilterChip(selected=state.hudSide==side,onClick={controller.setHudSide(side)},
                                label={Text(label)},modifier=Modifier.heightIn(min=48.dp))
                        }
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                AndroidAutoSettingsPanel(state.carConnected,controller.hasLocationPermission(),state.navigating,
                    onLocation={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))},
                    onUnavailable={navigationScope.launch { snackbar.showSnackbar(carSettingsUnavailable) }})
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Text("Location & sensors",style=MaterialTheme.typography.titleMedium)
                Text(state.locationProvider,style=MaterialTheme.typography.bodyMedium)
                Text(state.sensorDescription,style=MaterialTheme.typography.bodySmall,color=Muted)
                Text(state.headingStatus,style=MaterialTheme.typography.bodySmall,color=if(state.headingReliable)WaypointColors.accent else Color(0xFFFFCF89))
                if(state.fix!=null)Text("Position accuracy: ±${state.accuracy.roundToInt()} m",style=MaterialTheme.typography.bodySmall,color=Muted)
                state.satellitesVisible?.let{Text("Satellites: ${state.satellitesUsed ?: 0} used / $it visible",style=MaterialTheme.typography.bodySmall,color=Muted)}
                Text(state.gpsStatus,style=MaterialTheme.typography.bodyMedium,color=Muted)
                TextButton(onClick={context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))}) { Text("Open device location settings") }
                TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}) { Text("Manage location permissions") }
                TextButton(onClick={clearConfirm=true}) { Text("Clear saved places and history",color=MaterialTheme.colorScheme.error) }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                FeedbackSettingsRow(onClick={feedbackOpen=true})
                CommunitySettingsRow()
                Text("WAYPOINT",style=MaterialTheme.typography.headlineMedium,color=WaypointColors.accent)
                Text("Made in India · Open to the world",style=MaterialTheme.typography.bodySmall,color=Muted)
                Text("Open source. Every world included. No ads or subscriptions.",style=MaterialTheme.typography.bodyMedium)
                Text("OpenStreetMap with OSRM routes cars; Valhalla routes motorcycles, bicycles and walks. Ferrostar provides navigation guidance, rerouting and remaining ETA. Live traffic is not currently configured. Your location is shared with the selected routing and search providers when needed. Saved places stay on this phone.",style=MaterialTheme.typography.bodySmall,color=Muted)
                Text("Satellite imagery in the Elden Ring, Zelda and Hogwarts maps: Esri, Maxar, Earthstar Geographics, and the GIS User Community. Those maps ask Esri for imagery of the area on screen.",style=MaterialTheme.typography.bodySmall,color=Muted)
                Text("Map data and credits",style=MaterialTheme.typography.titleSmall)
                val creditsUri=LocalUriHandler.current
                TextButton(onClick={creditsUri.openUri("https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap contributors")}
                TextButton(onClick={creditsUri.openUri("https://openfreemap.org/")}){Text("Map tiles · OpenFreeMap")}
                TextButton(onClick={creditsUri.openUri("https://github.com/tilezen/joerd/blob/master/docs/attribution.md")}){Text("Terrain · Mapzen and data providers")}
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
            confirmButton={TextButton(onClick={search=false;tab=0;controller.acceptPendingNavigationImport(sharedDestinationId)}){Text("Open destination")}},
            dismissButton={TextButton(onClick={controller.dismissPendingNavigationImport(sharedDestinationId)}){Text("Cancel")}},
        )
        transitFor?.let { place -> TransitSheet(place,state.fix,state.world,transitPlanner) { transitFor=null } }
        externalPlace?.let { place -> AlertDialog(onDismissRequest={externalPlace=null},title={Text("Open in another map app?")},
            text={Text(if(state.navigating)"Waypoint’s voice guidance will stop. The other app will calculate its own route to ${place.name}." else "Choose an app to open ${place.name}. It will calculate its own route.")},
            confirmButton={TextButton(onClick={
                externalPlace=null
                runCatching { NavigationSharing.open(context,place,state.mode) }.onSuccess { if(state.navigating)controller.stopNavigation() }
                    .onFailure { uiScope.launch { snackbar.showSnackbar("No map app could open this place. Install a map app or try again.") } }
            }){Text("Choose app")}},dismissButton={TextButton(onClick={externalPlace=null}){Text("Cancel")}}) }
        if(clearConfirm) AlertDialog(onDismissRequest={clearConfirm=false},title={Text("Clear your places?")},text={Text("This removes your saved destinations and recent navigation history from this phone.")},confirmButton={TextButton(onClick={controller.clearHistory();clearConfirm=false}){Text("Clear places")}},dismissButton={TextButton(onClick={clearConfirm=false}){Text("Cancel")}})
        if(steps) ModalBottomSheet(onDismissRequest={steps=false},containerColor=Ink) {
            Text("Your route",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(horizontal=24.dp))
            state.selected?.let { place -> TextButton(onClick={steps=false;externalPlace=place},modifier=Modifier.padding(horizontal=16.dp).heightIn(min=48.dp)) { Text("Open in another map app") } }
            LazyColumn(Modifier.fillMaxWidth().padding(vertical=16.dp)) {
                items(state.route?.steps.orEmpty().drop(if(state.navigating)state.progress?.nextStep ?: 0 else 0)) { step ->
                    ListItem(headlineContent={Text(step.instruction)},supportingContent={Text(formatDistance(step.distance,state.imperial))},leadingContent={ManeuverGlyph(step,Modifier.size(32.dp),WaypointColors.accent,blocky=state.world.blockyArrows())},colors=ListItemDefaults.colors(containerColor=Ink))
                }
            }
        }
    }
}

@Composable fun CircleControl(icon: ImageVector,label: String,onClick:()->Unit) { FilledIconButton(onClick=onClick,modifier=Modifier.size(48.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Ink,contentColor=Cream)) { Icon(icon,label,Modifier.size(22.dp)) } }
@Composable fun Notice(text: String,icon: ImageVector) { Surface(color=Panel,shape=RoundedCornerShape(12.dp)) { Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,Modifier.size(20.dp),tint=Color(0xFFFFCF89));Text(text,style=MaterialTheme.typography.bodySmall) } } }
@Composable fun Attribution(world:World,mode:TravelMode?=null,navigating:Boolean=false) {
    val context=LocalContext.current
    val uri=LocalUriHandler.current
    val palette=hudPalette(world)
    val profile=remember(world){WorldLegendProfiles.forWorld(world)}
    val routeColor=Color(android.graphics.Color.parseColor(
        if(world==World.SAINTS && navigating)"#86D955" else world.routeColor))
    var open by remember { mutableStateOf(false) }
    TextButton(onClick={open=true},modifier=Modifier.heightIn(min=48.dp).semantics{contentDescription="Map information"}) {
        Row(Modifier.background(Ink.copy(alpha=.94f),RoundedCornerShape(4.dp)).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Icon(Icons.Default.Info,null,Modifier.size(14.dp),tint=Cream);Text("Map info",color=Cream,fontSize=TypeScale.Caption)}
    }
    if(open) AlertDialog(onDismissRequest={open=false},title={Text("Map information · ${world.title}",color=palette.foreground)},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Map key",style=MaterialTheme.typography.titleSmall,color=palette.foreground)
            MapLegendRow(world,"route",profile.navigation.route,routeColor=routeColor,textColor=palette.foreground,mode=mode,navigating=navigating)
            MapLegendRow(world,"your-location",profile.navigation.position,textColor=palette.foreground)
            MapLegendRow(world,"destination",profile.navigation.destination,textColor=palette.foreground)
            Text(profile.title,style=MaterialTheme.typography.titleSmall,color=palette.foreground,modifier=Modifier.padding(top=4.dp))
            val categories=MapPoiCategories.all(context)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    categories.filterIndexed { index, _ -> index % 2 == 0 }.forEach { category ->
                        MapLegendRow(world,category.id,profile.aliases[category.id] ?: category.label,textColor=palette.foreground)
                    }
                }
                Column(Modifier.weight(1f)) {
                    categories.filterIndexed { index, _ -> index % 2 == 1 }.forEach { category ->
                        MapLegendRow(world,category.id,profile.aliases[category.id] ?: category.label,textColor=palette.foreground)
                    }
                }
            }
            Text("Map data and credits",style=MaterialTheme.typography.titleSmall,color=palette.foreground,modifier=Modifier.padding(top=4.dp))
            TextButton(onClick={uri.openUri("https://www.openstreetmap.org/copyright")}){Text("© OpenStreetMap contributors",color=palette.accent)}
            TextButton(onClick={uri.openUri("https://openfreemap.org/")}){Text("Map tiles · OpenFreeMap",color=palette.accent)}
            if(world.hasTerrain())TextButton(onClick={uri.openUri("https://github.com/tilezen/joerd/blob/master/docs/attribution.md")}){Text("Terrain · Mapzen and data providers",color=palette.accent)}
        }
    },confirmButton={TextButton(onClick={open=false}){Text("Close",color=palette.accent)}},
        containerColor=palette.panel,titleContentColor=palette.foreground,textContentColor=palette.foreground)
}
@Composable private fun MapLegendRow(world:World,kind:String,label:String,modifier:Modifier=Modifier,
                                     routeColor:Color=Color(android.graphics.Color.parseColor(world.routeColor)),
                                     textColor:Color=MaterialTheme.colorScheme.onSurface,
                                     mode:TravelMode?=null,navigating:Boolean=false) {
    Row(modifier.heightIn(min=30.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        MapLegendIcon(world,kind,routeColor,mode,navigating)
        Text(label,style=MaterialTheme.typography.bodySmall,color=textColor,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
@Composable private fun MapLegendIcon(world:World,kind:String,routeColor:Color,mode:TravelMode?,navigating:Boolean) {
    if(kind=="route") {
        Canvas(Modifier.size(26.dp)) {
            val y=size.height/2;val width=4.dp.toPx();val start=2.dp.toPx();val end=size.width-2.dp.toPx()
            val grammar=world.routeGrammar(mode,navigating)
            val pattern=world.routeLineDash(mode,navigating)
            val effect=if(grammar==RouteGrammar.SOLID) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(pattern.map{it*width*1.5f}.toFloatArray())
            drawLine(routeColor,Offset(start,y),Offset(end,y),strokeWidth=width,cap=StrokeCap.Round,pathEffect=effect)
        }
    } else {
        val context=LocalContext.current
        val night=LocalNightMapTheme.current
        val bitmap=remember(context,world,kind,night) {
            when(kind) {
                "your-location" -> mapPlayer(world)
                "destination" -> mapDestination(context,world,night)
                else -> mapPoi(context,kind,world,night)
            }.asImageBitmap()
        }
        Image(bitmap=bitmap,contentDescription=null,modifier=Modifier.size(26.dp))
    }
}
@Composable fun SettingRow(title:String,subtitle:String,checked:Boolean,action:()->Unit) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Muted)};Switch(checked,onCheckedChange={action()},modifier=Modifier.semantics { contentDescription=title }) } }
fun maneuverIcon(step: Maneuver?): ImageVector = when { step==null->Icons.Default.North; step.type=="arrive"->Icons.Default.Flag; step.type.contains("roundabout")||step.type.contains("rotary")->Icons.Default.RoundaboutRight; step.modifier=="uturn"->if(step.uTurnRight)Icons.Default.UTurnRight else Icons.Default.UTurnLeft; step.modifier.contains("left")->Icons.Default.TurnLeft;step.modifier.contains("right")->Icons.Default.TurnRight;else->Icons.Default.Straight }

@Composable fun ManeuverBanner(state: NavState,onClick:()->Unit) = NavBanner(state,onClick)
@Composable fun NavigationFooter(state: NavState,onSteps:()->Unit,onStop:()->Unit) = NavBottomBar(state,onSteps,onStop)
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
@OptIn(ExperimentalLayoutApi::class)
@Composable fun DestinationCard(state: NavState,onSave:()->Unit,onClose:()->Unit,onLocate:()->Unit,onRetry:()->Unit,onStart:()->Unit,onSteps:()->Unit,onSelectRoute:(Int)->Unit,compact:Boolean=false,onQuest:(()->Unit)?=null,onShare:(()->Unit)?=null,roomyActions:Boolean=false,onTransit:(()->Unit)?=null) {
    val place=state.selected ?: return
    val palette=hudPalette(state.world)
    val (categoryIcon,categoryLabel)=placeCategory(place.kind)
    val detail=if(place.address=="Selected on map")"$categoryLabel · Selected on map" else place.address
    val expandedActions=roomyActions || LocalDensity.current.fontScale>1.3f
    Surface(color=palette.panel,contentColor=palette.foreground,shape=hudShape(state.world),border=hudBorder(state.world),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if(compact)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact)8.dp else 12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(color=palette.accent.copy(alpha=.15f),shape=hudShape(state.world)) {Icon(categoryIcon,categoryLabel,Modifier.padding(10.dp).size(22.dp),tint=palette.accent)}
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(if(state.arrived)"YOU HAVE ARRIVED" else place.name,style=if(compact)MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(state.arrived)place.name else detail,style=MaterialTheme.typography.bodySmall,color=palette.muted,maxLines=if(compact)1 else 2,overflow=TextOverflow.Ellipsis) }
                if(!expandedActions) DestinationActions(state,onSave,onQuest,onShare,false,onTransit)
            }
            if(expandedActions) DestinationActions(state,onSave,onQuest,onShare,true,onTransit)
            when {
                state.arrived -> Button(onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text("Done") }
                state.routing -> { LinearProgressIndicator(modifier=Modifier.fillMaxWidth());Text("Finding a ${state.mode.routeDescription} route…",style=MaterialTheme.typography.bodySmall,color=palette.muted) }
                state.routeError!=null -> { Text(state.routeError,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error);Button(onClick=onRetry,modifier=Modifier.fillMaxWidth()){Text("Try route again")} }
                state.route!=null -> {
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.align(Alignment.CenterVertically),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Icon(modeIcon(state.mode),null,tint=palette.muted,modifier=Modifier.size(20.dp))
                            Text("${ceil(state.route.seconds/60).toInt().coerceAtLeast(1)} min",style=MaterialTheme.typography.titleLarge,color=palette.accent)
                        }
                        Text("· ${formatDistance(state.route.meters,state.imperial)}",color=palette.muted,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.align(Alignment.CenterVertically))
                        TextButton(onClick=onSteps,modifier=Modifier.align(Alignment.CenterVertically)){Text("Steps",color=palette.accent)}
                        if(onTransit!=null && !state.navigating) TextButton(onClick=onTransit,modifier=Modifier.align(Alignment.CenterVertically).heightIn(min=48.dp)){Icon(Icons.Default.DirectionsBus,null,Modifier.size(18.dp),tint=palette.accent);Spacer(Modifier.width(6.dp));Text("Bus & transit",color=palette.accent)}
                    }
                    if(!state.navigating && state.routeOptions.size>1) {
                        RouteChipRow(state,onSelectRoute)
                    }
                    Button(onClick=onStart,enabled=state.gpsReady,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=hudShape(state.world)) { Icon(Icons.Default.Navigation,null,Modifier.size(18.dp));Spacer(Modifier.width(10.dp));Text(if(state.gpsReady)"Start navigation" else "Waiting for GPS…",fontWeight=FontWeight.Bold) }
                }
                else -> { Text("Use your precise location to get ${state.mode.routeDescription} directions.",style=MaterialTheme.typography.bodySmall,color=palette.muted);Button(onClick=onLocate,modifier=Modifier.fillMaxWidth()){Text("Enable GPS for directions")} }
            }
        }
    }
}

/** Route alternatives as one scrolling row of chips named by what differs, not "Route 1 / Route 2". */
@Composable private fun RouteChipRow(state:NavState,onSelect:(Int)->Unit) {
    val palette=hudPalette(state.world);val shape=hudShape(state.world)
    val routes=state.routeOptions
    val fastest=routes.minByOrNull{it.seconds};val shortest=routes.minByOrNull{it.meters}
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        routes.forEachIndexed { index,route ->
            val selected=route===state.route
            val minutes=ceil(route.seconds/60).toInt().coerceAtLeast(1)
            val label=when { route===fastest->"Fastest"; route===shortest->"Shorter"; else->"Alternative" }
            val extra=if(fastest!=null && route!==fastest) ceil((route.seconds-fastest.seconds)/60).toInt() else 0
            val via=route.steps.filter{it.road.isNotBlank()}.maxByOrNull{it.distance}?.road
            val distance=formatDistance(route.meters,state.imperial)
            Column(Modifier.heightIn(min=56.dp).clip(shape)
                .background(if(selected)palette.accent.copy(alpha=.15f) else palette.panel.copy(alpha=.82f))
                .border(BorderStroke(1.dp,if(selected)palette.accent else palette.muted.copy(alpha=.45f)),shape)
                .selectable(selected=selected,onClick={onSelect(index)},role=Role.RadioButton)
                .semantics { contentDescription="Route ${index+1}, $label, $minutes minutes, $distance, ${if(selected)"selected" else "not selected"}" }
                .padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(label,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=palette.foreground)
                Text("$minutes min · $distance"+if(extra>0)" · +$extra min" else "",style=MaterialTheme.typography.bodySmall,color=palette.accent)
                if(via!=null)Text("via $via",style=MaterialTheme.typography.bodySmall,color=palette.muted,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=180.dp))
            }
        }
    }
}

@Composable private fun RouteOptionRow(world:World,route:RoadRoute,index:Int,selected:Boolean,imperial:Boolean,onClick:()->Unit) {
    val palette=hudPalette(world)
    val shape=hudShape(world)
    val road=route.steps.firstOrNull { it.road.isNotBlank() }?.road
    Row(
        Modifier.fillMaxWidth().heightIn(min=56.dp).clip(shape)
            .background(if(selected)palette.accent.copy(alpha=.15f) else palette.panel.copy(alpha=.82f))
            .border(BorderStroke(1.dp,if(selected)palette.accent else palette.muted.copy(alpha=.45f)),shape)
            .selectable(selected=selected,onClick=onClick,role=Role.RadioButton)
            .semantics { contentDescription="Route ${index+1}, ${ceil(route.seconds/60).toInt().coerceAtLeast(1)} minutes, ${formatDistance(route.meters,imperial)}, ${if(selected)"selected" else "not selected"}" }
            .padding(horizontal=8.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,
    ) {
        RadioButton(selected=selected,onClick=null,modifier=Modifier.size(48.dp),colors=RadioButtonDefaults.colors(
            selectedColor=palette.accent,unselectedColor=palette.muted,
        ))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text("Route ${index+1}",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=palette.foreground)
            Text(road ?: "Route overview",style=MaterialTheme.typography.bodySmall,color=palette.muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment=Alignment.End) {
            Text("${ceil(route.seconds/60).toInt().coerceAtLeast(1)} min",style=MaterialTheme.typography.titleSmall,color=palette.accent)
            Text(formatDistance(route.meters,imperial),style=MaterialTheme.typography.bodySmall,color=palette.muted)
        }
    }
}

@Composable fun SearchScreen(state: NavState,onQuery:(String)->Unit,onSelect:(Place)->Unit,onBack:()->Unit,onMode:(TravelMode)->Unit,onSubmit:()->Unit={onQuery(state.query)}) {
    val keyboard=LocalSoftwareKeyboardController.current
    fun submit(){keyboard?.hide();onSubmit()}
    val palette=hudPalette(state.world)
    Surface(color=palette.panel,contentColor=palette.foreground,modifier=Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back to map")}
            OutlinedTextField(value=state.query,onValueChange={onQuery(it.take(NavigationSharing.MAX_INPUT))},label={Text("Place or address")},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={submit()}),modifier=Modifier.weight(1f),shape=hudShape(state.world),trailingIcon={if(state.query.isNotEmpty())IconButton(onClick={onQuery("")}){Icon(Icons.Default.Close,"Clear search")}},colors=OutlinedTextFieldDefaults.colors(focusedTextColor=palette.foreground,unfocusedTextColor=palette.foreground,cursorColor=palette.accent,focusedBorderColor=palette.accent,unfocusedBorderColor=palette.muted,focusedLabelColor=palette.accent,unfocusedLabelColor=palette.muted,focusedLeadingIconColor=palette.foreground,focusedTrailingIconColor=palette.foreground,unfocusedTrailingIconColor=palette.foreground))
        }
        Button(onClick={submit()},enabled=state.query.trim().length>=2,shape=hudShape(state.world),modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
            Icon(painterResource(R.drawable.waypoint_search),null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Search places")
        }
        ModeSegments(state.mode,onMode,state.world,Modifier.padding(horizontal=24.dp).fillMaxWidth())
        Text(if(state.fix==null)"Search worldwide · preview near Kolkata" else "Search near your location",style=MaterialTheme.typography.bodySmall,color=palette.muted,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp))
        if(state.searchNote!=null)Text(state.searchNote,style=MaterialTheme.typography.bodySmall,color=palette.muted,modifier=Modifier.padding(horizontal=24.dp,vertical=8.dp))
        if(state.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=24.dp))
        if(state.searchError!=null) { Notice(state.searchError,Icons.Default.CloudOff);TextButton(onClick={submit()}){Text("Retry search")} }
        LazyColumn {
            if(state.query.length<2) {
                item { Text(if(state.recent.isEmpty())"Your next destination starts here." else "Recent destinations",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(24.dp)) }
                items(state.recent,key={it.id}){PlaceRow(it,state,onSelect)}
                if(state.recent.isEmpty()) item { Text("Find an address, a landmark, or your next coffee stop. You can also press and hold the map to drop a pin.",color=palette.muted,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(horizontal=24.dp)) }
            } else {
                items(state.searchResults,key={it.id}){PlaceRow(it,state,onSelect)}
                if(!state.searching && state.searchResults.isEmpty() && state.searchError==null && state.searchNote==null) item{Text("No places found. Try a street, city, or fuller address.",modifier=Modifier.padding(24.dp),color=palette.muted)}
            }
        }
    } }
}
@Composable fun PlaceRow(place: Place,state: NavState,onSelect:(Place)->Unit) {
    ListItem(headlineContent={Text(place.name,maxLines=2,overflow=TextOverflow.Ellipsis)},supportingContent={Column { Text(place.address,maxLines=3,overflow=TextOverflow.Ellipsis);if(place.source.isNotEmpty())Text(place.source,style=MaterialTheme.typography.labelSmall,color=hudPalette(state.world).muted) }},leadingContent={Icon(painterResource(R.drawable.waypoint_map_pin),null,Modifier.size(24.dp),tint=Color(0xFF9CCB83))},trailingContent={state.fix?.let{Text(formatDistance(distance(it,place.point),state.imperial),style=MaterialTheme.typography.labelSmall,color=hudPalette(state.world).muted)}},modifier=Modifier.clickable{onSelect(place)},colors=hudPalette(state.world).let{ListItemDefaults.colors(containerColor=Color.Transparent,headlineColor=it.foreground,supportingColor=it.muted,trailingIconColor=it.muted)})
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
