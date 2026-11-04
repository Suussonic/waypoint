package com.waypoint.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Consume/close the response in the callback; cancellation never leaves an open response body. */
internal suspend fun sharingResponseHeaders(call: Call): Pair<Int,String?> = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object: Callback {
        override fun onFailure(call: Call, error: IOException) {
            if(continuation.isActive)continuation.resumeWithException(error)
        }
        override fun onResponse(call: Call, response: Response) {
            val headers=response.use { it.code to it.header("Location") }
            if(continuation.isActive)continuation.resume(headers)
        }
    })
}

/** Sharing transfers a destination, never another application's navigation session. */
internal object NavigationSharing {
    const val MAX_INPUT = 4096
    private val allowedHosts = setOf("maps.app.goo.gl", "goo.gl", "www.google.com", "google.com", "maps.google.com")
    fun supportedUrl(value: String): HttpUrl? {
        val url = value.toHttpUrlOrNull() ?: return null
        if (url.scheme != "https" || url.port != 443 || url.username.isNotEmpty() || url.password.isNotEmpty() || url.host !in allowedHosts) return null
        if (url.host in setOf("www.google.com", "google.com", "goo.gl") && url.encodedPath != "/maps" && !url.encodedPath.startsWith("/maps/")) return null
        return url
    }
    fun sharedUrl(value: String): String? = Regex("https://[^\\s<>]+", RegexOption.IGNORE_CASE).findAll(value).toList()
        .singleOrNull()?.value?.trimEnd('.', ',', ')', ']')

    fun parseText(value: String?): NavigationImport? {
        if (value == null || value.length > MAX_INPUT || value.any { it.isISOControl() && it !in "\n\r\t" }) return null
        val text = value.trim()
        if (text.isBlank()) return null
        if (text.startsWith("geo:") || text.startsWith("google.navigation:")) return NavigationImport.parse(Intent.ACTION_VIEW, text)
        val link = sharedUrl(text)
        if (link != null || text.startsWith("http", true)) {
            val url = supportedUrl(link ?: text) ?: return null
            if (url.host == "maps.app.goo.gl" || url.host == "goo.gl") return null
            for (key in listOf("destination", "daddr", "query", "q")) {
                val values = url.queryParameterValues(key)
                if (values.size > 1) return null
                values.singleOrNull()?.takeIf { it.isNotBlank() }?.let { return parseAddress(it) }
            }
            val point = Regex("!3d([+\\-0-9.]+)!4d([+\\-0-9.]+)").find(url.toString())
            if (point != null) return parseAddress("${point.groupValues[1]},${point.groupValues[2]}")
            val parts = url.pathSegments
            val placeIndex = parts.indexOf("place")
            if (placeIndex >= 0) return parts.getOrNull(placeIndex + 1)?.replace('+', ' ')?.let(::parseAddress)
            return null // @latitude,longitude describes a camera, not necessarily the shared place.
        }
        if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(text)) return null
        return parseAddress(text.replace(Regex("\\s+"), " "))
    }
    private fun parseAddress(value: String): NavigationImport? {
        if (value.length > 256 || value.startsWith("place_id:")) return null
        return NavigationImport.parse(Intent.ACTION_VIEW, "geo:0,0?q=" + URLEncoder.encode(value, "UTF-8"))
    }

    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(4, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS).callTimeout(5, TimeUnit.SECONDS).build()

    suspend fun resolve(value: String): NavigationImport? = withContext(Dispatchers.IO) {
        parseText(value)?.let { return@withContext it }
        var current = supportedUrl(sharedUrl(value) ?: value) ?: return@withContext null
        if (current.host !in setOf("maps.app.goo.gl", "goo.gl")) return@withContext null
        repeat(4) {
            coroutineContext.ensureActive()
            val (code,location)=sharingResponseHeaders(client.newCall(Request.Builder().url(current).head().build()))
            if (code !in 300..399) return@withContext parseText(current.toString())
            val target = location?.let { current.resolve(it) } ?: return@withContext null
            current = supportedUrl(target.toString()) ?: return@withContext null
            parseText(current.toString())?.let { return@withContext it }
        }
        null
    }

    fun googleDirections(place: Place, mode: TravelMode): String = "https://www.google.com/maps/dir/?api=1&destination=" +
        URLEncoder.encode("${place.point.lat},${place.point.lon}", "UTF-8") + "&travelmode=" + when (mode) {
            TravelMode.BICYCLE -> "bicycling"
            TravelMode.WALK -> "walking"
            TravelMode.MOTORCYCLE -> "two-wheeler"
            TravelMode.CAR -> "driving"
        }

    fun open(context: Context, place: Place, mode: TravelMode) {
        val maps = Intent(Intent.ACTION_VIEW, Uri.parse(googleDirections(place, mode))).setPackage("com.google.android.apps.maps")
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${place.point.lat},${place.point.lon}?q=" + URLEncoder.encode("${place.point.lat},${place.point.lon} (${place.name.take(120)})", "UTF-8")))
        val chooser = Intent.createChooser(generic, "Open in another map app")
        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(android.content.ComponentName(context, MainActivity::class.java)))
        val otherMaps = context.packageManager.queryIntentActivities(generic, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.packageName != context.packageName }
        if (!otherMaps) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(googleDirections(place, mode))))
            return
        }
        if (maps.resolveActivity(context.packageManager) != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(maps))
        try { context.startActivity(chooser) }
        catch (_: android.content.ActivityNotFoundException) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(googleDirections(place, mode)))) }
    }
}
