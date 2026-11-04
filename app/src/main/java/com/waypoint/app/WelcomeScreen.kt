package com.waypoint.app

import androidx.compose.ui.res.stringResource

import android.animation.ValueAnimator
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs

@Composable
fun WelcomeScreen(
    navigationState: NavState,
    world: World,
    savedFavorites: List<World>,
    hasLocationPermission: Boolean,
    permissionDenied: Boolean,
    onComplete: (World, List<World>) -> Unit,
    onRequestLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var favoriteNames by rememberSaveable(world) {
        mutableStateOf(savedFavorites.ifEmpty { listOf(world) }.joinToString(",") { it.name })
    }
    var selectedWorldName by rememberSaveable(world) { mutableStateOf(world.name) }
    val favoriteSet = remember(favoriteNames) { favoriteNames.split(',').filter(String::isNotBlank).toSet() }
    val selectedFavorites = remember(favoriteNames) {
        World.entries.filter { it.name in favoriteSet }.ifEmpty { listOf(world) }
    }
    val carouselWorlds = remember(favoriteNames) {
        val preferred = World.entries.filter { it.name in favoriteSet }
        (preferred + World.entries.filterNot { it.name in favoriteSet }).ifEmpty { World.entries }
    }
    val carouselIndex = carouselWorlds.indexOfFirst { it.name == selectedWorldName }.coerceAtLeast(0)
    val carouselWorld = carouselWorlds.getOrElse(carouselIndex) { world }

    fun complete() {
        val choice = World.entries.find { it.name == selectedWorldName } ?: world
        onComplete(choice, selectedFavorites)
    }
    fun moveWorld(direction: Int) {
        val currentIndex = carouselWorlds.indexOfFirst { it.name == selectedWorldName }.coerceAtLeast(0)
        val next = (currentIndex + direction + carouselWorlds.size) % carouselWorlds.size
        selectedWorldName = carouselWorlds[next].name
    }
    fun toggleFavorite(changed: World) {
        val next = favoriteSet.toMutableSet()
        if (!next.add(changed.name)) next.remove(changed.name)
        favoriteNames = World.entries.filter { it.name in next }.joinToString(",") { it.name }
    }

    BackHandler(enabled = step > 0) { step = (step - 1).coerceAtLeast(0) }
    LaunchedEffect(step, hasLocationPermission) {
        if (step == 5 && hasLocationPermission) complete()
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF2F65C5), Color(0xFF102C5D), Color(0xFF071326), Ink)),
        ).statusBarsPadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Explore, null, Modifier.size(24.dp), tint = Color(0xFF69D9FF))
            Text(
                "WAYPOINT",
                Modifier.weight(1f).padding(start = 9.dp),
                fontFamily = DisplayFont,
                fontSize = 23.sp,
                color = Cream,
                letterSpacing = 1.4.sp,
            )
            TextButton(
                onClick = { complete() },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Skip setup", color = Muted) }
        }
        LinearProgressIndicator(
            progress = { (step + 1) / 6f },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 20.dp),
            color = Color(0xFF168BFF),
            trackColor = Color.White.copy(alpha=.15f),
        )
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 16 }) togetherWith fadeOut(tween(160))
            },
            label = "First run steps",
        ) { visibleStep ->
            when (visibleStep) {
                0, 1, 2 -> StoryStep(
                    step = visibleStep,
                    world = carouselWorld,
                    onNext = { step = if (visibleStep == 2) 3 else visibleStep + 1 },
                    onBack = { step = (visibleStep - 1).coerceAtLeast(0) },
                )
                3 -> FavoriteWorldPicker(
                    selectedNames = favoriteSet,
                    onToggle = ::toggleFavorite,
                    onContinue = { step = 4 },
                )
                4 -> WorldCarouselStep(
                    navigationState = navigationState,
                    worlds = carouselWorlds,
                    world = carouselWorld,
                    index = carouselIndex,
                    onMove = ::moveWorld,
                    onUseWorld = { step = 5 },
                )
                else -> LocationStep(
                    world = carouselWorld,
                    permissionDenied = permissionDenied,
                    hasPermission = hasLocationPermission,
                    onAllow = onRequestLocation,
                    onOpenSettings = onOpenAppSettings,
                    onContinue = ::complete,
                )
            }
        }
    }
}

