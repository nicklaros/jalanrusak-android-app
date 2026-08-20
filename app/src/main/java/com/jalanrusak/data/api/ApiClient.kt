package com.jalanrusak.data.api

import com.jalanrusak.BuildConfig
import com.jalanrusak.data.api.dto.*
import com.jalanrusak.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.jalanrusak.com/api/v1/"

    private lateinit var apiService: JalanRusakApi
    private lateinit var tokenManager: TokenManager

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = runBlocking { tokenManager.getAccessToken() }

        val requestWithAuth = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }

        chain.proceed(requestWithAuth)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(JalanRusakApi::class.java)
    }

    fun getApi(): JalanRusakApi {
        if (!::apiService.isInitialized) {
            throw IllegalStateException("ApiClient not initialized. Call init() first.")
        }
        return apiService
    }
}

interface JalanRusakApi {

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    // Reports
    @POST("damaged-roads")
    suspend fun createReport(@Body request: CreateReportRequest): Response<ReportResponse>
}

// Wrapper for Response to handle errors more easily
inline fun <reified T> Response<T>.getBodyOrThrow(): T {
    if (isSuccessful && body() != null) {
        return body()!!
    }

    val errorBody = errorBody()?.string()
    throw ApiException(errorBody ?: "Unknown error")
}

class ApiException(message: String) : Exception(message)
