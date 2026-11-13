/*
 * Google Maps web autocomplete adapted from PimpinPumpkin/Vela:
 * core/src/main/java/app/vela/core/data/google/SuggestParser.kt and Calibration.kt.
 * Copyright (C) 2026 Vela contributors. GPL-3.0-or-later; see LICENSE.
 * This uses undocumented, keyless Google web endpoints; their shape may change.
 */
package com.waypoint.app.places

import com.waypoint.app.Coordinate
import com.waypoint.app.JsonSafety
import com.waypoint.app.Place
import com.waypoint.app.isValid
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal data class GoogleMapSuggestResult(val places: List<Place>, val queries: List<String>)

/** Debounced by the search screen; one cancellable request for the current input. */
internal class GoogleMapSuggestions(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun suggest(query: String, near: Coordinate): GoogleMapSuggestResult {
        val term = query.trim().take(160)
        val directLocation = term.startsWith("https:", true) || term.startsWith("geo:", true) ||
            COORDINATE_QUERY.matches(term)
        if (term.length < 2 || directLocation || !near.isValid()) return GoogleMapSuggestResult(emptyList(), emptyList())
        val locale = Locale.getDefault()
        val span = if (term.firstOrNull()?.isDigit() == true) ADDRESS_SPAN_METERS else DEFAULT_SPAN_METERS
        val pb = SUGGEST_PB
            .replace("{SPAN}", span.toString())
            .replace("{LNG}", near.lon.toString())
            .replace("{LAT}", near.lat.toString())
            .replace("{W}", "1024")
            .replace("{H}", "768")
        val endpoint = "https://www.google.com/s?tbm=map&gs_ri=maps&suggest=p&authuser=0" +
            "&hl=${enc(locale.language.ifBlank { "en" })}&gl=${enc(locale.country.lowercase(Locale.ROOT).ifBlank { "us" })}"
        val url = "$endpoint&pb=${enc(pb)}&q=${enc(term)}&tch=1&ech=${sequence.incrementAndGet()}"
        val request = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Language", "${locale.toLanguageTag()},en;q=0.8")
            .header("Referer", "https://www.google.com/maps/")
            .header("X-Requested-With", "com.waypoint.app")
            .build()
        return SuggestResponseParser.parse(request(request))
    }

    private suspend fun request(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.use {
                        check(it.isSuccessful) {
                            if (it.code == 429) "Google suggestions are temporarily limited."
                            else "Google suggestions are unavailable (${it.code})."
                        }
                        val source = it.body
                        require(source.contentLength() < 0 || source.contentLength() <= MAX_RESPONSE_BYTES) {
                            "Google suggestions response is too large."
                        }
                        val output = ByteArrayOutputStream()
                        source.byteStream().use { input ->
                            val buffer = ByteArray(8 * 1024)
                            while (true) {
                                if (!continuation.isActive) throw IOException("Suggestion request cancelled")
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= MAX_RESPONSE_BYTES) { "Google suggestions response is too large." }
                                output.write(buffer, 0, count)
                            }
                        }
                        output.toString(Charsets.UTF_8.name())
                    }
                    if (continuation.isActive) continuation.resumeWith(Result.success(body))
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }
            }
        })
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
        const val DEFAULT_SPAN_METERS = 20_000
        const val ADDRESS_SPAN_METERS = 40_000
        val COORDINATE_QUERY = Regex("^[+-]?[0-9.]+\\s*,\\s*[+-]?[0-9.]*\\s*(?:\\([^()]*\\))?$")
        const val SUGGEST_PB = "!2i5!4m12!1m3!1d{SPAN}!2d{LNG}!3d{LAT}!2m3!1f0!2f0!3f0!3m2!1i{W}!2i{H}!4f13.1" +
            "!7i20!10b1!12m6!1m2!18b1!30b1!2m2!1i203!2i100!19m4!1m3!1i1!2i1!3i1!20m1!1e1"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15; Waypoint) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"
        val sequence = AtomicInteger(0)
    }
}

