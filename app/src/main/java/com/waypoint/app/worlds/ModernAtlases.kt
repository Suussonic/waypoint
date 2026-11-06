package com.waypoint.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*

@Composable internal fun BoxScope.FarCryAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xE416211A)))) {AtlasTabs(s.world,a,activeColor=atlasAccentColor(s.world,Color(0xFFE7E7DD)));AtlasText(s.city,s.world,Modifier.padding(16.dp),24)}
    Column(Modifier.align(Alignment.CenterEnd).padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){AtlasTool(s.world,Icons.Default.LocalGasStation,"Find fuel",{a.nearby("Fuel")});AtlasTool(s.world,Icons.Default.Restaurant,"Find food",{a.nearby("Food")});AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}
    Row(Modifier.align(Alignment.BottomCenter).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){AtlasFind(s.world,a,Modifier.weight(1f));AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,light=true)}
}
@Composable internal fun BoxScope.BridgesAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopStart).padding(20.dp)){AtlasText("Map / INFORMATION",s.world,size=24);AtlasText(s.city.uppercase(),s.world,size=15);FixLabel(s,Modifier.padding(top=5.dp))}
    Column(Modifier.align(Alignment.CenterStart).padding(start=20.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    Row(Modifier.align(Alignment.BottomEnd).padding(20.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){AtlasAction(s.world,"SET ROUTE",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate);AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}
}
@Composable internal fun BoxScope.SpiderAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xF21C3545)))) {
        Row(Modifier.fillMaxWidth().background(atlasAccentColor(s.world,Color(0xFFE24350))),verticalAlignment=Alignment.CenterVertically){AtlasText("MAP",s.world,Modifier.weight(1f).padding(start=18.dp),25,color=atlasInkColor(s.world,Color.White));AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        PlaceLabel(s,Modifier.padding(12.dp))
    }
    Column(Modifier.align(Alignment.CenterStart).padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved)}
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xF2183344))).padding(12.dp),verticalAlignment=Alignment.CenterVertically){AtlasText("CUSTOM WAYPOINT",s.world,Modifier.weight(1f),17);AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
}
@Composable internal fun BoxScope.StarfieldAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopStart).padding(20.dp).widthIn(max=260.dp)) {AtlasText("SURFACE MAP",s.world,size=22);HorizontalDivider(Modifier.padding(vertical=6.dp),color=atlasAccentColor(s.world,Color(0xFFD3DDE3)));AtlasText(s.city,s.world,size=18);FixLabel(s);Row{AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}}
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){CompassRose(b,s.world,onRecenter=a.locate);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasAction(s.world,"Saved",a.saved);AtlasAction(s.world,"Locate",a.locate)}}
}
@Composable internal fun BoxScope.SaintsAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xF3161820))).padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){AtlasText("CITY MAP",s.world,Modifier.weight(1f),28,color=atlasAccentColor(s.world,Color(0xFFD3C5F5)));AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)};HorizontalDivider(thickness=3.dp,color=atlasAccentColor(s.world,Color(0xFF8166AE)));PlaceLabel(s,Modifier.padding(top=6.dp))}
    Column(Modifier.align(Alignment.CenterStart).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){AtlasAction(s.world,"LOCATIONS",a.search);AtlasAction(s.world,"SAVED",a.saved)}
    Row(Modifier.align(Alignment.BottomCenter).background(atlasSurfaceColor(s.world,Color(0xEE20252E))).padding(12.dp),verticalAlignment=Alignment.CenterVertically){AtlasFind(s.world,a,Modifier.weight(1f),"Search places");AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
}
@Composable internal fun BoxScope.MediciAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)){AtlasTabs(s.world,a,activeColor=atlasAccentColor(s.world,Color(0xFFEAD36D)));AtlasText(s.city.uppercase(),s.world,Modifier.padding(top=14.dp),26)}
    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp),horizontalAlignment=Alignment.End){AtlasAction(s.world,"PLACE WAYPOINT",a.search);Spacer(Modifier.height(7.dp));Row(verticalAlignment=Alignment.CenterVertically){FixLabel(s);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}}
}
@Composable internal fun BoxScope.MafiaAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(atlasSurfaceColor(s.world,Color(0xEF23241F))).padding(horizontal=16.dp)){Row(verticalAlignment=Alignment.CenterVertically){AtlasText("MAP",s.world,Modifier.weight(1f),26,font=AtlasFont);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)};HorizontalDivider(color=atlasAccentColor(s.world,Color(0xFFB0A585)));PlaceLabel(s)}
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(atlasSurfaceColor(s.world,Color(0xEF23241F))).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasAction(s.world,"Saved",a.saved);AtlasAction(s.world,"Center map",a.locate)}
}
