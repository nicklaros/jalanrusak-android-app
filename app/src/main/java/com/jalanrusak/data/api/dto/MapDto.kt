package com.jalanrusak.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Map Response DTO - GeoJSON FeatureCollection for public map display
 */
data class MapResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("features")
    val features: List<MapFeature>,

    @SerializedName("meta")
    val meta: MapMeta
)

/**
 * Map Feature DTO - Single GeoJSON feature
 */
data class MapFeature(
    @SerializedName("type")
    val type: String,

    @SerializedName("geometry")
    val geometry: MapGeometry,

    @SerializedName("properties")
    val properties: MapProperties
)

/**
 * Map Geometry DTO - GeoJSON geometry
 */
data class MapGeometry(
    @SerializedName("type")
    val type: String,

    @SerializedName("coordinates")
    val coordinates: List<List<Double>>  // [[lng, lat], ...] for LineString
)

/**
 * Map Properties DTO - Feature metadata
 */
data class MapProperties(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Map Meta DTO - Response metadata
 */
data class MapMeta(
    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("has_more")
    val hasMore: Boolean
)
