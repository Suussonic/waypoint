package com.waypoint.app

import org.json.JSONObject
import java.net.URI

/** Only stable, explicitly versioned APKs from this project's release channel are updates. */
data class ReleaseUpdate(val version: String, val versionCode: Int, val url: String, val sha256: String, val size: Long, val notes: String) {
    companion object {
        const val REPOSITORY = "the-geeky-couldron/waypoint"
        const val PAGE = "https://github.com/$REPOSITORY/releases/latest"
        const val API = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        const val MAX_BYTES = 150L * 1024 * 1024
        fun versionCode(version: String): Int? {
            val match = Regex("(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]{0,2})")
                .matchEntire(version) ?: return null
            val (major, minor, patch) = match.destructured
            return major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()
        }
        fun parse(json: String, installedCode: Int): ReleaseUpdate? {
            require(json.length <= 1_000_000) { "The release response is too large." }
            JsonSafety.checkDepth(json)
            val release = JSONObject(json)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null
            val tag = release.getString("tag_name")
            require(tag.startsWith("v")) { "The release has an unsupported version tag." }
            val version = tag.drop(1)
            val code = requireNotNull(versionCode(version)) { "The release has an unsupported version." }
            if (code <= installedCode) return null
            val assets = release.getJSONArray("assets")
            val matches = (0 until assets.length()).map { assets.getJSONObject(it) }.filter { it.optString("name") == "Waypoint.apk" && it.optString("state") == "uploaded" }
            require(matches.size == 1) { "The latest release does not have a ready Android download yet. Try again shortly." }
            val asset = matches.single()
            val url = asset.getString("browser_download_url")
            require(url == "https://github.com/$REPOSITORY/releases/download/$tag/Waypoint.apk") { "The update download is outside the trusted release channel." }
            val digest = asset.optString("digest")
            require(Regex("sha256:[a-fA-F0-9]{64}").matches(digest)) { "The release checksum is not ready. Try again shortly." }
            val size = asset.getLong("size")
            require(size in 1..MAX_BYTES) { "The update has an invalid download size." }
            return ReleaseUpdate(version, code, url, digest.substringAfter(':').lowercase(), size, release.optString("body").take(5000))
        }
        fun trustedDownloadHost(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
                (uri.host == "github.com" || uri.host == "api.github.com" || uri.host == "release-assets.githubusercontent.com" || uri.host == "objects.githubusercontent.com")
        }.getOrDefault(false)
    }
}
