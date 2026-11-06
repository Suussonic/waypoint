package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WavPcmTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private fun wav(rate: Int, channels: Int, samples: ShortArray, declaredData: Int? = null, extraChunk: Boolean = false): ByteArray {
        val data = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, s -> data[i * 2] = (s.toInt() and 0xFF).toByte(); data[i * 2 + 1] = (s.toInt() shr 8).toByte() }
        fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
        fun le16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
        var out = "RIFF".toByteArray() + le32(36 + data.size) + "WAVE".toByteArray() +
            "fmt ".toByteArray() + le32(16) + le16(1) + le16(channels) + le32(rate) + le32(rate * channels * 2) + le16(channels * 2) + le16(16)
        if (extraChunk) out += "LIST".toByteArray() + le32(3) + byteArrayOf(1, 2, 3, 0)   // odd size is padded
        return out + "data".toByteArray() + le32(declaredData ?: data.size) + data
    }

    @Test fun readsMonoSamplesAndRate() {
        val parsed = WavPcm.parse(wav(22050, 1, shortArrayOf(1, -2, 300, -32768, 32767)))!!
        assertEquals(22050, parsed.sampleRate)
        assertEquals(listOf<Short>(1, -2, 300, -32768, 32767), parsed.samples.toList())
    }

    @Test fun averagesStereoToMono() {
        val parsed = WavPcm.parse(wav(16000, 2, shortArrayOf(100, 300, -50, -150)))!!
        assertEquals(listOf<Short>(200, -100), parsed.samples.toList())
    }

    @Test fun skipsOtherChunksAndToleratesStreamingSizes() {
        val parsed = WavPcm.parse(wav(24000, 1, shortArrayOf(5, 6, 7), declaredData = 0, extraChunk = true))!!
        assertEquals(listOf<Short>(5, 6, 7), parsed.samples.toList())
        val oversize = WavPcm.parse(wav(24000, 1, shortArrayOf(5, 6, 7), declaredData = Int.MAX_VALUE))!!
        assertEquals(3, oversize.samples.size)
    }

    @Test fun rejectsGarbageAndNonPcm() {
        assertNull(WavPcm.parse(ByteArray(10)))
        assertNull(WavPcm.parse("RIFFxxxxWAVEfmt ".toByteArray() + ByteArray(40)))
    }

    @Test fun rejectsUnboundedEngineAudioBeforeAllocatingEffects() {
        assertNull(WavPcm.parse(wav(Int.MAX_VALUE, 1, shortArrayOf(1))))
        assertNull(WavPcm.parse(wav(4_000, 1, shortArrayOf(1))))
        assertNull(WavPcm.parse(wav(24_000, 3, shortArrayOf(1, 2, 3))))
        val oversized = temporaryFolder.newFile("large.wav")
        oversized.outputStream().use { it.write(ByteArray(WavPcm.MAX_BYTES + 1)) }
        assertNull(WavPcm.parseFile(oversized))
        assertThrows(IllegalArgumentException::class.java) {
            VoiceFx.process(shortArrayOf(1), Int.MAX_VALUE, World.SKYRIM.voiceFx())
        }
    }
}
