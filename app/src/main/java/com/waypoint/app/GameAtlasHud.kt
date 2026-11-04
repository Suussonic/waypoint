package com.waypoint.app

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

/** Each atlas has its own composition. Shared primitives carry touch and accessibility behavior. */
data class AtlasActions(val search:()->Unit,val huds:()->Unit,val saved:()->Unit,val settings:()->Unit,
    val locate:()->Unit,val tilt:()->Unit,val voice:()->Unit,val nearby:(String)->Unit)

@Composable fun GameAtlasChrome(state:NavState,actions:AtlasActions,modifier:Modifier=Modifier) {
    val heading=if(state.headingReliable)state.bearing else Float.NaN
    BoxWithConstraints(modifier.fillMaxSize()) {
        if(maxHeight<420.dp) CompactAtlas(state,heading,actions)
        else when(state.world) {
            World.LOS_SANTOS->LosSantosAtlas(state,actions)
            World.OLD_WEST->WesternAtlas(state,heading,actions)
            World.NIGHT_CITY->NightCityAtlas(state,actions)
            World.PIXEL->PixelAtlas(state,actions)
            World.GREEN_TERMINAL->TerminalAtlas(state,actions)
            World.SAN_ANDREAS->SanAndreasAtlas(state,heading,actions)
            World.HEAT->HeatAtlas(state,actions)
            World.RIVIA->WitcherAtlas(state,heading,actions)
            World.VICE_COAST->ViceAtlas(state,heading,actions)
            World.LIBERTY->LibertyAtlas(state,actions)
            World.CTOS->NudleAtlas(state,actions)
            World.HYRULE->SheikahAtlas(state,heading,actions)
            World.SKYRIM->SkyrimAtlas(state,heading,actions)
            World.LANDS_BETWEEN->EldenAtlas(state,heading,actions)
            World.HORIZON->ForzaAtlas(state,actions)
            World.ASSASSIN->AnimusAtlas(state,heading,actions)
            World.FAR_CRY->FarCryAtlas(state,actions)
            World.GHOST->GhostAtlas(state,heading,actions)
            World.BRIDGES->BridgesAtlas(state,actions)
            World.HORIZON_WILDS->EmbraceAtlas(state,heading,actions)
            World.SPIDER->SpiderAtlas(state,actions)
            World.HOGWARTS->HogwartsAtlas(state,heading,actions)
            World.STARFIELD->StarfieldAtlas(state,heading,actions)
            World.KINGDOM->BohemiaAtlas(state,heading,actions)
            World.SAINTS->SaintsAtlas(state,actions)
            World.JUST_CAUSE->MediciAtlas(state,actions)
            World.MAFIA->MafiaAtlas(state,actions)
            World.DIVISION->DivisionAtlas(state,actions)
            World.SEA_CHART->SeaChartAtlas(state,heading,actions)
            World.DOS_ATLAS->DosAtlas(state,actions)
            World.AINCRAD->AincradAtlas(state,heading,actions)
            World.COUNTER_STRIKE->CounterStrikeAtlas(state,heading,actions)
            World.DEFCON->DefconAtlas(state,actions)
            World.FIELD_OPS->FieldOpsAtlas(state,heading,actions)
            World.RETRO_QUEST->RetroQuestAtlas(state,actions)
            World.NIER->NierAtlas(state,heading,actions)
            World.STALKER->StalkerAtlas(state,heading,actions)
            World.HOME_CONSOLE->HomeConsoleAtlas(state,actions)
        }
        if(maxHeight<420.dp || state.world !in setOf(World.LOS_SANTOS,World.NIGHT_CITY,World.HORIZON,World.HOGWARTS,World.BRIDGES,World.FAR_CRY)) {
            AtlasTool(state.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",actions.tilt,
                Modifier.align(Alignment.CenterEnd).padding(end=8.dp))
        }
    }
}

