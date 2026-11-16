package com.waypoint.app

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.opengl.Matrix
import android.view.Choreographer
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.google.android.filament.Camera
import com.google.android.filament.IndirectLight
import com.google.android.filament.Renderer
import com.google.android.filament.View.BlendMode
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

internal const val CURSOR_MODEL_ASSET_DIRECTORY = "cursor-models"
internal const val MAX_CURSOR_MODEL_BYTES = 10 * 1024 * 1024
internal const val CURSOR_MODEL_IMAGE = "cursor-model"

/** Map snapshots are re-rendered in 5° steps so small camera drift does not restart Filament. */
internal fun cursorModelTilt(tilt: Double): Float = (Math.round(tilt.coerceIn(0.0,90.0)/5.0)*5).toFloat()

internal fun cursorModelFileNames(files: List<String>): List<String> =
    files.filter { it.endsWith(".glb",true) && '/' !in it && '\\' !in it }
        .sortedWith(compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it })

internal fun bundledCursorModels(assets: AssetManager): List<String> =
    cursorModelFileNames(assets.list(CURSOR_MODEL_ASSET_DIRECTORY).orEmpty().toList())

internal fun cursorModelDisplayName(model: String): String = model.substringBeforeLast('.')

internal fun validateCursorGlb(bytes: ByteArray) {
    require(bytes.size in 20..MAX_CURSOR_MODEL_BYTES) { "The bundled GLB model must be at most 10 MB." }
    val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(buffer.int==0x46546c67 && buffer.int==2 && buffer.int==bytes.size) { "Invalid GLB 2.0 file." }
    val size=buffer.int
    require(buffer.int==0x4e4f534a && size>0 && size<=buffer.remaining() && size%4==0) { "Invalid GLB JSON chunk." }
    val jsonBytes=ByteArray(size)
    buffer.get(jsonBytes)
    val json=JSONObject(String(jsonBytes,Charsets.UTF_8))
    require(json.getJSONObject("asset").getString("version")=="2.0") { "Only glTF 2.0 is supported." }
    for(key in listOf("buffers","images")) {
        val entries=json.optJSONArray(key) ?: continue
        for(index in 0 until entries.length()) {
            require(!entries.getJSONObject(index).has("uri")) { "Embed all geometry and textures in the GLB file." }
        }
    }
    require((json.optJSONArray("meshes")?.length() ?: 0)>0) { "The model contains no mesh." }
    require(buffer.remaining()>=8) { "Missing GLB geometry." }
    val binarySize=buffer.int
    require(buffer.int==0x004e4942 && binarySize>0 && binarySize==buffer.remaining() && binarySize%4==0) { "Invalid GLB binary chunk." }
}

@Composable
internal fun CursorModelSettings(state: NavState, controller: NavigationController) {
    val models=controller.cursorModels
    var preview by remember { mutableStateOf(state.cursorModel ?: models.firstOrNull()) }
    var ready by remember(preview) { mutableStateOf(false) }
    var error by remember(preview) { mutableStateOf<String?>(null) }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("3D position cursor",style=MaterialTheme.typography.titleMedium)
        Text("Choose a model included with Waypoint. Phone map only; Android Auto keeps its standard marker.",
            style=MaterialTheme.typography.bodySmall)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Replace the position cursor",Modifier.weight(1f))
            Switch(state.cursorModelEnabled,{controller.toggleCursorModel()},enabled=state.cursorModel!=null,
                modifier=Modifier.semantics { contentDescription="Replace the position cursor" })
        }
        if(models.isEmpty())Text("No 3D models are included in this version.")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                models.forEach { model ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(preview==model,{preview=model},
                            modifier=Modifier.semantics { contentDescription=cursorModelDisplayName(model) })
                        TextButton(onClick={preview=model}) { Text(cursorModelDisplayName(model)) }
                    }
                }
            }
            preview?.let { model ->
                key(model) {
                    CursorModelView(model,Modifier.size(128.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        autoRotate=true,onReady={ready=true},onError={error=it;ready=false})
                }
            }
        }
        preview?.let { model ->
            if(!ready && error==null)LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick={controller.setCursorModel(model)},enabled=ready) {
                Text(if(state.cursorModel==model && state.cursorModelEnabled)"Selected" else "Use this model")
            }
        }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }
}

