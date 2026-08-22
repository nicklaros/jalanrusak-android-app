package com.jalanrusak.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.data.api.dto.MapFeature
import com.jalanrusak.databinding.ActivityMapBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.CustomZoomButtonsDisplay
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val markers = mutableListOf<Marker>()
    private val polylines = mutableListOf<Polyline>()

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(
            (application as JalanRusakApp).provideGetMapReportsUseCase()
        )
    }

    private val clusterRenderer by lazy { ClusterRenderer(binding.map) }
    private var lastFeatures: List<MapFeature>? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var scaleBarOverlay: ScaleBarOverlay? = null
    private var legendExpanded = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) toggleFollow()
        else Toast.makeText(this, R.string.map_location_permission_denied, Toast.LENGTH_LONG).show()
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
        setupLegend()
        binding.locateFab.setOnClickListener { onLocateClicked() }
        observeState()
    }

    override fun onDestroy() {
        super.onDestroy()
        MapIcons.clearCache()
        binding.map.onDetach()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
        myLocationOverlay?.onResume()
        syncLocateFabTint()
    }

    override fun onPause() {
        super.onPause()
        myLocationOverlay?.onPause() // stops location updates
        binding.map.onPause()
    }

    private fun setupMap() {
        val map = binding.map

        // Set tile source (OpenStreetMap)
        map.setTileSource(TileSourceFactory.MAPNIK)

        // Enable zoom controls, positioned bottom-left to clear the locate FAB
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.zoomController.display.setPositions(
            false,
            CustomZoomButtonsDisplay.HorizontalPosition.LEFT,
            CustomZoomButtonsDisplay.VerticalPosition.BOTTOM
        )

        // Scale bar (density-corrected px offsets, lifted above the bottom pill)
        val density = resources.displayMetrics.density
        scaleBarOverlay = ScaleBarOverlay(map).apply {
            setCentred(true)
            setAlignBottom(true)
            setScaleBarOffset(0, (64 * density).toInt())
            setTextSize(12f * density)
            setMinZoom(4.0)
        }.also { map.overlays.add(it) }

        // Set initial position (Indonesia)
        val indonesia = GeoPoint(-2.5489, 118.0149)
        map.controller.setZoom(5.0)
        map.controller.setCenter(indonesia)

        // Listen for map changes: re-cluster instantly on zoom, debounce network loads
        val mapListener = object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent): Boolean {
                syncLocateFabTint() // follow auto-stops silently when the user pans
                debounceMapLoad()
                return false
            }

            override fun onZoom(event: org.osmdroid.events.ZoomEvent): Boolean {
                reclusterFromCache()
                debounceMapLoad()
                return false
            }
        }

        map.addMapListener(mapListener)

        // Decide whether the initial zoom level should fetch data or show a hint
        applyZoomGate()
    }

    /**
     * Debounce map loading - only loads after user stops moving the map
     * Cancels any pending requests and waits 1500ms after the last movement
     */
    private var mapMovementJob: Job? = null

    private fun debounceMapLoad() {
        // Cancel any pending load
        mapMovementJob?.cancel()

        // Start a new debounced load
        mapMovementJob = lifecycleScope.launch {
            delay(1500) // Wait 1.5 seconds after last movement
            loadReportsForCurrentBounds()
        }
    }

    //region Data loading & zoom gate

    private fun isDataZoom(): Boolean = binding.map.zoomLevelDouble >= MIN_DATA_ZOOM

    private fun applyZoomGate() {
        if (isDataZoom()) {
            loadReportsForCurrentBounds()
        } else {
            clearReportOverlays()
            showZoomHint()
        }
    }

    private fun loadReportsForCurrentBounds() {
        if (!isDataZoom()) {
            clearReportOverlays()
            showZoomHint()
            return
        }
        // Instant feedback from cached features while the debounced fetch runs
        lastFeatures?.let { displayReports(it) }
        viewModel.loadReportsForBounds(binding.map.boundingBox)
    }

    private fun reclusterFromCache() {
        if (isDataZoom()) lastFeatures?.let { displayReports(it) }
    }

    private fun clearReportOverlays() {
        markers.forEach { binding.map.overlays.remove(it) }
        polylines.forEach { binding.map.overlays.remove(it) }
        markers.clear()
        polylines.clear()
        binding.map.invalidate()
    }

    private fun showZoomHint() {
        binding.moreIndicator.text = getString(R.string.map_zoom_hint)
        binding.moreIndicator.visibility = View.VISIBLE
    }

    //endregion

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MapViewModel.MapUiState.Loading -> {
                            showLoading(true)
                        }
                        is MapViewModel.MapUiState.Success -> {
                            // Drop stale results that arrived after the user zoomed out
                            if (!isDataZoom()) {
                                showZoomHint()
                                showLoading(false)
                                return@collect
                            }
                            displayReports(state.features)
                            showMoreIndicator(false, 0)
                            showLoading(false)
                        }
                        is MapViewModel.MapUiState.SuccessWithMore -> {
                            if (!isDataZoom()) {
                                showZoomHint()
                                showLoading(false)
                                return@collect
                            }
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

    //region Rendering

    private fun displayReports(features: List<MapFeature>) {
        lastFeatures = features
        // Clear existing overlays (scale bar & my-location overlays are untracked -> survive)
        markers.forEach { binding.map.overlays.remove(it) }
        polylines.forEach { binding.map.overlays.remove(it) }
        markers.clear()
        polylines.clear()

        val points = mutableListOf<ClusterRenderer.Item>()
        features.forEach { feature ->
            when (feature.geometry.type) {
                "Point" -> feature.geometry.coordinates.firstOrNull()?.let { coords ->
                    points += ClusterRenderer.Item(GeoPoint(coords[1], coords[0]), feature)
                }
                "LineString" -> {
                    val polyline = Polyline().apply {
                        setPoints(feature.geometry.coordinates.map { coord ->
                            GeoPoint(coord[1], coord[0])
                        })
                        outlinePaint.color = MapIcons.statusColor(this@MapActivity, feature.properties.status)
                        outlinePaint.strokeWidth = 4f * resources.displayMetrics.density
                    }
                    binding.map.overlays.add(polyline)
                    polylines += polyline
                }
            }
        }

        markers += clusterRenderer.render(points, ::onClusterTap)
        markers.forEach { binding.map.overlays.add(it) }
        binding.map.invalidate()
    }

    private fun onClusterTap(centroid: GeoPoint) {
        val target = minOf(
            binding.map.zoomLevelDouble + CLUSTER_ZOOM_STEP,
            binding.map.maxZoomLevel
        )
        binding.map.controller.animateTo(centroid, target, null)
    }

    //endregion

    //region Locate me

    private fun onLocateClicked() {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        toggleFollow()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun toggleFollow() {
        val overlay = ensureMyLocationOverlay()
        if (overlay == null) {
            // No location provider available (GPS off, etc.)
            Toast.makeText(this, R.string.error_no_location, Toast.LENGTH_LONG).show()
            return
        }
        if (overlay.isFollowLocationEnabled) {
            overlay.disableFollowLocation()
        } else {
            overlay.enableFollowLocation()
            // Jump to the fix if we already have one; runOnFirstFix covers the cold start
            overlay.myLocation?.let { fix ->
                binding.map.controller.animateTo(fix, locateZoom(), null)
            }
        }
        syncLocateFabTint()
    }

    private fun ensureMyLocationOverlay(): MyLocationNewOverlay? {
        if (!hasLocationPermission()) return null // enableMyLocation() would throw
        myLocationOverlay?.let { return it }

        val overlay = MyLocationNewOverlay(binding.map)
        overlay.setDrawAccuracyEnabled(true)
        if (!overlay.enableMyLocation()) return null // provider unavailable

        overlay.runOnFirstFix {
            // Runs on a background looper -> hop to the main thread
            binding.map.post {
                if (overlay.isFollowLocationEnabled) {
                    overlay.myLocation?.let { fix ->
                        binding.map.controller.animateTo(fix, locateZoom(), null)
                    }
                }
            }
        }

        myLocationOverlay = overlay
        // Index 0 is the scale bar; markers are re-appended on render and stay on top
        binding.map.overlays.add(1, overlay)
        return overlay
    }

    /** Locate jumps to street level, but never zooms an already-closer view out. */
    private fun locateZoom(): Double = maxOf(binding.map.zoomLevelDouble, LOCATE_ZOOM)

    private fun syncLocateFabTint() {
        val following = myLocationOverlay?.isFollowLocationEnabled == true
        if (following) {
            binding.locateFab.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
            binding.locateFab.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.map_icon_stroke))
        } else {
            binding.locateFab.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.map_fab_idle))
            binding.locateFab.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.map_cluster_fill))
        }
    }

    //endregion

    private fun setupLegend() {
        binding.legendHeader.setOnClickListener {
            legendExpanded = !legendExpanded
            binding.legendRows.visibility = if (legendExpanded) View.VISIBLE else View.GONE
            binding.legendArrow.rotation = if (legendExpanded) 90f else 0f
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

    companion object {
        /** Below this zoom the API result is country-level noise: no fetch, show a hint. */
        const val MIN_DATA_ZOOM = 11.0
        const val CLUSTER_ZOOM_STEP = 2.0
        const val LOCATE_ZOOM = 13.0
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
