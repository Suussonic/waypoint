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
        val app=TestApplication(inst.targetContext,inst.context.assets)
        val prefs=app.getSharedPreferences("cursor-test",0)
        prefs.edit().clear().commit()
        try {
            val controller=NavigationController(app,preferencesName="cursor-test")
            assertEquals(listOf("Test.glb"),controller.cursorModels)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val context=object: ContextWrapper(activity) {
                        override fun getAssets()=inst.context.assets
                    }
                    activity.setContent {
                        val state=controller.state.collectAsState().value
                        CompositionLocalProvider(LocalContext provides context) {
                            MaterialTheme { CursorModelSettings(state,controller) }
                        }
                    }
                }
                val device=UiDevice.getInstance(inst)
                assertTrue(device.wait(Until.hasObject(By.text("Test")),5000))
                assertFalse(device.hasObject(By.text("Test.glb")))
                val use=device.wait(Until.findObject(By.text("Use this model").enabled(true)),20000)
                assertNotNull("Bundled preview finishes loading",use)
                use!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("Selected")),5000))
                assertFalse(device.hasObject(By.text("Choose GLB file")))
            }
            val restored=NavigationController(app,preferencesName="cursor-test")
            assertEquals("Test.glb",restored.state.value.cursorModel)
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
                    override fun getAssets()=inst.context.assets
                }
                activity.setContent {
                    CompositionLocalProvider(LocalContext provides context) {
                        CursorModelView("Test.glb",Modifier.size(128.dp).graphicsLayer { alpha=0f },
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

    private fun findTexture(view: View): TextureView? {
        if(view is TextureView)return view
        if(view is ViewGroup)for(index in 0 until view.childCount) {
            findTexture(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
