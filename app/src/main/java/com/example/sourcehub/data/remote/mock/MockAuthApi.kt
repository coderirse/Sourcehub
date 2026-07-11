package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.AuthApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockAuthApi(private val mockData: MockDataProvider) : AuthApi {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> {
        delay(Random.nextLong(300, 800))
        val user = mockData.mockUsers[request.email]
        return if (user != null && request.password == "password123") {
            ApiResponse(
                data = LoginResponse(
                    userId = user.id,
                    accessToken = "mock_access_token_${SecurityUtils.generateUuid().take(8)}",
                    refreshToken = "mock_refresh_token_${SecurityUtils.generateUuid().take(8)}",
                    userName = user.name,
                    userEmail = user.email,
                    avatarUrl = user.avatarUrl
                )
            )
        } else if (request.email == "test@sourcehub.com" && request.password == "password123") {
            ApiResponse(
                data = LoginResponse(
                    userId = "user_001",
                    accessToken = "mock_access_token_${SecurityUtils.generateUuid().take(8)}",
                    refreshToken = "mock_refresh_token_${SecurityUtils.generateUuid().take(8)}",
                    userName = "测试用户",
                    userEmail = request.email,
                    avatarUrl = "https://picsum.photos/200/200?random=99"
                )
            )
        } else {
            // Simulate occasional network error (10%)
            if (Random.nextInt(10) == 0) {
                throw Exception("Network error: Connection timeout")
            }
            ApiResponse(code = 401, message = "邮箱或密码错误")
        }
    }

    override suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse> {
        delay(Random.nextLong(400, 900))
        return if (request.email.contains("@") && request.password.length >= 6) {
            ApiResponse(
                data = RegisterResponse(
                    userId = "user_${SecurityUtils.generateUuid().take(8)}",
                    accessToken = "mock_access_token_${SecurityUtils.generateUuid().take(8)}",
                    refreshToken = "mock_refresh_token_${SecurityUtils.generateUuid().take(8)}",
                    userName = request.name,
                    userEmail = request.email
                )
            )
        } else {
            ApiResponse(code = 400, message = "邮箱格式不正确或密码少于6位")
        }
    }

    override suspend fun refreshToken(refreshToken: String): ApiResponse<TokenResponse> {
        delay(Random.nextLong(200, 500))
        return ApiResponse(
            data = TokenResponse(
                accessToken = "mock_access_token_${SecurityUtils.generateUuid().take(8)}",
                refreshToken = "mock_refresh_token_${SecurityUtils.generateUuid().take(8)}"
            )
        )
    }

    override suspend fun getProfile(): ApiResponse<UserProfileResponse> {
        delay(Random.nextLong(200, 600))
        return ApiResponse(
            data = UserProfileResponse(
                userId = "user_001",
                name = "测试用户",
                email = "test@sourcehub.com",
                avatarUrl = "https://picsum.photos/200/200?random=99",
                phone = "138****8888",
                createdAt = System.currentTimeMillis() - 86400000 * 30
            )
        )
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<UserProfileResponse> {
        delay(Random.nextLong(300, 700))
        return ApiResponse(
            data = UserProfileResponse(
                userId = "user_001",
                name = request.name ?: "测试用户",
                email = "test@sourcehub.com",
                avatarUrl = request.avatarUrl ?: "https://picsum.photos/200/200?random=99",
                phone = request.phone ?: "138****8888",
                createdAt = System.currentTimeMillis() - 86400000 * 30
            )
        )
    }

    override suspend fun forgotPassword(email: String): ApiResponse<Unit> {
        delay(Random.nextLong(500, 1000))
        return if (email.contains("@")) {
            ApiResponse(message = "密码重置邮件已发送")
        } else {
            ApiResponse(code = 400, message = "邮箱格式不正确")
        }
    }
}