@Composable
private fun StoryStep(step: Int, world: World, onNext: () -> Unit, onBack: () -> Unit) {
    val story = when (step) {
        0 -> "The worlds you love,\nright on the road." to
            stringResource(R.string.welcome_worlds_description)
        1 -> "Find the style\nthat feels like yours." to
            "Every world brings its own cartography, typography and navigation display."
        else -> "Go your own way." to
            "Choose a car, motorcycle or walking route, then follow spoken turn-by-turn guidance."
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF071326)),
        ) {
            if (step == 0) {
                IntroVideo(world, Modifier.fillMaxSize())
            } else {
                WorldArtworkBackdrop(world, Modifier.fillMaxSize())
                RoutePreviewOverlay(world, Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(story.first, fontFamily = DisplayFont, fontSize = 34.sp, lineHeight = 36.sp, color = Cream)
            Text(story.second, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
            if (step == 2) TravelModePreview(world)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Back")
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 3.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF147BFF), contentColor = Color.White),
            ) {
                Text(if (step == 2) "Choose your worlds" else "Continue", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun TravelModePreview(world: World) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "Car" to Icons.Default.DirectionsCar,
            "Motorcycle" to Icons.Default.TwoWheeler,
            "Walk" to Icons.AutoMirrored.Filled.DirectionsWalk,
        ).forEach { (label, icon) ->
            Surface(
                color = Panel,
                contentColor = Cream,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(icon, null, Modifier.size(18.dp), tint = world.accent)
                    Spacer(Modifier.width(5.dp))
                    Text(label, fontSize = 11.sp, maxLines = 1, color = Cream)
                }
            }
        }
    }
}

@Composable
private fun FavoriteWorldPicker(selectedNames: Set<String>, onToggle: (World) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Which worlds are yours?", fontFamily = DisplayFont, fontSize = 34.sp, lineHeight = 37.sp, color = Cream)
        Text(stringResource(R.string.favorites_hint), color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Text("${selectedNames.size} selected", color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(World.entries, key = { it.name }) { item ->
                FavoriteWorldTile(item, item.name in selectedNames) { onToggle(item) }
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 8.dp),
            shape = CircleShape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF147BFF), contentColor = Color.White),
        ) {
            Text("Choose a map", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(19.dp))
        }
    }
}

@Composable
private fun FavoriteWorldTile(world: World, selected: Boolean, onClick: () -> Unit) {
    val accent = world.accent
    Box(
        Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(17.dp))
            .semantics(mergeDescendants = true) { contentDescription = "${world.title}, ${if (selected) "selected" else "not selected"}" }
            .clickable(role = Role.Checkbox, onClick = onClick),
    ) {
        Image(painterResource(artworkFor(world)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.05f), 0.38f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.86f))))
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, "Selected", Modifier.size(17.dp), tint = Ink)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(world.title, fontFamily = worldFont(world), fontSize = 17.sp, lineHeight = 20.sp, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(world.inspiration(), fontSize = 11.sp, lineHeight = 14.sp, color = Cream.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) {
            Box(Modifier.fillMaxSize().drawWithContent {
                drawContent()
                drawRoundRect(color = accent.copy(alpha = 0.88f), style = Stroke(width = 2.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()))
            })
        }
    }
}

@Composable
private fun WorldCarouselStep(
    navigationState: NavState,
    worlds: List<World>,
    world: World,
    index: Int,
    onMove: (Int) -> Unit,
    onUseWorld: () -> Unit,
) {
    var mapError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select map to", fontFamily = DisplayFont, fontSize = 27.sp, lineHeight = 30.sp, color = Cream,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("start", fontFamily = DisplayFont, fontSize = 40.sp, lineHeight = 43.sp, color = Cream,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .semantics { contentDescription = "${world.title} themed map preview" }
                .pointerInput(world, worlds.size) {
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                        onDragEnd = { if (abs(drag) > 54f) onMove(if (drag < 0f) 1 else -1); drag = 0f },
                        onDragCancel = { drag = 0f },
                    )
                },
        ) {
            val previewState = navigationState.copy(world = world, selected = null, route = null, navigating = false, arrived = false, fix = null)
            WorldMap(previewState, {}, { mapError = it }, Modifier.fillMaxSize(), active = true, cameraZoomOverride = 2.8, cameraTiltOverride = 48.0)
            RoutePreviewOverlay(world, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .05f), .45f to Color.Transparent, .78f to Color.Black.copy(alpha = .16f), 1f to Color.Black.copy(alpha = .48f))))
            Surface(
                color = Color(0xD9141B23),
                contentColor = Cream,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(19.dp), tint = world.accent)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("KOLKATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cream, letterSpacing = 1.sp)
                        Text("${world.title} · map preview", fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (mapError != null) {
                Surface(color = Color(0xE5101518), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.Center).padding(18.dp)) {
                    Text("Map preview needs an internet connection", Modifier.padding(14.dp), color = Cream, fontSize = 13.sp)
                }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(48.dp).background(Color(0xCC111923), CircleShape)) {
                    Icon(Icons.Default.ChevronLeft, "Previous world", tint = Cream)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(world.title, fontFamily = worldFont(world), fontSize = 24.sp, lineHeight = 28.sp, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(world.inspiration(), color = world.accent, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onMove(1) }, modifier = Modifier.size(48.dp).background(Color(0xCC111923), CircleShape)) {
                    Icon(Icons.Default.ChevronRight, "Next world", tint = Cream)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.semantics { contentDescription = "World ${index + 1} of ${worlds.size}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                worlds.forEachIndexed { dotIndex, _ ->
                    Box(Modifier.width(7.dp).height(4.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(if (dotIndex == index) 10.dp else 4.dp).height(4.dp)
                            .clip(CircleShape)
                            .background(if (dotIndex == index) Color(0xFF56CFFF) else Color.White.copy(alpha = .48f)))
                    }
                }
            }
            Text("${index + 1} / ${worlds.size}", Modifier.padding(start = 8.dp), color = Cream.copy(alpha=.8f), fontSize = 11.sp)
        }
        Button(
            onClick = onUseWorld,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(bottom = 4.dp),
            shape = CircleShape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 3.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF147BFF), contentColor = Color.White),
        ) { Text("Continue", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(19.dp)) }
    }
}

