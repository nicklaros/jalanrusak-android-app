package com.jalanrusak.domain.usecase

import com.jalanrusak.data.repository.AuthRepository
import com.jalanrusak.util.Result

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Basic validation
        if (email.isBlank() || password.isBlank()) {
            return Result.Error("Email dan password harus diisi")
        }

        if (!isValidEmail(email)) {
            return Result.Error("Format email tidak valid")
        }

        return authRepository.login(email, password)
    }

    suspend fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    suspend fun logout(): Result<Unit> {
        return authRepository.logout()
    }

    suspend fun getUserEmail(): String? {
        return authRepository.getUserEmail()
    }

    suspend fun getUserName(): String? {
        return authRepository.getUserName()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")
        return emailRegex.matches(email)
    }
}
