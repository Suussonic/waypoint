package com.waypoint.app

import android.content.Context
import org.json.JSONArray

internal data class MapPoiCategory(
    val id: String,
    val label: String,
    val group: String,
    val classes: List<String>,
    val subclasses: List<String>,
)

/** One category registry drives map sprites, source-layer filters, and both map keys. */
internal object MapPoiCategories {
    private const val COLUMN_COUNT = 5
    private const val CELL_SIZE = 128
    @Volatile private var cached: List<MapPoiCategory>? = null

    fun all(context: Context): List<MapPoiCategory> = cached ?: synchronized(this) {
        cached ?: parse(context.assets.open("poi_categories.json").bufferedReader().use { it.readText() })
            .also { cached = it }
    }

    fun columns() = COLUMN_COUNT
    fun cellSize() = CELL_SIZE
    fun atlasWidth() = COLUMN_COUNT * CELL_SIZE
    fun atlasHeight(categoryCount: Int) = ((categoryCount + COLUMN_COUNT - 1) / COLUMN_COUNT) * CELL_SIZE

    fun parse(json: String): List<MapPoiCategory> {
        val rows = JSONArray(json)
        require(rows.length() in 32..64) { "The map key needs a broad, manageable POI set." }
        val categories = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            fun strings(key: String) = row.getJSONArray(key).let { values ->
                (0 until values.length()).map(values::getString)
            }
            MapPoiCategory(
                id = row.getString("id"),
                label = row.getString("label"),
                group = row.getString("group"),
                classes = strings("classes"),
                subclasses = strings("subclasses"),
            )
        }
        require(categories.firstOrNull()?.id == "place") { "The generic POI icon must be the first sprite." }
        require(categories.map { it.id }.distinct().size == categories.size) { "POI icon ids must be unique." }
        require(categories.all { it.id.matches(Regex("[a-z][a-z0-9_]{1,31}")) && it.label.isNotBlank() }) {
            "POI ids and labels must be valid."
        }
        return categories
    }
}
