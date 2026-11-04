package com.waypoint.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Original field-PDA chrome, informed by the classic game's map and task journal. */
@Composable internal fun BoxScope.StalkerAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    // The bezel is decorative. Controls stay native, labelled, and at least 48dp.
    Canvas(Modifier.fillMaxSize()) {
        val inset = 4.dp.toPx()
        val edge = Size(size.width - inset * 2, size.height - inset * 2)
        drawRect(Color(0xFF141A17), Offset(inset, inset), edge, style = Stroke(8.dp.toPx()))
        drawRect(Color(0xFF879080), Offset(inset, inset), edge, style = Stroke(1.dp.toPx()))
    }
    Column(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Map, null, tint = p.accent, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    AtlasText(s.world.title, s.world, size = 23, color = p.foreground)
                    AtlasText(s.city, s.world, size = 15, color = p.muted)
                }
            }
        }
        AtlasFind(s.world, a, label = "Find a place")
    }
    Column(Modifier.align(Alignment.BottomStart).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world)) {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { FixLabel(s) }
        }
        Row {
            AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds)
            AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved)
            AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings)
        }
    }
    Column(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        CompassRose(heading, s.world, onRecenter = a.locate)
        AtlasAction(s.world, "Locate", a.locate)
    }
}
