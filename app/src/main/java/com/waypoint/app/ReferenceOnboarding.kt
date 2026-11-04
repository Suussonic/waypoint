package com.waypoint.app

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import android.app.Activity
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs

private val introFont = FontFamily(Font(R.font.nunito))
private val introBlue = Color(0xFF3266C6)
private val introNavy = Color(0xFF061D50)
private val introBlack = Color(0xFF02040E)
private val introMuted = Color(0xFF9FA3B4)
private val introWhite = Color(0xFFFCFCFF)
private val featuredWorlds = listOf(
    World.LOS_SANTOS, World.OLD_WEST, World.GREEN_TERMINAL, World.PIXEL,
    World.NIGHT_CITY, World.SAN_ANDREAS, World.HEAT, World.RIVIA,
)

internal fun onboardingCarouselWorlds(favorites: List<World>): List<World> =
    (favorites + featuredWorlds + World.entries).distinct()

internal fun toggleFavoriteName(names: String, world: World): String {
    val next = names.split(',').filter(String::isNotBlank).distinct().toMutableList()
    if (!next.remove(world.name)) next.add(world.name)
    return next.joinToString(",")
}

internal data class GameChoice(val title: String, val world: World)
private data class ReferralChoice(val title: String, val icon: Int?, val color: Color)
private val referralChoices = listOf(
    ReferralChoice("Instagram", R.drawable.brand_instagram, Color(0xFFE4408A)),
    ReferralChoice("Twitter / X", R.drawable.brand_x, Color(0xFF14171A)),
    ReferralChoice("Reddit", R.drawable.brand_reddit, Color(0xFFFF4500)),
    ReferralChoice("YouTube", R.drawable.brand_youtube, Color(0xFFFF0033)),
    ReferralChoice("Google", R.drawable.brand_google, Color(0xFF4285F4)),
    ReferralChoice("GitHub", R.drawable.brand_github, Color(0xFF24292F)),
    ReferralChoice("Telegram", R.drawable.brand_telegram, Color(0xFF26A5E4)),
    ReferralChoice("Friend", null, Color(0xFFFFC84B)),
)
internal val gameChoices = listOf(
    GameChoice("Red Dead Redemption 2", World.OLD_WEST),
    GameChoice("Grand Theft Auto V", World.LOS_SANTOS),
    GameChoice("Minecraft", World.PIXEL),
    GameChoice("Grand Theft Auto San Andreas", World.SAN_ANDREAS),
    GameChoice("Cyberpunk 2077", World.NIGHT_CITY),
    GameChoice("Fallout", World.GREEN_TERMINAL),
    GameChoice("Need for Speed", World.HEAT),
    GameChoice("The Elder Scrolls V: Skyrim", World.SKYRIM),
    GameChoice("The Witcher 3: Wild Hunt", World.RIVIA),
    GameChoice("Elden Ring", World.LANDS_BETWEEN),
    GameChoice("Forza Horizon 5", World.HORIZON),
    GameChoice("The Legend of Zelda", World.HYRULE),
    GameChoice("Assassin's Creed", World.ASSASSIN),
    GameChoice("Watch Dogs", World.CTOS),
    GameChoice("Hogwarts Legacy", World.HOGWARTS),
    GameChoice("Grand Theft Auto Vice City", World.VICE_COAST),
    GameChoice("Grand Theft Auto IV", World.LIBERTY),
    GameChoice("Far Cry 5", World.FAR_CRY),
    GameChoice("Ghost of Tsushima", World.GHOST),
    GameChoice("Death Stranding", World.BRIDGES),
    GameChoice("Horizon Zero Dawn", World.HORIZON_WILDS),
    GameChoice("Marvel's Spider-Man", World.SPIDER),
    GameChoice("Starfield", World.STARFIELD),
    GameChoice("Kingdom Come: Deliverance II", World.KINGDOM),
    GameChoice("Saints Row: The Third", World.SAINTS),
    GameChoice("Just Cause 3", World.JUST_CAUSE),
    GameChoice("Mafia: Definitive Edition", World.MAFIA),
    GameChoice("The Division", World.DIVISION),
    GameChoice("Sea of Thieves", World.SEA_CHART),
    GameChoice("DOS / ASCII", World.DOS_ATLAS),
    GameChoice("Sword Art Online", World.AINCRAD),
    GameChoice("Counter-Strike", World.COUNTER_STRIKE),
    GameChoice("DEFCON", World.DEFCON),
    GameChoice("Call of Duty", World.FIELD_OPS),
    GameChoice("16-bit adventures", World.RETRO_QUEST),
    GameChoice("Wii U / console", World.HOME_CONSOLE),
    GameChoice("S.T.A.L.K.E.R.", World.STALKER),
    GameChoice("NieR:Automata", World.NIER),
)

