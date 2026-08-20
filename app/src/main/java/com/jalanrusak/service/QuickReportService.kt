package com.jalanrusak.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.domain.usecase.SubmitQuickReportUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuickReportService : Service() {

    companion object {
        const val ACTION_SUBMIT_REPORT = "com.jalanrusak.ACTION_SUBMIT_REPORT"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"

        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        lateinit var submitUseCase: SubmitQuickReportUseCase
        lateinit var locationManager: LocationManager

        private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
        val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

        sealed class ReportState {
            object Idle : ReportState()
            object Locating : ReportState()
            object Submitting : ReportState()
            data class Success(val reportId: String, val title: String) : ReportState()
            data class Error(val message: String) : ReportState()
            object NotLoggedIn : ReportState()
        }

        fun startReport(context: Context) {
            val intent = Intent(context, QuickReportService::class.java).apply {
                action = ACTION_SUBMIT_REPORT
            }
            context.startService(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        // Service will be created with dependencies injected by Application
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SUBMIT_REPORT -> {
                submitReport()
            }
        }
        return START_NOT_STICKY
    }

    private fun submitReport() {
        serviceScope.launch {
            try {
                // Update state to locating
                _reportState.value = ReportState.Locating

                // Check if logged in
                if (!submitUseCase.isLoggedIn()) {
                    _reportState.value = ReportState.NotLoggedIn
                    showNotification("Gagal", "Silakan masuk terlebih dahulu")
                    stopSelf()
                    return@launch
                }

                // Get location
                val location = locationManager.getCurrentLocation()

                // Update state to submitting
                _reportState.value = ReportState.Submitting

                // Submit report
                when (val result = submitUseCase(location.latitude, location.longitude)) {
                    is SubmitQuickReportUseCase.QuickReportResult.Success -> {
                        _reportState.value = ReportState.Success(result.reportId, result.title)
                        showNotification(
                            "Laporan Berhasil",
                            "Laporan ${result.title} telah dikirim"
                        )
                    }
                    is SubmitQuickReportUseCase.QuickReportResult.Error -> {
                        _reportState.value = ReportState.Error(result.message)
                        showNotification("Gagal", result.message)
                    }
                    is SubmitQuickReportUseCase.QuickReportResult.NotLoggedIn -> {
                        _reportState.value = ReportState.NotLoggedIn
                        showNotification("Gagal", "Silakan masuk terlebih dahulu")
                    }
                }

                // Stop service after completion
                kotlinx.coroutines.delay(2000)
                _reportState.value = ReportState.Idle
                stopSelf()

            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "Terjadi kesalahan")
                showNotification("Gagal", e.message ?: "Terjadi kesalahan")
                stopSelf()
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, JalanRusakApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
