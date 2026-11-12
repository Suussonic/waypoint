/*
 * Adapted from PimpinPumpkin/Vela, app/src/main/java/app/vela/offline/PmtilesReader.kt.
 * Copyright (C) 2026 Vela contributors.
 * Distributed under the GNU General Public License, version 3 or later; see LICENSE.
 */
package com.waypoint.app.places

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

/**
 * Just enough of the PMTiles v3 format to answer one question: does this archive actually hold a
 * tile at this place?
 *
 * The app needs it because an archive's bounding box is a rectangle and a region is not. A state's
 * box always overlaps its neighbors, so "the smallest box covering the view" can pick an archive
 * that has no tiles there, and the map then draws nothing at all - no error, no fallback, just the
 * land color with the traffic raster and the place pins still on top (issue #552). Asking the file
 * settles it.
 *
 * Reads the 127-byte header plus one or two directory pages, nothing else. Every failure answers
 * null, which callers treat as "cannot tell" and fall back to the old rule, so a format the reader
 * does not understand can never be worse than not having asked.
 */
object PmtilesReader {
    private const val HEADER_LEN = 127
    private const val COMPRESSION_NONE = 1
    private const val COMPRESSION_GZIP = 2

    /** The OpenMapTiles layer the roads live in; present only where the extract had data. */
    const val ROAD_LAYER = "transportation"

    /** Compressed tile and directory ranges from public place archives. */
    private const val MAX_TILE_BYTES = 4L * 1024 * 1024
    private const val MAX_DECOMPRESSED_TILE_BYTES = 8 * 1024 * 1024
    private const val MAX_COMPRESSED_DIRECTORY_BYTES = 16L * 1024 * 1024
    private const val MAX_DECOMPRESSED_DIRECTORY_BYTES = 8 * 1024 * 1024
    private const val MAX_DIRECTORY_ENTRIES = 65_536
    private const val MAX_TOTAL_ENTRIES = 1_000_000
    private const val MAX_DIRECTORY_STACK = 8_192
    private const val MAX_DIRECTORY_PAGES_CACHED = 8
    private const val MAX_HTTP_RANGE_BYTES = 16 * 1024 * 1024
    private const val MAX_TILE_BATCH_COUNT = 4_096
    private const val MAX_TILE_BATCH_BYTES = 32 * 1024 * 1024

    data class Header(
        val rootOffset: Long,
        val rootLength: Long,
        val metaOffset: Long,
        val metaLength: Long,
        val leafOffset: Long,
        val tileDataOffset: Long,
        val internalCompression: Int,
        val tileCompression: Int,
        val minZoom: Int,
        val maxZoom: Int,
    )

    fun header(file: File): Header? = runCatching {
        RandomAccessFile(file, "r").use { f ->
            val head = ByteArray(HEADER_LEN)
            if (f.read(head) < HEADER_LEN) return null
            parseHeader(head)
        }
    }.getOrNull()

    /** The v3 header from an archive's first [HEADER_LEN] bytes, or null when they are not one. */
    fun parseHeader(head: ByteArray): Header? {
        if (head.size < HEADER_LEN || String(head, 0, 7) != "PMTiles" || head[7].toInt() != 3) return null
        val header = Header(
            rootOffset = le64(head, 8),
            rootLength = le64(head, 16),
            metaOffset = le64(head, 24),
            metaLength = le64(head, 32),
            leafOffset = le64(head, 40),
            tileDataOffset = le64(head, 56),
            internalCompression = head[97].toInt() and 0xFF,
            tileCompression = head[98].toInt() and 0xFF,
            minZoom = head[100].toInt() and 0xFF,
            maxZoom = head[101].toInt() and 0xFF,
        )
        return header.takeIf {
            it.rootOffset >= 0 && it.rootLength in 1..MAX_COMPRESSED_DIRECTORY_BYTES &&
                it.leafOffset >= 0 && it.tileDataOffset >= 0 &&
                it.minZoom <= it.maxZoom && it.maxZoom <= 30
        }
    }

