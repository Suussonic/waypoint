package com.waypoint.app

import android.graphics.RectF
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Selects the nearest centerline only when it is close enough to be a safe visual correction. */
internal fun nearestRoadProjection(
    position:Coordinate,
    roads:List<List<Coordinate>>,
    heading:Float?,
    maxShiftMeters:Double,
    maxHeadingError:Float=55f,
):Coordinate? {
    if(!position.isValid() || !maxShiftMeters.isFinite() || maxShiftMeters<=0.0)return null
    val metresPerLatitude=111_320.0
    val metresPerLongitude=metresPerLatitude*cos(Math.toRadians(position.lat)).coerceAtLeast(1e-4)
    var nearest:Coordinate?=null
    var nearestDistance=Double.MAX_VALUE
    for(road in roads) for(index in 0 until road.lastIndex) {
        val a=road[index];val b=road[index+1]
        if(!a.isValid() || !b.isValid() || distance(a,b)<.2)continue
        if(heading!=null && heading.isFinite()) {
            val roadBearing=bearingBetween(a,b)
            val difference=kotlin.math.abs(headingDelta(heading,roadBearing))
            val undirected=min(difference,180f-difference)
            if(undirected>maxHeadingError)continue
        }
        val ax=shortestLongitudeDelta(position.lon,a.lon)*metresPerLongitude
        val ay=(a.lat-position.lat)*metresPerLatitude
        val bx=shortestLongitudeDelta(position.lon,b.lon)*metresPerLongitude
        val by=(b.lat-position.lat)*metresPerLatitude
        val dx=bx-ax;val dy=by-ay
        val lengthSquared=dx*dx+dy*dy
        if(lengthSquared<.04)continue
        val t=(-(ax*dx+ay*dy)/lengthSquared).coerceIn(0.0,1.0)
        val east=ax+dx*t;val north=ay+dy*t
        val gap=kotlin.math.hypot(east,north)
        if(gap<nearestDistance) {
            nearestDistance=gap
            nearest=Coordinate(a.lat+(b.lat-a.lat)*t,wrappedLongitude(a.lon+shortestLongitudeDelta(a.lon,b.lon)*t))
        }
    }
    return nearest?.takeIf { nearestDistance<=maxShiftMeters }
}

/** MapLibre's rendered road layers provide a world-independent visual centerline for the position marker. */
internal fun MapLibreMap.snapToVisibleRoad(position:Coordinate,mode:TravelMode,heading:Float?,accuracyMeters:Float):Coordinate? {
    val style=style ?: return null
    val roadLayers=style.layers.filterIsInstance<LineLayer>().filter { layer ->
        val id=layer.id.lowercase()
        val road=layer.sourceLayer=="transportation" && id.contains("road") && !id.contains("path") &&
            !id.contains("casing") && !id.contains("rail") && !id.contains("ferry")
        val path=(mode==TravelMode.WALK || mode==TravelMode.BICYCLE) && id.contains("path") &&
            !id.contains("rail") && !id.contains("ferry")
        road || path
    }.map { it.id }.distinct()
    if(roadLayers.isEmpty())return null

    val screen=projection.toScreenLocation(LatLng(position.lat,position.lon))
    val queryRadius=36f
    val features=queryRenderedFeatures(RectF(screen.x-queryRadius,screen.y-queryRadius,screen.x+queryRadius,screen.y+queryRadius),*roadLayers.toTypedArray())
    if(features.isEmpty())return null
    val polylines=features.flatMap(::roadPolylines)
    val accuracy=accuracyMeters.takeIf { it.isFinite() && it>0f }?.toDouble() ?: 0.0
    val maxShift=max(4.0,(accuracy*.7).coerceAtMost(7.0))
    return nearestRoadProjection(position,polylines,heading,maxShift)
}

private fun roadPolylines(feature:Feature):List<List<Coordinate>> = when(val geometry=feature.geometry()) {
    is LineString -> listOf(geometry.coordinates().map { Coordinate(it.latitude(),it.longitude()) })
    is MultiLineString -> geometry.coordinates().map { line -> line.map { Coordinate(it.latitude(),it.longitude()) } }
    else -> emptyList()
}
