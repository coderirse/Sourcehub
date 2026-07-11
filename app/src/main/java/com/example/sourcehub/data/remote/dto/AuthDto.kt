package com.example.sourcehub.data.remote.dto

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)

data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val userName: String,
    val userEmail: String,
    val avatarUrl: String
)

data class RegisterResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val userName: String,
    val userEmail: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserProfileResponse(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String,
    val phone: String,
    val createdAt: Long
)

data class UpdateProfileRequest(
    val name: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null
)
