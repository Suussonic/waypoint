package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class QuestImagesTest {
    @Test fun selectedPhotoIsOrientedResizedAndStrippedOfMetadata(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "updates/portrait-test.jpg").apply { parentFile!!.mkdirs() }
        var name: String? = null
        try {
            val bitmap = Bitmap.createBitmap(480, 960, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawColor(Color.BLUE)
                drawRect(0f, 0f, 480f, 480f, Paint().apply { color = Color.RED })
            }
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }; bitmap.recycle()
            ExifInterface(source.path).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setAttribute(ExifInterface.TAG_ARTIST, "Synthetic test fixture")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "1/1,0/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                saveAttributes()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", source)
            name = QuestImages.import(context, uri)
            val copy = QuestImages.file(context, name)!!
            assertTrue(copy.canonicalPath.startsWith(File(context.filesDir, "quest-portraits").canonicalPath + "/"))
            val decoded = BitmapFactory.decodeFile(copy.path)
            assertEquals(384, decoded.width); assertEquals(384, decoded.height)
            assertTrue(Color.red(decoded.getPixel(300, 192)) > Color.blue(decoded.getPixel(300, 192)))
            assertTrue(Color.blue(decoded.getPixel(80, 192)) > Color.red(decoded.getPixel(80, 192)))
            decoded.recycle()
            val exif = ExifInterface(copy.path)
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertNull(exif.getAttribute(ExifInterface.TAG_ARTIST))
        } finally { name?.let { QuestImages.delete(context, it) }; source.delete() }
    }

    @Test fun invalidSchemesAndPathsCannotReadArbitraryFiles(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNull(QuestImages.file(context, "../../outside.jpg"))
        try { QuestImages.import(context, android.net.Uri.parse("file:///private-test.jpg")); fail("Only a picker content URI is accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
