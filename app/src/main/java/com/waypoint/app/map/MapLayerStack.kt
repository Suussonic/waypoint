package com.waypoint.app

import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.Layer

/** Put 3D buildings above roads and routes while keeping street names readable. */
internal fun Style.raiseBuildingExtrusionsAboveRoads() {
    val roadLabels = getLayer("waypoint-road-labels") ?: return
    getLayers().filterIsInstance<FillExtrusionLayer>().forEach { extrusion ->
        removeLayer(extrusion)
        addLayerBelow(extrusion, roadLabels.id)
    }
}

/** Route paint stays over the road strokes but passes behind extruded building geometry. */
internal fun Style.addRouteLayerBelowBuildings(layer: Layer) {
    val firstExtrusion = getLayers().firstOrNull { it is FillExtrusionLayer }
    if (firstExtrusion == null) addLayer(layer) else addLayerBelow(layer, firstExtrusion.id)
}
