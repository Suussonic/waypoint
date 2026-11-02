package com.waypoint.app

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

class FeedbackIssueTest {
    @Test fun keepsUsersWordsAndEscapesThemForGitHub() {
        val url = feedbackIssueUrl(
            FeedbackKind.BUG,
            "  I can't see Kolkata & nearby cafés  ",
            "  My map says 5 > 3. कृपया ठीक करें 🙂  ",
            "Old West",
            "1.0.0-r1",
        )!!
        assertEquals("https://github.com/the-geeky-couldron/waypoint/issues/new", url.substringBefore('?'))
        val query = URI(url).rawQuery.split('&').associate { field ->
            val (key, value) = field.split('=', limit = 2)
            key to URLDecoder.decode(value, "UTF-8")
        }
        assertEquals("I can't see Kolkata & nearby cafés", query["title"])
        assertTrue(query["body"]!!.contains("My map says 5 > 3. कृपया ठीक करें 🙂"))
        assertTrue(query["body"]!!.contains("- Type: Bug"))
        assertTrue(query["body"]!!.contains("- World: Old West"))
        assertTrue(query["body"]!!.contains("- Waypoint version: 1.0.0-r1"))
        assertFalse(query["body"]!!.contains("location"))
    }

    @Test fun requiresBothUserFieldsAndBoundsTheLink() {
        assertNull(feedbackIssueUrl(FeedbackKind.IDEA, " ", "I want this", "Pixel World", "1.0.0-r1"))
        assertNull(feedbackIssueUrl(FeedbackKind.IDEA, "A title", " ", "Pixel World", "1.0.0-r1"))
        assertNull(feedbackIssueUrl(FeedbackKind.OTHER, "A title", "🙂".repeat(1_500), "Pixel World", "1.0.0-r1"))
    }
}
