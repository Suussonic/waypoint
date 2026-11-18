package com.waypoint.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.random.Random

private const val TAG = "WorldClips"

/**
 * Pre-rendered Trevor clips for Los Santos navigation. Each instruction is spoken as
 * `distance clip + manoeuvre clip`, chained without a gap; street names are intentionally not voiced.
 * Other worlds and any instruction without a matching clip use Android text-to-speech.
 *
 * The distance grids below match the generated Los Santos clip set.
 */
internal object ClipPlan {
    private val gridMetres = listOf(20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 140, 150, 160, 180, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900)
    private val gridKm = listOf(1.0 to "1", 1.5 to "1p5", 2.0 to "2", 3.0 to "3", 4.0 to "4", 5.0 to "5", 10.0 to "10")
    private val gridFeet = listOf(50, 100, 150, 200, 250, 300, 400, 500, 600, 800)
    private val gridMiles = listOf(0.1 to "0p1", 0.2 to "0p2", 0.25 to "0p25", 0.3 to "0p3", 0.4 to "0p4", 0.5 to "0p5", 0.6 to "0p6",
        0.7 to "0p7", 0.75 to "0p75", 0.8 to "0p8", 0.9 to "0p9", 1.0 to "1", 1.5 to "1p5", 2.0 to "2", 3.0 to "3", 5.0 to "5")

    private val numberWords = mapOf("a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0, "half a" to 0.5, "a half" to 0.5, "a quarter" to 0.25,
        "a quarter of a" to 0.25, "three quarters of a" to 0.75, "three quarters" to 0.75, "one and a half" to 1.5)
    private val distance = Regex(
        """\b(?:in|after|for|then in)\s+(three quarters of a|three quarters|a quarter of a|a quarter|one and a half|half a|a half|an?|one|two|three|four|five|six|seven|eight|nine|ten|\d+(?:[.,]\d+)?)\s*(kilometres?|kilometers?|km|metres?|meters?|m|feet|foot|ft|miles?|mi)\b""",
        RegexOption.IGNORE_CASE)

    private fun nearestMetres(m: Double) = "dist_m_${gridMetres.minByOrNull { abs(it - m) }}"
    private fun nearestFeet(f: Double) = "dist_ft_${gridFeet.minByOrNull { abs(it - f) }}"
    private fun nearestKm(k: Double) = "dist_km_${gridKm.minByOrNull { abs(it.first - k) }!!.second}"
    private fun nearestMiles(mi: Double) = "dist_mi_${gridMiles.minByOrNull { abs(it.first - mi) }!!.second}"

    /** Ferrostar speaks "Turn left" with the distance only in the trigger distance, so snap that to a clip. */
    fun distanceFromTrigger(meters: Double, imperial: Boolean): String {
        if (!meters.isFinite() || meters < 25.0) return "dist_now"
        if (imperial) {
            val feet = meters * 3.28084
            return if (feet < 900.0) nearestFeet(feet) else nearestMiles(meters / 1609.344)
        }
        return if (meters < 950.0) nearestMetres(meters) else nearestKm(meters / 1000.0)
    }

    fun plan(text: String, random: Random = Random.Default, available: Set<String>,
             triggerMeters: Double? = null, imperial: Boolean = false, allowCatch: Boolean = false): List<String>? {
        val t = text.lowercase()
        val clips = mutableListOf<String>()
        val man = maneuver(t) ?: return null
        val isEvent = man.startsWith("evt_")
        if (!isEvent) {
            val dist = distanceClip(t) ?: (triggerMeters?.let { distanceFromTrigger(it, imperial) } ?: "dist_now")
            // A whole-sentence recording keeps one natural intonation; fall back to stitching two clips for rarer distances.
            val whole = "comb_${dist.removePrefix("dist_")}__${man.removePrefix("man_")}"
            if (whole in available) clips += whole else { clips += dist; clips += man }
        } else clips += man
        if (allowCatch) {
            val catches = available.filter { it.startsWith("catch_") }
            if (catches.isNotEmpty()) clips += catches.random(random)
        }
        return clips.takeIf { c -> c.all { it in available } }
            ?: clips.filter { it in available }.takeIf { it.isNotEmpty() && man in available }
    }

