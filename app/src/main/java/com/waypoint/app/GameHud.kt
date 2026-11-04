package com.waypoint.app

import androidx.compose.ui.res.stringResource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/** HUD treatments use navigation data only. No simulated game statistics. */
data class HudPalette(val panel:Color,val foreground:Color,val accent:Color,val muted:Color)
fun hudPalette(world:World)=when(world) {
    World.NIER->HudPalette(Color(0xF7D8D4BC),Color(0xFF35362E),Color(0xFF793D2B),Color(0xFF5D5C4D))
    World.STALKER->HudPalette(Color(0xF7212624),Color(0xFFE9E9DE),Color(0xFFE6C274),Color(0xFFB8BDB0))
    World.DIVISION->HudPalette(Color(0xF21A2B2D),Color(0xFFF2F4EA),Color(0xFFFFAE61),Color(0xFFB8CCC7))
    World.SEA_CHART->HudPalette(Color(0xF7E9DAB4),Color(0xFF302D24),Color(0xFF7C352C),Color(0xFF625642))
    World.DOS_ATLAS->HudPalette(Color(0xF91D160F),Color(0xFFFFDF9B),Color(0xFFFFCF80),Color(0xFFD2B58A))
    World.AINCRAD->HudPalette(Color(0xF7FCFAEE),Color(0xFF383B3B),Color(0xFF8E531A),Color(0xFF68614D))
    World.COUNTER_STRIKE->HudPalette(Color(0xF5282927),Color(0xFFF2F0E5),Color(0xFFFFD37F),Color(0xFFBFC3BB))
    World.DEFCON->HudPalette(Color(0xF5051026),Color(0xFFD9E9FF),Color(0xFF9AC8FF),Color(0xFFA5BBD6))
    World.FIELD_OPS->HudPalette(Color(0xF5252D25),Color(0xFFF0F2E8),Color(0xFFCDDCAC),Color(0xFFBCC7B0))
    World.RETRO_QUEST->HudPalette(Color(0xF532284A),Color(0xFFFFF1CF),Color(0xFFFFE0A0),Color(0xFFD6C5DF))
    World.HOME_CONSOLE->HudPalette(Color(0xF8F7FBFD),Color(0xFF203E4B),Color(0xFF12678C),Color(0xFF4A6472))
    World.ASSASSIN->HudPalette(Color(0xED20282A),Color(0xFFF4EDDB),Color(0xFFEBD28C),Color(0xFFCBC8B7))
    World.FAR_CRY->HudPalette(Color(0xEE17231F),Color(0xFFF5F4E8),Color(0xFFF4D57B),Color(0xFFC3CBBD))
    World.GHOST->HudPalette(Color(0xF2202520),Color(0xFFF2EEE1),Color(0xFFF2EEE1),Color(0xFFC5C9BD))
    World.BRIDGES->HudPalette(Color(0xEF142733),Color(0xFFC4F1EC),Color(0xFF9AE5DD),Color(0xFFA4C0C9))
    World.HORIZON_WILDS->HudPalette(Color(0xEF27251F),Color(0xFFF5ECD7),Color(0xFFF0DBAC),Color(0xFFCCC2AA))
    World.SPIDER->HudPalette(Color(0xF2152B3B),Color(0xFFD5F2F5),Color(0xFFFFB1AA),Color(0xFFA7CAD5))
    World.HOGWARTS->HudPalette(Color(0xF21C2734),Color(0xFFF1E4C9),Color(0xFFE5CE93),Color(0xFFC4BDAC))
    World.STARFIELD->HudPalette(Color(0xF2203144),Color(0xFFEFF2EE),Color(0xFFF0D3A1),Color(0xFFBDCBD1))
    World.KINGDOM->HudPalette(Color(0xF22C2820),Color(0xFFF0E2BD),Color(0xFFE6CF94),Color(0xFFCCBEA0))
    World.SAINTS->HudPalette(Color(0xF2221C32),Color(0xFFF2EAFA),Color(0xFFD0A6FB),Color(0xFFC5B7D3))
    World.JUST_CAUSE->HudPalette(Color(0xF2162F3E),Color(0xFFD4F1EF),Color(0xFF87ECDF),Color(0xFFB1CCD0))
    World.MAFIA->HudPalette(Color(0xF2232621),Color(0xFFF4E8CE),Color(0xFFE8D29A),Color(0xFFC9C6B1))

    World.VICE_COAST->HudPalette(Color(0xF51E1D29),Color(0xFFFFD3EA),Color(0xFFFFAFE1),Color(0xFFD2CDD4))
    World.LIBERTY->HudPalette(Color(0xF516181A),Color(0xFFF1F1EE),Color(0xFFFFD06D),Color(0xFFB9C0C2))
    World.CTOS->HudPalette(Color(0xF5F8F9F3),Color(0xFF18272D),Color(0xFF185D89),Color(0xFF42545B))
    World.HYRULE->HudPalette(Color(0xF50B1C29),Color(0xFF9DDBF8),Color(0xFFF0D18B),Color(0xFFAAC9D7))
    World.SKYRIM->HudPalette(Color(0xEE12191E),Color(0xFFE5EFF1),Color(0xFFECF3F4),Color(0xFFB8C9D0))
    World.LANDS_BETWEEN->HudPalette(Color(0xF52A2922),Color(0xFFF1E5C7),Color(0xFFE3CC8D),Color(0xFFCBC1A5))
    World.HORIZON->HudPalette(Color(0xF5FEFCFC),Color(0xFF2D2430),Color(0xFF9D0062),Color(0xFF625361))
    World.OLD_WEST,World.RIVIA->HudPalette(Color(0xF51D1A16),Color(0xFFF5EAD4),Color(0xFFF5EAD4),Color(0xFFCEC0A8))
    World.NIGHT_CITY->HudPalette(Color(0xF20D1720),Color(0xFF8FF6ED),Color(0xFFF5E95C),Color(0xFF9FBFC3))
    World.GREEN_TERMINAL->HudPalette(Color(0xF507180D),Color(0xFFA4F780),Color(0xFFA4F780),Color(0xFF91C77C))
    World.PIXEL->HudPalette(Color(0xF52B2B29),Color(0xFFF3F1E7),Color(0xFFACED75),Color(0xFFCECEBF))
    World.HEAT->HudPalette(Color(0xF2191427),Color(0xFFFFF0FB),Color(0xFF9CF5F7),Color(0xFFD7B6D4))
    else->HudPalette(Color(0xF2191B1B),Color(0xFFF5F5F0),Color(0xFFB6DB91),Color(0xFFC0C5BF))
}
fun hudShape(world:World):Shape=when(world) {
    World.NIER->RectangleShape
    World.STALKER->CutCornerShape(4.dp)
    World.HOME_CONSOLE->RoundedCornerShape(12.dp)
    World.AINCRAD->RoundedCornerShape(4.dp)
    World.DIVISION,World.FIELD_OPS->CutCornerShape(topEnd=8.dp,bottomStart=8.dp)
    World.NIGHT_CITY,World.HEAT,World.SPIDER->CutCornerShape(topStart=10.dp,bottomEnd=10.dp)
    World.CTOS,World.HORIZON->RoundedCornerShape(3.dp)
    World.OLD_WEST,World.RIVIA->RoundedCornerShape(3.dp)
    else->RectangleShape
}
fun hudBorder(world:World):BorderStroke?=when(world) {
    World.STALKER->BorderStroke(1.dp,Color(0xFF778071))
    World.DIVISION->BorderStroke(1.dp,Color(0xFF9E7651))
    World.SEA_CHART->BorderStroke(1.dp,Color(0xFF8C7956))
    World.AINCRAD->BorderStroke(1.dp,Color(0xFFBBA86F))
    World.DOS_ATLAS->BorderStroke(1.dp,Color(0xFFD2B58A))
    World.DEFCON->BorderStroke(1.dp,Color(0xFF4F80B4))
    World.RETRO_QUEST->BorderStroke(2.dp,Color(0xFFDDCBA0))
    World.HOGWARTS,World.KINGDOM,World.ASSASSIN->BorderStroke(1.dp,Color(0xFF9D8A5F))
    World.BRIDGES,World.STARFIELD->BorderStroke(1.dp,Color(0xFF698E9F))
    World.HYRULE->BorderStroke(1.dp,Color(0xFFA9935B))
    World.LANDS_BETWEEN->BorderStroke(1.dp,Color(0xFF93876A))
    World.SKYRIM->BorderStroke(1.dp,Color(0xFF829298))
    World.NIGHT_CITY->BorderStroke(1.dp,Color(0xFF965150))
    World.GREEN_TERMINAL->BorderStroke(1.dp,Color(0xFF6BAD59))
    World.PIXEL->BorderStroke(2.dp,Color(0xFF92968A))
    else->null
}
fun hudFont(world:World):FontFamily=world.readingTypeface().family

