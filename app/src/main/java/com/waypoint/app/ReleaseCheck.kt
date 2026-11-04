package com.waypoint.app

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal fun releaseCheckCall(client:OkHttpClient):Call = client.newCall(Request.Builder()
    .url(ReleaseUpdate.API).header("Accept","application/vnd.github+json")
    .header("X-GitHub-Api-Version","2022-11-28")
    .header("User-Agent","Waypoint/${BuildConfig.VERSION_NAME}").build())

/** Shared bounded response/parser for automatic alerts and foreground update checks. */
internal fun readReleaseCheck(call:Call):ReleaseUpdate? = call.execute().use { response ->
    when(response.code) {
        404 -> throw IOException("No public release is available yet. Try again after the first build is published.")
        403,429 -> throw IOException("GitHub's update limit was reached. Please try again later.")
    }
    if(!response.isSuccessful)throw IOException("GitHub is unavailable (${response.code}). Please try again.")
    val body=response.body ?: throw IOException("GitHub returned an empty response.")
    val bytes=body.byteStream().use { input ->
        val output=java.io.ByteArrayOutputStream()
        val buffer=ByteArray(8192)
        while(output.size()<=1_000_000) {
            val count=input.read(buffer)
            if(count<0)break
            output.write(buffer,0,count)
        }
        output.toByteArray()
    }
    require(bytes.size<=1_000_000) { "The release response is too large." }
    ReleaseUpdate.parse(bytes.toString(Charsets.UTF_8),BuildConfig.VERSION_CODE)
}
