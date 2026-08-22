package com.jalanrusak.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Top Area Response DTO - represents one ranked area entry
 */
data class TopAreaResponse(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("level")
    val level: String,
    @SerializedName("report_count")
    val reportCount: Int,
    @SerializedName("province_name")
    val provinceName: String?,
    @SerializedName("city_name")
    val cityName: String?,
    @SerializedName("district_name")
    val districtName: String?
)

/**
 * Top Areas List Response DTO
 */
data class TopAreasListResponse(
    @SerializedName("level")
    val level: String,
    @SerializedName("data")
    val data: List<TopAreaResponse>
)
