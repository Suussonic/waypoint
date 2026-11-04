package com.waypoint.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Original mobile arrangements derived from the map/HUD references in the sourcebook. */
@Composable internal fun BoxScope.DivisionAtlas(s: NavState, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = p.panel, border = hudBorder(s.world), shape = hudShape(s.world)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, null, tint = p.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp)); AtlasText("DARK ZONE", s.world, size = 26, color = p.accent)
                }
                AtlasText(s.city, s.world, size = 16)
                AtlasText(s.fix?.let { String.format(Locale.ROOT,"%.4f° / %.4f°", it.lat, it.lon) } ?: "Choose a destination to begin", s.world, size = 13, color = p.muted)
            }
        }
        AtlasFind(s.world, a)
    }
    Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FixLabel(s)
        Row { AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds); AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved); AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings) }
    }
    Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CompassRose(if (s.headingReliable) s.bearing else Float.NaN, s.world, onRecenter = a.locate)
        AtlasAction(s.world, "Locate", a.locate)
    }
}

@Composable internal fun BoxScope.SeaChartAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = p.panel, border = hudBorder(s.world)) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SEA CHART", fontFamily = worldFont(s.world), fontSize = 24.sp, color = p.foreground)
                Text(s.city, fontFamily = ReadingSerifFont, fontSize = 12.sp, color = p.muted)
                FixLabel(s)
            }
        }
        AtlasFind(s.world, a, label = "Where shall we go?")
    }
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompassRose(heading, s.world, ornate = true, onRecenter = a.locate)
            AtlasAction(s.world, "Locate", a.locate)
        }
        Surface(color = p.panel, border = hudBorder(s.world)) {
            Column(Modifier.padding(4.dp)) {
                AtlasAction(s.world, "Worlds", a.huds)
                AtlasAction(s.world, "Saved places", a.saved)
                AtlasAction(s.world, "Settings", a.settings)
            }
        }
    }
}

@Composable internal fun BoxScope.DosAtlas(s: NavState, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = p.panel, border = hudBorder(s.world)) {
            Column(Modifier.padding(12.dp)) {
                AtlasText("WAYPOINT / DOS ATLAS", s.world, size = 20)
                HorizontalDivider(color = p.accent, modifier = Modifier.padding(vertical = 8.dp))
                AtlasText(s.city, s.world, size = 14)
            }
        }
        AtlasFind(s.world, a, label = "Find a place")
    }
    Surface(color = p.panel, border = hudBorder(s.world), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(8.dp)) {
            FixLabel(s)
            Row { AtlasAction(s.world, "Worlds", a.huds, Modifier.weight(1f)); AtlasAction(s.world, "Saved", a.saved, Modifier.weight(1f)) }
            Row { AtlasAction(s.world, "Locate", a.locate, Modifier.weight(1f)); AtlasAction(s.world, "Settings", a.settings, Modifier.weight(1f)) }
        }
    }
}

@Composable internal fun BoxScope.AincradAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AINCRAD", color = p.foreground, fontFamily = SansFont, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.city, color = p.muted, fontFamily = SansFont, fontSize = 13.sp)
                    FixLabel(s)
                }
                Icon(Icons.Default.Explore, null, tint = p.accent, modifier = Modifier.size(32.dp))
            }
        }
        AtlasFind(s.world, a)
    }
    Surface(color = p.panel, shape = hudShape(s.world), border = hudBorder(s.world), modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
        Column(Modifier.padding(8.dp)) {
            AtlasAction(s.world, "Worlds", a.huds)
            AtlasAction(s.world, "Saved places", a.saved)
            AtlasAction(s.world, "Settings", a.settings)
        }
    }
    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CompassRose(heading, s.world, ornate = true, onRecenter = a.locate)
        AtlasAction(s.world, "Locate", a.locate)
    }
}

@Composable internal fun BoxScope.CounterStrikeAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CompassRose(heading, s.world, onRecenter = a.locate)
        Surface(color = p.panel, modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                AtlasText("TACTICAL RADAR", s.world, size = 18, color = p.accent)
                AtlasText(s.city, s.world, size = 13)
            }
        }
    }
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AtlasFind(s.world, a)
        Row(Modifier.fillMaxWidth().background(p.panel), horizontalArrangement = Arrangement.SpaceEvenly) {
            AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds)
            AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved)
            AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings)
            AtlasTool(s.world, Icons.Default.MyLocation, "Locate", a.locate)
        }
        FixLabel(s)
    }
}

@Composable internal fun BoxScope.DefconAtlas(s: NavState, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)) {
        Surface(color = p.panel, border = hudBorder(s.world)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AtlasText("VECTOR COMMAND", s.world, size = 22)
                AtlasText(s.city, s.world, size = 13, color = p.muted)
                HorizontalDivider(color = p.accent.copy(alpha = .6f))
                FixLabel(s)
            }
        }
    }
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AtlasFind(s.world, a, label = "Set a destination")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds)
            AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved)
            AtlasTool(s.world, Icons.Default.MyLocation, "Locate", a.locate)
            AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings)
        }
    }
}

@Composable internal fun BoxScope.FieldOpsAtlas(s: NavState, heading: Float, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = p.panel, shape = hudShape(s.world), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) { AtlasText("FIELD OPS", s.world, size = 26); AtlasText(s.city, s.world, size = 14, color = p.muted) }
            }
            Spacer(Modifier.width(12.dp)); CompassRose(heading, s.world, onRecenter = a.locate)
        }
        AtlasFind(s.world, a, label = "Choose your destination")
    }
    Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FixLabel(s)
        Row { AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds); AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved); AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings) }
    }
    AtlasTool(s.world, Icons.Default.MyLocation, "Locate", a.locate, Modifier.align(Alignment.BottomEnd).padding(12.dp))
}

@Composable internal fun BoxScope.RetroQuestAtlas(s: NavState, a: AtlasActions) {
    val p = hudPalette(s.world)
    Surface(color = p.panel, border = hudBorder(s.world), modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasText("16-BIT QUEST", s.world, size = 22)
            AtlasText(s.city, s.world, size = 12, color = p.muted)
            FixLabel(s)
            AtlasFind(s.world, a, label = "Find a place")
        }
    }
    Surface(color = p.panel, border = hudBorder(s.world), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row { AtlasAction(s.world, "Worlds", a.huds, Modifier.weight(1f)); AtlasAction(s.world, "Saved", a.saved, Modifier.weight(1f)) }
            Row { AtlasAction(s.world, "Locate", a.locate, Modifier.weight(1f)); AtlasAction(s.world, "Settings", a.settings, Modifier.weight(1f)) }
        }
    }
}

@Composable internal fun BoxScope.HomeConsoleAtlas(s: NavState, a: AtlasActions) {
    val p = hudPalette(s.world)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = p.panel, shape = hudShape(s.world)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Where next?", fontFamily = SansFont, fontSize = 24.sp, color = p.foreground, fontWeight = FontWeight.SemiBold)
                Text(s.city, fontFamily = SansFont, fontSize = 14.sp, color = p.muted)
            }
        }
        AtlasFind(s.world, a)
    }
    Surface(color = p.panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AtlasTool(s.world, Icons.Default.Layers, "Worlds", a.huds)
                AtlasTool(s.world, Icons.Default.BookmarkBorder, "Saved", a.saved)
                AtlasTool(s.world, Icons.Default.Tune, "Settings", a.settings)
                AtlasTool(s.world, Icons.Default.MyLocation, "Locate", a.locate)
            }
            FixLabel(s)
        }
    }
}