fun World.inspiration()=when(this) {
    World.NIER->"NieR:Automata"
    World.STALKER->"S.T.A.L.K.E.R.: Shadow of Chernobyl"
    World.DIVISION->"Tom Clancy’s The Division"
    World.SEA_CHART->"Sea of Thieves"
    World.DOS_ATLAS->"DOS / ASCII interfaces"
    World.AINCRAD->"Sword Art Online"
    World.COUNTER_STRIKE->"Counter-Strike"
    World.DEFCON->"DEFCON"
    World.FIELD_OPS->"Call of Duty: Modern Warfare"
    World.RETRO_QUEST->"16-bit console adventures"
    World.HOME_CONSOLE->"Wii U console interface"
    World.LOS_SANTOS->"Grand Theft Auto V"
    World.OLD_WEST->"Red Dead Redemption 2"
    World.NIGHT_CITY->"Cyberpunk 2077"
    World.PIXEL->"Minecraft"
    World.GREEN_TERMINAL->"Fallout · Pip-Boy"
    World.SAN_ANDREAS->"GTA: San Andreas"
    World.HEAT->"Need for Speed"
    World.RIVIA->"The Witcher 3"
    World.VICE_COAST->"GTA: Vice City"
    World.LIBERTY->"Grand Theft Auto IV"
    World.CTOS->"Watch Dogs 2"
    World.HYRULE->"Zelda: Breath of the Wild"
    World.SKYRIM->"The Elder Scrolls V"
    World.LANDS_BETWEEN->"Elden Ring"
    World.HORIZON->"Forza Horizon 5"
    World.ASSASSIN->"Assassin’s Creed Origins"
    World.FAR_CRY->"Far Cry 5"
    World.GHOST->"Ghost of Tsushima"
    World.BRIDGES->"Death Stranding"
    World.HORIZON_WILDS->"Horizon Zero Dawn"
    World.SPIDER->"Marvel’s Spider-Man"
    World.HOGWARTS->"Hogwarts Legacy"
    World.STARFIELD->"Starfield"
    World.KINGDOM->"Kingdom Come: Deliverance II"
    World.SAINTS->"Saints Row: The Third"
    World.JUST_CAUSE->"Just Cause 3"
    World.MAFIA->"Mafia: Definitive Edition"
}

