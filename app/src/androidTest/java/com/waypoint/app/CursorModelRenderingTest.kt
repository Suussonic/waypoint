package com.waypoint.app

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CursorModelRenderingTest {
    private class TestApplication(context: Context, private val testAssets: AssetManager): Application() {
        init { attachBaseContext(context) }
        override fun getAssets()=testAssets
    }

    @Test fun bundledSelectionSurvivesRestartWithoutExposingImports() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val productionAssets=inst.targetContext.assets
        val app=TestApplication(inst.targetContext,productionAssets)
        val prefs=app.getSharedPreferences("cursor-test",0)
        prefs.edit().clear().commit()
        try {
            val controller=NavigationController(app,preferencesName="cursor-test")
            assertEquals(listOf("Bicycle.glb","Motorcycle.glb","Waypoint.glb"),controller.cursorModels)
            lateinit var useLabel: String
            lateinit var selectedLabel: String
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    useLabel=activity.getString(R.string.cursor_use)
                    selectedLabel=activity.getString(R.string.cursor_selected)
                    val context=object: ContextWrapper(activity) {
                        override fun getAssets()=productionAssets
                    }
                    activity.setContent {
                        val state=controller.state.collectAsState().value
                        CompositionLocalProvider(LocalContext provides context) {
                            MaterialTheme { CursorModelSettings(state,controller) }
                        }
                    }
                }
                val device=UiDevice.getInstance(inst)
                assertTrue(device.wait(Until.hasObject(By.text("Car")),5000))
                assertFalse(device.hasObject(By.text("Waypoint.glb")))
                device.findObject(By.desc("Car")).click()
                val use=device.wait(Until.findObject(By.text(useLabel).enabled(true)),20000)
                assertNotNull("Bundled preview finishes loading",use)
                use!!.click()
                assertTrue(device.wait(Until.hasObject(By.text(selectedLabel)),5000))
                assertFalse(device.hasObject(By.text("Choose GLB file")))
            }
            val restored=NavigationController(app,preferencesName="cursor-test")
            assertEquals("Waypoint.glb",restored.state.value.cursorModel)
            assertTrue(restored.state.value.cursorModelEnabled)
            inst.runOnMainSync { restored.toggleCursorModel() }
            assertFalse(prefs.getBoolean("cursorModelEnabled",true))
            assertThrows(IllegalArgumentException::class.java) { restored.setCursorModel("external.glb") }
            prefs.edit().putString("cursorModel","removed.glb").putBoolean("cursorModelEnabled",true).commit()
            val missing=NavigationController(app,preferencesName="cursor-test")
            assertNull(missing.state.value.cursorModel)
            assertFalse(missing.state.value.cursorModelEnabled)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun snapshotHasVisibleMeshAndTransparentBackground() {
        val inst=InstrumentationRegistry.getInstrumentation()
        var ready=CountDownLatch(1)
        var image: Bitmap?=null
        var error: String?=null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val context=object: ContextWrapper(activity) {
                    override fun getAssets()=inst.targetContext.assets
                }
                activity.setContent {
                    CompositionLocalProvider(LocalContext provides context) {
                        CursorModelView("Waypoint.glb",Modifier.size(128.dp).graphicsLayer { alpha=0f },
                            onSnapshot={image=it;ready.countDown()},onError={error=it;ready.countDown()})
                    }
                }
            }
            repeat(3) { attempt ->
                assertTrue("Snapshot after attach $attempt",ready.await(20,TimeUnit.SECONDS))
                assertNull(error)
                val bitmap=requireNotNull(image)
                val pixels=IntArray(bitmap.width*bitmap.height)
                bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                assertTrue("Visible mesh",pixels.count { (it ushr 24)>128 }>100)
                assertTrue("Transparent background",pixels.count { (it ushr 24)==0 }>100)
                bitmap.recycle()
                if(attempt<2)scenario.onActivity { activity ->
                    ready=CountDownLatch(1)
                    image=null
                    val surface=requireNotNull(findTexture(activity.window.decorView))
                    val host=surface.parent as ViewGroup
                    val parent=host.parent as ViewGroup
                    val index=parent.indexOfChild(host)
                    val params=host.layoutParams
                    parent.removeView(host)
                    parent.addView(host,index,params)
                    assertNotSame("Reattach must create a new renderer surface",surface,findTexture(host))
                }
            }
        }
    }

    @Test fun bundledModelsRemainVisibleAtFlatAndTiltedMapAngles() {
        val inst=InstrumentationRegistry.getInstrumentation()
        for(model in listOf("Waypoint.glb","Bicycle.glb","Motorcycle.glb")) {
            var flatPixels: IntArray?=null
            for(tilt in listOf(0f,DRIVE_TILT.toFloat())) {
                val ready=CountDownLatch(1)
                var image: Bitmap?=null
                var error: String?=null
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        val context=object: ContextWrapper(activity) {
                            override fun getAssets()=inst.targetContext.assets
                        }
                        activity.setContent {
                            CompositionLocalProvider(LocalContext provides context) {
                                CursorModelView(model,Modifier.size(128.dp).graphicsLayer { alpha=0f },tilt=tilt,
                                    onSnapshot={image=it;ready.countDown()},
                                    onError={error=it;ready.countDown()})
                            }
                        }
                    }
                    assertTrue("$model at $tilt° snapshot completes",ready.await(20,TimeUnit.SECONDS))
                    assertNull("$model at $tilt° render error",error)
                    val bitmap=requireNotNull(image)
                    val pixels=IntArray(bitmap.width*bitmap.height)
                    bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                    assertTrue("$model at $tilt° visible mesh",pixels.count { (it ushr 24)>128 }>100)
                    assertTrue("$model at $tilt° transparent background",pixels.count { (it ushr 24)==0 }>100)
                    if(tilt==0f)flatPixels=pixels
                    else assertFalse("$model tilt changes its rendered image",requireNotNull(flatPixels).contentEquals(pixels))
                    bitmap.recycle()
                }
            }
        }
    }

    @Test fun settingsPreviewsRenderAllModelsAtThreeQuarterAngle() {
        val inst=InstrumentationRegistry.getInstrumentation()
        for(model in listOf("Waypoint.glb","Motorcycle.glb","Bicycle.glb")) {
            val ready=CountDownLatch(1)
            var image: Bitmap?=null
            var error: String?=null
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val context=object: ContextWrapper(activity) {
                        override fun getAssets()=inst.targetContext.assets
                    }
                    activity.setContent {
                        CompositionLocalProvider(LocalContext provides context) {
                            CursorModelView(model,Modifier.size(144.dp),tilt=42f,
                                azimuth=CURSOR_MODEL_SETTINGS_PREVIEW_AZIMUTH,
                                onReady={ready.countDown()},onError={error=it;ready.countDown()})
                        }
                    }
                }
                assertTrue("$model settings preview completes",ready.await(20,TimeUnit.SECONDS))
                assertNull("$model settings preview renders without error",error)
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
                while(image==null && System.nanoTime()<deadline) {
                    scenario.onActivity { activity ->
                        val rendered=findTexture(activity.window.decorView)?.bitmap
                        if(rendered!=null) {
                            if(rendered.hasVisiblePixels()) {
                                image=Bitmap.createScaledBitmap(rendered,405,405,true).also {
                                    if(it!==rendered)rendered.recycle()
                                }
                            } else rendered.recycle()
                        }
                    }
                    if(image==null)Thread.sleep(100)
                }
                val bitmap=requireNotNull(image) { "$model settings preview has no visible frame" }
                val pixels=IntArray(bitmap.width*bitmap.height)
                bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                assertTrue("$model shows a visible vehicle",pixels.count { (it ushr 24)>128 }>100)
                val frontLightRows=mutableListOf<Int>()
                val rearLightRows=mutableListOf<Int>()
                pixels.forEachIndexed { index,pixel -> if((pixel ushr 24)>128) {
                    val red=(pixel shr 16) and 0xff
                    val green=(pixel shr 8) and 0xff
                    val blue=pixel and 0xff
                    val row=index/bitmap.width
                    if(red>150 && green>150 && blue>125)frontLightRows+=row
                    if(red>80 && red>green*1.8 && red>blue*1.8)rearLightRows+=row
                } }
                assertTrue("$model front light is rendered",frontLightRows.isNotEmpty())
                assertTrue("$model rear light is rendered",rearLightRows.isNotEmpty())
                assertTrue("$model front points toward the preview camera",
                    frontLightRows.average()>rearLightRows.average())
                bitmap.recycle()
            }
        }
    }

    @Test fun topDownSnapshotShowsTheVehicleBody() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val ready=CountDownLatch(1)
        var image: Bitmap?=null
        var error: String?=null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val context=object: ContextWrapper(activity) {
                    override fun getAssets()=inst.targetContext.assets
                }
                activity.setContent {
                    CompositionLocalProvider(LocalContext provides context) {
                        CursorModelView("Waypoint.glb",Modifier.size(128.dp).graphicsLayer { alpha=0f },
                            tilt=0f,onSnapshot={image=it;ready.countDown()},onError={error=it;ready.countDown()})
                    }
                }
            }
            assertTrue("Top-down snapshot completes",ready.await(20,TimeUnit.SECONDS))
            assertNull(error)
            val bitmap=requireNotNull(image)
            var minX=bitmap.width;var maxX=-1;var minY=bitmap.height;var maxY=-1
            val row=IntArray(bitmap.width)
            for(y in 0 until bitmap.height) {
                bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
                row.forEachIndexed { x,pixel -> if((pixel ushr 24)>128) {
                    minX=minOf(minX,x);maxX=maxOf(maxX,x);minY=minOf(minY,y);maxY=maxOf(maxY,y)
                } }
            }
            val visibleWidth=maxX-minX+1
            val visibleHeight=maxY-minY+1
            assertTrue("Top-down vehicle silhouette stays longer than it is wide ($visibleWidth x $visibleHeight)",
                visibleHeight>visibleWidth*1.35f)
            bitmap.recycle()
        }
    }

    private fun findTexture(view: View): TextureView? {
        if(view is TextureView)return view
        if(view is ViewGroup)for(index in 0 until view.childCount) {
            findTexture(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun Bitmap.hasVisiblePixels(): Boolean {
        val row=IntArray(width)
        for(y in 0 until height) {
            getPixels(row,0,width,0,y,width,1)
            if(row.any { (it ushr 24)>128 })return true
        }
        return false
    }
}
