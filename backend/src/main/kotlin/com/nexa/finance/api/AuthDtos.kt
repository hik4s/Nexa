package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String,
    val timeZone: String = "UTC",
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RecoveryRequestDto(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
)

@Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    val displayName: String? = null,
    val timeZone: String? = null,
)

@Serializable
data class UserResponse(
    val userId: String,
    val email: String,
    val displayName: String,
    val timeZone: String,
    val status: String,
)

@Serializable
data class SessionResponse(
    val userId: String,
    val token: String,
    val expiresAt: String,
)

@Serializable
data class RecoveryResponse(
    val token: String,
    val expiresAt: String,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)
