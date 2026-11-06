package com.waypoint.app

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*

@Composable internal fun BoxScope.WitcherAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xF2111210))).padding(horizontal=16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasText("WORLD MAP",s.world,Modifier.weight(1f),19,font=AtlasFont);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        HorizontalDivider(color=atlasAccentColor(s.world,Color(0xFF776A45)));PlaceLabel(s,serif=true)
    }
    Column(Modifier.align(Alignment.BottomStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        CompassRose(b,s.world,onRecenter=a.locate);AtlasAction(s.world,"Saved places",a.saved)
        AtlasFind(s.world,a,Modifier.width(256.dp),"Search places")
    }
    AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,Modifier.align(Alignment.BottomEnd).padding(16.dp),round=true)
}
@Composable private fun SheikahBand(modifier:Modifier=Modifier) {
    val c=atlasAccentColor(World.HYRULE,Color(0xFFB69A5E))
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val u=8.dp.toPx()
        for(i in 0..(size.width/u).toInt() step 3) {
            val x=i*u
            val p=Path().apply{moveTo(x,0f);lineTo(x,1.5f*u);lineTo(x+2*u,1.5f*u);lineTo(x+2*u,.5f*u);lineTo(x+u,.5f*u);lineTo(x+u,u)}
            drawPath(p,c,style=Stroke(1.dp.toPx()))
        }
    }
}
@Composable internal fun BoxScope.SheikahAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xF20B1C29)))) {
        SheikahBand();Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasText("Map",s.world,Modifier.weight(1f),24,font=AtlasFont,color=atlasAccentColor(s.world,Color(0xFF83DBFF)));AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    }
    Column(Modifier.align(Alignment.BottomCenter).background(atlasSurfaceColor(s.world,Color(0xF20B1C29)))) {
        Row(Modifier.padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasText(s.city,s.world,Modifier.weight(1f).padding(horizontal=8.dp),17);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
        SheikahBand()
    }
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(top=112.dp,end=20.dp),onRecenter=a.locate)
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun BoxScope.SkyrimAtlas(s:NavState,b:Float,a:AtlasActions) {
    Row(Modifier.align(Alignment.TopCenter).padding(top=20.dp).background(atlasSurfaceColor(s.world,Color(0xB0131A1D))).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
        AtlasText("LOCAL",s.world,size=15);Box(Modifier.padding(horizontal=20.dp).width(1.dp).height(22.dp).background(atlasAccentColor(s.world,Color.LightGray)));AtlasText("MAP",s.world,size=24)
    }
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(top=90.dp,end=18.dp),onRecenter=a.locate)
    Column(Modifier.align(Alignment.BottomCenter).background(atlasSurfaceColor(s.world,Color(0xEA12191E))).padding(horizontal=12.dp,vertical=8.dp)) {
        AtlasText(s.city.uppercase(),s.world,Modifier.padding(bottom=6.dp),19)
        HorizontalDivider(color=atlasAccentColor(s.world,Color(0xFF8C9599)))
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,maxItemsInEachRow=5){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    }
}
@Composable internal fun BoxScope.EldenAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopStart).padding(24.dp)) {AtlasText("Map",s.world,size=30,color=atlasInkColor(s.world,Color(0xFF302C20)),font=AtlasFont);Box(Modifier.padding(top=8.dp).width(92.dp).height(1.dp).background(atlasAccentColor(s.world,Color(0xFFCBB77B))));PlaceLabel(s,Modifier.padding(top=10.dp),serif=true)}
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(top=105.dp,end=24.dp),ornate=true,onRecenter=a.locate)
    Column(Modifier.align(Alignment.BottomCenter).background(atlasSurfaceColor(s.world,Color(0xED28271F))).padding(12.dp)) {
        AtlasFind(s.world,a,label="Search places")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,round=true);AtlasAction(s.world,"Worlds",a.huds);AtlasAction(s.world,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    }
}
@Composable internal fun BoxScope.AnimusAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xE0192428)))) {AtlasTabs(s.world,a,activeColor=atlasAccentColor(s.world,Color(0xFFE4C277)));HorizontalDivider(color=atlasAccentColor(s.world,Color(0xFFE4C277)));PlaceLabel(s,Modifier.padding(12.dp),serif=true)}
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(top=170.dp,end=18.dp),onRecenter=a.locate)
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xDD1A2327))).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {AtlasFind(s.world,a,Modifier.weight(1f));AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
}
@Composable internal fun BoxScope.GhostAtlas(s:NavState,b:Float,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter)) {
        AtlasTabs(s.world,a,activeColor=atlasAccentColor(s.world,Color(0xFFEDE8DA)));
        Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.Top){Column(Modifier.weight(1f)){AtlasText("MAP",s.world,size=28,color=atlasInkColor(s.world,Color(0xFF252921)));AtlasText(s.city,s.world,size=18,color=atlasInkColor(s.world,Color(0xFF252921)))};AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search)}
    }
    Row(Modifier.align(Alignment.BottomCenter).padding(18.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {CompassRose(b,s.world,ornate=true,onRecenter=a.locate);Column(horizontalAlignment=Alignment.End){AtlasAction(s.world,"GUIDING ROUTE",a.search);AtlasAction(s.world,stringResource(R.string.recenter),a.locate);FixLabel(s)}}
}
@Composable internal fun BoxScope.EmbraceAtlas(s:NavState,b:Float,a:AtlasActions) {
    Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xE5161916))).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasText("MAP",s.world,Modifier.weight(1f),23);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {PlaceLabel(s);Spacer(Modifier.height(8.dp));Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(10.dp)){CompassRose(b,s.world,onRecenter=a.locate);AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}}
}
@Composable internal fun BoxScope.HogwartsAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).padding(12.dp)) {
        Row(Modifier.fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xF21B2833))),verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasText("WORLD MAP",s.world,Modifier.weight(1f),18,font=AtlasFont);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        Box(Modifier.fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xED51353B))).padding(10.dp),contentAlignment=Alignment.Center){AtlasText(s.city,s.world,size=16,font=AtlasFont)}
    }
    CompassRose(b,s.world,Modifier.align(Alignment.TopEnd).padding(top=170.dp,end=24.dp),ornate=true,onRecenter=a.locate)
    Column(Modifier.align(Alignment.BottomCenter).padding(18.dp)) {AtlasFind(s.world,a);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AtlasAction(s.world,"Saved",a.saved);AtlasAction(s.world,"Locate",a.locate);AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}}
}
@Composable internal fun BoxScope.BohemiaAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world,inset=12f)
    Row(Modifier.align(Alignment.TopCenter).padding(24.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){AtlasText("Map",s.world,Modifier.weight(1f),30,color=atlasInkColor(s.world,Color(0xFF302C20)),font=AtlasFont);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    Column(Modifier.align(Alignment.BottomCenter).padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        CompassRose(b,s.world,ornate=true,onRecenter=a.locate)
        PlaceLabel(s,serif=true)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasAction(s.world,"Saved",a.saved,Modifier.weight(1f));AtlasAction(s.world,"Locate",a.locate,Modifier.weight(1f))}
    }
}
