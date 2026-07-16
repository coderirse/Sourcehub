package com.example.sourcehub.data.remote.api

import com.example.sourcehub.data.remote.dto.*

/**
 * 认证操作的 API 契约。
 *
 * 实现包括 [MockAuthApi]（内存实现，用于开发）和
 * [RetrofitAuthApi]（通过 HTTP 调用 Ktor 后端）。
 * 两者都遵循此接口，使仓库层可以在运行时切换它们。
 */
interface AuthApi {
    /** 使用邮箱和密码进行认证。返回 JWT 令牌和用户信息。 */
    suspend fun login(request: LoginRequest): ApiResponse<LoginResponse>

    /** 创建新账号。成功时返回令牌（自动登录）。 */
    suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse>

    /** 用刷新令牌换取新的访问/刷新令牌对。 */
    suspend fun refreshToken(refreshToken: String): ApiResponse<TokenResponse>

    /** 获取当前已认证用户的完整个人资料。 */
    suspend fun getProfile(): ApiResponse<UserProfileResponse>

    /** 更新当前用户的姓名、头像和/或电话。 */
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<UserProfileResponse>

    /** 向指定邮箱发送密码重置邮件。 */
    suspend fun forgotPassword(email: String): ApiResponse<Unit>
}
