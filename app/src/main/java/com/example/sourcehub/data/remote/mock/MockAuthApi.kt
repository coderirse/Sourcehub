package com.example.sourcehub.data.remote.mock

import com.example.sourcehub.data.local.mock.MockDataProvider
import com.example.sourcehub.data.remote.api.AuthApi
import com.example.sourcehub.data.remote.dto.*
import com.example.sourcehub.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [AuthApi] 的内存模拟实现，用于开发和测试。
 *
 * ## 模拟行为
 *
 * **登录** ([login]):
 * - 在 [MockDataProvider.mockUsers] 中查找邮箱。如果找到且密码为 `"password123"`，
 *   则返回包含生成的模拟令牌的成功登录响应。
 * - 同样接受硬编码测试账号 `test@sourcehub.com / password123`。
 * - 否则返回 401 错误。
 * - **错误模拟**: 10% 的失败登录会抛出网络异常，
 *   以测试 [AuthRepositoryImpl] 中的错误处理路径。
 * - **延迟**: 300-800 毫秒随机延迟以模拟网络延迟。
 *
 * **注册** ([register]):
 * - 验证邮箱包含 `@` 且密码至少 6 个字符。否则返回 400。
 * - 成功时返回生成的用户 ID 和令牌（自动登录）。
 * - **延迟**: 400-900 毫秒。
 *
 * **令牌刷新** ([refreshToken]):
 * - 始终成功，返回新的令牌对。模拟中不模拟过期。
 * - **延迟**: 200-500 毫秒。
 *
 * **个人资料** ([getProfile], [updateProfile]):
 * - 返回硬编码的测试用户资料。[updateProfile] 将请求字段与默认值合并。
 * - **延迟**: 200-700 毫秒。
 *
 * **忘记密码** ([forgotPassword]):
 * - 验证邮箱格式。如果包含 `@` 则返回成功。
 * - **延迟**: 500-1000 毫秒。
 */
class MockAuthApi(private val mockData: MockDataProvider) : AuthApi {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> {
        // 随机延迟以模拟网络延迟 (300-800 毫秒)。
        delay(Random.nextLong(300, 800))
        val user = mockData.mockUsers[request.email]
        return if (user != null && request.password == "password123") {
            // 已知的模拟用户 — 登录成功。
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
            // 硬编码的备用测试账号。
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
            // 模拟偶发网络错误（10% 概率）以测试
            // AuthRepositoryImpl 中的异常处理分支。
            if (Random.nextInt(10) == 0) {
                throw Exception("Network error: Connection timeout")
            }
            ApiResponse(code = 401, message = "邮箱或密码错误")
        }
    }

    override suspend fun register(request: RegisterRequest): ApiResponse<RegisterResponse> {
        delay(Random.nextLong(400, 900))
        return if (request.email.contains("@") && request.password.length >= 6) {
            // 基本验证通过 — 使用生成的令牌创建新用户。
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
        // 始终成功 — 生成新的令牌对。
        return ApiResponse(
            data = TokenResponse(
                accessToken = "mock_access_token_${SecurityUtils.generateUuid().take(8)}",
                refreshToken = "mock_refresh_token_${SecurityUtils.generateUuid().take(8)}"
            )
        )
    }

    override suspend fun getProfile(): ApiResponse<UserProfileResponse> {
        delay(Random.nextLong(200, 600))
        // 返回硬编码的测试用户。createdAt 约 30 天前。
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
        // 将请求字段与默认值合并，缺失字段保持其之前的值。
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
