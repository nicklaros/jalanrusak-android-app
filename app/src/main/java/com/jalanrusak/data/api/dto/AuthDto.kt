package com.jalanrusak.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Login request DTO
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

/**
 * Login response DTO
 */
data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("user")
    val user: UserDto
)

/**
 * User DTO
 */
data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String?
)

/**
 * Refresh token request
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Refresh token response
 */
data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String
)