@Composable fun HudHeader(state:NavState,onWorld:()->Unit,onSettings:()->Unit) {
    val world=state.world;val p=hudPalette(world)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(onClick=onWorld,modifier=Modifier.weight(1f),color=p.panel,contentColor=p.foreground,shape=hudShape(world),border=hudBorder(world)) {
            Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Image(painterResource(worldEmblemFor(world)),null,Modifier.size(36.dp),contentScale=ContentScale.Fit)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)) {
                    Text(if(world==World.VICE_COAST)"Vice Coast" else world.title,fontFamily=worldFont(world),fontSize=when {
                        world.title.length>=13 -> 20.sp
                        world==World.PIXEL -> 22.sp
                        worldFont(world) in listOf(WesternFont,AtlasFont) -> 21.sp
                        else -> 27.sp
                    },lineHeight=30.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(world.inspiration(),fontSize=11.sp,lineHeight=15.sp,color=if(world==World.NIGHT_CITY)Color(0xFFFF8B8B) else p.muted,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ExpandMore,"Change HUD theme",Modifier.size(22.dp),tint=p.accent)
            }
        }
        HudControl(world,Icons.Default.Tune,"Settings",onSettings)
    }
}

@Composable fun HudSearch(world:World,onSearch:()->Unit) {
    val p=hudPalette(world)
    Surface(onClick=onSearch,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),color=p.panel,contentColor=p.foreground,shape=hudShape(world),border=hudBorder(world)) {
        Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Search,null,Modifier.size(22.dp),tint=p.accent)
            Text(if(world==World.GREEN_TERMINAL)"SEARCH DESTINATION" else "Set a destination",fontFamily=if(world in listOf(World.GREEN_TERMINAL,World.PIXEL))PixelFont else FontFamily.Default,fontSize=if(world in listOf(World.GREEN_TERMINAL,World.PIXEL))21.sp else 15.sp,lineHeight=24.sp,modifier=Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward,null,Modifier.size(18.dp),tint=p.muted)
        }
    }
}

