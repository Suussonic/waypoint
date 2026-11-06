package com.waypoint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class VoiceFxTest {
    private val rate = 22050
    private fun tone(hz: Double, seconds: Double = 0.5, amp: Double = 0.5) =
        ShortArray((rate * seconds).toInt()) { (sin(2 * PI * hz * it / rate) * amp * 32767).toInt().toShort() }
    private fun rms(a: ShortArray, from: Int = 0, to: Int = a.size): Double =
        sqrt((from until to).sumOf { (a[it] / 32768.0) * (a[it] / 32768.0) } / (to - from).coerceAtLeast(1))

    @Test fun everyWorldProducesCleanBoundedAudio() {
        val speechLike = ShortArray(rate) { ((sin(2 * PI * 180 * it / rate) + 0.5 * sin(2 * PI * 1200 * it / rate)) * 9000).toInt().toShort() }
        for (world in World.entries) {
            val out = VoiceFx.process(speechLike, rate, world.voiceFx())
            assertTrue("$world output shorter than input", out.size >= speechLike.size)
            assertTrue("$world is silent", rms(out) > 0.02)
            assertTrue("$world clips", out.all { abs(it.toInt()) <= 32767 })
        }
    }

    @Test fun emptyAndSilentInputAreSafe() {
        assertEquals(0, VoiceFx.process(ShortArray(0), rate, World.OLD_WEST.voiceFx()).size)
        val silent = VoiceFx.process(ShortArray(2000), rate, World.NIGHT_CITY.voiceFx())
        assertTrue(silent.all { abs(it.toInt()) < 400 })
    }

    @Test fun isDeterministic() {
        val input = tone(300.0)
        assertArrayEquals(VoiceFx.process(input, rate, World.STALKER.voiceFx()), VoiceFx.process(input, rate, World.STALKER.voiceFx()))
    }

    @Test fun radioBandRemovesVeryLowAndVeryHighTones() {
        val preset = VoiceFxPreset(highPassHz = 400f, lowPassHz = 3000f)
        val low = VoiceFx.process(tone(80.0), rate, preset)
        val mid = VoiceFx.process(tone(1000.0), rate, preset)
        val high = VoiceFx.process(tone(9000.0), rate, preset)
        // Each output is renormalised, so compare shape: the in-band tone must stay far louder than the others
        // when they are all mixed through the same filter at the same input level.
        val mix = ShortArray(rate) { ((sin(2 * PI * 80 * it / rate) + sin(2 * PI * 1000 * it / rate) + sin(2 * PI * 9000 * it / rate)) * 6000).toInt().toShort() }
        val out = VoiceFx.process(mix, rate, preset)
        fun band(hz: Double) = sqrt((0 until out.size).sumOf { i -> out[i] / 32768.0 * sin(2 * PI * hz * i / rate) }.let { it * it } +
            (0 until out.size).sumOf { i -> out[i] / 32768.0 * kotlin.math.cos(2 * PI * hz * i / rate) }.let { it * it })
        assertTrue("1 kHz should dominate 80 Hz", band(1000.0) > 4 * band(80.0))
        assertTrue("1 kHz should dominate 9 kHz", band(1000.0) > 4 * band(9000.0))
        assertTrue(rms(low) > 0 && rms(mid) > 0 && rms(high) > 0)
    }

    @Test fun bitCrusherLimitsTheNumberOfLevels() {
        val out = VoiceFx.process(tone(500.0), rate, VoiceFxPreset(bitDepth = 4))
        assertTrue("Expected a handful of levels, got ${out.toSet().size}", out.toSet().size <= 20)
    }

    @Test fun reverbAddsATailAndWorldsSoundDifferent() {
        val input = tone(400.0, 0.3)
        val hall = VoiceFx.process(input, rate, World.SKYRIM.voiceFx())
        assertTrue(hall.size > input.size + rate / 2)
        assertTrue("reverb tail should still carry sound", rms(hall, input.size + 2000, input.size + 4000) > 0.001)
        val a = VoiceFx.process(input, rate, World.SKYRIM.voiceFx())
        val b = VoiceFx.process(input, rate, World.PIXEL.voiceFx())
        assertTrue(!a.contentEquals(b))
    }
}
