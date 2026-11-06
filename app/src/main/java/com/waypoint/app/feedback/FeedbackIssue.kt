package com.waypoint.app

import java.net.URLEncoder

internal const val WAYPOINT_ISSUES_URL = "https://github.com/the-geeky-couldron/waypoint/issues"
private const val MAX_ISSUE_URL_LENGTH = 6_500

internal enum class FeedbackKind(val label: String) {
    BUG("Bug"), IDEA("Idea"), OTHER("Other")
}

/** GitHub opens this draft for the user to review and submit under their own account. */
internal fun feedbackIssueUrl(
    kind: FeedbackKind,
    title: String,
    details: String,
    world: String,
    version: String,
): String? {
    val cleanTitle = title.trim()
    val cleanDetails = details.trim()
    if (cleanTitle.isEmpty() || cleanDetails.isEmpty()) return null

    val body = buildString {
        append("### My feedback\n\n")
        append(cleanDetails)
        append("\n\n### App context\n\n")
        append("- Type: ").append(kind.label).append('\n')
        append("- World: ").append(world).append('\n')
        append("- Waypoint version: ").append(version)
    }
    val url = "$WAYPOINT_ISSUES_URL/new?title=${encodeIssueField(cleanTitle)}&body=${encodeIssueField(body)}"
    return url.takeIf { it.length <= MAX_ISSUE_URL_LENGTH }
}

private fun encodeIssueField(value: String): String = URLEncoder.encode(value, "UTF-8")
