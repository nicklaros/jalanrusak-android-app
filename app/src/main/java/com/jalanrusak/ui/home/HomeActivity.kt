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
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.databinding.ActivityHomeBinding
import com.jalanrusak.ui.login.LoginActivity
import com.jalanrusak.ui.login.LoginViewModel
import com.jalanrusak.ui.login.LoginViewModelFactory
import com.jalanrusak.ui.overlay.QuickReportOverlay
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: LoginViewModel by viewModels {
        // TODO: Inject dependencies properly
        LoginViewModelFactory((application as JalanRusakApp).provideLoginUseCase())
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

        setupUI()
        observeState()
        refreshPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Permissions may also have been granted from system settings
        refreshPermissionStatus()
    }

    private fun setupUI() {
        binding.reportNowButton.setOnClickListener { submitReport() }
        binding.grantButton.setOnClickListener { requestPermissions() }
        binding.logoutButton.setOnClickListener { viewModel.logout() }
    }

    private fun submitReport() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, R.string.home_need_location_permission, Toast.LENGTH_LONG).show()
            requestPermissions()
            return
        }
        startActivity(Intent(this, QuickReportOverlay::class.java))
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userEmail.collect { email ->
                    binding.emailText.text = email?.let {
                        getString(R.string.home_logged_in_as, it)
                    } ?: getString(R.string.home_logged_in_plain)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn.collect { isLoggedIn ->
                    if (!isLoggedIn) {
                        goToLogin()
                    }
                }
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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

        // Notifications don't need a runtime permission before Android 13
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