    /** True when the archive holds the tile, false when it demonstrably does not, null when the
     *  file could not be read the way this reader expects. */
    fun hasTile(file: File, z: Int, x: Int, y: Int): Boolean? = entryFor(file, z, x, y)?.let { true } ?: run {
        val h = header(file) ?: return null
        if (z < h.minZoom || z > h.maxZoom) null else false
    }

    /** True when the archive's tile here carries the road network, false when it holds only the
     *  base layers, null when it cannot be told.
     *
     *  This, not mere tile presence, is the question the mount rule has to ask. A bake emits tiles
     *  across its WHOLE bounding box because planetiler's base data (water, landcover, Natural
     *  Earth) is global: a state archive therefore has tiles over its neighbors and out to sea, and
     *  they render as empty land. Only the OSM-derived layers stop at the extract, so the presence
     *  of [ROAD_LAYER] is what separates "this archive draws the map here" from "this archive draws
     *  nothing here". */
    fun hasRoads(file: File, z: Int, x: Int, y: Int): Boolean? {
        val h = header(file) ?: return null
        if (z < h.minZoom || z > h.maxZoom) return null
        val e = entryFor(file, z, x, y) ?: return false
        if (e.length !in 1..MAX_TILE_BYTES) return null
        val tileOffset = safeAdd(h.tileDataOffset, e.offset) ?: return null
        return runCatching {
            RandomAccessFile(file, "r").use { f ->
                val raw = ByteArray(e.length.toInt())
                if (tileOffset > f.length() - raw.size) return@use null
                f.seek(tileOffset)
                f.readFully(raw)
                val body = when (h.tileCompression) {
                    COMPRESSION_NONE -> raw
                    COMPRESSION_GZIP -> gunzipBounded(raw, MAX_DECOMPRESSED_TILE_BYTES) ?: return@use null
                    else -> return@runCatching null
                }
                layerNames(body).contains(ROAD_LAYER)
            }
        }.getOrNull()
    }

    /** One tile's bytes, decompressed, or null when the archive does not hold it or cannot be read. */
    fun tileBytes(file: File, z: Int, x: Int, y: Int): ByteArray? {
        val h = header(file) ?: return null
        if (z < h.minZoom || z > h.maxZoom) return null
        val e = entryFor(file, z, x, y) ?: return null
        if (e.length !in 1..MAX_TILE_BYTES) return null
        val tileOffset = safeAdd(h.tileDataOffset, e.offset) ?: return null
        return runCatching {
            RandomAccessFile(file, "r").use { f ->
                val raw = ByteArray(e.length.toInt())
                if (tileOffset > f.length() - raw.size) return@use null
                f.seek(tileOffset)
                f.readFully(raw)
                when (h.tileCompression) {
                    COMPRESSION_NONE -> raw
                    COMPRESSION_GZIP -> gunzipBounded(raw, MAX_DECOMPRESSED_TILE_BYTES)
                    else -> null
                }
            }
        }.getOrNull()
    }

    /** Byte ranges of an archive: a local file or an HTTP URL. Null when the range cannot be read. */
    fun interface RangeReader {
        fun read(offset: Long, length: Int): ByteArray?
    }

