package com.waypoint.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object QuestImages {
    private val safeName = Regex("[a-f0-9-]{36}\\.jpg")
    fun isImageName(name: String) = safeName.matches(name)
    fun file(context: Context, name: String): File? = if (isImageName(name)) File(File(context.filesDir, "quest-portraits"), name) else null
    fun delete(context: Context, name: String) { file(context, name)?.delete() }
    suspend fun cleanAbandoned(context: Context, used: Set<String>) = withContext(Dispatchers.IO) {
        // Keep recent drafts across process death, rotation and the system photo picker.
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
        File(context.filesDir, "quest-portraits").listFiles()?.forEach {
            if (isImageName(it.name) && it.name !in used && it.lastModified() < cutoff) it.delete()
        }
    }

    /** Decode bounds before pixels; only our small re-encoded JPEG is retained, without EXIF metadata. */
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose a photo from the photo picker." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= 16 * 1024 * 1024) { "Choose a photo smaller than 16 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("This photo could not be opened. Try a different photo.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..16000 && bounds.outHeight in 1..16000) { "This photo could not be opened. Try a different photo." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("This photo format is not supported.")
        val orientation = runCatching { ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
        val side = minOf(upright.width, upright.height)
        val crop = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(crop, 384, 384, true)
        val directory = File(context.filesDir, "quest-portraits").apply { mkdirs() }
        val name = "${UUID.randomUUID()}.jpg"
        try {
            File(directory, name).outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
        } catch (failure: Exception) {
            File(directory, name).delete()
            throw failure
        } finally {
            if (scaled !== crop) scaled.recycle()
            if (crop !== upright) crop.recycle()
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
        }
        name
    }
}
