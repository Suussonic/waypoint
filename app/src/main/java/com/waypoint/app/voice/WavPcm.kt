package com.waypoint.app

import java.io.ByteArrayOutputStream
import java.io.File

/** Minimal reader and writer for the 16-bit PCM WAV files Android's text-to-speech engines produce. */
internal class WavPcm(val sampleRate: Int, val samples: ShortArray) {
    companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MIN_SAMPLE_RATE = 8_000
        const val MAX_SAMPLE_RATE = 96_000
        const val MAX_FRAMES = 1_100_000

        /** TTS engines write this file; length can still change after their completion callback. */
        fun parseFile(file: File): WavPcm? {
            if (file.length() > MAX_BYTES) return null
            val output = ByteArrayOutputStream()
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_BYTES) return null
                    output.write(buffer, 0, count)
                }
            }
            return parse(output.toByteArray())
        }

        /** Returns mono 16-bit samples (channels are averaged), or null when the file is not readable PCM. */
        fun parse(bytes: ByteArray): WavPcm? {
            if (bytes.size !in 44..MAX_BYTES || String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF" || String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE") return null
            var channels = 0
            var rate = 0
            var bits = 0
            var offset = 12
            while (offset + 8 <= bytes.size) {
                val id = String(bytes, offset, 4, Charsets.US_ASCII)
                val declared = le32(bytes, offset + 4)
                if (declared < 0) return null
                val body = offset + 8
                if (id == "fmt " && declared >= 16 && body + 16 <= bytes.size) {
                    if (le16(bytes, body) != 1) return null
                    channels = le16(bytes, body + 2)
                    rate = le32(bytes, body + 4)
                    bits = le16(bytes, body + 14)
                } else if (id == "data") {
                    if (channels !in 1..2 || rate !in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE || bits != 16) return null
                    // Streaming writers may leave the size as zero or too large; trust the file length then.
                    val available = bytes.size - body
                    val length = if (declared <= 0 || declared > available) available else declared
                    val frames = length / (2 * channels)
                    if (frames > MAX_FRAMES) return null
                    val out = ShortArray(frames)
                    for (i in 0 until frames) {
                        var sum = 0
                        for (c in 0 until channels) sum += le16s(bytes, body + (i * channels + c) * 2)
                        out[i] = (sum / channels).toShort()
                    }
                    return WavPcm(rate, out)
                }
                val next = body.toLong() + declared + (declared and 1)
                if (next > bytes.size) return null
                offset = next.toInt()
            }
            return null
        }

        private fun le16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
        private fun le16s(b: ByteArray, o: Int) = le16(b, o).toShort().toInt()
        private fun le32(b: ByteArray, o: Int) = le16(b, o) or (le16(b, o + 2) shl 16)
    }
}