@Composable internal fun AtlasText(text:String,world:World,modifier:Modifier=Modifier,size:Int=16,color:Color=hudPalette(world).foreground,font:FontFamily=hudFont(world)) {
    Text(text,modifier,fontFamily=font,fontSize=size.sp,lineHeight=(size+4).sp,color=color,maxLines=1,overflow=TextOverflow.Ellipsis)
}
internal fun atlasToolCaption(label:String)=when(label) {
    "Set a destination"->"Search"
    "Use my location"->"Locate"
    "Toggle 2D and 3D"->"Map tilt"
    "Find fuel"->"Fuel"
    "Find food"->"Food"
    "Close route preview"->"Back"
    "Mute guidance"->"Mute"
    "Unmute guidance"->"Sound on"
    else->label
}
@Composable internal fun AtlasTool(world:World,icon:ImageVector,label:String,action:()->Unit,modifier:Modifier=Modifier,light:Boolean=false,round:Boolean=false) {
    val p=hudPalette(world)
    val background=if(light)p.foreground else p.panel
    val foreground=if(light)p.panel else p.foreground
    val shape=if(round)CircleShape else hudShape(world)
    Column(modifier.padding(4.dp).width(64.dp).heightIn(min=70.dp).clickable(role=Role.Button,onClick=action).semantics(mergeDescendants=true){contentDescription=label},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Surface(color=background,contentColor=foreground,shape=shape,modifier=Modifier.size(48.dp),border=if(round)BorderStroke(1.dp,foreground) else hudBorder(world)) {
            Box(contentAlignment=Alignment.Center){Icon(icon,null,Modifier.size(21.dp))}
        }
        Text(atlasToolCaption(label),fontFamily=PipBoyFont,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,color=p.foreground,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center,
            modifier=Modifier.fillMaxWidth().background(p.panel,hudShape(world)).padding(horizontal=2.dp,vertical=2.dp))
    }
}
@Composable internal fun AtlasAction(world:World,label:String,action:()->Unit,modifier:Modifier=Modifier,active:Boolean=false) {
    val p=hudPalette(world)
    Surface(onClick=action,color=if(active)p.foreground else p.panel,contentColor=if(active)p.panel else p.foreground,shape=hudShape(world),modifier=modifier.padding(4.dp).heightIn(min=48.dp)) {
        Box(Modifier.padding(horizontal=12.dp,vertical=10.dp),contentAlignment=Alignment.Center){AtlasText(label,world,size=15,color=if(active)p.panel else p.foreground)}
    }
}
@Composable internal fun AtlasTabs(world:World,a:AtlasActions,modifier:Modifier=Modifier,activeColor:Color=hudPalette(world).foreground) {
    val p=hudPalette(world)
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).background(p.panel),verticalAlignment=Alignment.CenterVertically) {
        listOf("MAP" to a.locate,"Worlds" to a.huds,"Saved" to a.saved).forEachIndexed { i,(t,fn)->
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min=56.dp).background(if(i==0)activeColor else Color.Transparent).clickable(onClick=fn).padding(horizontal=4.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
                AtlasText(t,world,size=14,color=if(i==0)p.panel else p.foreground)
            }
        }
        AtlasTool(world,Icons.Default.Tune,"Settings",a.settings)

    }
}
@Composable internal fun AtlasFind(world:World,a:AtlasActions,modifier:Modifier=Modifier,label:String="Search places") {
    val p=hudPalette(world)
    Surface(onClick=a.search,modifier=modifier.padding(4.dp).heightIn(min=48.dp).semantics{contentDescription="Set a destination"},color=p.panel,contentColor=p.foreground,shape=hudShape(world),border=hudBorder(world)) {
        Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Icon(Icons.Default.Search,null,Modifier.size(21.dp));Text(label,fontFamily=hudFont(world),fontSize=16.sp,lineHeight=21.sp,color=p.foreground,modifier=Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis);Icon(Icons.Default.ChevronRight,null,Modifier.size(18.dp))}
    }
}
@Composable internal fun FixLabel(s:NavState,modifier:Modifier=Modifier) {
    val p=hudPalette(s.world)
    Surface(color=p.panel,modifier=modifier) {Column(Modifier.padding(horizontal=10.dp,vertical=6.dp)) {
        AtlasText(if(s.gpsReady)"GPS ±${s.accuracy.roundToInt()} m · ${s.mode.label}" else s.gpsStatus,s.world,size=12,color=p.foreground)
        if(s.gpsReady && !s.headingReliable)AtlasText("Compass calibrating",s.world,size=11,color=p.muted)
    }}
}
@Composable internal fun PlaceLabel(s:NavState,modifier:Modifier=Modifier,serif:Boolean=false) {
    val p=hudPalette(s.world)
    Surface(color=p.panel,modifier=modifier) {AtlasText(s.city,s.world,Modifier.padding(horizontal=12.dp,vertical=7.dp),18,font=if(serif)AtlasFont else hudFont(s.world))}
}
@Composable internal fun CompassRose(bearing:Float,world:World,modifier:Modifier=Modifier,ornate:Boolean=false,onRecenter:()->Unit={}) {
    val p=hudPalette(world);val reliable=bearing.isFinite()
    val target=if(reliable)((bearing%360)+360)%360 else 0f
    var unwrapped by remember { mutableFloatStateOf(target) }
    LaunchedEffect(target) {unwrapped+=headingDelta(unwrapped%360,target)}
    val rotation by androidx.compose.animation.core.animateFloatAsState(unwrapped,androidx.compose.animation.core.tween(120),label="Compass heading")
    val description=if(reliable)stringResource(R.string.heading_recenter, target.roundToInt()) else stringResource(R.string.heading_unavailable_recenter)
    Box(modifier.size(if(ornate)100.dp else 72.dp).background(p.panel.copy(alpha=.96f),CircleShape)
        .clickable(role=Role.Button,onClick=onRecenter).semantics(mergeDescendants=true){contentDescription=description},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val r=size.minDimension/2
            drawCircle(p.foreground.copy(alpha=.65f),r,style=Stroke(1.dp.toPx()))
            // The fixed top index is the direction of travel. The north rose rotates beneath it.
            drawLine(p.foreground,Offset(center.x,0f),Offset(center.x,7.dp.toPx()),2.dp.toPx())
            if(reliable)rotate(-rotation) {
                for(i in 0 until 24)rotate(i*15f) {
                    drawLine(p.foreground.copy(alpha=if(i%6==0).85f else .35f),Offset(center.x,5.dp.toPx()),Offset(center.x,if(i%6==0)12.dp.toPx() else 8.dp.toPx()),1.dp.toPx())
                }
                val triangle=Path().apply{moveTo(center.x,3.dp.toPx());lineTo(center.x-4.dp.toPx(),15.dp.toPx());lineTo(center.x+4.dp.toPx(),15.dp.toPx());close()}
                drawPath(triangle,if(world in listOf(World.OLD_WEST,World.GHOST))Color(0xFFE0725E) else p.accent)
            }
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(if(reliable)listOf("N","NE","E","SE","S","SW","W","NW")[((target+22.5f)/45).toInt()%8] else "—",fontFamily=hudFont(world),fontSize=if(ornate)20.sp else 15.sp,lineHeight=if(ornate)24.sp else 18.sp,letterSpacing=0.sp,color=p.foreground)
            if(reliable)Text("${target.roundToInt()%360}°",fontFamily=hudFont(world),fontSize=11.sp,lineHeight=14.sp,letterSpacing=0.sp,color=p.muted)
        }
    }
}
@Composable internal fun AtlasFrame(world:World,modifier:Modifier=Modifier,inset:Float=8f) {
    val p=hudPalette(world)
    Canvas(modifier.fillMaxSize()) {
        val edge=inset.dp.toPx(); val c=p.accent.copy(alpha=.6f)
        if(world in listOf(World.HYRULE,World.HOGWARTS,World.KINGDOM,World.LANDS_BETWEEN)) {
            drawRect(c,Offset(edge,edge),Size(size.width-2*edge,size.height-2*edge),style=Stroke(1.dp.toPx()))
            for(x in listOf(edge,size.width-edge))for(y in listOf(edge,size.height-edge)){
                val sx=if(x<size.width/2)1 else -1;val sy=if(y<size.height/2)1 else -1
                drawLine(c,Offset(x,y+22*sy),Offset(x+22*sx,y),2.dp.toPx())
                drawCircle(c,3.dp.toPx(),Offset(x+8*sx,y+8*sy),style=Stroke(1.dp.toPx()))
            }
        } else if(world in listOf(World.NIGHT_CITY,World.SPIDER,World.BRIDGES,World.STARFIELD,World.JUST_CAUSE)) {
            val col=if(world==World.NIGHT_CITY || world==World.SPIDER)Color(0xFFD44B50) else c
            listOf(edge,size.height-edge).forEach {y->drawLine(col,Offset(edge,y),Offset(size.width-edge,y),1.dp.toPx())}
            for(x in listOf(edge,size.width-edge)) {drawLine(col,Offset(x,edge),Offset(x,edge+50.dp.toPx()),2.dp.toPx());drawLine(col,Offset(x,size.height-edge-50.dp.toPx()),Offset(x,size.height-edge),2.dp.toPx())}
            if(world==World.STARFIELD) for(x in 0..30)for(y in 0..60)drawCircle(c.copy(alpha=.14f),.7.dp.toPx(),Offset(x*14.dp.toPx(),y*14.dp.toPx()))
        }
    }
}

