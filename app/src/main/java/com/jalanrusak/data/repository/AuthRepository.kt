package com.jalanrusak.data.repository

import com.jalanrusak.data.api.ApiClient
import com.jalanrusak.data.api.dto.LoginRequest
import com.jalanrusak.data.api.dto.RefreshTokenRequest
import com.jalanrusak.data.local.TokenManager
import com.jalanrusak.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val tokenManager: TokenManager
) {
    private val api = ApiClient.getApi()

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.catchSuspend {
            val response = api.login(LoginRequest(email, password))
            val loginResponse = response.getBodyOrThrow()

            // Save tokens and user info
            tokenManager.saveTokens(
                loginResponse.accessToken,
                loginResponse.refreshToken
            )
            tokenManager.saveUser(
                loginResponse.user.id,
                loginResponse.user.email,
                loginResponse.user.name ?: ""
            )
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.catch {
            tokenManager.clearAll()
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    suspend fun refreshToken(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.catchSuspend {
            val refreshToken = tokenManager.getRefreshToken()
                ?: throw Exception("No refresh token available")

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            val refreshResponse = response.getBodyOrThrow()

            tokenManager.saveTokens(refreshResponse.accessToken, refreshToken)
        }
    }

    suspend fun getUserEmail(): String? {
        return tokenManager.getUserEmail()
    }

    suspend fun getUserName(): String? {
        return tokenManager.getUserName()
    }
}