@Composable
private fun LocationStep(
    world: World,
    permissionDenied: Boolean,
    hasPermission: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(25.dp))) {
            WorldArtworkBackdrop(world, Modifier.fillMaxSize())
            RoutePreviewOverlay(world, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .06f), .5f to Color.Black.copy(alpha = .2f), 1f to Ink.copy(alpha = .97f))))
            Icon(Icons.Default.LocationOn, null, Modifier.align(Alignment.Center).size(72.dp), tint = world.accent)
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Your location, when you need it.", fontFamily = DisplayFont, fontSize = 34.sp, lineHeight = 37.sp, color = Cream)
            Text("Location shows where you are and lets Waypoint build a route from your current position. You can still explore the maps without it.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
            if (permissionDenied && !hasPermission) {
                Text("Location is still off. You can enable it in Android settings, or continue without it.", color = world.accent, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (hasPermission) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = CircleShape, elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 3.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF147BFF), contentColor = Color.White)) {
                Text("Open your map", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(19.dp))
            }
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = CircleShape, elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 3.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF147BFF), contentColor = Color.White)) {
                Text("Allow location", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.LocationOn, null, Modifier.size(19.dp))
            }
            if (permissionDenied) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open Android settings", color = Cream) }
            }
            TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continue without location", color = Muted) }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun IntroVideo(world: World, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    if (!ValueAnimator.areAnimatorsEnabled()) {
        WorldArtworkBackdrop(world, modifier)
    } else {
        var player by remember { mutableStateOf<VideoView?>(null) }
        Box(modifier.background(Ink)) {
            WorldArtworkBackdrop(world, Modifier.fillMaxSize())
            AndroidView(
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setVideoURI(Uri.parse("android.resource://${viewContext.packageName}/${R.raw.waypoint_intro}"))
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            mediaPlayer.setVolume(0f, 0f)
                            alpha = 1f
                            start()
                        }
                        setOnErrorListener { _, _, _ -> this@apply.alpha = 0f; true }
                    }.also { player = it }
                },
                update = { view -> if (player !== view) player = view },
                modifier = Modifier.fillMaxSize(),
            )
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                val videoView = player ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_STOP -> videoView.pause()
                    Lifecycle.Event.ON_START -> if (!videoView.isPlaying) videoView.start()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                player?.stopPlayback()
            }
        }
    }
}

@Composable
private fun WorldArtworkBackdrop(world: World, modifier: Modifier = Modifier) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val transition = rememberInfiniteTransition(label = "Artwork drift")
    val zoom by transition.animateFloat(
        initialValue = 1.025f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Artwork zoom",
    )
    val drift by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Artwork drift amount",
    )
    val density = LocalDensity.current
    Image(
        painter = painterResource(artworkFor(world)),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            val scale = if (motionEnabled) zoom else 1f
            scaleX = scale
            scaleY = scale
            translationX = if (motionEnabled) with(density) { drift.dp.toPx() } else 0f
        },
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun RoutePreviewOverlay(world: World, modifier: Modifier = Modifier) {
    val routeStyle = world.routeStyle()
    val pixelRoute = routeStyle == WorldRouteStyle.PIXEL
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val geometry = routeGeometry(world, width, height)
        val route = geometry.path
        val base = Color(android.graphics.Color.parseColor(world.routeColor))
        val cap = if (pixelRoute) StrokeCap.Square else StrokeCap.Round
        drawPath(route, base.copy(alpha = .22f), style = Stroke(width = 18.dp.toPx(), cap = cap))
        drawPath(route, base.copy(alpha = .70f), style = Stroke(width = 8.dp.toPx(), cap = cap))
        drawPath(route, world.accent.copy(alpha = .96f), style = Stroke(width = 3.dp.toPx(), cap = cap))

        val start = geometry.stops.first()
        val destination = geometry.stops.last()
        drawCircle(world.accent.copy(alpha = .16f), radius = 24.dp.toPx(), center = destination)
        drawCircle(base.copy(alpha = .28f), radius = 13.dp.toPx(), center = destination)
        drawCircle(world.accent, radius = 6.dp.toPx(), center = destination)
        drawCircle(Ink, radius = 2.dp.toPx(), center = destination)
        drawCircle(Cream, radius = 7.dp.toPx(), center = start)
        drawCircle(Ink, radius = 3.dp.toPx(), center = start)
    }
}