    private fun maneuver(t: String): String? {
        val side = when { "left" in t -> "left"; "right" in t -> "right"; else -> null }
        return when {
            Regex("""arriv|reached your destination|destination""").containsMatchIn(t) ->
                when { "on the left" in t -> "evt_arrive_left"; "on the right" in t -> "evt_arrive_right"; else -> "evt_arrive" }
            Regex("""recalculat|rerout|new route""").containsMatchIn(t) -> "evt_reroute"
            "u-turn" in t || "u turn" in t || "turn around" in t -> "man_uturn"
            "roundabout" in t || "rotary" in t || "traffic circle" in t -> {
                val n = Regex("""exit\s*(\d)|(\d)(?:st|nd|rd|th)|\b(first|second|third|fourth)\b""").find(t)?.let {
                    it.groupValues[1].toIntOrNull() ?: it.groupValues[2].toIntOrNull()
                        ?: when (it.groupValues[3]) { "first" -> 1; "second" -> 2; "third" -> 3; else -> 4 }
                }
                if (n != null && n in 1..4) "man_roundabout_$n" else "man_roundabout"
            }
            "merge" in t -> "man_merge"
            Regex("""\b(exit|ramp)\b""").containsMatchIn(t) && side != null -> "man_exit_$side"
            "keep" in t && side != null -> "man_keep_$side"
            "sharp" in t && side != null -> "man_sharp_$side"
            Regex("""slight|bear|veer|fork""").containsMatchIn(t) && side != null -> "man_slight_$side"
            "turn" in t && side != null -> "man_$side"
            Regex("""continue|straight|head|proceed""").containsMatchIn(t) -> "man_straight"
            side != null -> "man_$side"
            else -> null
        }
    }

    private fun distanceClip(t: String): String? {
        val m = distance.find(t) ?: return null
        val q = m.groupValues[1]
        val unit = m.groupValues[2]
        val n = numberWords[q] ?: q.replace(',', '.').toDoubleOrNull() ?: return null
        return when {
            unit.startsWith("k") -> if (n < 0.95) nearestMetres(n * 1000.0) else nearestKm(n)
            unit.startsWith("mi") -> if (n < 0.08) nearestFeet(n * 5280.0) else nearestMiles(n)
            unit == "ft" || unit.startsWith("f") -> if (n >= 900.0) nearestMiles(n / 5280.0) else nearestFeet(n)
            else -> if (n >= 950.0) nearestKm(n / 1000.0) else nearestMetres(n)
        }
    }
}

internal interface WorldClipPlayer {
    /** True when clips are playing (the caller must not also speak); false means use the normal voice. */
    fun play(world: World, text: String, triggerMeters: Double?, onFailure: () -> Unit): Boolean
    fun stop()
    fun release()
}

