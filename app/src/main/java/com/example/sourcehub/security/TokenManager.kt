/**
 * 基于 Android EncryptedSharedPreferences 的安全 JWT令牌 存储。
 *
 * 此类管理 SourceHub 应用的 OAuth2/JWT 令牌生命周期：
 * - 持久化：访问令牌、刷新令牌和令牌过期时间存储在加密的
 *   SharedPreferences 文件中，其主密钥由 Android 密钥库持有。
 * - 响应式：[isLoggedIn] [StateFlow] 允许 Compose 屏幕和 ViewModel
 *   在不轮询的情况下观察认证状态变化。
 * - 过期：访问令牌在 [TOKEN_LIFETIME_MS]（MVP Mock 后端为 15 分钟）
 *   后被视为无效。调用者应使用 [getAccessToken]，当令牌过期时返回
 *   `null`，在仓库层触发刷新流程。
 *
 * 加密方案使用 AES-256-GCM 作为主密钥，AES-256-SIV 用于首选项键 /
 * AES-256-GCM 用于首选项值，符合 AndroidX Security Crypto 库的推荐。
 */
package com.example.sourcehub.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 在加密的设备存储中管理认证令牌。
 *
 * @param context 任意 Android [Context]；[EncryptedSharedPreferences] 内部
 *                使用应用级存储，因此 Activity 引用是安全的。
 */
class TokenManager(context: Context) {

    /**
     * 在 Android 密钥库内生成的 AES-256-GCM 主密钥（在可用时由硬件支持）。
     * 此密钥用于加密 SharedPreferences 文件本身。
     */
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * 加密 SharedPreferences 实例。文件内容使用 [masterKey] 进行静态加密；
     * 各个键和值进一步使用各自的 AES-256 方案加密，提供纵深防御。
     */
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 可观察的登录状态。通过调用 [hasValidToken] 初始化，以确保状态
     * 在进程重启后保持正确。UI 元素收集此 Flow 以响应式地显示/隐藏
     * 需要认证的内容。
     */
    private val _isLoggedIn = MutableStateFlow(hasValidToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /**
     * 持久化 OAuth2 令牌对，并将过期时间戳设置为
     * `now + [TOKEN_LIFETIME_MS]`。将 [isLoggedIn] 更新为 `true`。
     *
     * @param accessToken  短生命周期的 JWT 访问令牌。
     * @param refreshToken 用于获取新访问令牌的长生命周期刷新令牌。
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + TOKEN_LIFETIME_MS)
            .apply()
        _isLoggedIn.value = true
    }

    /**
     * 返回当前的访问令牌，如果已过期或不存在则返回 `null`。
     *
     * 调用者应将 `null` 返回值视为在重定向到登录屏幕之前尝试令牌刷新
     * （通过 [getRefreshToken]）的信号。
     */
    fun getAccessToken(): String? {
        if (isTokenExpired()) return null
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * 返回存储的刷新令牌，它本身可能在服务端已过期。
     * 仓库层负责处理来自刷新端点的 401 响应。
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 检查当前是否存储了未过期的访问令牌。
     *
     * 这是一个便捷方法，调用 [getAccessToken] 并用于初始化
     * [isLoggedIn] Flow——非空的访问令牌意味着用户已登录。
     */
    fun hasValidToken(): Boolean {
        return getAccessToken() != null
    }

    /**
     * 当存储的过期时间戳已过去（或不存在）时返回 `true`。
     *
     * 不存在的时间戳 (0L) 将被视为已过期，以便在新安装后的
     * 首次使用时刷新令牌。
     */
    fun isTokenExpired(): Boolean {
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0L)
        return System.currentTimeMillis() > expiry
    }

    /**
     * 清除所有存储的令牌，并将 [isLoggedIn] 设置为 `false`。
     *
     * 在显式退出登录时调用，也在服务器返回无法通过令牌刷新
     * 解决的 401 响应时调用。
     */
    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
        _isLoggedIn.value = false
    }

    /**
     * 返回存储的用户 ID，如果没有任何保存则返回空字符串。
     * 用户 ID 与令牌对分开存储，以便个人资料屏幕即使在访问令牌
     * 刷新期间也能显示它。
     */
    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }

    /**
     * 持久化服务器分配的用户 ID。在成功登录或注册后调用。
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    companion object {
        /** EncryptedSharedPreferences 文件名（存储在应用内部存储中）。 */
        private const val PREFS_FILE_NAME = "sourcehub_secure_prefs"

        // SharedPreferences 键——用户不可见，但为纵深防御仍然加密。
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"

        /**
         * Mock 访问令牌生命周期：15 分钟。
         * 在生产环境中，这将由 JWT 本身的 `exp` 声明或
         * OAuth2 令牌端点返回的 `expires_in` 字段驱动。
         */
        private const val TOKEN_LIFETIME_MS = 15 * 60 * 1000L // Mock 15 分钟
    }
}