/** Preview is live 3D; map snapshots are rendered once and placed by MapLibre itself. */
@Composable
internal fun CursorModelView(model: String, modifier: Modifier, autoRotate: Boolean=false,
    tilt: Float=45f, onSnapshot: ((Bitmap)->Unit)?=null,
    onReady: ()->Unit={}, onError: (String)->Unit={}) {
    val assets=LocalContext.current.assets
    val owner=LocalLifecycleOwner.current
    val lifecycle by owner.lifecycle.currentStateAsState()
    val error by rememberUpdatedState(onError)
    val ready by rememberUpdatedState(onReady)
    val snapshot by rememberUpdatedState(onSnapshot)
    val currentTilt by rememberUpdatedState(tilt)
    val cursorPixels=with(LocalDensity.current) { 64.dp.toPx() }
    var viewer by remember { mutableStateOf<ModelViewer?>(null) }
    var texture by remember { mutableStateOf<TextureView?>(null) }
    var buffer by remember { mutableStateOf<ByteBuffer?>(null) }
    val alive=remember { booleanArrayOf(false) }
    val normalization=remember { arrayOfNulls<FloatArray>(1) }
    LaunchedEffect(model,assets) {
        try {
            buffer=withContext(Dispatchers.IO) {
                require(model in bundledCursorModels(assets)) { "This model is not included in this version." }
                val bytes=assets.open("$CURSOR_MODEL_ASSET_DIRECTORY/$model").use { input ->
                    val output=java.io.ByteArrayOutputStream()
                    val chunk=ByteArray(8192)
                    var count=input.read(chunk)
                    while(count!=-1) {
                        require(output.size()+count<=MAX_CURSOR_MODEL_BYTES) { "The bundled GLB model exceeds 10 MB." }
                        output.write(chunk,0,count)
                        count=input.read(chunk)
                    }
                    output.toByteArray()
                }
                validateCursorGlb(bytes)
                ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply { put(bytes);flip() }
            }
        } catch(problem: IOException) { error("Cannot read the bundled 3D model.") }
        catch(problem: IllegalArgumentException) { error(problem.message ?: "Invalid GLB model.") }
        catch(problem: JSONException) { error("Invalid glTF data in this file.") }
    }
    AndroidView(factory={context ->
        Utils.init()
        Gltfio.init()
        // ModelViewer is invalid after detach, so the host creates a fresh child on attach.
        object: FrameLayout(context) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                // Bulk removal would dispatch detach twice to the already-destroyed viewer.
                if(childCount>0)removeViewAt(0)
                val surface=TextureView(context).apply {
                    isOpaque=false
                    var releaseLight: (() -> Unit)?=null
                    // Release our light before ModelViewer's listener destroys the engine.
                    addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {}
                        override fun onViewDetachedFromWindow(view: View) {
                            alive[0]=false;viewer=null;texture=null
                            releaseLight?.invoke();releaseLight=null
                        }
                    })
                    val helper=UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply { isOpaque=false }
                    val renderer=ModelViewer(this,uiHelper=helper)
                    val ambient=IndirectLight.Builder().irradiance(3,FloatArray(27).apply {
                        this[0]=1f;this[1]=1f;this[2]=1f
                    }).intensity(30_000f).build(renderer.engine)
                    renderer.scene.indirectLight=ambient
                    val light=renderer.engine.lightManager.getInstance(renderer.light)
                    renderer.engine.lightManager.setDirection(light,-.4f,-1f,-.6f)
                    renderer.engine.lightManager.setIntensity(light,65_000f)
                    releaseLight={renderer.scene.indirectLight=null;renderer.engine.destroyIndirectLight(ambient)}
                    renderer.view.blendMode=BlendMode.TRANSLUCENT
                    // Post-processing (FXAA) aborts on some GL drivers and loses alpha on others.
                    renderer.view.isPostProcessingEnabled=false
                    renderer.renderer.clearOptions=Renderer.ClearOptions().apply {
                        clear=true;clearColor=floatArrayOf(0f,0f,0f,0f)
                    }
                    normalization[0]=null
                    viewer=renderer;texture=this;alive[0]=true
                }
                addView(surface,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
            }
        }
    },modifier=modifier,onRelease={viewer=null;texture=null})
    LaunchedEffect(viewer,buffer) {
        val renderer=viewer?.takeIf { alive[0] } ?: return@LaunchedEffect
        val data=buffer ?: return@LaunchedEffect
        renderer.loadModelGlb(data)
        val asset=renderer.asset
        if(asset==null) { error("This GLB cannot be rendered.");return@LaunchedEffect }
        val extent=asset.boundingBox.halfExtent
        if(extent.any { !it.isFinite() } || extent.maxOrNull()!!<=0f || asset.boundingBox.center.any { !it.isFinite() }) {
            renderer.destroyModel();error("The model has invalid dimensions.");return@LaunchedEffect
        }
        renderer.transformToUnitCube(Float3(0f,0f,0f))
        val tm=renderer.engine.transformManager
        normalization[0]=tm.getTransform(tm.getInstance(asset.root),FloatArray(16)).apply {
            if(onSnapshot!=null)this[13]+=extent[1]/extent.maxOrNull()!!
        }
    }
    DisposableEffect(viewer,lifecycle,if(onSnapshot!=null)tilt else null) {
        val renderer=viewer
        val frames=Choreographer.getInstance()
        val pose=FloatArray(16)
        val result=FloatArray(16)
        var firstFrame=0L
        var reported=false
        var loadedFrames=0
        val callback=object: Choreographer.FrameCallback {
            override fun doFrame(time: Long) {
                if(renderer==null || !alive[0])return
                val normalized=normalization[0]
                val asset=renderer.asset
                if(asset!=null && normalized!=null) {
                    if(firstFrame==0L)firstFrame=time
                    if(snapshot!=null) {
                        val width=renderer.view.viewport.width
                        val height=renderer.view.viewport.height
                        if(width>0 && height>0) {
                            val units=3.0/cursorPixels
                            renderer.camera.setProjection(Camera.Projection.ORTHO,
                                -width/2.0*units,width/2.0*units,-height/2.0*units,height/2.0*units,.05,1000.0)
                        }
                    }
                    Matrix.setIdentityM(pose,0)
                    Matrix.translateM(pose,0,0f,0f,-4f)
                    Matrix.rotateM(pose,0,if(autoRotate)20f else 90f-currentTilt,1f,0f,0f)
                    if(autoRotate)Matrix.rotateM(pose,0,((time-firstFrame)/1e9*30.0%360.0).toFloat(),0f,1f,0f)
                    Matrix.multiplyMM(result,0,pose,0,normalized,0)
                    val tm=renderer.engine.transformManager
                    tm.setTransform(tm.getInstance(asset.root),result)
                }
                renderer.render(time)
                if(asset!=null && renderer.progress>=1f) {
                    if(!reported) { reported=true;ready() }
                    loadedFrames++
                    if(snapshot!=null && loadedFrames>=4) {
                        // TextureView content can lag behind Filament, so wait for real pixels.
                        val bitmap=texture?.bitmap
                        if(bitmap!=null && bitmap.hasVisiblePixels()) { snapshot?.invoke(bitmap);return }
                        bitmap?.recycle()
                        if(loadedFrames>=120) { error("The model rendered no visible pixels.");return }
                    }
                }
                frames.postFrameCallback(this)
            }
        }
        if(renderer!=null && lifecycle.isAtLeast(Lifecycle.State.RESUMED))frames.postFrameCallback(callback)
        onDispose { frames.removeFrameCallback(callback) }
    }
}

private fun Bitmap.hasVisiblePixels(): Boolean {
    val row=IntArray(width)
    for(y in 0 until height) {
        getPixels(row,0,width,0,y,width,1)
        if(row.any { (it ushr 24)!=0 })return true
    }
    return false
}
