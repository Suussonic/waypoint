package com.waypoint.app

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.core.SpokenInstructionObserver
import kotlinx.coroutines.flow.StateFlow
import uniffi.ferrostar.SpokenInstruction
import java.util.concurrent.atomic.AtomicReference
import java.util.Locale

internal enum class GuidanceVoiceStatus(val problem: String? = null) {
    IDLE, STARTING, READY,
    ENGINE_UNAVAILABLE("Android text-to-speech could not start. Visual guidance remains active. Check Android speech settings, then turn voice off and on to retry."),
    LANGUAGE_UNAVAILABLE("No English text-to-speech voice is available. Choose or download an English voice in Android settings. Visual guidance remains active."),
    PLAYBACK_FAILED("Android text-to-speech playback failed. Check speech settings and navigation volume, then turn voice off and on to retry."),
}

/** Handles asynchronous engine startup without replaying a backlog of obsolete turns. */
internal class GuidanceVoice(
    private val delegate: SpokenInstructionObserver,
    private val initialize: ((GuidanceVoiceStatus) -> Unit) -> Unit,
    private val release: () -> Unit,
    private val onStatus: (GuidanceVoiceStatus) -> Unit,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) : SpokenInstructionObserver {
    @Volatile var status = GuidanceVoiceStatus.IDLE
        private set
    private var generation = 0L
    private var closed = false
    private var pending: Pair<SpokenInstruction, Long>? = null
    override val muteState: StateFlow<Boolean> get() = delegate.muteState

    @Synchronized fun prepare() {
        if (closed || status == GuidanceVoiceStatus.STARTING || status == GuidanceVoiceStatus.READY) return
        val ticket = ++generation
        status = GuidanceVoiceStatus.STARTING
        onStatus(status)
        try {
            initialize { next -> initialized(ticket, next) }
        } catch (_: Exception) {
            initialized(ticket, GuidanceVoiceStatus.ENGINE_UNAVAILABLE)
        }
    }

    @Synchronized private fun initialized(ticket: Long, next: GuidanceVoiceStatus) {
        if (closed || ticket != generation) return
        status = next
        onStatus(next)
        val waiting = pending
        pending = null
        if (next == GuidanceVoiceStatus.READY && !isMuted && waiting != null &&
            nowMillis() - waiting.second in 0..10_000) {
            delegate.onSpokenInstructionTrigger(waiting.first)
        }
    }

    @Synchronized override fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction) {
        if (closed || isMuted) return
        when (status) {
            GuidanceVoiceStatus.READY -> delegate.onSpokenInstructionTrigger(spokenInstruction)
            GuidanceVoiceStatus.STARTING -> {
                pending = spokenInstruction to nowMillis()
            }
            else -> Unit
        }
    }

    @Synchronized override fun stopAndClearQueue() {
        pending = null
        delegate.stopAndClearQueue()
    }

    @Synchronized override fun setMuted(isMuted: Boolean) {
        delegate.setMuted(isMuted)
        if (isMuted) stopAndClearQueue() else prepare()
    }

    @Synchronized fun shutdown() {
        closed = true
        generation++
        stopAndClearQueue()
        release()
        status = GuidanceVoiceStatus.IDLE
    }
}

