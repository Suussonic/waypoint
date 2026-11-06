package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** A real GPU sphere. MapLibre Native currently renders only the flat Mercator projection. */
@Composable
internal fun OnboardingGlobe(world: World, modifier: Modifier, startAtMs: Int = 0, closeUp: Boolean = false) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val renderer = remember(startAtMs, closeUp) { GlobeRenderer(startAtMs, closeUp) }
    val view = remember(context, renderer) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setZOrderOnTop(true)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    DisposableEffect(view, owner) {
        val observer = LifecycleEventObserver { _, _ ->
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
            else view.onPause()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            view.onPause()
        }
    }
    LaunchedEffect(view, world.file) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open("globe/textures/${world.file}.png").use { stream ->
                    BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inScaled = false
                    })
                }
            } catch (error: Exception) {
                Log.e("WaypointGlobe", "Unable to load ${world.file} globe texture", error)
                null
            }
        }
        if (bitmap != null) view.queueEvent { renderer.replaceTexture(bitmap, world == World.PIXEL) }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

private class GlobeRenderer(startAtMs: Int, private val closeUp: Boolean) : GLSurfaceView.Renderer {
    private val startNanos = SystemClock.elapsedRealtimeNanos() - startAtMs * 1_000_000L
    private val vertices: FloatBuffer
    private val indices: ShortBuffer
    private val indexCount: Int
    private var program = 0
    private var texture = 0
    private var bitmap: Bitmap? = null
    private var pixelated = false
    private var textureDirty = false
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val modelView = FloatArray(16)
    private val mvp = FloatArray(16)

    init {
        val latitudeSteps = 64
        val longitudeSteps = 128
        val points = FloatArray((latitudeSteps + 1) * (longitudeSteps + 1) * 5)
        var offset = 0
        for (latIndex in 0..latitudeSteps) {
            val lat = Math.PI * (latIndex.toDouble() / latitudeSteps - .5)
            for (lonIndex in 0..longitudeSteps) {
                val lon = Math.PI * 2 * (lonIndex.toDouble() / longitudeSteps - .5)
                val cosLat = kotlin.math.cos(lat)
                points[offset++] = (cosLat * kotlin.math.sin(lon)).toFloat()
                points[offset++] = kotlin.math.sin(lat).toFloat()
                points[offset++] = (cosLat * kotlin.math.cos(lon)).toFloat()
                points[offset++] = lonIndex.toFloat() / longitudeSteps
                points[offset++] = 1f - latIndex.toFloat() / latitudeSteps
            }
        }
        vertices = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(points); position(0) }
        val triangles = ShortArray(latitudeSteps * longitudeSteps * 6)
        offset = 0
        for (latIndex in 0 until latitudeSteps) {
            for (lonIndex in 0 until longitudeSteps) {
                val a = latIndex * (longitudeSteps + 1) + lonIndex
                val b = a + 1
                val c = a + longitudeSteps + 1
                val d = c + 1
                triangles[offset++] = a.toShort()
                triangles[offset++] = b.toShort()
                triangles[offset++] = c.toShort()
                triangles[offset++] = b.toShort()
                triangles[offset++] = d.toShort()
                triangles[offset++] = c.toShort()
            }
        }
        indices = ByteBuffer.allocateDirect(triangles.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(triangles); position(0) }
        indexCount = triangles.size
    }

    /** Called on the GL thread; the clock and camera stay untouched. */
    fun replaceTexture(next: Bitmap, useNearest: Boolean) {
        bitmap?.recycle()
        bitmap = next
        pixelated = useNearest
        textureDirty = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) Log.e("WaypointGlobe", "Globe program: ${GLES20.glGetProgramInfoLog(program)}")
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        texture = ids[0]
        textureDirty = bitmap != null
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        if (closeUp) GLES20.glClearColor(2f / 255f, 9f / 255f, 3f / 255f, 1f)
        else GLES20.glClearColor(0f, 0f, 0f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection, 0, 42f, width.toFloat() / height.coerceAtLeast(1), .1f, 10f)
        if (closeUp) projection[9] = .70f
        Matrix.setLookAtM(view, 0, 0f, 0f, if (closeUp) 1.9f else 2.9f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0 || texture == 0) return
        if (textureDirty) {
            bitmap?.let {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
                val filter = if (pixelated) GLES20.GL_NEAREST else GLES20.GL_LINEAR
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0)
                textureDirty = false
            }
        }
        if (bitmap == null) return
        val centerLongitude = ((55.0 + (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000_000.0 * 6.0) % 360.0).toFloat()
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, if (closeUp) 16f else 25f, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, -centerLongitude, 0f, 1f, 0f)
        Matrix.multiplyMM(modelView, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, modelView, 0)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMvp"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModel"), 1, false, model, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uBottomFade"), if (closeUp) 1f else 0f)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        val uv = GLES20.glGetAttribLocation(program, "aUv")
        vertices.position(0)
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 20, vertices)
        GLES20.glEnableVertexAttribArray(position)
        vertices.position(3)
        GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, 20, vertices)
        GLES20.glEnableVertexAttribArray(uv)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indices)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) Log.e("WaypointGlobe", "Globe shader: ${GLES20.glGetShaderInfoLog(shader)}")
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPosition;
            attribute vec2 aUv;
            varying vec2 vUv;
            varying vec3 vNormal;
            void main() {
                vUv = aUv;
                vNormal = normalize((uModel * vec4(aPosition, 0.0)).xyz);
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uBottomFade;
            varying vec2 vUv;
            varying vec3 vNormal;
            void main() {
                vec3 base = texture2D(uTexture, vUv).rgb;
                float light = max(0.0, dot(normalize(vNormal), normalize(vec3(-0.32, 0.42, 0.85))));
                float shade = 0.49 + 0.51 * light;
                float fade = uBottomFade > 0.5 ? smoothstep(0.0, 500.0, gl_FragCoord.y) : 1.0;
                vec3 backdrop = vec3(2.0 / 255.0, 9.0 / 255.0, 3.0 / 255.0);
                gl_FragColor = vec4(mix(backdrop, base * shade, fade), 1.0);
            }
        """
    }
}