internal class AssetClipVoice(context: Context) : WorldClipPlayer {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    // Preparing a few tiny files is quick, but never on the main thread.
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "waypoint-clip-voice").apply { isDaemon = true } }
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    /** world -> base clip id -> concrete ids (a clip may have several recorded variants, suffixed __1, __2, ...). */
    private val manifests = HashMap<World, Map<String, List<String>>>()
    private val lastVariant = HashMap<String, String>()
    @Volatile private var token = 0L
    private var lastCatchAt = 0L
    @Volatile private var lastArriveAt = 0L
    private var players: List<MediaPlayer> = emptyList()
    private var focus: AudioFocusRequest? = null

    private fun clips(world: World): Map<String, List<String>> = manifests.getOrPut(world) {
        runCatching {
            val raw = app.assets.open("voice/${world.name}/manifest.json").bufferedReader().use { it.readText() }
            Regex("\"([a-z0-9_]+)\"").findAll(raw).map { it.groupValues[1] }.toList()
                .groupBy { it.replace(Regex("__\\d+$"), "") }
        }.getOrDefault(emptyMap())
    }

    /** A random variant of [base], never the one used last time for this world when another exists. */
    private fun pick(world: World, base: String, available: Map<String, List<String>>): String {
        val options = available[base].orEmpty()
        if (options.size <= 1) return options.firstOrNull() ?: base
        val key = "${world.name}/$base"
        val choice = options.filter { it != lastVariant[key] }.random()
        lastVariant[key] = choice
        return choice
    }

    override fun play(world: World, text: String, triggerMeters: Double?, onFailure: () -> Unit): Boolean {
        if (world != World.LOS_SANTOS) return false
        val available = clips(world)
        if (available.isEmpty()) { Log.d(TAG, "${world.name} no-clips fallback: $text"); return false }
        val imperial = app.getSharedPreferences("waypoint", 0).getBoolean("imperial", false)
        val now = android.os.SystemClock.elapsedRealtime()
        // Catchphrases are a treat, not a habit: only with time to spare (or on a reroute/arrival), at most once
        // every 45 s, and about one instruction in four.
        val roomForCatch = (triggerMeters ?: 0.0) >= 150.0 || text.contains("arriv", true) || text.contains("recalc", true)
        val allowCatch = roomForCatch && now - lastCatchAt > 45_000L && Random.nextInt(4) == 0
        val plan = ClipPlan.plan(text, available = available.keys, triggerMeters = triggerMeters, imperial = imperial, allowCatch = allowCatch)
        if (plan == null) { Log.d(TAG, "${world.name} unparsed fallback: $text"); return false }
        // The app now speaks arrival itself and Ferrostar may too; say it once.
        if (plan.first().startsWith("evt_arrive")) {
            if (now - lastArriveAt < 15_000L) { Log.d(TAG, "${world.name} duplicate arrival skipped"); return true }
            lastArriveAt = now
        }
        if (plan.any { it.startsWith("catch_") }) lastCatchAt = now
        val resolved = plan.map { pick(world, it, available) }
        Log.d(TAG, "${world.name} play $resolved <- $text (trigger=${triggerMeters?.toInt()})")
        val mine = ++token
        worker.execute { start(world, resolved, mine, onFailure) }
        return true
    }

    /** Prepares every clip first, links them with setNextMediaPlayer so they play back to back, then starts the first. */
    private fun start(world: World, ids: List<String>, mine: Long, onFailure: () -> Unit) {
        main.post { if (mine == token) releasePlayers() }
        val prepared = ArrayList<MediaPlayer>()
        for (id in ids) {
            val mp = MediaPlayer()
            val ok = runCatching {
                app.assets.openFd("voice/${world.name}/$id.ogg").use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                mp.setAudioAttributes(attrs)
                mp.prepare()
            }.isSuccess
            if (ok) prepared += mp else { Log.d(TAG, "${world.name} could not prepare $id"); runCatching { mp.release() } }
        }
        if (prepared.isEmpty()) { Log.d(TAG, "${world.name} nothing prepared for $ids"); main.post { if (mine == token) onFailure() }; return }
        for (i in 0 until prepared.size - 1) prepared[i].setNextMediaPlayer(prepared[i + 1])
        prepared.forEachIndexed { i, mp ->
            mp.setOnCompletionListener {
                Log.d(TAG, "${world.name} finished ${ids[i]}")
                runCatching { it.release() }
                if (i == prepared.size - 1) main.post { if (mine == token) finish() }
            }
            mp.setOnErrorListener { p, what, extra -> Log.d(TAG, "${world.name} error on ${ids[i]}: $what/$extra"); runCatching { p.release() }; if (i == prepared.size - 1) main.post { finish() }; true }
        }
        main.post {
            if (mine != token) { Log.d(TAG, "${world.name} superseded before start: ${ids.first()}"); prepared.forEach { runCatching { it.release() } }; return@post }
            players = prepared
            val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener { }.build()
            val granted = focus?.let { audio.requestAudioFocus(it) }
            Log.d(TAG, "${world.name} starting ${ids.first()} (${prepared.first().duration} ms) focus=$granted volume=${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}")
            runCatching { prepared.first().start() }.onFailure { Log.d(TAG, "start failed: $it"); releasePlayers(); finish(); onFailure() }
        }
    }

    private fun finish() {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun releasePlayers() {
        players.forEach { runCatching { it.stop() }; runCatching { it.release() } }
        players = emptyList()
    }

    private fun stopNow() {
        token++
        releasePlayers()
        finish()
    }

    override fun stop() {
        // Judge at run time, not call time: Ferrostar's stop is queued just before the arrival line is requested, so it
        // runs after that request and must not cancel it.
        main.post {
            if (android.os.SystemClock.elapsedRealtime() - lastArriveAt < 8_000L) { Log.d(TAG, "stop ignored, arrival line is playing"); return@post }
            stopNow()
        }
    }
    override fun release() { main.post { stopNow() }; worker.shutdown() }
}
