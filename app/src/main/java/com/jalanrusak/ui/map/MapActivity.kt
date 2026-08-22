package com.jalanrusak.ui.map

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.databinding.ActivityMapBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tileonline.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val markers = mutableListOf<Marker>()
    private val polylines = mutableListOf<Polyline>()

    // Debounce job for map movements
    private var mapMovementJob: Job? = null

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(
            (application as JalanRusakApp).provideGetMapReportsUseCase()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        observeState()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.map.onDetach()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    private fun setupMap() {
        val map = binding.map

        // Set tile source (OpenStreetMap)
        map.setTileSource(TileSourceFactory.MAPNIK)

        // Enable zoom controls
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(MapView.ZoomControllerVisibility.SHOW_AND_FADEOUT)

        // Set initial position (Indonesia)
        val indonesia = GeoPoint(-2.5489, 118.0149)
        map.controller.setZoom(5.0)
        map.controller.setCenter(indonesia)

        // Listen for map changes with proper debounce
        val mapListener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent): Boolean {
                debounceMapLoad()
                return false
            }

            override fun onZoom(event: org.osmdroid.events.ZoomEvent): Boolean {
                debounceMapLoad()
                return false
            }
        }

        map.addMapListener(mapListener)

        // Initial load (no debounce needed for first load)
        loadReportsForCurrentBounds()
    }

    /**
     * Debounce map loading - only loads after user stops moving the map
     * Cancels any pending requests and waits 1500ms after the last movement
     */
    private fun debounceMapLoad() {
        // Cancel any pending load
        mapMovementJob?.cancel()

        // Start a new debounced load
        mapMovementJob = lifecycleScope.launch {
            delay(1500) // Wait 1.5 seconds after last movement
            loadReportsForCurrentBounds()
        }
    }

    private fun loadReportsForCurrentBounds() {
        val bounds = binding.map.boundingBox
        viewModel.loadReportsForBounds(bounds)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MapViewModel.MapUiState.Loading -> {
                            showLoading(true)
                        }
                        is MapViewModel.MapUiState.Success -> {
                            displayReports(state.features)
                            showMoreIndicator(false, 0)
                            showLoading(false)
                        }
                        is MapViewModel.MapUiState.SuccessWithMore -> {
                            displayReports(state.features)
                            showMoreIndicator(true, state.total)
                            showLoading(false)
                        }
                        is MapViewModel.MapUiState.Error -> {
                            showError(state.message)
                            showLoading(false)
                        }
                        is MapViewModel.MapUiState.Idle -> {
                            // Initial state
                        }
                    }
                }
            }
        }
    }

    private fun displayReports(features: List<com.jalanrusak.data.api.dto.MapFeature>) {
        // Clear existing overlays
        markers.forEach { binding.map.overlays.remove(it) }
        polylines.forEach { binding.map.overlays.remove(it) }
        markers.clear()
        polylines.clear()

        features.forEach { feature ->
            val geometry = feature.geometry
            if (geometry.type == "Point") {
                // Single point - add as marker
                val coords = geometry.coordinates.first()
                val position = GeoPoint(coords[1], coords[0])

                val marker = Marker(binding.map).apply {
                    this.position = position
                    this.title = feature.properties.title
                    this.subDescription = "Status: ${feature.properties.status}"
                    this.icon = getStatusIcon(feature.properties.status)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                binding.map.overlays.add(marker)
                markers.add(marker)
            } else if (geometry.type == "LineString") {
                // Line - draw polyline
                val polyline = Polyline().apply {
                    setPoints(geometry.coordinates.map { coord ->
                        GeoPoint(coord[1], coord[0])
                    })
                    outlinePaint.color = getStatusColor(feature.properties.status)
                    outlinePaint.strokeWidth = 8f
                }

                binding.map.overlays.add(polyline)
                polylines.add(polyline)
            }
        }

        binding.map.invalidate()
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "verified" -> 0xFF4CAF40.toInt()  // Green
            "under_verification" -> 0xFFFF9800.toInt()  // Orange
            "submitted" -> 0xFF2196F3.toInt()  // Blue
            "pending_resolved" -> 0xFF9C27B0.toInt()  // Purple
            else -> 0xFFF44336.toInt()  // Red
        }
    }

    private fun getStatusIcon(status: String): GradientDrawable? {
        // Return a colored drawable based on status
        val color = when (status) {
            "verified" -> 0xFF4CAF40.toInt()
            "under_verification" -> 0xFFFF9800.toInt()
            "submitted" -> 0xFF2196F3.toInt()
            "pending_resolved" -> 0xFF9C27B0.toInt()
            else -> 0xFFF44336.toInt()
        }

        // Create a simple circle marker with the status color
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(120, 120) // Size in pixels
        }
    }

    private fun showMoreIndicator(show: Boolean, total: Int) {
        val moreText = binding.moreIndicator
        if (show) {
            moreText.text = getString(R.string.map_more_results, total)
            moreText.visibility = View.VISIBLE
        } else {
            moreText.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// Factory for MapViewModel
class MapViewModelFactory(
    private val getMapReportsUseCase: com.jalanrusak.domain.usecase.GetMapReportsUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MapViewModel(getMapReportsUseCase) as T
    }
}