/** First run follows the reference's cinematic pages, questions, and map carousel. */
@Composable
fun ReferenceOnboarding(
    initialWorld: World,
    initialFavorites: List<World>,
    hasLocationPermission: Boolean,
    permissionDenied: Boolean,
    onComplete: (World, List<World>) -> Unit,
    onRequestLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.hide(WindowInsetsCompat.Type.navigationBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.navigationBars()) }
    }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var source by rememberSaveable { mutableStateOf("") }
    var favoriteNames by rememberSaveable { mutableStateOf(initialFavorites.distinct().joinToString(",") { it.name }) }
    var selectedWorldName by rememberSaveable { mutableStateOf(initialWorld.name) }
    val favoriteWorlds = remember(favoriteNames) {
        favoriteNames.split(',').mapNotNull { name -> World.entries.find { it.name == name } }.distinct()
    }
    val favorites = remember(favoriteWorlds) { favoriteWorlds.map { it.name }.toSet() }
    val carouselWorlds = remember(favoriteWorlds) { onboardingCarouselWorlds(favoriteWorlds) }
    val selectedWorld = carouselWorlds.firstOrNull { it.name == selectedWorldName } ?: carouselWorlds.first()
    val selectedIndex = carouselWorlds.indexOf(selectedWorld).coerceAtLeast(0)

    fun complete() {
        onComplete(selectedWorld, favoriteWorlds)
    }
    fun move(direction: Int) {
        val currentIndex = carouselWorlds.indexOfFirst { it.name == selectedWorldName }.coerceAtLeast(0)
        selectedWorldName = carouselWorlds[(currentIndex + direction + carouselWorlds.size) % carouselWorlds.size].name
    }
    fun toggle(world: World) {
        // Read the latest saved state in the event handler. Multiple taps before
        // recomposition must not overwrite a choice using the old derived list.
        favoriteNames = toggleFavoriteName(favoriteNames, world)
    }

    BackHandler(step > 0) { step-- }
    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "Onboarding page",
    ) { page ->
        when (page) {
            in 0..2 -> IntroStoryPage(page) { step = page + 1 }
            3 -> ReferralPage(source, { source = it }) { step = 4 }
            4 -> FavoriteGamesPage(favorites, ::toggle) {
                selectedWorldName = favoriteNames.split(',').firstOrNull { it.isNotBlank() } ?: initialWorld.name
                step = 5
            }
            5 -> OnboardingMapCarousel(selectedWorld, selectedIndex, carouselWorlds.size, ::move) { step = 6 }
            else -> OnboardingLocationPage(
                world = selectedWorld,
                hasPermission = hasLocationPermission,
                permissionDenied = permissionDenied,
                onAllow = onRequestLocation,
                onSettings = onOpenAppSettings,
                onContinue = ::complete,
            )
        }
    }
}

private fun introBackground(): Brush = Brush.verticalGradient(
    0f to introBlue, .25f to Color(0xFF2454AE), .52f to introNavy,
    .74f to introBlack, 1f to Color(0xFF030722),
)