internal fun androidGuidanceVoice(context: Context, world: () -> World? = { null },
                                  onStatus: (GuidanceVoiceStatus) -> Unit): GuidanceVoice {
    val delegate = AndroidTtsObserver(context)
    val baselineVoice = AtomicReference<Voice?>(null)
    fun applyTone(tts: TextToSpeech, style: WorldVoiceStyle?) {
        // Preserve Android's speech preferences as the baseline. Unsupported
        // voice or tone controls must never prevent a valid turn from being spoken.
        runCatching {
            val originalVoice = baselineVoice.get() ?: tts.defaultVoice
            val preferred = chooseEnglishVoice(tts, style, originalVoice)
            if (preferred != null && tts.setVoice(preferred) == TextToSpeech.ERROR) {
                originalVoice?.let(tts::setVoice)
            }
            val pitch = Settings.Secure.getInt(context.contentResolver,Settings.Secure.TTS_DEFAULT_PITCH,100)
                .takeIf { it in 10..600 }?.div(100f) ?: 1f
            val rate = Settings.Secure.getInt(context.contentResolver,Settings.Secure.TTS_DEFAULT_RATE,100)
                .takeIf { it in 10..600 }?.div(100f) ?: 1f
            if(tts.setPitch(pitch * (style?.pitch ?: 1f)) == TextToSpeech.ERROR)tts.setPitch(pitch)
            if(tts.setSpeechRate(rate * (style?.pace ?: 1f)) == TextToSpeech.ERROR)tts.setSpeechRate(rate)
        }
    }
    val fxVoice = AndroidFxVoice(context, ::applyTone)
    val speaker = WorldGuidanceSpeaker(delegate, world, tone = { style ->
        delegate.tts?.let { tts -> applyTone(tts, style) }
    }, fxVoice = fxVoice)
    val main = Handler(Looper.getMainLooper())
    return GuidanceVoice(speaker, initialize = { report ->
        // Ferrostar 0.59.0 does not start TTS in its constructor. Release failed
        // instances before retrying; install the listener before explicitly starting.
        delegate.statusObserver = null
        delegate.shutdown()
        delegate.statusObserver = object : AndroidTtsStatusListener {
            override fun onTtsInitialized(tts: TextToSpeech?, status: Int) {
                main.post {
                    val result = if (tts == null || status != TextToSpeech.SUCCESS) {
                        GuidanceVoiceStatus.ENGINE_UNAVAILABLE
                    } else runCatching {
                        tts.setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        // Route instructions are currently English. Prefer the user's
                        // English dialect, then any installed English variant.
                        val preferred = context.resources.configuration.locales[0]
                        val locales = buildList {
                            if (preferred.language == "en") add(preferred)
                            tts.defaultVoice?.locale?.takeIf { it.language == "en" }?.let(::add)
                            add(Locale.ENGLISH)
                            addAll(tts.availableLanguages.orEmpty().filter { it.language == "en" }.sortedBy { it.toLanguageTag() })
                        }.distinct()
                        if (locales.any { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }) {
                            baselineVoice.set(tts.voice ?: tts.defaultVoice)
                            GuidanceVoiceStatus.READY
                        } else GuidanceVoiceStatus.LANGUAGE_UNAVAILABLE
                    }.getOrDefault(GuidanceVoiceStatus.ENGINE_UNAVAILABLE)
                    report(result)
                }
            }
            override fun onTtsShutdownAndRelease() = Unit
            override fun onTtsSpeakError(utteranceId: String, status: Int) {
                main.post { report(GuidanceVoiceStatus.PLAYBACK_FAILED) }
            }
        }
        delegate.start()
    }, release = {
        delegate.statusObserver = null
        delegate.shutdown()
        fxVoice.release()
    }, onStatus = onStatus)
}

/**
 * Use an installed, offline English voice when the current world's requested locale is available.
 * Many Android TTS engines expose only one English voice; in that case the existing device voice
 * remains the fallback and the per-world pitch, pace and wording still apply.
 */
private fun chooseEnglishVoice(tts: TextToSpeech, style: WorldVoiceStyle?, baseline: Voice?): Voice? {
    if (style == null) return baseline
    val requested = Locale.forLanguageTag(style.voiceLocale)
    val installed = tts.voices.orEmpty()
        .filter { it.locale.language.equals("en", ignoreCase = true) && !it.isNetworkConnectionRequired }
        .sortedWith(
            compareBy<Voice> { if (it.locale.country.equals(requested.country, ignoreCase = true)) 0 else 1 }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
                .thenBy { it.name },
        )
    if (installed.isEmpty()) return baseline
    val regional = installed.filter { it.locale.country.equals(requested.country, ignoreCase = true) }
    val candidates = regional.ifEmpty { installed }
    val variant = style.voiceVariant.coerceAtLeast(0) % candidates.size
    return candidates[variant]
}
