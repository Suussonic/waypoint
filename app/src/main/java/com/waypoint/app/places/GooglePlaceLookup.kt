/*
 * Keyless place lookup flow adapted from PimpinPumpkin/Vela's documented Maps web endpoints
 * and hidden photo WebView pattern. Copyright (C) 2026 Vela contributors.
 * GPL-3.0-or-later; see LICENSE. Google endpoints and response paths are undocumented and may change.
 */
package com.waypoint.app.places

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.waypoint.app.Coordinate
import com.waypoint.app.JsonSafety
import com.waypoint.app.distance
import com.waypoint.app.isValid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val MAX_GOOGLE_PHOTO_BRIDGE_JSON_CHARS = 64 * 1024

private fun isTrustedGooglePhotoUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    val host = uri.host?.lowercase(Locale.ROOT) ?: return false
    uri.scheme == "https" && (host == "googleusercontent.com" || host.endsWith(".googleusercontent.com"))
}.getOrDefault(false)

internal data class GooglePlaceDetails(
    val name: String,
    val address: String,
    val category: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val price: String?,
    val status: String?,
    val hours: List<String>,
    val phone: String?,
    val website: String?,
    val featureId: String?,
    val previewPhotos: List<String>,
) {
    val mapUrl: String
        get() = googleCid(featureId)?.let { "https://www.google.com/maps?cid=$it" }
            ?: "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(name, "UTF-8")}"
}

