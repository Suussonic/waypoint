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
import androidx.compose.ui.unit.dp

/** Original machine-atlas chrome: quiet paper tones, square controls and score-like rules. */
@Composable internal fun BoxScope.NierAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Map, null, tint = p.foreground, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AtlasText(s.world.title, s.world, size = 22, color = p.foreground)
                        AtlasText(s.city, s.world, size = 15, color = p.muted)
                    }
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                    repeat(3) { i ->
                        val y = (2 + i * 3).dp.toPx()
                        drawLine(p.muted.copy(alpha = .35f), Offset(0f, y), Offset(size.width, y), .5.dp.toPx())
                    }
                    drawCircle(p.foreground, 2.5.dp.toPx(), Offset(6.dp.toPx(), 5.dp.toPx()))
                }
            }
        }
        AtlasFind(s.world, a, label = "Find a place")
    }
    Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world)) {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { FixLabel(s) }
        }
        Row {
            AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds)
            AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved)
            AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings)
        }
    }
    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CompassRose(heading, s.world, onRecenter = a.locate)
        AtlasAction(s.world, "Locate", a.locate)
    }
}
