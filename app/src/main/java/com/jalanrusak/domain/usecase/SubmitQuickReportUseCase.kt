package com.jalanrusak.domain.usecase

import com.jalanrusak.data.repository.ReportRepository
import com.jalanrusak.data.repository.AuthRepository
import com.jalanrusak.util.Result

class SubmitQuickReportUseCase(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) {
    sealed class QuickReportResult {
        data class Success(val reportId: String, val title: String) : QuickReportResult()
        data class Error(val message: String) : QuickReportResult()
        object NotLoggedIn : QuickReportResult()
    }

    suspend operator fun invoke(lat: Double, lng: Double): QuickReportResult {
        // Check if user is logged in
        if (!authRepository.isLoggedIn()) {
            return QuickReportResult.NotLoggedIn
        }

        // Validate coordinates
        if (lat < -11 || lat > 6) {
            return QuickReportResult.Error("Koordinat latitude tidak valid")
        }
        if (lng < 95 || lng > 141) {
            return QuickReportResult.Error("Koordinat longitude tidak valid")
        }

        // Submit report
        return when (val result = reportRepository.submitQuickReport(lat, lng)) {
            is Result.Success -> {
                QuickReportResult.Success(
                    reportId = result.data.id,
                    title = result.data.title
                )
            }
            is Result.Error -> {
                QuickReportResult.Error(result.message)
            }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    suspend fun getUserEmail(): String? {
        return authRepository.getUserEmail()
    }
}
