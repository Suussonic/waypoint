package com.waypoint.app

import java.net.URLDecoder

/** External links propose destinations; they never authorize replacing active guidance. */
internal sealed interface NavigationImport {
    data class Point(val coordinate: Coordinate, val label: String) : NavigationImport
    data class Search(val query: String) : NavigationImport

    companion object {
        private val coordinate = Regex("^([+-]?[0-9]{1,3}(?:\\.[0-9]{1,12})?),\\s*([+-]?[0-9]{1,3}(?:\\.[0-9]{1,12})?)(?:\\s*\\(([^()]{1,120})\\))?$")

        fun parse(action: String?, value: String?): NavigationImport? = runCatching {
            if (action != "android.intent.action.VIEW" || value == null || value.length > 4096) return null
            if (':' !in value) return null
            val scheme = value.substringBefore(':')
            if (scheme != "geo" && scheme != "google.navigation") return null
            val raw = value.substringAfter(':').substringBefore('#')
            if (raw.startsWith("//")) return null
            val parameters = if (scheme == "google.navigation") raw else raw.substringAfter('?', "")
            val queries = parameters.split('&').filter { it.substringBefore('=') == "q" }
            if (queries.size > 1) return null
            val encoded = queries.singleOrNull()?.substringAfter('=', "")
                ?: if (scheme == "geo") raw.substringBefore('?') else return null
            // Decode once, after splitting the encoded parameters. Preserve '+' in a bare geo pair.
            val text = URLDecoder.decode(if (queries.isEmpty()) encoded.replace("+", "%2B") else encoded, "UTF-8").trim()
            if (text.isEmpty() || text.length > 256 || text.any { it.isISOControl() }) return null
            val match = coordinate.matchEntire(text)
            if (match != null) {
                val point = Coordinate(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
                if (!point.isValid()) return null
                Point(point, match.groupValues[3].trim().ifBlank { "Shared destination" })
            } else {
                // A malformed coordinate pair must not silently become a different geocoding query.
                if (Regex("^[+\\-0-9.]+\\s*,").containsMatchIn(text)) return null
                Search(text)
            }
        }.getOrNull()
    }
}

/** A result belongs only to the most recent import and the navigation state it started in. */
internal class NavigationImportGate {
    private var generation = 0L
    fun begin(): Long = ++generation
    fun invalidate() { generation++ }
    fun canApply(ticket: Long, navigating: Boolean): Boolean = ticket == generation && !navigating
}

/** A parsed external link is a proposal until the Waypoint user accepts it. */
internal class NavigationImportConsent {
    private var nextId = 0L
    private var pending: Pair<Long, NavigationImport>? = null
    fun offer(proposal: NavigationImport, navigating: Boolean): Long? {
        if (navigating) { pending = null; return null }
        val id = ++nextId
        pending = id to proposal
        return id
    }
    fun accept(id: Long, navigating: Boolean): NavigationImport? {
        val current = pending ?: return null
        if (current.first != id) return null
        pending = null
        return if (navigating) null else current.second
    }
    fun dismiss(id: Long): Boolean {
        if (pending?.first != id) return false
        pending = null
        return true
    }
    fun dismissAll() { pending = null }
}

internal fun Coordinate.isValid() = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
