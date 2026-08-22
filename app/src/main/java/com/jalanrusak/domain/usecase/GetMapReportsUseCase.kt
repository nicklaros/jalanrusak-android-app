package com.jalanrusak.domain.usecase

import com.jalanrusak.data.api.ApiClient
import com.jalanrusak.data.api.dto.MapResponse
import com.jalanrusak.util.Result

/**
 * Use case for retrieving damaged roads for public map display
 */
class GetMapReportsUseCase(
    private val apiClient: ApiClient
) {
    suspend operator fun invoke(
        bbox: String,
        status: String? = null,
        limit: Int = 100
    ): Result<MapResponse> {
        return try {
            val response = apiClient.getMapReports(bbox, status, limit)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
