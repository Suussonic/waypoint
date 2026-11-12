package com.waypoint.app.places

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PmtilesReaderBudgetTest {
    @Test fun rangeReadsStopBeforeCrossingTheArchiveBudget() {
        val header = ByteArray(127).apply {
            "PMTiles".toByteArray().copyInto(this)
            this[7] = 3
            this[16] = 1 // one-byte root directory
            this[100] = 1
            this[101] = 1
        }
        var requestedBytes = 0
        val archive = PmtilesReader.Archive(
            keyBase = "budget-fixture",
            reader = PmtilesReader.RangeReader { offset, length ->
                requestedBytes += length
                if (offset == 0L && length == header.size) header else ByteArray(length)
            },
            maxBytesRead = header.size.toLong(),
        )

        assertEquals(1, archive.header?.maxZoom)
        assertTrue(archive.tiles(1, listOf(0 to 0)).isEmpty())
        assertTrue(archive.readBudgetExceeded)
        assertEquals(header.size, requestedBytes)
        assertEquals(header.size.toLong(), archive.bytesRead)
    }
}
