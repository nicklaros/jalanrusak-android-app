package com.jalanrusak.ui.overlay

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jalanrusak.R
import com.jalanrusak.databinding.OverlayQuickReportBinding
import com.jalanrusak.service.QuickReportService
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class QuickReportOverlay : AppCompatActivity() {

    private lateinit var binding: OverlayQuickReportBinding

    private companion object {
        // Safety net so the overlay is never stuck on screen if the
        // service dies without reaching a terminal state
        private const val REPORT_TIMEOUT_MS = 20_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up overlay window
        setFinishOnTouchOutside(false)

        binding = OverlayQuickReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The activity is in the foreground now, so the service can be started
        // here even when the whole flow was triggered by a widget tap
        QuickReportService.startReport(this)

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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var sawResult = false
                withTimeoutOrNull(REPORT_TIMEOUT_MS) {
                    QuickReportService.reportState.collect { state ->
                        when (state) {
                            is QuickReportService.ReportState.Idle -> {
                                // Service not started yet; show initial state
                                // (after a result, stay on it until finish())
                                if (!sawResult) showLocatingState()
                            }
                            is QuickReportService.ReportState.Locating -> {
                                showLocatingState()
                            }
                            is QuickReportService.ReportState.Submitting -> {
                                showSubmittingState()
                            }
                            is QuickReportService.ReportState.Success -> {
                                sawResult = true
                                showSuccessState(state.reportId, state.title)
                                // Auto-close after 2 seconds
                                kotlinx.coroutines.delay(2000)
                                finish()
                            }
                            is QuickReportService.ReportState.Error -> {
                                sawResult = true
                                showErrorState(state.message)
                                // Auto-close after 3 seconds
                                kotlinx.coroutines.delay(3000)
                                finish()
                            }
                            is QuickReportService.ReportState.NotLoggedIn -> {
                                sawResult = true
                                showNotLoggedInState()
                                // Auto-close after 3 seconds
                                kotlinx.coroutines.delay(3000)
                                finish()
                            }
                        }
                    }
                }
                // Reached only when the timeout expired without a result
                finish()
            }
        }
    }

    private fun showLocatingState() {
        binding.iconImage.setImageResource(R.drawable.ic_location)
        binding.messageText.text = getString(R.string.quick_report_locating)
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showSubmittingState() {
        binding.iconImage.setImageResource(R.drawable.ic_upload)
        binding.messageText.text = getString(R.string.quick_report_submitting)
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showSuccessState(reportId: String, title: String) {
        binding.iconImage.setImageResource(R.drawable.ic_success)
        binding.messageText.text = getString(R.string.quick_report_success)
        binding.progressBar.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.iconImage.setImageResource(R.drawable.ic_error)
        binding.messageText.text = message
        binding.progressBar.visibility = View.GONE
    }

    private fun showNotLoggedInState() {
        binding.iconImage.setImageResource(R.drawable.ic_error)
        binding.messageText.text = getString(R.string.error_not_logged_in)
        binding.progressBar.visibility = View.GONE
    }
}
