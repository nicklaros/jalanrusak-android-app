package com.jalanrusak.ui.map

import com.jalanrusak.data.api.dto.MapFeature
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Grid-based clustering in screen-pixel space: features that fall into the same
 * 80dp cell are merged into one count bubble. Pure function per call - returns
 * new markers; the caller owns the overlay list. Polyline features are not
 * clustered (handled by the caller).
 */
class ClusterRenderer(private val mapView: MapView) {

    data class Item(val position: GeoPoint, val feature: MapFeature)

    private val cellPx = CELL_SIZE_DP * mapView.resources.displayMetrics.density

    fun render(items: List<Item>, onClusterTap: (centroid: GeoPoint) -> Unit): List<Marker> {
        val projection = mapView.projection
        val buckets = HashMap<Long, MutableList<Item>>(items.size)
        for (item in items) {
            val p = projection.toPixels(item.position, null)
            val key = cellKey(p.x, p.y)
            buckets.getOrPut(key) { mutableListOf() }.add(item)
        }
        return buckets.values.map { bucket ->
            if (bucket.size == 1) pinMarker(bucket[0]) else clusterMarker(bucket, onClusterTap)
        }
    }

    private fun cellKey(x: Int, y: Int): Long =
        ((x / cellPx).toLong() shl 32) or ((y / cellPx).toLong() and 0xFFFFFFFFL)

    private fun pinMarker(item: Item) = Marker(mapView).apply {
        position = item.position
        title = item.feature.properties.title
        subDescription = "Status: ${item.feature.properties.status}"
        icon = MapIcons.pin(mapView.context, item.feature.properties.status)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // tip = geo position
    }

    private fun clusterMarker(bucket: MutableList<Item>, onClusterTap: (GeoPoint) -> Unit): Marker {
        // Averaging is safe for Indonesia (~95-141E); would break across the antimeridian.
        val centroid = GeoPoint(
            bucket.sumOf { it.position.latitude } / bucket.size,
            bucket.sumOf { it.position.longitude } / bucket.size
        )
        return Marker(mapView).apply {
            position = centroid
            icon = MapIcons.cluster(mapView.context, bucket.size)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> onClusterTap(centroid); true } // consume tap, no info window
        }
    }

    companion object {
        const val CELL_SIZE_DP = 80f
    }
}