@Composable private fun BoxScope.LosSantosAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {AtlasText(s.city,s.world,Modifier.weight(1f),24);AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search)}
        Spacer(Modifier.height(6.dp));AtlasTabs(s.world,a)
    }
    Column(Modifier.align(Alignment.CenterEnd).padding(end=16.dp),horizontalAlignment=Alignment.End) {
        listOf("Food" to Icons.Default.Restaurant,"Fuel" to Icons.Default.LocalGasStation,"Parking" to Icons.Default.LocalParking).forEach{(t,i)->
            Surface(onClick={a.nearby(t)},color=Color(0xDD111111),modifier=Modifier.heightIn(min=48.dp)) {Row(Modifier.padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){Text(t,color=Color.White,fontSize=12.sp);Icon(i,null,tint=Color.White,modifier=Modifier.padding(start=12.dp).size(18.dp))}}
        }
        AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt,light=true)
    }
    Row(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).background(Color(0xDB111111)).padding(8.dp)) {FixLabel(s);HudStatusBars(s)}
        AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,light=true)
    }
}
@Composable private fun BoxScope.WesternAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(top=22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Surface(color=Color(0xEE171512),shape=CutCornerShape(5.dp)){AtlasText(s.city.uppercase(),s.world,Modifier.padding(horizontal=20.dp,vertical=10.dp).widthIn(max=260.dp),20,font=WesternFont)}
        Box(Modifier.width(120.dp).height(2.dp).background(Color(0xFFCCB990)))
    }
    AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search,Modifier.align(Alignment.TopEnd).padding(top=88.dp,end=18.dp),round=true)
    Column(Modifier.align(Alignment.BottomStart).padding(start=18.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,round=true);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved,round=true);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds,round=true)}
        Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(12.dp)){CompassRose(b,s.world,ornate=true,onRecenter=a.locate);Column {FixLabel(s);AtlasAction(s.world,"Settings",a.settings)}}
    }
}
@Composable private fun BoxScope.NightCityAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){AtlasText("MAP",s.world,Modifier.weight(1f),30,color=Color(0xFFFF656A));AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        HorizontalDivider(color=Color(0xFFCC3D49));AtlasText(s.city.uppercase(),s.world,Modifier.padding(top=6.dp),17,color=Color(0xFF87DAD9))
    }
    Column(Modifier.align(Alignment.CenterEnd).padding(end=18.dp),horizontalAlignment=Alignment.End) {
        AtlasText("DESTINATIONS",s.world,size=12,color=Color(0xFFF2DA5D));Spacer(Modifier.height(7.dp))
        AtlasAction(s.world,"FIND LOCATION",a.search);AtlasAction(s.world,"SAVED PLACES",a.saved)
    }
    Column(Modifier.align(Alignment.BottomCenter).padding(18.dp)) {FixLabel(s);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AtlasAction(s.world,"Worlds",a.huds);AtlasAction(s.world,stringResource(R.string.recenter),a.locate);AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}}
}
@Composable private fun PixelSlot(label:String,res:Int,action:()->Unit) {
    Column(Modifier.width(64.dp).clickable(role=Role.Button,onClick=action).semantics(mergeDescendants=true){contentDescription=label},horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(Color(0xFF565653)).drawBehind {
            drawRect(Color(0xFFB5B5AC),size=Size(size.width,3.dp.toPx()));drawRect(Color(0xFFB5B5AC),size=Size(3.dp.toPx(),size.height))
            drawRect(Color(0xFF22231F),Offset(0f,size.height-3.dp.toPx()),Size(size.width,3.dp.toPx()));drawRect(Color(0xFF22231F),Offset(size.width-3.dp.toPx(),0f),Size(3.dp.toPx(),size.height))
        },contentAlignment=Alignment.Center){Image(ImageBitmap.imageResource(res),null,Modifier.size(30.dp),filterQuality=FilterQuality.None)}
        Text(atlasToolCaption(label),fontFamily=PipBoyFont,fontSize=12.sp,lineHeight=16.sp,letterSpacing=0.sp,color=Color.White,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.fillMaxWidth().background(Color(0xFF171B15)).padding(vertical=4.dp))
    }
}
@Composable private fun BoxScope.PixelAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopStart).padding(16.dp).background(Color(0xA0101510)).padding(8.dp)) {
        AtlasText(s.city,s.world,size=14);AtlasText(if(s.fix==null)"Map preview" else "${"%.4f".format(s.fix.lat)}, ${"%.4f".format(s.fix.lon)}",s.world,size=11)
    }
    Column(Modifier.align(Alignment.BottomCenter).padding(bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        FixLabel(s);Box(Modifier.width(260.dp).height(5.dp).background(Color(0xFF162212))) {Box(Modifier.fillMaxHeight().fillMaxWidth(if(s.gpsReady)1f else 0f).background(Color(0xFF8BD341)))}
        Row(Modifier.border(3.dp,Color(0xFF141613)),horizontalArrangement=Arrangement.spacedBy(2.dp)) {PixelSlot("Set a destination",R.drawable.pixel_search,a.search);PixelSlot("Use my location",R.drawable.pixel_locate,a.locate);PixelSlot("Worlds",R.drawable.pixel_huds,a.huds);PixelSlot("Saved",R.drawable.pixel_saved,a.saved);PixelSlot("Settings",R.drawable.pixel_settings,a.settings)}
    }
}
@Composable private fun BoxScope.TerminalAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(18.dp)) {
        AtlasTabs(s.world,a,activeColor=Color(0xFFA4F780));HorizontalDivider(color=Color(0xFF8BE56C),thickness=2.dp)
        Row(verticalAlignment=Alignment.CenterVertically){AtlasText("LOCAL MAP",s.world,Modifier.weight(1f),18);AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search)}
    }
    Column(Modifier.align(Alignment.BottomCenter).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){AtlasText(s.city.uppercase(),s.world,Modifier.weight(1f),18);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
        HorizontalDivider(color=Color(0xFF8BE56C),thickness=2.dp);FixLabel(s,Modifier.fillMaxWidth())
    }
}
@Composable private fun BoxScope.SanAndreasAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {AtlasText("Map",s.world,size=42,color=Color(0xFF1D241C),font=WesternFont);Row(verticalAlignment=Alignment.CenterVertically) {AtlasAction(s.world,"Worlds",a.huds);AtlasAction(s.world,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}}
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(18.dp),onRecenter=a.locate)
    Column(Modifier.align(Alignment.BottomCenter).padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {PlaceLabel(s);Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){AtlasFind(s.world,a,Modifier.weight(1f));AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,round=true)}}
}
@Composable private fun BoxScope.ViceAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {AtlasText("Map",s.world,size=44,color=Color(0xFF6B184E),font=ViceFont);PlaceLabel(s)}
    Column(Modifier.align(Alignment.TopEnd).padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    Row(Modifier.align(Alignment.BottomCenter).padding(18.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)) {CompassRose(b,s.world,onRecenter=a.locate);Column(Modifier.weight(1f)){AtlasFind(s.world,a);Row(verticalAlignment=Alignment.CenterVertically){AtlasAction(s.world,"Saved",a.saved);AtlasAction(s.world,"Locate",a.locate)}}}
}
@Composable private fun BoxScope.LibertyAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)) {AtlasTabs(s.world,a,activeColor=Color(0xFFE1E4E2));AtlasText(s.city,s.world,Modifier.padding(top=8.dp),22)}
    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){AtlasAction(s.world,"Set a destination",a.search);AtlasAction(s.world,"Center on player",a.locate);FixLabel(s)}
}
@Composable private fun BoxScope.NudleAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAF6)).padding(start=14.dp),verticalAlignment=Alignment.CenterVertically){Text("nudle",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Color(0xFF236AAD),modifier=Modifier.weight(1f));AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        AtlasFind(s.world,a,label="Search places")
    }
    Column(Modifier.align(Alignment.CenterStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){AtlasTool(s.world,Icons.Default.Restaurant,"Find food",{a.nearby("Food")},round=true);AtlasTool(s.world,Icons.Default.LocalGasStation,"Find fuel",{a.nearby("Fuel")},round=true);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved,round=true)}
    Row(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){PlaceLabel(s);FixLabel(s)};AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,light=true,round=true)}
}
@Composable private fun BoxScope.HeatAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopStart).padding(18.dp)){AtlasText("NIGHT / MAP",s.world,size=28,color=Color(0xFFEC72C8));AtlasText(s.city,s.world,size=16);Row(verticalAlignment=Alignment.CenterVertically){AtlasAction(s.world,"Worlds",a.huds);AtlasAction(s.world,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}}
    Column(Modifier.align(Alignment.BottomEnd).padding(18.dp),horizontalAlignment=Alignment.End){AtlasAction(s.world,"SET ROUTE",a.search,active=true);Spacer(Modifier.height(6.dp));Row(verticalAlignment=Alignment.CenterVertically){FixLabel(s);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}}
}
@Composable private fun BoxScope.ForzaAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)) {Row(Modifier.background(Color(0xFFF5F2F2)),verticalAlignment=Alignment.CenterVertically){AtlasAction(s.world,"WORLD MAP",a.search,Modifier.weight(1f),true);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)};PlaceLabel(s)}
    Column(Modifier.align(Alignment.CenterStart).padding(start=16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}
    Row(Modifier.align(Alignment.BottomCenter).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){AtlasFind(s.world,a,Modifier.weight(1f),"Find a place");AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,light=true)}
}
@Composable private fun BoxScope.CompactAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopStart).widthIn(max=310.dp).padding(12.dp)){AtlasTabs(s.world,a);PlaceLabel(s)}
    Row(Modifier.align(Alignment.BottomEnd).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
    FixLabel(s,Modifier.align(Alignment.BottomStart).padding(12.dp))
}
