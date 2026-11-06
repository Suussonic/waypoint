package com.waypoint.app

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Game-style processing for synthesized speech: band-limited radio and comms sound, grit, bit-crushed
 * retro tone, robotic ring modulation, room and hall reverb. It only shapes audio; it does not imitate any
 * person or character, and it is deterministic (a fixed noise seed), so the same input always gives the
 * same output. Operates on 16-bit mono PCM.
 */
internal data class VoiceFxPreset(
    val highPassHz: Float = 0f,        // 0 disables
    val lowPassHz: Float = 0f,         // 0 disables
    val drive: Float = 0f,             // soft-clip amount; 0 disables
    val bitDepth: Int = 0,             // 0 disables; e.g. 8 for chunky retro tone
    val holdSamples: Int = 1,          // sample-rate reduction: hold each sample this many times
    val ringHz: Float = 0f,            // 0 disables; carrier frequency for a robotic tone
    val ringMix: Float = 0f,
    val tremoloHz: Float = 0f,
    val tremoloDepth: Float = 0f,
    val echoMs: Int = 0,
    val echoMix: Float = 0f,
    val reverbMix: Float = 0f,
    val crackle: Float = 0f,           // level of sparse clicks and noise floor
    val tailSeconds: Float = 0f,       // silence appended so reverb and echo can ring out
)