@Composable
private fun IntroStoryPage(
    page: Int,
    onContinue: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(introBackground())) {
        val heroHeight = maxHeight * .75f
        val titleScale = when {
            maxWidth < 350.dp -> .82f
            maxWidth < 390.dp -> .92f
            else -> 1f
        }
        when (page) {
            0 -> AnimatedGlobeHero(Modifier.fillMaxWidth().height(heroHeight))
            1 -> LoopingOnboardingVideo(R.raw.onboarding_icons_motion,
                Modifier.fillMaxWidth().height(heroHeight)) {
                IconsHero(Modifier.fillMaxSize())
            }
            2 -> PhoneHero(Modifier.fillMaxWidth().height(heroHeight))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Transparent, .50f to Color.Transparent,
            .70f to introBlack.copy(alpha = .55f), .76f to introBlack,
            1f to Color(0xFF030722),
        )))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 20.dp).padding(bottom = 119.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (page) {
                0 -> {
                    IntroHeading("Waypoint", (49 * titleScale).toInt())
                    IntroHeading("Real streets", (42 * titleScale).toInt())
                    IntroHeading("new worlds", (36 * titleScale).toInt())
                }
                1 -> {
                    IntroHeading("Map Icons of your", (34 * titleScale).toInt())
                    IntroHeading(stringResource(R.string.favorite_game), (47 * titleScale).toInt())
                }
                2 -> {
                    IntroHeading("Navigate with", (34 * titleScale).toInt())
                    IntroHeading("your phone", (49 * titleScale).toInt())
                }
            }
            Spacer(Modifier.height(18.dp))
            val description = when (page) {
                0 -> stringResource(R.string.intro_find_game)
                1 -> stringResource(R.string.intro_real_streets)
                else -> "Follow real routes on a game-inspired map with clear turn-by-turn voice guidance."
            }
            Text(description, color = introMuted, fontFamily = introFont, fontSize = 16.sp,
                lineHeight = 20.sp, textAlign = TextAlign.Center, maxLines = 3)
        }
        OnboardingAction("Continue", true, onContinue)
    }
}

@Composable
private fun IntroHeading(text: String, size: Int) {
    Text(text, color = introWhite, fontFamily = introFont, fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp, lineHeight = (size + 4).sp, textAlign = TextAlign.Center,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun BoxScope.OnboardingAction(
    title: String,
    blue: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    bottomPadding: Dp = 30.dp,
) {
    val shape = CircleShape
    val gradient = if (blue) Brush.verticalGradient(
        listOf(Color(0xFF66D9FF), Color(0xFF0794FF), Color(0xFF0060F3), Color(0xFF677CF9)),
    ) else Brush.verticalGradient(listOf(Color.White, Color(0xFFF2F2F2), Color(0xFFCBCBCB)))
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(horizontal = 24.dp).padding(bottom = bottomPadding)
            .height(55.dp).shadow(if (blue) 18.dp else 5.dp, shape)
            .background(if (enabled) gradient else Brush.verticalGradient(listOf(Color(0xFF254C78), Color(0xFF172E64))), shape)
            .border(2.dp, if (blue) Color(0xFFA8DCFF) else Color.White, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = if (blue) Color.White else Color.Black, fontFamily = introFont,
                fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Box(Modifier.size(19.dp).clip(RoundedCornerShape(5.dp))
                .background(if (blue) Color.White else Color.Black), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp),
                    tint = if (blue) Color(0xFF0875FF) else Color.White)
            }
        }
    }
}

@Composable
private fun GlobeHero(modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val diameter = maxWidth * 1.01f
        Box(Modifier.align(Alignment.TopCenter).offset(y = 92.dp).size(diameter)
            .shadow(14.dp, CircleShape).clip(CircleShape).background(Color(0xFF30B8E0))) {
            Image(painterResource(R.drawable.onboarding_globe), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(
                Color(0x1554D9FF), Color(0x552D87D6), Color(0xAA061C63),
            ))))
            Text("WAYPOINT", Modifier.align(Alignment.Center), color = Color.White,
                fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 51.sp,
                letterSpacing = 1.sp)
        }
        Sticker(R.drawable.onboarding_marker_food, 78.dp, maxWidth * .02f, 94.dp, -16f)
        Sticker(R.drawable.onboarding_marker_sights, 76.dp, maxWidth * .75f, 102.dp, 18f)
        Sticker(R.drawable.onboarding_marker_cash, 78.dp, maxWidth * .08f, 345.dp, -12f)
        Sticker(R.drawable.onboarding_marker_nature, 78.dp, maxWidth * .72f, 352.dp, 12f)
        Sticker(R.drawable.onboarding_marker_place, 72.dp, maxWidth * .41f, 26.dp, -8f)
    }
}

@Composable
private fun AnimatedGlobeHero(modifier: Modifier) {
    LoopingOnboardingVideo(R.raw.onboarding_globe_motion, modifier) {
        GlobeHero(Modifier.fillMaxSize())
    }
}

