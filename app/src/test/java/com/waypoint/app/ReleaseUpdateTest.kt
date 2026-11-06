package com.waypoint.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReleaseUpdateTest {
    private fun fixture(tag: String = "v1.0.0") = JSONObject().put("tag_name", tag).put("draft", false).put("prerelease", false)
        .put("body", "New maps and fixes").put("assets", JSONArray().put(JSONObject().put("name", "Waypoint.apk").put("state", "uploaded")
            .put("size", 54000000).put("digest", "sha256:" + "a".repeat(64))
            .put("browser_download_url", "https://github.com/${ReleaseUpdate.REPOSITORY}/releases/download/$tag/Waypoint.apk")))
    private fun rejects(json: JSONObject) { assertThrows(IllegalArgumentException::class.java) { ReleaseUpdate.parse(json.toString(), 1) } }
    @Test fun readsVerifiedNewRelease() { val r = ReleaseUpdate.parse(fixture().toString(), 2003)!!; assertEquals(1_000_000, r.versionCode); assertEquals("1.0.0", r.version); assertEquals(54000000, r.size) }
    @Test fun sameVersionIsNotAnUpdate() { assertNull(ReleaseUpdate.parse(fixture().toString(), 1_000_000)) }
    @Test fun olderVersionIsNotAnUpdate() { assertNull(ReleaseUpdate.parse(fixture("v0.2.0").toString(), 1_000_000)) }
    @Test fun ignoresDrafts() { assertNull(ReleaseUpdate.parse(fixture().put("draft", true).toString(), 1)) }
    @Test fun ignoresPrereleases() { assertNull(ReleaseUpdate.parse(fixture().put("prerelease", true).toString(), 1)) }
    @Test fun rejectsNonVersionTag() { rejects(fixture("latest")) }
    @Test fun rejectsBetaTags() { rejects(fixture("v0.3.0-beta")) }
    @Test fun rejectsMissingApk() { rejects(fixture().put("assets", JSONArray())) }
    @Test fun rejectsMissingChecksum() { val f=fixture();f.getJSONArray("assets").getJSONObject(0).remove("digest");rejects(f) }
    @Test fun rejectsForeignDownload() { val f=fixture();f.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://evil.example/Waypoint.apk");rejects(f) }
    @Test fun acceptsLargerReleaseWhenSizeIsDeclared() {
        val size = 200L * 1024 * 1024
        val f = fixture()
        f.getJSONArray("assets").getJSONObject(0).put("size", size)
        assertEquals(size, ReleaseUpdate.parse(f.toString(), 1)!!.size)
    }
    @Test fun rejectsZeroAndNegativeSizes() {
        for (size in listOf(0, -1)) {
            val f = fixture()
            f.getJSONArray("assets").getJSONObject(0).put("size", size)
            rejects(f)
        }
    }
    @Test fun rejectsDuplicateApks() { val f=fixture();f.getJSONArray("assets").put(f.getJSONArray("assets").getJSONObject(0));rejects(f) }
    @Test fun versionOrderingIsNumeric() {
        assertTrue(ReleaseUpdate.versionCode("0.10.0")!! > ReleaseUpdate.versionCode("0.9.99")!!)
        assertEquals(1_002_003, ReleaseUpdate.versionCode("1.2.3"))
        assertEquals(1_000_000, ReleaseUpdate.versionCode("1.0.0"))
        assertTrue(ReleaseUpdate.versionCode("1.0.1")!! > ReleaseUpdate.versionCode("1.0.0")!!)
    }
    @Test fun rejectsAmbiguousVersions() { listOf("01.2.3","1.1000.0","1.0","-1.0.0","1.0.0+2","1.0.0-r1").forEach { assertNull(ReleaseUpdate.versionCode(it)) } }
    @Test fun restrictsDownloadRedirects() { assertTrue(ReleaseUpdate.trustedDownloadHost("https://release-assets.githubusercontent.com/file"));listOf("http://github.com/a","https://github.com.evil.test/a","https://github.com:444/a","https://user@github.com/a").forEach { assertFalse(ReleaseUpdate.trustedDownloadHost(it)) } }
}