    /** One archive, file or HTTP, held for many tile reads: the header once and each directory page
     *  once (directories are cached across searches too, keyed by the archive's own header so a
     *  rebaked file never reuses a stale one), and [tiles] reads a batch of tiles in as few ranges
     *  as their order in the file allows. For a search that walks a few hundred tiles; over HTTP a
     *  ring of neighbors is usually one or two requests, because PMTiles stores nearby tiles
     *  together. Not thread-safe; close it when done. */
    class Archive(
        private val keyBase: String,
        private val reader: RangeReader,
        private val onClose: () -> Unit = {},
        private val maxBytesRead: Long = Long.MAX_VALUE,
    ) : java.io.Closeable {
        var bytesRead: Long = 0L
            private set
        var readBudgetExceeded: Boolean = false
            private set
        var rangeReadFailed: Boolean = false
            private set

        private fun readRange(offset: Long, length: Int): ByteArray? {
            if (length < 1 || length.toLong() > maxBytesRead - bytesRead) {
                readBudgetExceeded = true
                return null
            }
            bytesRead += length.toLong()
            return reader.read(offset, length).also { if (it == null) rangeReadFailed = true }
        }

        private val headBytes: ByteArray? by lazy { readRange(0, HEADER_LEN) }
        val header: Header? by lazy { headBytes?.let(::parseHeader) }
        private val key: String by lazy { keyBase + "|" + (headBytes?.contentHashCode() ?: 0) }

        private fun dir(h: Header, offset: Long, length: Long): List<Entry>? {
            if (offset < 0 || length !in 1..MAX_COMPRESSED_DIRECTORY_BYTES || offset > Long.MAX_VALUE - length) return null
            val k = "$key|$offset|$length"
            synchronized(DIR_CACHE) { DIR_CACHE[k] }?.let { return it }
            val raw = readRange(offset, length.toInt())?.takeIf { it.size == length.toInt() } ?: return null
            val d = decodeDirectory(raw, h.internalCompression) ?: return null
            synchronized(DIR_CACHE) { DIR_CACHE[k] = d }
            return d
        }

        private fun entry(h: Header, z: Int, x: Int, y: Int): Entry? {
            val want = tileId(z, x, y)
            var offset = h.rootOffset
            var length = h.rootLength
            repeat(4) {
                val e = find(dir(h, offset, length) ?: return null, want) ?: return null
                if (e.runLength > 0) return e
                offset = safeAdd(h.leafOffset, e.offset) ?: return null
                length = e.length
            }
            return null
        }

        fun tile(z: Int, x: Int, y: Int): ByteArray? = tiles(z, listOf(x to y))[x to y]

        /** The tiles among [coords] the archive holds, decompressed. Reads that sit within
         *  [GAP_BYTES] of each other in the file are merged into one range of at most [SPAN_BYTES]. */
        fun tiles(z: Int, coords: List<Pair<Int, Int>>): Map<Pair<Int, Int>, ByteArray> = runCatching {
            val h = header ?: return emptyMap()
            if (z < h.minZoom || z > h.maxZoom || coords.size > MAX_TILE_BATCH_COUNT) return emptyMap()
            val wanted = coords.mapNotNull { c ->
                entry(h, z, c.first, c.second)?.takeIf {
                    it.offset >= 0 && it.length in 1..MAX_TILE_BYTES && it.offset <= Long.MAX_VALUE - it.length
                }?.let { c to it }
            }
                .sortedBy { it.second.offset }
            val out = HashMap<Pair<Int, Int>, ByteArray>()
            var totalDecodedBytes = 0L
            var i = 0
            while (i < wanted.size) {
                val start = wanted[i].second.offset
                var end = start + wanted[i].second.length
                var j = i + 1
                while (j < wanted.size) {
                    val e = wanted[j].second
                    if (e.offset - end > GAP_BYTES || e.offset + e.length - start > SPAN_BYTES) break
                    end = maxOf(end, e.offset + e.length)
                    j++
                }
                val rangeOffset = safeAdd(h.tileDataOffset, start) ?: return emptyMap()
                if (end <= start || end - start > MAX_HTTP_RANGE_BYTES) return emptyMap()
                val rangeLength = (end - start).toInt()
                val span = readRange(rangeOffset, rangeLength)?.takeIf { it.size == rangeLength }
                if (span != null) for (k in i until j) {
                    val (c, e) = wanted[k]
                    val at = (e.offset - start).toInt()
                    if (at < 0 || at + e.length > span.size) continue
                    val raw = span.copyOfRange(at, at + e.length.toInt())
                    val body = when (h.tileCompression) {
                        COMPRESSION_NONE -> raw
                        COMPRESSION_GZIP -> gunzipBounded(raw, MAX_DECOMPRESSED_TILE_BYTES)
                        else -> null
                    }
                    if (body != null) {
                        totalDecodedBytes += body.size.toLong()
                        if (totalDecodedBytes > MAX_TILE_BATCH_BYTES) return emptyMap()
                        out[c] = body
                    }
                }
                i = j
            }
            out
        }.getOrDefault(emptyMap())

        override fun close() = onClose()

        companion object {
            private const val GAP_BYTES = 16L * 1024
            private const val SPAN_BYTES = 1024L * 1024
            /** Directory pages by archive and offset; a state's leaf pages are tens of KB each. */
            private val DIR_CACHE = object : LinkedHashMap<String, List<Entry>>(64, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Entry>>?) = size > MAX_DIRECTORY_PAGES_CACHED
            }

            fun file(file: File): Archive {
                val f = RandomAccessFile(file, "r")
                val reader = RangeReader { offset, length ->
                    if (offset < 0 || length !in 1..MAX_HTTP_RANGE_BYTES || offset > f.length() - length) null
                    else runCatching { ByteArray(length).also { f.seek(offset); f.readFully(it) } }.getOrNull()
                }
                return Archive(
                    "${file.absolutePath}|${file.length()}|${file.lastModified()}",
                    reader,
                    onClose = { runCatching { f.close() } },
                )
            }

            /** An archive served over HTTP (the places archives on the release host): each read is
             *  one `Range` request, answered 206. */
            fun http(
                client: okhttp3.OkHttpClient,
                url: String,
                maxBytesRead: Long = Long.MAX_VALUE,
            ): Archive = Archive(url, RangeReader { offset, length ->
                if (offset < 0 || length !in 1..MAX_HTTP_RANGE_BYTES || offset > Long.MAX_VALUE - length) null else runCatching {
                    client.newCall(
                        okhttp3.Request.Builder().url(url).header("Range", "bytes=$offset-${offset + length - 1}").build(),
                    ).execute().use { response ->
                        if (response.code != 206) return@use null
                        val body = response.body ?: return@use null
                        runCatching { body.byteStream().use { it.readBoundedBytes(length) } }
                            .getOrNull()?.takeIf { it.size == length }
                    }
                }.getOrNull()
            }, maxBytesRead = maxBytesRead)
        }
    }

    /** The directory entry for a tile, or null when the archive does not hold it. */
    private fun entryFor(file: File, z: Int, x: Int, y: Int): Entry? {
        val h = header(file) ?: return null
        if (z < h.minZoom || z > h.maxZoom) return null
        val want = tileId(z, x, y)
        return runCatching {
            RandomAccessFile(file, "r").use { f ->
                var offset = h.rootOffset
                var length = h.rootLength
                // A root entry can point at a leaf directory instead of a tile; the loop is bounded
                // rather than recursive because no published archive nests deeper than that.
                repeat(4) {
                    val dir = readDirectory(f, offset, length, h.internalCompression) ?: return@runCatching null
                    val e = find(dir, want) ?: return@runCatching null
                    if (e.runLength > 0) return@runCatching e
                    offset = safeAdd(h.leafOffset, e.offset) ?: return@runCatching null
                    length = e.length
                }
                null
            }
        }.getOrNull()
    }

    /** The layer names in a Mapbox Vector Tile: repeated Layer (field 3), each with a name
     *  (field 1). Nothing else in the tile is read. */
    fun layerNames(tile: ByteArray): Set<String> {
        val names = HashSet<String>()
        val r = Varints(tile)
        while (r.hasMore()) {
            val key = r.next()
            val field = (key shr 3).toInt()
            when ((key and 7L).toInt()) {
                0 -> r.next()
                1 -> r.skip(8)
                2 -> {
                    val len = r.next().toInt()
                    if (len < 0 || len > tile.size) return names
                    if (field == 3) {
                        val end = r.position() + len
                        // The layer's own fields; only its name (field 1, length-delimited) matters.
                        while (r.position() < end && r.hasMore()) {
                            val k2 = r.next()
                            val f2 = (k2 shr 3).toInt()
                            when ((k2 and 7L).toInt()) {
                                0 -> r.next()
                                1 -> r.skip(8)
                                2 -> {
                                    val l2 = r.next().toInt()
                                    if (f2 == 1 && l2 in 0..256) names.add(r.string(l2)) else r.skip(l2)
                                }
                                5 -> r.skip(4)
                                else -> return names
                            }
                        }
                        r.seek(end)
                    } else r.skip(len)
                }
                5 -> r.skip(4)
                else -> return names
            }
        }
        return names
    }

    data class Entry(val id: Long, val offset: Long, val length: Long, val runLength: Long)

    /** Every tile entry in the archive, leaf directories walked, in id order. The delta applier
     *  needs the whole set to fingerprint an archive; the mount probe only ever wants one tile and
     *  uses [entryFor], which stops as soon as it finds it. */
    fun entries(file: File): List<Entry>? {
        val h = header(file) ?: return null
        return entriesAt(file, h.rootOffset, h.rootLength, h.leafOffset, h.internalCompression)
    }

    /** The same walk against a directory the header does not point at yet. The delta applier uses
     *  it to fingerprint what a patch WOULD produce before it commits the header to it. */
    fun entriesAt(file: File, rootOffset: Long, rootLength: Long, leafOffset: Long, compression: Int): List<Entry>? = runCatching {
        val h = Triple(rootOffset, rootLength, leafOffset)
        RandomAccessFile(file, "r").use { f ->
            val out = ArrayList<Entry>()
            val stack = ArrayDeque<Pair<Long, Long>>()
            stack.addLast(h.first to h.second)
            var pages = 0
            while (stack.isNotEmpty()) {
                if (pages++ >= 8192) return@runCatching null // a malformed archive must not spin forever
                val (off, len) = stack.removeLast()
                val dir = readDirectory(f, off, len, compression) ?: return@runCatching null
                for (e in dir) {
                    if (e.runLength == 0L) {
                        val leafOffset = safeAdd(h.third, e.offset) ?: return@runCatching null
                        if (stack.size >= MAX_DIRECTORY_STACK) return@runCatching null
                        stack.addLast(leafOffset to e.length)
                    } else {
                        if (out.size >= MAX_TOTAL_ENTRIES) return@runCatching null
                        out.add(e)
                    }
                }
            }
            out.sortBy { it.id }
            out
        }
    }.getOrNull()

    private fun find(entries: List<Entry>, want: Long): Entry? {
        var lo = 0
        var hi = entries.size - 1
        var best: Entry? = null
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val e = entries[mid]
            if (e.id <= want) { best = e; lo = mid + 1 } else hi = mid - 1
        }
        val e = best ?: return null
        // A run covers [id, id + runLength); a leaf pointer (runLength 0) covers everything from
        // its id up to the next entry, which is why it is returned without a range test.
        if (e.runLength == 0L) return e
        return if (want < e.id + e.runLength) e else null
    }

    private fun readDirectory(f: RandomAccessFile, offset: Long, length: Long, compression: Int): List<Entry>? {
        if (offset < 0 || length !in 1..MAX_COMPRESSED_DIRECTORY_BYTES || length > f.length() || offset > f.length() - length) return null
        val raw = ByteArray(length.toInt())
        f.seek(offset)
        f.readFully(raw)
        return decodeDirectory(raw, compression)
    }

    private fun decodeDirectory(raw: ByteArray, compression: Int): List<Entry>? {
        val bytes = when (compression) {
            COMPRESSION_NONE -> raw.takeIf { it.size <= MAX_DECOMPRESSED_DIRECTORY_BYTES } ?: return null
            COMPRESSION_GZIP -> gunzipBounded(raw, MAX_DECOMPRESSED_DIRECTORY_BYTES) ?: return null
            else -> return null
        }
        val r = Varints(bytes)
        val count = r.next()
        if (count !in 1..MAX_DIRECTORY_ENTRIES.toLong()) return null
        val n = count.toInt()
        val ids = LongArray(n)
        var last = 0L
        for (i in 0 until n) {
            val delta = r.next()
            if (delta < 0 || last > Long.MAX_VALUE - delta) return null
            last += delta
            ids[i] = last
        }
        val runs = LongArray(n)
        val lengths = LongArray(n)
        for (i in 0 until n) {
            runs[i] = r.next()
            if (runs[i] < 0) return null
        }
        for (i in 0 until n) {
            lengths[i] = r.next()
            if (lengths[i] !in 1..MAX_COMPRESSED_DIRECTORY_BYTES) return null
        }
        val offsets = LongArray(n)
        for (i in 0 until n) {
            val v = r.next()
            // 0 means "directly after the previous entry", which is how a clustered archive avoids
            // writing an offset per tile.
            offsets[i] = when {
                v == 0L && i > 0 -> safeAdd(offsets[i - 1], lengths[i - 1]) ?: return null
                v <= 0 -> return null
                else -> v - 1
            }
        }
        if (r.hasMore() || !r.isValid()) return null
        return List(n) { Entry(ids[it], offsets[it], lengths[it], runs[it]) }
    }

    internal class Varints(private val b: ByteArray) {
        private var i = 0
        private var valid = true
        fun hasMore() = i < b.size
        fun isValid() = valid
        fun position() = i
        fun seek(at: Int) { i = at.coerceIn(0, b.size) }
        fun skip(n: Int) {
            if (n < 0) { valid = false; return }
            i = (i.toLong() + n).coerceIn(0L, b.size.toLong()).toInt()
        }
        fun string(n: Int): String {
            if (n < 0) { valid = false; return "" }
            val s = String(b, i, n.coerceAtMost(b.size - i))
            skip(n)
            return s
        }
        fun next(): Long {
            var shift = 0
            var value = 0L
            while (i < b.size && shift <= 63) {
                val c = b[i++].toInt() and 0xFF
                if (shift == 63 && c and 0x7E != 0) { valid = false; return 0L }
                value = value or ((c and 0x7F).toLong() shl shift)
                if (c and 0x80 == 0) return value
                shift += 7
            }
            valid = false
            return value
        }
    }

    private fun safeAdd(left: Long, right: Long): Long? =
        if (left < 0 || right < 0 || left > Long.MAX_VALUE - right) null else left + right

    private fun gunzipBounded(raw: ByteArray, maxBytes: Int): ByteArray? = runCatching {
        GZIPInputStream(raw.inputStream()).use { it.readBoundedBytes(maxBytes) }
    }.getOrNull()

    /** The Hilbert-curve id PMTiles orders tiles by. */
    fun tileId(z: Int, x: Int, y: Int): Long {
        var acc = 0L
        for (t in 0 until z) acc += (1L shl t) * (1L shl t)
        var n = 1L shl z
        var xx = x.toLong()
        var yy = y.toLong()
        var d = 0L
        var s = n / 2
        while (s > 0) {
            val rx = if (xx and s > 0L) 1L else 0L
            val ry = if (yy and s > 0L) 1L else 0L
            d += s * s * ((3L * rx) xor ry)
            if (ry == 0L) {
                if (rx == 1L) { xx = s - 1 - xx; yy = s - 1 - yy }
                val t = xx; xx = yy; yy = t
            }
            s /= 2
        }
        return acc + d
    }

    /** Web-mercator tile column and row for a coordinate. */
    fun tileOf(lat: Double, lng: Double, z: Int): Pair<Int, Int> {
        val n = 1 shl z
        val x = ((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        val y = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n)
            .toInt().coerceIn(0, n - 1)
        return x to y
    }

    internal fun le64(b: ByteArray, at: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[at + i].toLong() and 0xFF)
        return v
    }
}
