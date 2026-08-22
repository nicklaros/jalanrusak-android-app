package com.jalanrusak

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.jalanrusak.data.api.ApiClient
import com.jalanrusak.data.local.TokenManager
import com.jalanrusak.data.repository.AuthRepository
import com.jalanrusak.data.repository.ReportRepository
import com.jalanrusak.domain.usecase.GetMapReportsUseCase
import com.jalanrusak.domain.usecase.GetTopAreasUseCase
import com.jalanrusak.domain.usecase.LoginUseCase
import com.jalanrusak.domain.usecase.SubmitQuickReportUseCase
import com.jalanrusak.service.LocationManager
import com.jalanrusak.service.QuickReportService

class JalanRusakApp : Application() {

    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    private lateinit var reportRepository: ReportRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var submitQuickReportUseCase: SubmitQuickReportUseCase
    private lateinit var getTopAreasUseCase: GetTopAreasUseCase
    private lateinit var getMapReportsUseCase: GetMapReportsUseCase
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        // Pin the app to light theme: primary use is outdoors in daylight,
        // and map tiles are always light (see values/colors.xml)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        createNotificationChannel()
        initializeDependencies()
    }

    private fun initializeDependencies() {
        // Initialize DataStore
        tokenManager = TokenManager(this)

        // Initialize API client
        ApiClient.init(tokenManager)

        // Initialize repositories
        authRepository = AuthRepository(tokenManager)
        reportRepository = ReportRepository()

        // Initialize use cases
        loginUseCase = LoginUseCase(authRepository)
        submitQuickReportUseCase = SubmitQuickReportUseCase(reportRepository, authRepository)
        getTopAreasUseCase = GetTopAreasUseCase(reportRepository)
        getMapReportsUseCase = GetMapReportsUseCase(ApiClient)

        // Initialize services
        locationManager = LocationManager(applicationContext)

        // Inject dependencies into QuickReportService
        QuickReportService.submitUseCase = submitQuickReportUseCase
        QuickReportService.locationManager = locationManager
    }

    fun provideLoginUseCase(): LoginUseCase = loginUseCase

    fun provideSubmitQuickReportUseCase(): SubmitQuickReportUseCase = submitQuickReportUseCase

    fun provideGetTopAreasUseCase(): GetTopAreasUseCase = getTopAreasUseCase

    fun provideGetMapReportsUseCase(): GetMapReportsUseCase = getMapReportsUseCase

    fun provideLocationManager(): LocationManager = locationManager

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "report_notifications"
    }
}
