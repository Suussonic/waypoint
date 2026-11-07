package com.waypoint.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Large text and short windows get one scroll owner, so route actions cannot cover the modes. */
@Composable internal fun AdaptiveRoutePreview(state: NavState, modifier: Modifier,
    onBack: () -> Unit, onSettings: () -> Unit, onMode: (TravelMode) -> Unit,
    onLocate: () -> Unit, onSave: () -> Unit, onRetry: () -> Unit, onStart: () -> Unit,
    onSteps: () -> Unit, onSelectRoute: (Int) -> Unit, onQuest: () -> Unit, onShare: () -> Unit, mapError: String?,
    compactLandscape: Boolean = false) {
    val palette=hudPalette(state.world)
    val needsFix=!state.gpsReady || state.accuracy>20 || !state.headingReliable
    Column(modifier.padding(if(compactLandscape)8.dp else 12.dp),
        verticalArrangement=Arrangement.spacedBy(if(compactLandscape)8.dp else 10.dp)) {
        if(compactLandscape) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val headerWidth=maxWidth
                if(headerWidth>=304.dp) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ChromeButton(state.world,Icons.AutoMirrored.Filled.ArrowBack,"Close route preview",onBack)
                        ModeSegments(state.mode,onMode,state.world,Modifier.weight(1f))
                        ChromeButton(state.world,Icons.Default.Tune,"Settings",onSettings)
                    }
                } else {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            ChromeButton(state.world,Icons.AutoMirrored.Filled.ArrowBack,"Close route preview",onBack)
                            Text("Route",fontFamily=worldFont(state.world),style=MaterialTheme.typography.titleMedium,
                                modifier=Modifier.weight(1f))
                            ChromeButton(state.world,Icons.Default.Tune,"Settings",onSettings)
                        }
                        if(headerWidth>=192.dp) ModeSegments(state.mode,onMode,state.world,Modifier.fillMaxWidth())
                        else TransportModes(state.mode,onMode,state.world)
                    }
                }
            }
        } else {
            Surface(color=palette.panel,contentColor=palette.foreground,shape=hudShape(state.world)) {
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick=onBack,modifier=Modifier.semantics { contentDescription="Close route preview" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,null)
                    }
                    Text("Route",fontFamily=worldFont(state.world),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    IconButton(onClick=onSettings,modifier=Modifier.semantics { contentDescription="Settings" }) {
                        Icon(Icons.Default.Tune,null)
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(if(compactLandscape)8.dp else 12.dp)) {
            if(!compactLandscape) TransportModes(state.mode,onMode,state.world)
            if(mapError!=null)Notice(mapError,Icons.Default.CloudOff)
            if(compactLandscape) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(needsFix) FixLabel(state,Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    ChromeButton(state.world,Icons.Default.MyLocation,"Use my location",onLocate)
                }
            } else {
                Surface(color=palette.panel,contentColor=palette.foreground,shape=hudShape(state.world)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        FixLabel(state)
                        TextButton(onClick=onLocate,modifier=Modifier.heightIn(min=48.dp)) {
                            Icon(Icons.Default.MyLocation,null,tint=palette.foreground)
                            Spacer(Modifier.width(8.dp))
                            Text("Use my location",color=palette.foreground)
                        }
                    }
                }
            }
            DestinationCard(state,onSave,onBack,onLocate,onRetry,onStart,onSteps,onSelectRoute,
                compact=compactLandscape,onQuest=onQuest,onShare=onShare,roomyActions=!compactLandscape)
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.CenterEnd) { Attribution(state.world) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DestinationActions(state: NavState,onSave: () -> Unit,
    onQuest: (() -> Unit)?,onShare: (() -> Unit)?,expanded: Boolean) {
    val palette=hudPalette(state.world)
    val saved=state.saved.any { it.id==state.selected?.id }
    var more by remember { mutableStateOf(false) }
    @Composable fun Save() {
        val saveIcon = if(saved) R.drawable.waypoint_bookmark_filled else R.drawable.waypoint_bookmark
        TextButton(onClick=onSave,contentPadding=if(expanded)PaddingValues(horizontal=12.dp,vertical=8.dp) else PaddingValues(2.dp),
            modifier=(if(expanded)Modifier else Modifier.width(64.dp)).heightIn(min=56.dp).semantics { contentDescription="Save place" }) {
            if(expanded) {
                Icon(painterResource(saveIcon),null,tint=palette.accent)
                Spacer(Modifier.width(8.dp));Text(if(saved)"Saved" else "Save",color=palette.accent)
            } else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(painterResource(saveIcon),null,tint=palette.accent)
                Text(if(saved)"Saved" else "Save",fontFamily=hudFont(state.world),fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,maxLines=1,softWrap=false,color=palette.accent)
            }
        }
    }
    @Composable fun More() {
        if(onQuest!=null || onShare!=null) Box {
            TextButton(onClick={more=true},contentPadding=if(expanded)PaddingValues(horizontal=12.dp,vertical=8.dp) else PaddingValues(2.dp),
                modifier=(if(expanded)Modifier else Modifier.width(48.dp)).heightIn(min=56.dp)) {
                if(expanded) {
                    Icon(Icons.Default.MoreVert,null,tint=palette.accent);Spacer(Modifier.width(8.dp));Text("More",color=palette.accent)
                } else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MoreVert,null,tint=palette.accent);Text("More",fontSize=12.sp,color=palette.accent)
                }
            }
            DropdownMenu(expanded=more,onDismissRequest={more=false}) {
                onQuest?.let { DropdownMenuItem(text={Text("Add a quest here")},onClick={more=false;it()},leadingIcon={Icon(Icons.Default.Assignment,null)}) }
                onShare?.let { DropdownMenuItem(text={Text("Open in another map app")},onClick={more=false;it()},leadingIcon={Icon(Icons.Default.OpenInNew,null)}) }
            }
        }
    }
    if(expanded) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Save();More() }
    else Row(verticalAlignment=Alignment.CenterVertically) { Save();More() }
}
