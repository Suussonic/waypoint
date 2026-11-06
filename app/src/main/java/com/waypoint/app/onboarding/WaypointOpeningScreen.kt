package com.waypoint.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A short cold-start interstitial inspired by the reference app's full-bleed
 * loading screen, rebuilt around Waypoint's own map mark and green palette.
 */
@Composable
internal fun WaypointOpeningScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_050, easing = LinearOutSlowInEasing),
        )
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 210, easing = FastOutLinearInEasing),
        )
        onFinished()
    }

    val logoReveal = (progress.value / 0.24f).coerceIn(0f, 1f)
    val progressLabel = when {
        progress.value < 0.55f -> "Preparing your map"
        progress.value < 0.92f -> "Finding your next route"
        else -> "Ready to explore"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(WaypointColors.page)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF213020), WaypointColors.page.copy(alpha = 0f)),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = size.maxDimension * 0.72f,
                ),
            )

            val street = Color(0xFF9AAD8D).copy(alpha = 0.10f)
            val route = WaypointColors.accent.copy(alpha = 0.20f)
            val line = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
            val routes = listOf(
                Path().apply {
                    moveTo(-w * 0.08f, h * 0.18f)
                    cubicTo(w * 0.25f, h * 0.25f, w * 0.18f, h * 0.34f, w * 0.50f, h * 0.38f)
                    cubicTo(w * 0.78f, h * 0.42f, w * 0.82f, h * 0.25f, w * 1.08f, h * 0.30f)
                },
                Path().apply {
                    moveTo(-w * 0.06f, h * 0.68f)
                    cubicTo(w * 0.23f, h * 0.64f, w * 0.22f, h * 0.78f, w * 0.50f, h * 0.73f)
                    cubicTo(w * 0.81f, h * 0.68f, w * 0.79f, h * 0.84f, w * 1.06f, h * 0.78f)
                },
                Path().apply {
                    moveTo(w * 0.16f, -h * 0.02f)
                    cubicTo(w * 0.20f, h * 0.20f, w * 0.34f, h * 0.21f, w * 0.33f, h * 0.43f)
                    cubicTo(w * 0.31f, h * 0.67f, w * 0.18f, h * 0.78f, w * 0.24f, h * 1.03f)
                },
                Path().apply {
                    moveTo(w * 0.83f, -h * 0.04f)
                    cubicTo(w * 0.75f, h * 0.16f, w * 0.88f, h * 0.28f, w * 0.77f, h * 0.47f)
                    cubicTo(w * 0.67f, h * 0.64f, w * 0.87f, h * 0.81f, w * 0.80f, h * 1.04f)
                },
            )
            routes.forEach { drawPath(it, color = street, style = line) }

            val waypoints = listOf(
                Offset(0.11f, 0.20f), Offset(0.87f, 0.24f), Offset(0.16f, 0.53f),
                Offset(0.84f, 0.57f), Offset(0.12f, 0.79f), Offset(0.90f, 0.84f),
            )
            waypoints.forEachIndexed { index, point ->
                val center = Offset(w * point.x, h * point.y)
                drawCircle(
                    color = route.copy(alpha = if (index % 2 == 0) 0.9f else 0.6f),
                    radius = 11.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.1.dp.toPx()),
                )
                drawCircle(
                    color = WaypointColors.accent.copy(alpha = 0.48f),
                    radius = 2.4.dp.toPx(),
                    center = center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha = logoReveal
                    val scale = 0.94f + 0.06f * logoReveal
                    scaleX = scale
                    scaleY = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(WaypointColors.surfaceDeep.copy(alpha = 0.92f))
                    .border(1.dp, WaypointColors.accent.copy(alpha = 0.34f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .border(1.dp, WaypointColors.quiet.copy(alpha = 0.13f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "WAYPOINT",
                color = WaypointColors.paper,
                fontFamily = DisplayFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "MAP YOUR NEXT QUEST",
                color = WaypointColors.muted.copy(alpha = 0.82f),
                fontSize = TypeScale.Caption,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.0.sp,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = progressLabel,
                color = WaypointColors.quiet,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WaypointColors.paper.copy(alpha = 0.12f))
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.value, 0f..1f)
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(WaypointColors.accent, WaypointColors.accentHover),
                            ),
                        ),
                )
            }
        }
    }
}