internal object VoiceFx {
    fun process(input: ShortArray, sampleRate: Int, preset: VoiceFxPreset): ShortArray {
        if (input.isEmpty()) return input
        require(sampleRate in WavPcm.MIN_SAMPLE_RATE..WavPcm.MAX_SAMPLE_RATE && input.size <= WavPcm.MAX_FRAMES) {
            "Unsupported synthesized audio."
        }
        require(preset.tailSeconds in 0f..1f) { "Unsupported voice effect tail." }
        val tail = (preset.tailSeconds * sampleRate).toInt().coerceAtLeast(0)
        require(input.size.toLong() + tail <= WavPcm.MAX_FRAMES + WavPcm.MAX_SAMPLE_RATE) {
            "Synthesized audio is too long."
        }
        var x = FloatArray(input.size + tail) { if (it < input.size) input[it] / 32768f else 0f }

        if (preset.highPassHz > 0f) x = Biquad.highPass(preset.highPassHz, sampleRate).run(x)
        if (preset.lowPassHz > 0f) x = Biquad.lowPass(min(preset.lowPassHz, sampleRate / 2f - 100f), sampleRate).run(x)
        if (preset.drive > 0f) for (i in x.indices) x[i] = tanh(x[i] * (1f + preset.drive * 3f)) / tanh(1f + preset.drive * 3f)
        if (preset.holdSamples > 1) {
            var held = 0f
            for (i in x.indices) { if (i % preset.holdSamples == 0) held = x[i]; x[i] = held }
        }
        if (preset.bitDepth in 2..15) {
            val levels = (1 shl (preset.bitDepth - 1)).toFloat()
            for (i in x.indices) x[i] = Math.round(x[i] * levels) / levels
        }
        if (preset.ringHz > 0f && preset.ringMix > 0f) {
            for (i in x.indices) {
                val carrier = sin(2.0 * PI * preset.ringHz * i / sampleRate).toFloat()
                x[i] = x[i] * (1f - preset.ringMix) + x[i] * carrier * preset.ringMix
            }
        }
        if (preset.tremoloHz > 0f && preset.tremoloDepth > 0f) {
            for (i in x.indices) {
                val lfo = 1f - preset.tremoloDepth * (0.5f + 0.5f * sin(2.0 * PI * preset.tremoloHz * i / sampleRate).toFloat())
                x[i] *= lfo
            }
        }
        if (preset.echoMs > 0 && preset.echoMix > 0f) {
            val delay = (preset.echoMs * sampleRate / 1000).coerceAtLeast(1)
            val dry = x.copyOf()
            for (i in delay until x.size) x[i] += dry[i - delay] * preset.echoMix + (if (i >= 2 * delay) dry[i - 2 * delay] * preset.echoMix * 0.5f else 0f)
        }
        if (preset.reverbMix > 0f) x = Reverb.apply(x, sampleRate, preset.reverbMix)
        if (preset.crackle > 0f) {
            var seed = 0x2545F491
            fun rnd(): Float { seed = seed xor (seed shl 13); seed = seed xor (seed ushr 17); seed = seed xor (seed shl 5); return (seed and 0xFFFF) / 65535f }
            for (i in x.indices) {
                x[i] += (rnd() - 0.5f) * preset.crackle * 0.12f
                if (rnd() < 0.0009f * preset.crackle * 20f) x[i] += (rnd() - 0.5f) * preset.crackle * 3f
            }
        }

        var peak = 0f
        for (v in x) peak = max(peak, abs(v))
        val gain = if (peak > 0.0001f) 0.89f / peak else 1f
        return ShortArray(x.size) { (x[it] * gain * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
    }

    /** RBJ cookbook second-order filters. */
    private class Biquad(val b0: Float, val b1: Float, val b2: Float, val a1: Float, val a2: Float) {
        fun run(x: FloatArray): FloatArray {
            val y = FloatArray(x.size)
            var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f
            for (i in x.indices) {
                val out = b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
                x2 = x1; x1 = x[i]; y2 = y1; y1 = out; y[i] = out
            }
            return y
        }
        companion object {
            private const val Q = 0.7071f
            fun lowPass(hz: Float, rate: Int): Biquad {
                val w = 2.0 * PI * hz / rate; val alpha = (sin(w) / (2 * Q)).toFloat(); val c = cos(w).toFloat()
                val a0 = 1 + alpha
                return Biquad((1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)
            }
            fun highPass(hz: Float, rate: Int): Biquad {
                val w = 2.0 * PI * hz / rate; val alpha = (sin(w) / (2 * Q)).toFloat(); val c = cos(w).toFloat()
                val a0 = 1 + alpha
                return Biquad((1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)
            }
        }
    }

    /** Small Schroeder reverb: four parallel combs into two series all-passes. */
    private object Reverb {
        fun apply(x: FloatArray, rate: Int, mix: Float): FloatArray {
            val combMs = intArrayOf(29, 37, 41, 43)
            val wet = FloatArray(x.size)
            for (ms in combMs) {
                val d = (ms * rate / 1000).coerceAtLeast(1)
                val buf = FloatArray(x.size)
                for (i in x.indices) buf[i] = x[i] + (if (i >= d) buf[i - d] * 0.72f else 0f)
                for (i in x.indices) wet[i] += buf[i] * 0.25f
            }
            var out = wet
            for (ms in intArrayOf(5, 1)) {
                val d = (ms * rate / 1000 + 7).coerceAtLeast(1)
                val ap = FloatArray(out.size)
                for (i in out.indices) ap[i] = -0.5f * out[i] + (if (i >= d) out[i - d] + 0.5f * ap[i - d] else 0f)
                out = ap
            }
            return FloatArray(x.size) { x[it] * (1f - mix * 0.5f) + out[it] * mix * 0.35f }
        }
    }
}

/** One effects preset per kind of world, matching the groups used for spoken wording in WorldPersona. */
internal fun World.voiceFx(): VoiceFxPreset = when (this) {
    World.OLD_WEST -> VoiceFxPreset(highPassHz = 300f, lowPassHz = 3600f, drive = 0.4f, reverbMix = 0.12f, crackle = 0.02f, tailSeconds = 0.3f)
    World.RIVIA, World.SKYRIM, World.LANDS_BETWEEN, World.KINGDOM, World.HOGWARTS, World.HYRULE, World.AINCRAD, World.RETRO_QUEST ->
        VoiceFxPreset(highPassHz = 90f, lowPassHz = 9000f, reverbMix = 0.38f, echoMs = 120, echoMix = 0.12f, tailSeconds = 0.9f)
    World.NIGHT_CITY, World.CTOS, World.SPIDER ->
        VoiceFxPreset(highPassHz = 180f, lowPassHz = 8000f, bitDepth = 12, tremoloHz = 7f, tremoloDepth = 0.08f, reverbMix = 0.15f, tailSeconds = 0.4f)
    World.GREEN_TERMINAL, World.DOS_ATLAS, World.DEFCON, World.STARFIELD, World.NIER, World.HOME_CONSOLE ->
        VoiceFxPreset(highPassHz = 200f, lowPassHz = 5200f, bitDepth = 10, holdSamples = 2, ringHz = 55f, ringMix = 0.45f, tailSeconds = 0.2f)
    World.LOS_SANTOS, World.SAN_ANDREAS, World.VICE_COAST, World.LIBERTY, World.SAINTS, World.MAFIA ->
        VoiceFxPreset(highPassHz = 150f, lowPassHz = 6500f, drive = 0.25f, reverbMix = 0.05f, tailSeconds = 0.15f)
    World.HEAT, World.HORIZON ->
        VoiceFxPreset(highPassHz = 200f, lowPassHz = 7500f, drive = 0.6f, reverbMix = 0.04f, tailSeconds = 0.1f)
    World.DIVISION, World.COUNTER_STRIKE, World.FIELD_OPS, World.FAR_CRY, World.STALKER ->
        VoiceFxPreset(highPassHz = 420f, lowPassHz = 3100f, drive = 0.8f, crackle = 0.025f, tailSeconds = 0.15f)
    World.HORIZON_WILDS, World.BRIDGES, World.JUST_CAUSE ->
        VoiceFxPreset(highPassHz = 110f, lowPassHz = 8500f, reverbMix = 0.22f, tailSeconds = 0.6f)
    World.GHOST, World.ASSASSIN ->
        VoiceFxPreset(highPassHz = 140f, lowPassHz = 6200f, reverbMix = 0.26f, tailSeconds = 0.7f)
    World.SEA_CHART -> VoiceFxPreset(highPassHz = 160f, lowPassHz = 5200f, reverbMix = 0.2f, tremoloHz = 0.8f, tremoloDepth = 0.06f, crackle = 0.01f, tailSeconds = 0.5f)
    World.PIXEL -> VoiceFxPreset(highPassHz = 150f, lowPassHz = 5000f, bitDepth = 8, holdSamples = 4, tailSeconds = 0.1f)
}