@Composable
private fun LoopingOnboardingVideo(rawId: Int, modifier: Modifier, startAtMs: Int = 0, fallback: @Composable () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var textureView by remember { mutableStateOf<TextureView?>(null) }
    var videoReady by remember { mutableStateOf(false) }
    Box(modifier) {
        if (!videoReady) fallback()
        if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
            AndroidView(
                factory = { TextureView(it).also { view -> textureView = view } },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (videoReady) 1f else .01f },
            )
        }
    }
    DisposableEffect(textureView, owner, rawId, startAtMs) {
        val view = textureView
        var player: MediaPlayer? = null
        var surface: Surface? = null
        var prepared = false
        val observer = LifecycleEventObserver { _, _ ->
            if (prepared) {
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player?.start()
                else player?.pause()
            }
        }
        owner.lifecycle.addObserver(observer)
        if (view != null) {
            view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    if (player != null) return
                    surface = Surface(texture)
                    runCatching {
                        MediaPlayer().apply {
                            setDataSource(context, Uri.parse("android.resource://${context.packageName}/$rawId"))
                            setSurface(surface)
                            isLooping = true
                            setOnPreparedListener {
                                prepared = true
                                if (startAtMs > 0) it.seekTo(startAtMs)
                                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) it.start()
                            }
                            setOnErrorListener { _, _, _ -> videoReady = false; true }
                            prepareAsync()
                        }
                    }.onSuccess { player = it }
                }
                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { videoReady = true }
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    player?.release()
                    player = null
                    prepared = false
                    surface?.release()
                    surface = null
                    return true
                }
            }
            if (view.isAvailable) {
                view.surfaceTexture?.let { view.surfaceTextureListener?.onSurfaceTextureAvailable(it, view.width, view.height) }
            }
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            view?.surfaceTextureListener = null
            player?.release()
            surface?.release()
        }
    }
}

