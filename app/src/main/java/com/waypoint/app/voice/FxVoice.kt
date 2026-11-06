package com.waypoint.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Speaks an instruction through a world's effects chain. Failure is reported so the plain voice can take over. */
internal interface FxVoice {
    fun speak(text: String, preset: VoiceFxPreset, style: WorldVoiceStyle?, onFailure: () -> Unit)
    fun stop()
    fun release()
}

/**
 * Renders the instruction with its own Android text-to-speech instance (the device's installed voice, so no
 * voice data ships with the app), runs [VoiceFx] over the audio, and plays it as navigation guidance with
 * ducking. The newest instruction replaces any earlier one, as turn guidance should. Nothing is kept on disk
 * after playback.
 */
internal class AndroidFxVoice(
    context: Context,
    private val configure: (TextToSpeech, WorldVoiceStyle?) -> Unit,
) : FxVoice {
    private val app = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "waypoint-fx-voice").apply { isDaemon = true } }
    private val generation = AtomicLong(0)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    @Volatile private var engine: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var failed = false
    @Volatile private var track: AudioTrack? = null
    private var released = false

    @Synchronized private fun ensureEngine() {
        if (engine != null || released || failed) return
        engine = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = engine
                ready = tts != null && runCatching { tts.setLanguage(java.util.Locale.ENGLISH) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
                if (!ready) failed = true
            } else failed = true
        }
    }

    override fun speak(text: String, preset: VoiceFxPreset, style: WorldVoiceStyle?, onFailure: () -> Unit) {
        ensureEngine()
        val ticket = generation.incrementAndGet()
        stopTrack()
        if (failed || released) { onFailure(); return }
        worker.execute {
            val ok = runCatching { render(ticket, text, preset, style) }.getOrDefault(false)
            if (!ok && generation.get() == ticket) onFailure()
        }
    }

    /** Returns false only when nothing could be played, so the caller can fall back to the plain voice. */
    private fun render(ticket: Long, text: String, preset: VoiceFxPreset, style: WorldVoiceStyle?): Boolean {
        // The engine may still be starting on the very first instruction.
        val deadline = SystemClock.elapsedRealtime() + 3000
        while (!ready && !failed && SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
        val tts = engine?.takeIf { ready } ?: return false
        val file = File(app.cacheDir, "fx-${UUID.randomUUID()}.wav")
        try {
            runCatching { configure(tts, style) }
            val done = CountDownLatch(1)
            var error = false
            val id = UUID.randomUUID().toString()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { if (utteranceId == id) done.countDown() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { if (utteranceId == id) { error = true; done.countDown() } }
                override fun onError(utteranceId: String?, errorCode: Int) { if (utteranceId == id) { error = true; done.countDown() } }
            })
            if (tts.synthesizeToFile(text, null, file, id) != TextToSpeech.SUCCESS) return false
            val synthesisDeadline = SystemClock.elapsedRealtime() + 10_000
            while (!done.await(100, TimeUnit.MILLISECONDS)) {
                if (file.length() > WavPcm.MAX_BYTES) { tts.stop(); return false }
                if (generation.get() != ticket || SystemClock.elapsedRealtime() >= synthesisDeadline) {
                    tts.stop()
                    return false
                }
            }
            if (error || file.length() > WavPcm.MAX_BYTES) return false
            if (generation.get() != ticket) return true   // replaced while rendering; the newer call owns playback
            val wav = WavPcm.parseFile(file) ?: return false
            if (wav.samples.isEmpty()) return false
            val processed = VoiceFx.process(wav.samples, wav.sampleRate, preset)
            if (generation.get() != ticket) return true
            return play(ticket, processed, wav.sampleRate)
        } finally {
            file.delete()
        }
    }

    private fun play(ticket: Long, pcm: ShortArray, rate: Int): Boolean {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener { }.build()
        audio.requestAudioFocus(focus)
        val created = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC).build()
        try {
            if (created.write(pcm, 0, pcm.size) != pcm.size) return false
            synchronized(this) { if (generation.get() != ticket) return true; track = created }
            created.play()
            val end = SystemClock.elapsedRealtime() + pcm.size * 1000L / rate + 1500
            while (generation.get() == ticket && created.playbackHeadPosition < pcm.size && SystemClock.elapsedRealtime() < end) Thread.sleep(40)
            return true
        } finally {
            synchronized(this) { if (track === created) track = null }
            runCatching { created.stop() }
            runCatching { created.release() }
            audio.abandonAudioFocusRequest(focus)
        }
    }

    private fun stopTrack() {
        val t = synchronized(this) { track.also { track = null } } ?: return
        runCatching { t.pause(); t.flush(); t.stop() }
    }

    override fun stop() {
        generation.incrementAndGet()
        stopTrack()
    }

    @Synchronized override fun release() {
        released = true
        generation.incrementAndGet()
        stopTrack()
        worker.shutdownNow()
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }
}
