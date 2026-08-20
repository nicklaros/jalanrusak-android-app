package com.jalanrusak.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.databinding.ActivityHomeBinding
import com.jalanrusak.ui.login.LoginActivity
import com.jalanrusak.ui.overlay.QuickReportOverlay
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var topAreasAdapter: TopAreasAdapter
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            (application as JalanRusakApp).provideGetTopAreasUseCase(),
            (application as JalanRusakApp).provideLoginUseCase()
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopAreasList()
        setupUI()
        observeState()
        refreshPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLoginStatus()
        refreshPermissionStatus()
    }

    private fun setupTopAreasList() {
        topAreasAdapter = TopAreasAdapter(
            onItemClick = { topArea ->
                // Could show details or navigate to map in the future
                Toast.makeText(
                    this,
                    "${topArea.name}: ${topArea.reportCount} laporan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.topAreasRecycler.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = topAreasAdapter
        }
    }

    private fun setupUI() {
        binding.reportNowButton.setOnClickListener {
            onReportNowClicked()
        }

        binding.grantButton.setOnClickListener { requestPermissions() }

        binding.logoutButton.setOnClickListener { viewModel.logout() }

        binding.retryButton.setOnClickListener {
            viewModel.loadTopAreas()
        }
    }

    private fun onReportNowClicked() {
        if (viewModel.isLoggedIn.value) {
            submitReport()
        } else {
            showLoginRequiredAndNavigate()
        }
    }

    private fun observeState() {
        // Top areas UI state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeViewModel.HomeUiState.Idle -> {
                            // Initial state
                        }
                        is HomeViewModel.HomeUiState.Loading -> {
                            showLoading()
                        }
                        is HomeViewModel.HomeUiState.Success -> {
                            showTopAreas(state.data)
                        }
                        is HomeViewModel.HomeUiState.Error -> {
                            showError(state.message)
                        }
                    }
                }
            }
        }

        // Login status
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn.collect { isLoggedIn ->
                    updateLoginUI(isLoggedIn)
                }
            }
        }

        // User email
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userEmail.collect { email ->
                    binding.emailText.text = email?.let {
                        getString(R.string.home_logged_in_as, it)
                    } ?: getString(R.string.home_logged_in_plain)
                }
            }
        }

        // Show login prompt
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showLoginPrompt.collect { show ->
                    if (show) {
                        showLoginRequiredAndNavigate()
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.topAreasRecycler.visibility = View.GONE
        binding.errorText.visibility = View.GONE
        binding.retryButton.visibility = View.GONE
    }

    private fun showTopAreas(data: List<com.jalanrusak.data.api.dto.TopAreaResponse>) {
        binding.loadingLayout.visibility = View.GONE
        binding.topAreasRecycler.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.retryButton.visibility = View.GONE
        topAreasAdapter.submitList(data)
    }

    private fun showError(message: String) {
        binding.loadingLayout.visibility = View.GONE
        binding.topAreasRecycler.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.retryButton.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    private fun updateLoginUI(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            binding.logoutButton.visibility = View.VISIBLE
        } else {
            binding.logoutButton.visibility = View.GONE
        }
    }

    private fun showLoginRequiredAndNavigate() {
        Toast.makeText(
            this@HomeActivity,
            R.string.home_login_required,
            Toast.LENGTH_LONG
        ).show()
        viewModel.clearLoginPrompt()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun submitReport() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, R.string.home_need_location_permission, Toast.LENGTH_LONG).show()
            requestPermissions()
            return
        }
        startActivity(Intent(this, QuickReportOverlay::class.java))
    }

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun refreshPermissionStatus() {
        val locationGranted = hasLocationPermission()

        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        setStatus(binding.locationStatusText, locationGranted)
        setStatus(binding.notificationStatusText, notificationsGranted)

        binding.grantButton.visibility =
            if (locationGranted && notificationsGranted) View.GONE else View.VISIBLE
    }

    private fun setStatus(view: TextView, granted: Boolean) {
        if (granted) {
            view.text = getString(R.string.home_permission_granted)
            view.setTextColor(ContextCompat.getColor(this, R.color.success))
        } else {
            view.text = getString(R.string.home_permission_denied)
            view.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }
}

// Factory for HomeViewModel
class HomeViewModelFactory(
    private val getTopAreasUseCase: com.jalanrusak.domain.usecase.GetTopAreasUseCase,
    private val loginUseCase: com.jalanrusak.domain.usecase.LoginUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(getTopAreasUseCase, loginUseCase) as T
    }
}