@Composable
private fun IconsHero(modifier: Modifier) {
    val motion = rememberInfiniteTransition(label = "Icon collage motion")
    val drift by motion.animateFloat(0f, 6.283f,
        infiniteRepeatable(tween(7600), RepeatMode.Restart), label = "Sticker drift")
    val stickers = remember {
        listOf(
            IntroSticker(R.drawable.onboarding_marker_food, -.05f, 12.dp, 110.dp, -13f, 0f),
            IntroSticker(R.drawable.onboarding_marker_pub, .38f, -8.dp, 105.dp, 11f, 1.7f),
            IntroSticker(R.drawable.onboarding_marker_sights, .78f, 16.dp, 104.dp, 17f, 2.6f),
            IntroSticker(R.drawable.onboarding_marker_stay, -.04f, 123.dp, 104.dp, -21f, 1.1f),
            IntroSticker(R.drawable.onboarding_marker_parking, .38f, 120.dp, 108.dp, -7f, 3.6f),
            IntroSticker(R.drawable.onboarding_marker_cafe, .78f, 124.dp, 104.dp, 14f, 4.1f),
            IntroSticker(R.drawable.onboarding_marker_place, -.04f, 240.dp, 104.dp, -17f, 2.3f),
            IntroSticker(R.drawable.onboarding_marker_shop, .38f, 238.dp, 108.dp, 9f, 5.1f),
            IntroSticker(R.drawable.onboarding_marker_nature, .78f, 244.dp, 104.dp, 26f, .8f),
            IntroSticker(R.drawable.onboarding_marker_health, -.04f, 354.dp, 104.dp, -10f, 4.4f),
            IntroSticker(R.drawable.onboarding_marker_fuel, .38f, 354.dp, 108.dp, 12f, 1.9f),
            IntroSticker(R.drawable.onboarding_marker_cash, .78f, 354.dp, 104.dp, -8f, 3.1f),
        )
    }
    BoxWithConstraints(modifier.clipToBounds()) {
        stickers.forEach { sticker ->
            Box(Modifier.offset(x = maxWidth * sticker.x, y = sticker.y)
                .size(sticker.size).graphicsLayer {
                    val bob = if (android.animation.ValueAnimator.areAnimatorsEnabled())
                        kotlin.math.sin(drift + sticker.phase) else 0f
                    translationY = bob * 5.dp.toPx()
                    rotationZ = sticker.tilt + bob * 1.5f
                }) {
                Image(painterResource(sticker.image), null,
                    Modifier.fillMaxSize().offset(x = (-3).dp, y = 2.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xFFFF3151)))
                Image(painterResource(sticker.image), null,
                    Modifier.fillMaxSize().offset(x = 3.dp, y = (-2).dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xFF44E9F2)))
                Image(painterResource(sticker.image), null,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

private data class IntroSticker(val image: Int, val x: Float, val y: Dp,
                                val size: Dp, val tilt: Float, val phase: Float)

@Composable
private fun Sticker(image: Int, size: Dp, x: Dp, y: Dp, tilt: Float) {
    val motion = rememberInfiniteTransition(label = "Sticker motion")
    val bob by motion.animateFloat(-3f, 3f,
        infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "Sticker float")
    Image(painterResource(image), null,
        Modifier.offset(x, y).size(size).graphicsLayer {
            rotationZ = tilt
            translationY = if (android.animation.ValueAnimator.areAnimatorsEnabled()) bob.dp.toPx() else 0f
        }, contentScale = ContentScale.Fit)
}

@Composable
private fun PhoneHero(modifier: Modifier) {
    BoxWithConstraints(modifier) {
        HorsePhoneHero(Modifier.fillMaxSize())
        Sticker(R.drawable.onboarding_marker_parking, 66.dp, maxWidth * .02f, 70.dp, -10f)
        Sticker(R.drawable.onboarding_marker_sights, 68.dp, maxWidth * .78f, 162.dp, 9f)
    }
}

@Composable
private fun ReferralPage(selected: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(introBackground()).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 22.dp, bottom = 115.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Where did you hear\nabout us?", color = introWhite, fontFamily = introFont,
                fontWeight = FontWeight.ExtraBold, fontSize = 33.sp, lineHeight = 41.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                referralChoices.forEach { choice ->
                    val name = choice.title
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .background(Color(0x1FFFFFFF), CircleShape)
                        .border(2.dp, if (selected == name) Color.White else Color.White.copy(alpha = .09f), CircleShape)
                        .selectable(selected = selected == name, role = Role.RadioButton, onClick = { onSelect(name) })
                        .padding(horizontal = 23.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(29.dp).background(choice.color, CircleShape), contentAlignment = Alignment.Center) {
                            if (choice.icon == null) {
                                Icon(Icons.Default.Person, null, Modifier.size(18.dp), tint = Color.White)
                            } else {
                                Icon(painterResource(choice.icon), null, Modifier.size(19.dp), tint = Color.White)
                            }
                        }
                        Spacer(Modifier.size(17.dp))
                        Text(name, color = introWhite, fontFamily = introFont,
                            modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (selected == name) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = introWhite)
                    }
                }
            }
        }
        OnboardingAction("Continue", true, onContinue, selected.isNotBlank())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteGamesPage(selected: Set<String>, onToggle: (World) -> Unit, onContinue: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(introBackground()).statusBarsPadding()) {
        val headingSize = if (maxWidth < 360.dp) 31.sp else 34.sp
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 22.dp, bottom = 105.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.favorite_games_question), color = introWhite, fontFamily = introFont,
                fontWeight = FontWeight.ExtraBold, fontSize = headingSize,
                lineHeight = 42.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.choose_multiple_games), color = introWhite, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text(pluralStringResource(R.plurals.games_selected, selected.size, selected.size), color = introWhite, fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    gameChoices.forEach { choice ->
                        val chosen = choice.world.name in selected
                        val selectionDescription = stringResource(if (chosen) R.string.game_selected else R.string.game_not_selected)
                        Box(Modifier.heightIn(min = 60.dp).clip(CircleShape)
                            .background(if (chosen) Color(0xFF224D91) else Color(0x17FFFFFF), CircleShape)
                            .border(2.dp, if (chosen) Color.White else Color.White.copy(alpha = .09f), CircleShape)
                            .toggleable(value = chosen, role = Role.Checkbox, onValueChange = { onToggle(choice.world) })
                            .semantics { stateDescription = selectionDescription }
                            .padding(horizontal = 18.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Image(painterResource(worldEmblemFor(choice.world)), null,
                                    Modifier.size(28.dp), contentScale = ContentScale.Fit)
                                Text(choice.title, color = introWhite, fontFamily = introFont,
                                    modifier = Modifier.weight(1f, fill = false),
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (chosen) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = introWhite)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        OnboardingAction("Continue", true, onContinue, selected.isNotEmpty())
    }
}

private fun carouselName(world: World): String = when (world) {
    World.LOS_SANTOS -> "Los Santos"
    World.OLD_WEST -> "Saint Denis II"
    World.GREEN_TERMINAL -> "PipBoy"
    World.PIXEL -> "Pixel Map"
    World.NIGHT_CITY -> "Night City"
    World.SAN_ANDREAS -> "San Andreas"
    World.HEAT -> "Heat Racing"
    World.RIVIA -> "The Rivia III"
    else -> world.title
}

@Composable
private fun OnboardingMapCarousel(
    world: World,
    index: Int,
    worldCount: Int,
    onMove: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF020903))) {
        // The reference presents a close view of a sphere. Keep the surface
        // inside a viewport above the controls so its native layer cannot
        // cover the arrows or Continue button.
        val globeViewportHeight = (maxHeight * .56f).coerceAtMost(450.dp)
        Box(Modifier.align(Alignment.TopCenter).offset(y = 170.dp)
            .fillMaxWidth().height(globeViewportHeight).clipToBounds()) {
            OnboardingGlobe(world, Modifier.fillMaxSize(), closeUp = true)
            Box(Modifier.fillMaxSize().pointerInput(world) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount -> distance += amount; change.consume() },
                    onDragEnd = { if (abs(distance) > 55f) onMove(if (distance < 0) 1 else -1); distance = 0f },
                    onDragCancel = { distance = 0f },
                )
            })
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0xB8000702), .18f to Color.Transparent,
            .70f to Color.Transparent, 1f to Color(0xEB010501),
        )))
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select Map to", color = Color.White, fontFamily = introFont,
                fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
            Text("Start", color = Color.White, fontFamily = introFont,
                fontWeight = FontWeight.ExtraBold, fontSize = 39.sp)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 23.dp, vertical = 133.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CarouselArrow(Icons.AutoMirrored.Filled.ArrowBack, "Previous map") { onMove(-1) }
            Text(carouselName(world), Modifier.weight(1f).padding(horizontal = 6.dp),
                color = Color.White, fontFamily = worldFont(world), fontWeight = FontWeight.Bold,
                fontSize = 24.sp, maxLines = 1, textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis)
            CarouselArrow(Icons.AutoMirrored.Filled.ArrowForward, "Next map") { onMove(1) }
        }
        Text("${index + 1} / $worldCount",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 106.dp)
                .semantics { contentDescription = "Map ${index + 1} of $worldCount" },
            color = Color.White.copy(alpha = .8f), fontFamily = introFont,
            fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OnboardingAction("Continue", false, onContinue)
    }
}