@Composable fun HudQuickPlaces(world:World,onSearch:(String)->Unit) {
    val p=hudPalette(world)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("Coffee" to Icons.Default.LocalCafe,"Fuel" to Icons.Default.LocalGasStation,"Parking" to Icons.Default.LocalParking).forEach{(label,icon)->
            Surface(onClick={onSearch(label)},color=p.panel,contentColor=p.foreground,shape=hudShape(world),border=hudBorder(world),modifier=Modifier.heightIn(min=48.dp)) {
                Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    Icon(icon,null,Modifier.size(17.dp));Text(label,fontSize=12.sp,lineHeight=16.sp)
                }
            }
        }
    }
}

@Composable fun HudControl(world:World,icon:ImageVector,label:String,onClick:()->Unit,active:Boolean=false) {
    val p=hudPalette(world)
    val western=world in listOf(World.OLD_WEST,World.RIVIA,World.LANDS_BETWEEN)
    val shape=if(western)CircleShape else hudShape(world)
    val border=if(western)BorderStroke(if(active)3.dp else 2.dp,p.foreground) else hudBorder(world)
    Surface(onClick=onClick,shape=shape,color=if(active && !western) p.accent else p.panel,
        contentColor=if(active && !western)if(world in listOf(World.CTOS,World.HORIZON))Color.White else Color(0xFF101A13) else p.foreground,border=border,
        modifier=Modifier.size(52.dp).semantics{contentDescription=label}) {
        Box(contentAlignment=Alignment.Center) { Icon(icon,null,Modifier.size(24.dp)) }
    }
}

@Composable fun HudLocationReadout(state:NavState,modifier:Modifier=Modifier) {
    val p=hudPalette(state.world)
    Column(modifier.padding(end=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(state.navigating) Surface(color=p.panel,contentColor=p.foreground,shape=if(state.world==World.OLD_WEST)CircleShape else hudShape(state.world),border=hudBorder(state.world)) {
            Row(Modifier.padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(state.gpsReady)(state.speed*if(state.imperial)2.23694 else 3.6).roundToInt().toString() else "—",fontFamily=hudFont(state.world),fontSize=36.sp,lineHeight=41.sp,fontWeight=FontWeight.Bold)
                Text(if(state.imperial)"MPH" else "KM/H",fontSize=10.sp,lineHeight=14.sp,modifier=Modifier.padding(bottom=8.dp))
            }
        }
        Surface(color=p.panel,contentColor=p.foreground,shape=hudShape(state.world)) {
            Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Icon(if(state.gpsReady)Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,null,Modifier.size(13.dp),tint=p.accent)
                Text(state.city,fontFamily=if(state.world==World.OLD_WEST)WesternFont else FontFamily.Default,fontSize=if(state.world==World.OLD_WEST)11.sp else 12.sp,lineHeight=16.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable fun HudIdlePanel(state:NavState,onLocate:()->Unit,onSearch:()->Unit) {
    val p=hudPalette(state.world)
    Surface(color=p.panel,contentColor=p.foreground,shape=hudShape(state.world),border=hudBorder(state.world),modifier=Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(start=14.dp,end=6.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(if(state.gpsReady)"READY TO NAVIGATE" else state.gpsStatus.uppercase(),fontFamily=hudFont(state.world),fontSize=18.sp,lineHeight=24.sp,color=p.accent,maxLines=2)
                    Text(when {state.gpsReady->"Accuracy ±${state.accuracy.roundToInt()} m · ${state.mode.label}";state.fix!=null->"Accuracy ±${state.accuracy.roundToInt()} m · Move near a window";else->"Use precise location for directions"},fontSize=11.sp,lineHeight=15.sp,color=p.muted)
                }
                IconButton(onClick=if(state.gpsReady)onSearch else onLocate) { Icon(if(state.gpsReady)Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.MyLocation,if(state.gpsReady)"Set a destination" else "Connect GPS",tint=p.accent) }
            }
            if(state.world in listOf(World.LOS_SANTOS,World.SAN_ANDREAS)) HudStatusBars(state)
            if(state.world==World.PIXEL) Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF101A0D))) { Box(Modifier.fillMaxWidth(if(state.gpsReady)1f else 0f).height(4.dp).background(p.accent)) }
        }
    }
}

