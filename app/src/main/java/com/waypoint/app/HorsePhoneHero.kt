package com.waypoint.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/** Silent, frame-rate-independent POV animation, paused with the onboarding lifecycle. */
@Composable
internal fun HorsePhoneHero(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = remember(context) { HorsePhoneView(context) }
    AndroidView(factory = { view }, modifier = modifier)
    DisposableEffect(view, lifecycleOwner) {
        fun update() {
            view.setPlaying(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycleOwner.lifecycle.addObserver(observer)
        update()
        onDispose {
            view.setPlaying(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private class HorsePhoneView(context: Context) : View(context) {
    private val rider = BitmapFactory.decodeResource(resources, R.drawable.onboarding_horse_pov)
    private val map = BitmapFactory.decodeResource(resources, R.drawable.onboarding_horse_map)
    private val horn = BitmapFactory.decodeResource(resources, R.drawable.onboarding_saddle_horn)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val lettering = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 245, 222)
        textSize = 22f
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.nunito), Typeface.BOLD)
    }
    private val screen = Path().apply { addRoundRect(RectF(143f, 829f, 936f, 1125f), 20f, 20f, Path.Direction.CW) }
    private val instruction = RectF(164f, 846f, 431f, 911f)
    private val hornTarget = RectF(365f, 1055f, 490f, 1185f)
    private val mapTarget = RectF()
    private val arrow = Path().apply {
        moveTo(192f, 891f); lineTo(192f, 865f)
        moveTo(181f, 877f); lineTo(192f, 865f); lineTo(203f, 877f)
    }
    private val outerMarker = marker(25f, -38f)
    private val innerMarker = marker(18f, -28f)
    private val vertices = FloatArray((MESH_COLUMNS + 1) * (MESH_ROWS + 1) * 2)
    // Skinning weights are computed once; only vertex positions change per frame.
    private val skin = Array((MESH_COLUMNS + 1) * (MESH_ROWS + 1)) { i ->
        val x = (i % (MESH_COLUMNS + 1)) * 30.0
        val y = (i / (MESH_COLUMNS + 1)) * 20.0
        doubleArrayOf(x, y, smooth((790-y)/290), smooth((790-y)/360),
            smooth((442-y)/125)*smooth((446-x)/65),
            smooth((421-y)/125)*smooth((x-530)/65),
            exp(-((x-376)/55)*((x-376)/55))*smooth((y-365)/90)*smooth((765-y)/95),
            smooth((y-1175)/160)*smooth((710-x)/190))
    }
    private var playing = false
    private var startedAt = 0L
    private var elapsedMs = 0L

    init {
        // This is decorative artwork; the native heading carries the accessible content.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setPlaying(play: Boolean) {
        if (play == playing) return
        if (play) startedAt = SystemClock.uptimeMillis()
        else elapsedMs = (elapsedMs + SystemClock.uptimeMillis() - startedAt) % LOOP_MS
        playing = play
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val animate = ValueAnimator.areAnimatorsEnabled()
        val elapsed = if (animate) elapsedMs + if (playing) SystemClock.uptimeMillis() - startedAt else 0L else 0L
        val time = (elapsed % LOOP_MS).toDouble() / 1000.0
        val phase = time / STRIDE_SECONDS * PI * 2.0
        val breath = sin(time / 6.4 * PI * 2)
        // Steady-state mass/spring/damper response to four-beat walking forces.
        // Saddle and mounted phone move together; the rider absorbs high-frequency motion.
        // The neck leads with a gentle nod instead of sharing a single rigid rotation.
        val sway = spring(phase, 1, 6.0, 2.6, .85)
        val bob = spring(phase, 2, 7.5, 3.2, .82) + spring(phase, 4, .55, 3.2, 1.0)
        val roll = spring(phase, 1, .28, 2.3, .9)
        val headX = spring(phase + .28, 1, 4.4, 1.7, .7) * (1 + .07 * breath)
        val headY = spring(phase + .42, 2, 12.0, 2.0, .66) * (1 + .06 * breath)
        val headRoll = spring(phase + .2, 1, .20, 1.7, .8) * PI / 180
        val depth = spring(phase + .7, 2, .0055, 2.0, .7)
        val mane = 1.05 * sin(phase * 2 - .8) + .28 * sin(phase * 3 - .6)
        val handX = spring(phase, 1, 1.15, 2.1, .85)
        val handY = spring(phase, 2, 1.7, 2.1, .85)
        val leftEar = -.045 * pulse(time, 1.45, .52) + .012 * pulse(time, 1.96, .28)
        val rightEar = .055 * pulse(time, 4.1, .48) - .014 * pulse(time, 4.57, .25)
        val mapY = (time / 6.4 * 640).toFloat()
        skin.forEachIndexed { i, w ->
            val x=w[0]; val y=w[1]; val neck=w[2]; val upper=w[3]
            var dx=headX*neck-(y-550)*headRoll*neck+(x-505)*depth*upper
            var dy=headY*neck+(x-505)*headRoll*neck+(y-550)*depth*upper
            dx+=-(y-426)*leftEar*w[4]-(y-412)*rightEar*w[5]+mane*w[6]+handX*w[7]
            dy+=(x-359)*leftEar*w[4]+(x-610)*rightEar*w[5]+handY*w[7]
            vertices[i*2]=(x+dx).toFloat()
            vertices[i*2+1]=(y+dy).toFloat()
        }
        canvas.save()
        // Preserve the original projection and proportions at every phone width.
        val scale = width / 1080f
        canvas.scale(scale, scale)
        canvas.translate(sway.toFloat(), bob.toFloat())
        canvas.rotate(roll.toFloat(), 540f, 1080f)
        canvas.drawBitmapMesh(rider, MESH_COLUMNS, MESH_ROWS, vertices, 0, null, 0, imagePaint)
        canvas.save()
        canvas.clipPath(screen)
        // A single double-height texture avoids filtered tile-edge hairlines on Android.
        mapTarget.set(143f, 189f + mapY, 937f, 1469f + mapY)
        canvas.drawBitmap(map, null, mapTarget, imagePaint)
        ink.style = Paint.Style.FILL
        ink.color = Color.rgb(36, 40, 36)
        canvas.drawRoundRect(instruction, 12f, 12f, ink)
        ink.color = Color.rgb(255, 245, 222)
        ink.strokeWidth = 5f
        ink.style = Paint.Style.STROKE
        canvas.drawPath(arrow, ink)
        canvas.drawText("Continue straight", 218f, 886f, lettering)
        ink.style = Paint.Style.FILL
        canvas.save()
        canvas.translate(520f, 1053f)
        ink.color = Color.rgb(22, 27, 25)
        canvas.drawPath(outerMarker, ink)
        ink.color = Color.rgb(255, 253, 240)
        canvas.drawPath(innerMarker, ink)
        ink.color = Color.rgb(23, 27, 25)
        canvas.drawCircle(0f, 3f, 11f, ink)
        canvas.restore()
        canvas.restore()
        // The foreground saddle horn must occlude the map, never slide beneath it.
        canvas.drawBitmap(horn, null, hornTarget, imagePaint)
        canvas.restore()
        if (playing && animate && isShown) postInvalidateOnAnimation()
    }

    private fun smooth(value: Double): Double {
        val v=value.coerceIn(0.0, 1.0)
        return v*v*(3-2*v)
    }

    private fun pulse(time: Double, start: Double, duration: Double): Double {
        if (time < start || time > start + duration) return 0.0
        val wave=sin(PI*(time-start)/duration)
        return wave*wave
    }

    private fun spring(phase: Double, harmonic: Int, amplitude: Double, naturalHz: Double, damping: Double): Double {
        val ratio=(harmonic/STRIDE_SECONDS)/naturalHz
        val lag=atan2(2*damping*ratio, 1-ratio*ratio)
        return amplitude*sin(harmonic*phase-lag)/hypot(1-ratio*ratio, 2*damping*ratio)
    }

    private fun marker(radius: Float, tip: Float) = Path().apply {
        moveTo(0f, tip)
        cubicTo(radius * .5f, tip * .68f, radius, -12f, radius, 3f)
        arcTo(RectF(-radius, 3f - radius, radius, 3f + radius), 0f, 180f)
        cubicTo(-radius, -12f, -radius * .5f, tip * .68f, 0f, tip)
        close()
    }

    companion object {
        private const val MESH_COLUMNS = 36
        private const val MESH_ROWS = 80
        private const val LOOP_MS = 6400L
        private const val STRIDE_SECONDS = 1.28
    }
}
