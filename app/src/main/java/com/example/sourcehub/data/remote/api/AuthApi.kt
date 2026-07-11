package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

interface AuthApi {
    suspend fun login(request: LoginRequest): ApiResponse<LoginResponse>
    suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse>
    suspend fun refreshToken(refreshToken: String): ApiResponse<TokenResponse>
    suspend fun getProfile(): ApiResponse<UserProfileResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<UserProfileResponse>
    suspend fun forgotPassword(email: String): ApiResponse<Unit>
}
