package com.jalanrusak.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Point DTO for GPS coordinates
 */
data class PointDto(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

/**
 * Create report request DTO
 */
data class CreateReportRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("path_points")
    val pathPoints: List<PointDto>,
    @SerializedName("photo_urls")
    val photoUrls: List<String>? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("subdistrict_code")
    val subdistrictCode: String? = null
)

/**
 * Report response DTO
 */
data class ReportResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("subdistrict_code")
    val subdistrictCode: String?,
    @SerializedName("path")
    val path: GeometryDto,
    @SerializedName("description")
    val description: String?,
    @SerializedName("photo_urls")
    val photoUrls: List<String>,
    @SerializedName("author_id")
    val authorId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Geometry DTO (Point or LineString)
 */
data class GeometryDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>
)

/**
 * Error response DTO
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: String?,
    @SerializedName("message")
    val message: String
)
