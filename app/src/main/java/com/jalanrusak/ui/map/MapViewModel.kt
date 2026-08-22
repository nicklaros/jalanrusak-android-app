package com.jalanrusak.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jalanrusak.data.api.dto.MapFeature
import com.jalanrusak.domain.usecase.GetMapReportsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

class MapViewModel(
    private val getMapReportsUseCase: GetMapReportsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadReportsForBounds(bounds: BoundingBox) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            try {
                // OSMDroid BoundingBox: lonWest, latSouth, lonEast, latNorth
                // API expects: minLng,minLat,maxLng,maxLat
                val bbox = "${bounds.lonWest},${bounds.latSouth}," +
                          "${bounds.lonEast},${bounds.latNorth}"

                val result = getMapReportsUseCase(bbox)

                when (result) {
                    is com.jalanrusak.util.Result.Success -> {
                        val response = result.data
                        if (response.meta.hasMore) {
                            _uiState.value = MapUiState.SuccessWithMore(
                                response.features,
                                response.meta.total
                            )
                        } else {
                            _uiState.value = MapUiState.Success(
                                response.features,
                                response.meta.total
                            )
                        }
                    }
                    is com.jalanrusak.util.Result.Error -> {
                        _uiState.value = MapUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class MapUiState {
        object Idle : MapUiState()
        object Loading : MapUiState()
        data class Success(val features: List<MapFeature>, val total: Int) : MapUiState()
        data class SuccessWithMore(val features: List<MapFeature>, val total: Int) : MapUiState()
        data class Error(val message: String) : MapUiState()
    }
}