/** Defensive positional decoder adapted from Vela's SuggestParser and its calibration paths. */
private object SuggestResponseParser {
    private val featureId = Regex("^0x[0-9a-f]+:0x[0-9a-f]+$", RegexOption.IGNORE_CASE)

    fun parse(body: String): GoogleMapSuggestResult {
        val embedded = unwrap(body) ?: return GoogleMapSuggestResult(emptyList(), emptyList())
        val payload = stripGuard(embedded)
        if (runCatching { JsonSafety.checkDepth(payload) }.isFailure) return GoogleMapSuggestResult(emptyList(), emptyList())
        val root = runCatching { JSONArray(payload) }.getOrNull() ?: return GoogleMapSuggestResult(emptyList(), emptyList())
        val rows = root.optJSONArray(0)?.optJSONArray(1) ?: return GoogleMapSuggestResult(emptyList(), emptyList())
        val places = ArrayList<Place>()
        val queries = ArrayList<String>()
        for (index in 0 until minOf(rows.length(), 40)) {
            val row = rows.optJSONArray(index) ?: continue
            val block = findContentBlock(row) ?: continue
            val title = stringAt(block, 0, 0)?.takeIf(String::isNotBlank) ?: continue
            val primary = stringAt(block, 1, 0)?.takeIf(String::isNotBlank) ?: title
            val secondary = stringAt(block, 2, 0)?.takeIf(String::isNotBlank)
            val lat = numberAt(block, 11, 2) ?: numberAt(block, 13, 0, 3, 2)
            val lon = numberAt(block, 11, 3) ?: numberAt(block, 13, 0, 3, 3)
            if (lat == null || lon == null) {
                queries.add(title.take(120))
                continue
            }
            val point = Coordinate(lat, lon).takeIf(Coordinate::isValid) ?: continue
            val fid = stringAt(block, 13, 0, 0)?.takeIf(featureId::matches)
            places += Place(
                id = fid ?: "google-suggest:${primary.hashCode()}:${"%.5f".format(Locale.ROOT, lat)},${"%.5f".format(Locale.ROOT, lon)}",
                name = primary.take(160),
                address = secondary?.takeIf { !it.equals(primary, ignoreCase = true) }?.take(240).orEmpty(),
                point = point,
                kind = "place",
                source = "Google Maps suggestions",
            )
        }
        return GoogleMapSuggestResult(
            places.distinctBy { it.id }.take(8),
            queries.distinct().take(3),
        )
    }

    private fun findContentBlock(row: JSONArray): JSONArray? {
        for (index in 0 until row.length()) {
            val candidate = row.optJSONArray(index) ?: continue
            val first = candidate.optJSONArray(0) ?: continue
            if (first.opt(0) is String) return candidate
        }
        return null
    }

    private fun stringAt(root: JSONArray?, vararg path: Int): String? = when (val value = at(root, *path)) {
        is String -> value.takeIf { it.isNotBlank() && it != "null" }
        is Number -> value.toString()
        else -> null
    }

    private fun numberAt(root: JSONArray?, vararg path: Int): Double? = when (val value = at(root, *path)) {
        is Number -> value.toDouble().takeIf(Double::isFinite)
        is String -> value.toDoubleOrNull()?.takeIf(Double::isFinite)
        else -> null
    }

    private fun at(root: JSONArray?, vararg path: Int): Any? {
        var current: Any? = root
        for (index in path) current = (current as? JSONArray)?.opt(index) ?: return null
        return current
    }

    private fun unwrap(body: String): String? {
        val start = body.indexOf('{').takeIf { it >= 0 } ?: return null
        val end = firstObjectEnd(body, start) ?: return null
        val data = runCatching { JSONObject(body.substring(start, end + 1)).optString("d") }.getOrNull()
        return data?.takeIf(String::isNotBlank)
    }

    private fun firstObjectEnd(text: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') inString = false
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> if (--depth == 0) return index
            }
        }
        return null
    }

    private fun stripGuard(value: String): String {
        for (guard in listOf(")]}'\n", ")]}'", ")]}", "while(1);", "for(;;);")) {
            if (value.startsWith(guard)) return value.removePrefix(guard).trimStart('\n', '\r')
        }
        return value
    }
}
