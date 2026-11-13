package com.waypoint.app.places

import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Reads at most [maxBytes], probing one extra byte so unknown-length streams are also bounded. */
internal fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 8 * 1024))
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer, 0, minOf(buffer.size, maxBytes - total + 1))
        if (count < 0) break
        if (count > maxBytes - total) throw IOException("Input exceeds the configured size limit.")
        output.write(buffer, 0, count)
        total += count
    }
    return output.toByteArray()
}

/** Checks Content-Length early, then still enforces the limit while streaming the response. */
internal fun ResponseBody.readBoundedBytes(maxBytes: Int): ByteArray {
    require(maxBytes > 0)
    if (contentLength() > maxBytes) throw IOException("Response exceeds the configured size limit.")
    return byteStream().use { it.readBoundedBytes(maxBytes) }
}
