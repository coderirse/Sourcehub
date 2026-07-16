package com.example.sourcehub.data.remote.dto

/**
 * 认证领域的传输对象。
 *
 * 这些类型表示与后端认证端点交换的 JSON 传输格式。
 * 它们与领域模型保持分离，使网络层
 * 可以独立于界面层演进。
 */

/** POST /api/auth/login 的请求体。 */
data class LoginRequest(val email: String, val password: String)

/** POST /api/auth/register 的请求体。 */
data class RegisterRequest(val name: String, val email: String, val password: String)

/**
 * 成功登录的响应。
 *
 * @property userId 已认证用户的唯一 ID。
 * @property accessToken 用于 API 授权的短期 JWT。
 * @property refreshToken 用于获取新访问令牌的长期令牌。
 * @property userName 已认证用户的显示名称。
 * @property userEmail 已认证用户的邮箱地址。
 * @property avatarUrl 用户头像的 URL。
 */
data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val userName: String,
    val userEmail: String,
    val avatarUrl: String
)

/**
 * 成功注册的响应（自动登录）。
 *
 * 字段与 [LoginResponse] 一致，但省略 [avatarUrl]，
 * 因为新用户使用默认头像。
 */
data class RegisterResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val userName: String,
    val userEmail: String
)

/**
 * 令牌刷新端点的响应。
 *
 * @property accessToken 新的短期 JWT。
 * @property refreshToken 新的长期令牌（轮换）。
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

/**
 * GET /api/auth/profile 和 PUT /api/auth/profile 的响应。
 *
 * @property userId 用户的唯一 ID。
 * @property name 显示名称。
 * @property email 邮箱地址。
 * @property avatarUrl 头像 URL。
 * @property phone 脱敏电话号码。
 * @property createdAt 账号创建时间戳（毫秒）。
 */
data class UserProfileResponse(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String,
    val phone: String,
    val createdAt: Long
)

/**
 * PUT /api/auth/profile 的请求体。
 *
 * 所有字段可为空 — 仅更新提供的字段。
 */
data class UpdateProfileRequest(
    val name: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null
)