@Composable internal fun HudStatusBars(state:NavState) {
    val progress=state.progress?.let{1f-(it.remaining/(state.route?.meters ?: 1.0)).toFloat()} ?: 0f
    Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        listOf(Triple("GPS",if(state.gpsReady)1f else 0f,Color(0xFF91BC77)),Triple("ROUTE",progress,Color(0xFF7EC5DE)),Triple("VOICE",if(state.voice)1f else 0f,Color(0xFFE5C76C))).forEach{(label,fraction,color)->
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(label,fontSize=9.sp,lineHeight=12.sp,color=Color(0xFFC0C5BF),letterSpacing=0.5.sp)
                Box(Modifier.fillMaxWidth().height(4.dp).background(color.copy(alpha=0.22f))) { Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(color)) }
            }
        }
    }
}

@Composable fun HudManeuver(state:NavState,onClick:()->Unit) {
    val p=hudPalette(state.world)
    val guidance=routeGuidance(state.route,state.progress)
    val distanceText=when { !state.gpsReady -> "Finding GPS"; state.routing -> "Rerouting…"; state.offRoute -> "Off route"; else -> formatDistance(state.progress?.toTurn ?: 0.0,state.imperial) }
    val instructionText=when { !state.gpsReady -> "Waiting for a fresh, accurate location"; state.routing -> "Updating the route from your position"; state.offRoute -> if(state.routeError!=null)"Route unavailable · check connection and keep moving" else "Checking your position for a new route"; else -> guidance.instruction }
    Surface(onClick=onClick,color=p.panel,contentColor=p.foreground,shape=hudShape(state.world),border=hudBorder(state.world),
        modifier=Modifier.fillMaxWidth().semantics {
            contentDescription="$distanceText. $instructionText. Show route steps"
            liveRegion=LiveRegionMode.Polite
        }) {
        Column {
            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Icon(if(guidance.continuingToArrival)Icons.Default.Straight else maneuverIcon(guidance.maneuver),null,Modifier.size(44.dp),tint=p.accent)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(distanceText,fontFamily=hudFont(state.world),fontWeight=FontWeight.Bold,fontSize=34.sp,lineHeight=39.sp,color=p.accent)
                    Text(instructionText,fontSize=16.sp,lineHeight=22.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ExpandMore,"Show route steps",Modifier.size(20.dp),tint=p.muted)
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(android.graphics.Color.parseColor(state.world.routeColor))))
        }
    }
}

