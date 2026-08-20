package com.jalanrusak.data.repository

import com.jalanrusak.data.api.ApiClient
import com.jalanrusak.data.api.getBodyOrThrow
import com.jalanrusak.data.api.dto.CreateReportRequest
import com.jalanrusak.data.api.dto.PointDto
import com.jalanrusak.data.api.dto.ReportResponse
import com.jalanrusak.util.Result
import com.jalanrusak.util.catchSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ReportRepository {

    private val api = ApiClient.getApi()

    suspend fun submitQuickReport(lat: Double, lng: Double, title: String? = null): Result<ReportResponse> = withContext(Dispatchers.IO) {
        Result.catchSuspend {
            val reportTitle = title ?: generateDefaultTitle(lat, lng)

            val request = CreateReportRequest(
                title = reportTitle,
                pathPoints = listOf(PointDto(lat, lng)),
                photoUrls = null,
                description = null,
                subdistrictCode = null
            )

            val response = api.createReport(request)
            response.getBodyOrThrow()
        }
    }

    private fun generateDefaultTitle(lat: Double, lng: Double): String {
        val date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(Date())
        return "Kerusakan jalan - $date"
    }
}
