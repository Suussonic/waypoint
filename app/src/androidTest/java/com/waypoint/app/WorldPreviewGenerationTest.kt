package com.waypoint.app

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Explicit asset-generation check; requires network tiles. Run separately from offline tests. */
@RunWith(AndroidJUnit4::class)
class WorldPreviewGenerationTest {
    @Test fun captureNewRealMapPreviews() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val folder = File(context.getExternalFilesDir(null), "world-previews")
        val status = File(folder, "status.txt")
        status.delete()
        val only=InstrumentationRegistry.getArguments().getString("previewWorld") ?: "new"
        val expected=if(only=="new") newWorlds else World.entries.filter { it.file==only }.toSet()
        check(expected.isNotEmpty())
        context.startActivity(Intent().setClassName(context.packageName, "com.waypoint.app.WorldPreviewCaptureActivity")
            .putExtra("only", only).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val deadline = SystemClock.elapsedRealtime() + 180_000
        while (!status.exists() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(500)
        assertTrue("Capture activity completed", status.exists())
        assertTrue(status.readText(), status.readText().startsWith("Captured ${expected.size}/${expected.size}"))
        expected.forEach { assertTrue("${it.file} actual map image", File(folder, "${it.file}.png").length() > 10_000) }
        UiDevice.getInstance(instrumentation).pressBack()
    }
}