/** One opt-in, one request, limited to the place the person opened. No account or Places API key. */
internal class GooglePlaceLookup(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build(),
) {
    suspend fun details(place: NearbyPlace): GooglePlaceDetails? = withContext(Dispatchers.IO) {
        val pb = VelaSearchPb.build(place.name, place.point, spanMeters = 1_500.0)
        val locale = Locale.getDefault()
        val url = "https://www.google.com/search?tbm=map&authuser=0&hl=${enc(locale.language.ifBlank { "en" })}" +
            "&gl=${enc(locale.country.lowercase(Locale.ROOT).ifBlank { "us" })}" +
            "&q=${enc(place.name)}&pb=${enc(pb)}"
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; Waypoint) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*").header("Accept-Language", "${locale.toLanguageTag()},en;q=0.8")
            .header("Referer", "https://www.google.com/maps/")
            .header("X-Requested-With", "com.waypoint.app")
            .build()
        val root = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                if (response.code == 429) "Google place lookup is temporarily limited. Try again later."
                else "Google place details are unavailable (${response.code})."
            }
            val body = response.body.readBoundedBytes(MAX_RESPONSE_BYTES)
            val text = body.toString(Charsets.UTF_8).removePrefix(")]}'\n").removePrefix(")]}'")
            JsonSafety.checkDepth(text)
            JSONObjectOrArray.parse(text)
        }
        val rows = (root as? JSONArray)?.optJSONArray(64) ?: return@withContext null
        val candidates = (0 until rows.length()).mapNotNull { index ->
            val entry = rows.optJSONArray(index) ?: return@mapNotNull null
            val parsed = parseEntry(entry) ?: return@mapNotNull null
            val proximity = distance(place.point, parsed.first)
            if (!proximity.isFinite() || proximity > MAX_MATCH_DISTANCE_METERS) return@mapNotNull null
            val score = matchScore(place.name, parsed.second.name)
            if (score < MIN_NAME_MATCH) return@mapNotNull null
            (proximity - score * 80.0) to parsed.second
        }
        candidates.minByOrNull { it.first }?.second
    }

    private fun parseEntry(entry: JSONArray): Pair<Coordinate, GooglePlaceDetails>? {
        val name = stringAt(entry, 1, 11)?.takeIf(String::isNotBlank) ?: return null
        val lat = numberAt(entry, 1, 9, 2) ?: return null
        val lon = numberAt(entry, 1, 9, 3) ?: return null
        val point = Coordinate(lat, lon).takeIf(Coordinate::isValid) ?: return null
        val address = stringAt(entry, 1, 39)?.takeIf(String::isNotBlank)
            ?: stringsAt(entry, 1, 2).filterNot { it.equals(name, ignoreCase = true) }.distinct().joinToString(", ")
        val status = (stringAt(entry, 1, 203, 1, 4, 0) ?: stringAt(entry, 1, 203, 1, 8, 0))?.take(100)
        val featureId = stringAt(entry, 1, 10)?.takeIf { it.length <= 100 }
        val details = GooglePlaceDetails(
            name = name.take(160), address = address.take(320),
            category = stringAt(entry, 1, 13, 0)?.take(100),
            rating = numberAt(entry, 1, 4, 7)?.takeIf { it in 0.0..5.0 },
            reviewCount = stringAt(entry, 1, 4, 8)?.filter(Char::isDigit)?.toIntOrNull()?.takeIf { it in 0..100_000_000 },
            price = stringAt(entry, 1, 4, 2)?.take(40), status = status,
            hours = parseHours(arrayAt(entry, 1, 203, 0)),
            phone = stringAt(entry, 1, 178, 0, 0)?.take(80),
            website = stringAt(entry, 1, 7, 0)?.takeIf(::safeWebUrl)?.take(300),
            featureId = featureId,
            previewPhotos = photoUrls(arrayAt(entry, 1, 72, 0)).take(8),
        )
        return point to details
    }

    private fun photoUrls(photos: JSONArray?): List<String> {
        if (photos == null) return emptyList()
        return (0 until minOf(photos.length(), 32)).mapNotNull { index ->
            val photo = photos.optJSONArray(index) ?: return@mapNotNull null
            val url = stringAt(photo, 6, 0)?.takeIf(::isTrustedGooglePhotoUrl) ?: return@mapNotNull null
            url.replace(Regex("=w\\d+-h\\d+.*$"), "=w640-h480")
        }.distinct()
    }

    private fun parseHours(days: JSONArray?): List<String> {
        if (days == null) return emptyList()
        return (0 until minOf(days.length(), 7)).mapNotNull { index ->
            val day = days.optJSONArray(index) ?: return@mapNotNull null
            val name = stringAt(day, 0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val ranges = arrayAt(day, 3) ?: return@mapNotNull null
            val hours = (0 until minOf(ranges.length(), 6)).mapNotNull { range -> stringAt(ranges.optJSONArray(range), 0)?.takeIf(String::isNotBlank) }
                .distinct().joinToString(", ")
            if (hours.isBlank()) null else "$name: $hours"
        }
    }

    private fun stringsAt(root: JSONArray, vararg path: Int): List<String> {
        val array = arrayAt(root, *path) ?: return emptyList()
        return (0 until minOf(array.length(), 24)).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() && s != "null" } }
    }

    private fun stringAt(root: JSONArray?, vararg path: Int): String? {
        val value = at(root, *path) ?: return null
        return when (value) {
            is String -> value.takeIf { it.isNotBlank() && it != "null" }
            is Number -> value.toString()
            is JSONArray -> value.optString(0).takeIf { it.isNotBlank() && it != "null" }
            else -> null
        }
    }

    private fun numberAt(root: JSONArray, vararg path: Int): Double? = when (val value = at(root, *path)) {
        is Number -> value.toDouble().takeIf(Double::isFinite)
        is String -> value.toDoubleOrNull()?.takeIf(Double::isFinite)
        else -> null
    }

    private fun arrayAt(root: JSONArray?, vararg path: Int): JSONArray? = at(root, *path) as? JSONArray

    private fun at(root: JSONArray?, vararg path: Int): Any? {
        var value: Any? = root
        for (index in path) value = (value as? JSONArray)?.opt(index) ?: return null
        return value
    }

    private fun matchScore(expected: String, actual: String): Double {
        fun words(value: String) = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .split(Regex("\\s+")).filter { it.length >= 3 && it !in setOf("the", "and", "shop", "store", "restaurant", "place") }.toSet()
        val a = words(expected); val b = words(actual)
        if (a.isEmpty() || b.isEmpty()) return if (expected.equals(actual, true)) 1.0 else 0.0
        return a.intersect(b).size.toDouble() / a.size
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun safeWebUrl(value: String): Boolean = runCatching {
        val uri = Uri.parse(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    companion object {
        private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
        private const val MAX_MATCH_DISTANCE_METERS = 2_500.0
        private const val MIN_NAME_MATCH = 0.5
    }
}

private object JSONObjectOrArray {
    fun parse(text: String): Any = when (text.trimStart().firstOrNull()) {
        '[' -> JSONArray(text)
        '{' -> JSONObject(text)
        else -> error("Google returned an unexpected place response.")
    }
}

private fun googleCid(featureId: String?): String? = runCatching {
    val low = featureId?.substringAfter(':', "")?.removePrefix("0x")?.takeIf { it.isNotBlank() } ?: return null
    BigInteger(low, 16).toString()
}.getOrNull()

/** Off-screen page fetch, limited to Google's own Maps host and to photos for one opened place. */
internal class GoogleMapPhotoFetcher(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val mutex = Mutex()
    private val bridgeName = "_waypoint${UUID.randomUUID().toString().replace("-", "").take(12)}"
    @Volatile private var webView: WebView? = null
    @Volatile private var requestId: String? = null
    @Volatile private var result: CompletableDeferred<List<String>>? = null

    suspend fun photos(details: GooglePlaceDetails): List<String> {
        val cid = googleCid(details.featureId) ?: return details.previewPhotos
        return mutex.withLock {
            val id = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<List<String>>()
            requestId = id
            result = deferred
            try {
                withContext(Dispatchers.Main) {
                    val view = ensureWebView()
                    val locale = Locale.getDefault()
                    view.loadUrl("https://www.google.com/maps?cid=$cid&hl=${enc(locale.language.ifBlank { "en" })}&gl=${enc(locale.country.lowercase(Locale.ROOT).ifBlank { "us" })}")
                }
                withTimeoutOrNull(22_000L) { deferred.await() }.orEmpty().ifEmpty { details.previewPhotos }
            } finally {
                requestId = null
                result = null
                withContext(Dispatchers.Main) {
                    webView?.let { view ->
                        runCatching { view.stopLoading(); view.loadUrl("about:blank"); view.onPause() }
                    }
                }
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onResult(id: String, json: String) {
            if (id != requestId) return
            if (json.length > MAX_GOOGLE_PHOTO_BRIDGE_JSON_CHARS) {
                result?.complete(emptyList())
                return
            }
            val photos = runCatching {
                val array = JSONArray(json)
                (0 until minOf(array.length(), 32)).mapNotNull { index ->
                    array.optString(index).takeIf { isTrustedGooglePhotoUrl(it) && !it.contains("streetviewpixels") }
                }.distinct()
            }.getOrDefault(emptyList())
            result?.complete(photos)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView(): WebView {
        webView?.let { return it }
        val view = WebView(appContext)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = false
        view.settings.setGeolocationEnabled(false)
        view.settings.mediaPlaybackRequiresUserGesture = true
        view.settings.setSupportZoom(false)
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        view.addJavascriptInterface(Bridge(), bridgeName)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return true
                return !allowedGoogleHost(uri)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = !allowedGoogleHost(Uri.parse(url ?: ""))

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view == null || requestId == null || !allowedGoogleHost(Uri.parse(url ?: ""))) return
                val id = requestId ?: return
                main.postDelayed({
                    if (requestId == id) view.evaluateJavascript(photoScraper(id, bridgeName), null)
                }, 1_300L)
            }
        }
        // Like Vela, the page is measured off-screen so Google's photo rail can populate; it is never attached to a window.
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(900, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1_800, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 900, 1_800)
        webView = view
        return view
    }

    suspend fun close() = withContext(Dispatchers.Main) {
        webView?.let { view ->
            runCatching { view.stopLoading(); view.removeJavascriptInterface(bridgeName); view.loadUrl("about:blank"); view.destroy() }
        }
        webView = null
        requestId = null
        result?.complete(emptyList())
        result = null
    }

    private fun allowedGoogleHost(uri: Uri): Boolean = uri.scheme == "https" &&
        (uri.host == "google.com" || uri.host?.endsWith(".google.com") == true)

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun photoScraper(id: String, bridge: String): String {
        val safeId = JSONObject.quote(id)
        val bridgeRef = "window[${JSONObject.quote(bridge)}]"
        return """
            (function(){
              var id=$safeId, bridge=$bridgeRef, tries=0, found={}, stable=0, last=0, opened=false;
              function key(u){return u.replace(/=[wshpc]\d+.*$/,'').replace(/=s\d+.*$/,'');}
              function ok(u){try{var a=new URL(u);return a.protocol==='https:' && /(^|\.)googleusercontent\.com$/i.test(a.hostname) &&
                !/streetviewpixels|\/a[-/]|ACg8oc|ALV-/.test(u) && !/=s(16|24|32|40|48|64|80|96|128)(-|$)/.test(u);}catch(e){return false;}}
              function get(el){var u=el.currentSrc||el.src||''; if(!u && el.style)u=el.style.backgroundImage||'';
                if(!u){try{u=getComputedStyle(el).backgroundImage||'';}catch(e){}}
                var m=u.match(/url\([\"']?([^\"')]+)/);return m?m[1]:u;}
              function collect(){[].slice.call(document.querySelectorAll('img,[role="img"],[style*="background"]')).forEach(function(el){
                var u=get(el);if(ok(u)){var k=key(u);if(!found[k])found[k]=u;}});}
              function openPhotos(){if(opened)return; var all=[].slice.call(document.querySelectorAll('button,[role="button"],a'));
                for(var i=0;i<all.length;i++){var label=((all[i].getAttribute('aria-label')||'')+' '+(all[i].textContent||'')).trim().toLowerCase();
                  if(/(^|\\s)(photos?|all photos)(\\s|$)/.test(label)&&!/street ?view|review|profile|video/.test(label)){try{all[i].click();opened=true;}catch(e){}return;}}}
              function scroll(){[].slice.call(document.querySelectorAll('div')).forEach(function(d){if(d.scrollHeight>d.clientHeight+300&&d.clientHeight>100){d.scrollTop=d.scrollHeight;}});}
              function finish(){var out=Object.keys(found).map(function(k){return found[k].replace(/=w\\d+-h\\d+.*$/,'=w640-h480').replace(/=s\\d+.*$/,'=s640');}).slice(0,24);
                try{bridge.onResult(id,JSON.stringify(out));}catch(e){}}
              function tick(){if(tries++>18){finish();return;}collect();openPhotos();scroll();var n=Object.keys(found).length;
                stable=n===last?stable+1:0;last=n;if(n>=8&&stable>=2){finish();return;}setTimeout(tick,650);}
              tick();
            })();
        """.trimIndent()
    }
}
