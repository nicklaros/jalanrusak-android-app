package com.jalanrusak.ui.overlay

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.jalanrusak.R
import com.jalanrusak.databinding.OverlayQuickReportBinding
import com.jalanrusak.service.QuickReportService
import kotlinx.coroutines.launch

class QuickReportOverlay : Activity() {

    private lateinit var binding: OverlayQuickReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up overlay window
        setFinishOnTouchOutside(false)

        binding = OverlayQuickReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindow()
        observeState()
    }

    private fun setupWindow() {
        val params = window.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.CENTER
        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        window.attributes = params
    }

    private fun observeState() {
        lifecycleScope.launch {
            QuickReportService.reportState.collect { state ->
                when (state) {
                    is QuickReportService.ReportState.Idle -> {
                        // Service not started yet or completed
                        finish()
                    }
                    is QuickReportService.ReportState.Locating -> {
                        showLocatingState()
                    }
                    is QuickReportService.ReportState.Submitting -> {
                        showSubmittingState()
                    }
                    is QuickReportService.ReportState.Success -> {
                        showSuccessState(state.reportId, state.title)
                        // Auto-close after 2 seconds
                        kotlinx.coroutines.delay(2000)
                        finish()
                    }
                    is QuickReportService.ReportState.Error -> {
                        showErrorState(state.message)
                        // Auto-close after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        finish()
                    }
                    is QuickReportService.ReportState.NotLoggedIn -> {
                        showNotLoggedInState()
                        // Auto-close after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        finish()
                    }
                }
            }
        }
    }

    private fun showLocatingState() {
        binding.iconImage.setImageResource(R.drawable.ic_location)
        binding.messageText.text = getString(R.string.quick_report_locating)
        binding.progressBar.visibility = android.view.VISIBLE
    }

    private fun showSubmittingState() {
        binding.iconImage.setImageResource(R.drawable.ic_upload)
        binding.messageText.text = getString(R.string.quick_report_submitting)
        binding.progressBar.visibility = android.view.VISIBLE
    }

    private fun showSuccessState(reportId: String, title: String) {
        binding.iconImage.setImageResource(R.drawable.ic_success)
        binding.messageText.text = getString(R.string.quick_report_success)
        binding.progressBar.visibility = android.view.GONE
    }

    private fun showErrorState(message: String) {
        binding.iconImage.setImageResource(R.drawable.ic_error)
        binding.messageText.text = message
        binding.progressBar.visibility = android.view.GONE
    }

    private fun showNotLoggedInState() {
        binding.iconImage.setImageResource(R.drawable.ic_error)
        binding.messageText.text = getString(R.string.error_not_logged_in)
        binding.progressBar.visibility = android.view.GONE
    }
}
