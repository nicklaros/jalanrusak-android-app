package com.jalanrusak.domain.usecase

import com.jalanrusak.data.api.dto.TopAreasListResponse
import com.jalanrusak.data.repository.ReportRepository
import com.jalanrusak.util.Result

class GetTopAreasUseCase(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(level: String = "city"): Result<TopAreasListResponse> {
        return reportRepository.getTopAreas(level)
    }
}