@Composable fun HudRouteFooter(state:NavState,onSteps:()->Unit,onStop:()->Unit) {
    val p=hudPalette(state.world)
    val seconds=state.progress?.seconds ?: state.route?.seconds ?: 0.0
    Surface(color=p.panel,contentColor=p.foreground,shape=hudShape(state.world),border=hudBorder(state.world),modifier=Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                TextButton(onClick=onStop,contentPadding=PaddingValues(2.dp),modifier=Modifier.width(64.dp).heightIn(min=64.dp).semantics{contentDescription="End navigation"}) { Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Icon(Icons.Default.Close,null,tint=p.foreground);Text("End trip",color=p.foreground,fontFamily=PipBoyFont,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,maxLines=1,softWrap=false)} }
                Column(Modifier.weight(1f).clickable(role=Role.Button,onClick=onSteps).heightIn(min=48.dp),verticalArrangement=Arrangement.Center) {
                    Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("${ceil(seconds/60).toInt().coerceAtLeast(1)} min",fontFamily=hudFont(state.world),fontSize=29.sp,lineHeight=33.sp,color=p.accent)
                        Text(formatDistance(state.progress?.remaining ?: state.route?.meters ?: 0.0,state.imperial),fontSize=13.sp,lineHeight=18.sp,modifier=Modifier.padding(bottom=4.dp))
                    }
                    Text("${SimpleDateFormat("h:mm a",Locale.getDefault()).format(Date(System.currentTimeMillis()+(seconds*1000).toLong()))} arrival · ${if(state.gpsReady) (state.speed * if(state.imperial)2.23694 else 3.6).roundToInt().toString() else "—"} ${if(state.imperial)"mph" else "km/h"}",fontSize=12.sp,lineHeight=16.sp,color=p.muted)
                }
                TextButton(onClick=onSteps,contentPadding=PaddingValues(2.dp),modifier=Modifier.width(64.dp).heightIn(min=64.dp).semantics{contentDescription="Route steps"}) { Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Icon(Icons.AutoMirrored.Filled.FormatListBulleted,null,tint=p.foreground);Text("Steps",color=p.foreground,fontFamily=PipBoyFont,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,maxLines=1,softWrap=false)} }
            }
            if(state.world in listOf(World.LOS_SANTOS,World.SAN_ANDREAS)) HudStatusBars(state)
        }
    }
}

@Composable fun HudNavigationBar(world:World,selected:Int,onSelect:(Int)->Unit) {
    val p=hudPalette(world)
    Surface(color=p.panel,contentColor=p.foreground) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=8.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(if(world==World.PIXEL)5.dp else 0.dp)) {
            listOf("Map" to Icons.Default.Map,"Worlds" to Icons.Default.Layers,"Saved" to Icons.Default.BookmarkBorder,"Quests" to Icons.Default.Assignment).forEachIndexed { index,(label,icon)->
                val active=selected==index
                val frame=when {world==World.PIXEL->Modifier.border(if(active)2.dp else 1.dp,if(active)Cream else Color(0xFF76796D))
                    world==World.GREEN_TERMINAL && active->Modifier.border(1.dp,p.accent)
                    else->Modifier}
                Column(Modifier.weight(1f).then(frame).selectable(selected=active,role=Role.Tab,onClick={onSelect(index)}).heightIn(min=52.dp).padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Icon(icon,null,Modifier.size(21.dp),tint=if(active)p.accent else p.muted)
                    Text(if(world==World.GREEN_TERMINAL)label.uppercase() else label,fontFamily=FontFamily.Default,fontSize=12.sp,lineHeight=16.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal,color=if(active)p.accent else p.muted)
                    if(world !in listOf(World.PIXEL,World.GREEN_TERMINAL)) Box(Modifier.width(28.dp).height(2.dp).background(if(active)if(world==World.OLD_WEST)Color(0xFFD75749) else p.accent else Color.Transparent))
                }
            }
        }
    }
}

private object WorldPreviewBitmaps {
    private val cache=object:LruCache<Int,Bitmap>(32*1024*1024) {
        override fun sizeOf(key:Int,value:Bitmap)=value.byteCount
    }
    fun cached(id:Int):Bitmap?=synchronized(cache){cache.get(id)}
    fun load(context:Context,id:Int):Bitmap? {
        cached(id)?.let{return it}
        val decoded=BitmapFactory.decodeResource(context.resources,id) ?: return null
        return synchronized(cache) {
            cache.get(id)?.also{decoded.recycle()} ?: decoded.also{cache.put(id,it)}
        }
    }
}