@Composable
private fun CarouselArrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(49.dp)
        .background(Color(0xB52A2B2B), CircleShape)
        .border(1.dp, Color.White.copy(alpha = .13f), CircleShape)) {
        Icon(icon, label, Modifier.size(27.dp), tint = Color.White)
    }
}

@Composable
private fun OnboardingLocationPage(
    world: World,
    hasPermission: Boolean,
    permissionDenied: Boolean,
    onAllow: () -> Unit,
    onSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF020903))) {
        val diameter = maxWidth.coerceAtMost(500.dp)
        Box(Modifier.align(Alignment.TopCenter).offset(y = 60.dp).size(diameter).clip(CircleShape)) {
            OnboardingGlobe(world, Modifier.fillMaxSize(), startAtMs = 12000)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0xB8000902), .52f to Color.Transparent,
            .73f to Color(0xB9000702), 1f to Color(0xFF020903),
        )))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(horizontal = 20.dp).padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            IntroHeading("Allow location for", 33)
            IntroHeading("Waypoint", 48)
            Spacer(Modifier.height(18.dp))
            Text("Waypoint uses your location to show where you are and guide you along real routes.",
                color = introMuted, fontFamily = introFont, fontSize = 16.sp,
                lineHeight = 21.sp, textAlign = TextAlign.Center)
            if (permissionDenied && !hasPermission) {
                Spacer(Modifier.height(8.dp))
                Text("Location is off. Enable it in Android settings or explore without it.",
                    color = introWhite, fontFamily = introFont, fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onSettings))
            }
        }
        OnboardingAction(if (hasPermission) "Open map" else "Allow location", false,
            if (hasPermission) onContinue else onAllow, bottomPadding = if (hasPermission) 30.dp else 61.dp)
        if (!hasPermission) {
            Text("Continue without location", Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 14.dp).clickable(role = Role.Button, onClick = onContinue)
                .padding(7.dp), color = introWhite, fontFamily = introFont, fontSize = 13.sp)
        }
    }
}