@Composable internal fun WorldPreviewArtwork(world:World,modifier:Modifier) {
    val context=LocalContext.current.applicationContext
    val imageId=world.previewImage()
    var bitmap by remember(imageId){mutableStateOf(WorldPreviewBitmaps.cached(imageId))}
    var small by remember(imageId){mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)}
    LaunchedEffect(imageId) {
        if(bitmap==null) {
            // Even the tiny placeholder requires decompressing a PNG. Do that
            // off the UI thread so a cold catalogue fling doesn't block input.
            small=withContext(Dispatchers.IO) {
                val options=BitmapFactory.Options().apply{inSampleSize=8}
                BitmapFactory.decodeResource(context.resources,imageId,options)?.asImageBitmap()
            }
            bitmap=withContext(Dispatchers.IO){WorldPreviewBitmaps.load(context,imageId)}
        }
    }
    val full=remember(bitmap){bitmap?.asImageBitmap()}
    if(full!=null || small!=null) Image(full ?: requireNotNull(small),null,modifier,contentScale=ContentScale.Crop)
    else Box(modifier.background(world.ground))
}


fun modeIcon(mode:TravelMode)=when(mode){TravelMode.CAR->Icons.Default.DirectionsCar;TravelMode.MOTORCYCLE->Icons.Default.TwoWheeler;TravelMode.BICYCLE->Icons.Default.DirectionsBike;TravelMode.WALK->Icons.AutoMirrored.Filled.DirectionsWalk}
@Composable fun TransportModes(selected:TravelMode,onSelect:(TravelMode)->Unit,world:World) {
    val palette=hudPalette(world)
    val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().background(palette.panel).selectableGroup()) {
        val columns=if(maxWidth<360.dp || fontScale>1.3f)2 else 4
        Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
            TravelMode.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    row.forEach { mode ->
                        val active=selected==mode
                        Column(Modifier.weight(1f).selectable(selected=active,role=Role.RadioButton,onClick={onSelect(mode)}).heightIn(min=52.dp).padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                            Icon(modeIcon(mode),mode.label,Modifier.size(20.dp),tint=if(active)palette.accent else palette.muted)
                            Text(mode.label,fontSize=11.sp,lineHeight=15.sp,color=if(active)palette.accent else palette.muted,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                            Box(Modifier.width(24.dp).height(2.dp).background(if(active)palette.accent else Color.Transparent))
                        }
                    }
                }
            }
        }
    }
}

fun World.matchesGameQuery(query:String):Boolean {
    val aliases=when(this){
        World.LOS_SANTOS->"GTA GTA5 GTA V"
        World.OLD_WEST->"RDR RDR2 Red Dead II"
        World.NIGHT_CITY->"CP2077 Cyberpunk"
        World.PIXEL->"Minecraft MC"
        World.SAN_ANDREAS->"GTA SA"
        World.VICE_COAST->"GTA VC"
        World.LIBERTY->"GTA4 GTA IV"
        World.HYRULE->"BOTW TOTK Zelda"
        World.ASSASSIN->"AC Origins"
        World.HORIZON_WILDS->"HZD Horizon Zero Dawn"
        World.HORIZON->"FH5 Forza Horizon"
        World.KINGDOM->"KCD KCD2"
        World.HEAT->"NFS Heat Need for Speed"
        World.RIVIA->"The Continent Witcher 3 Wild Hunt"
        World.SAINTS->"SR3 Saints Row"
        World.DIVISION->"Tom Clancy The Division Dark Zone"
        World.SEA_CHART->"SoT Sea of Thieves Pirate Nautical"
        World.DOS_ATLAS->"MS DOS ASCII terminal console"
        World.AINCRAD->"SAO Sword Art Online Hollow Realization"
        World.COUNTER_STRIKE->"CS CSGO CS2 Counter Strike radar"
        World.DEFCON->"Defcon vector command"
        World.FIELD_OPS->"CoD Call of Duty Modern Warfare Warzone"
        World.RETRO_QUEST->"retro SNES 16 bit Zelda adventure"
        World.NIER->"Nier NieR Automata Yoko Taro YoRHa 2B 9S android pod machine"
        World.STALKER->"STALKER S.T.A.L.K.E.R. Shadow of Chernobyl Chornobyl Clear Sky Call of Pripyat PDA Zone"
        World.HOME_CONSOLE->"Wii WiiU Wii U Nintendo console"
        else->""
    }
    fun normalize(value:String)=value.filter{it.isLetterOrDigit()}.lowercase()
    val haystack=normalize("$title ${inspiration()} $aliases")
    return normalize(query).let{it.isBlank() || haystack.contains(it)}
}
