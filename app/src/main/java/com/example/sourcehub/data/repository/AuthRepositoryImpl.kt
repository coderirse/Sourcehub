package com.example.sourcehub.data.repository

import com.example.sourcehub.data.local.db.SourcehubDbHelper
import com.example.sourcehub.data.local.prefs.PreferencesManager
import com.example.sourcehub.data.remote.api.AuthApi
import com.example.sourcehub.data.remote.dto.LoginRequest
import com.example.sourcehub.data.remote.dto.RegisterRequest
import com.example.sourcehub.data.remote.dto.UpdateProfileRequest
import com.example.sourcehub.domain.model.User
import com.example.sourcehub.domain.repository.AuthRepository
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.security.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AuthRepository] 的实现，委托给 [AuthApi] 后端。
 *
 * ## 职责
 * - 登录/注册：调用 API，通过 [TokenManager] 持久化 JWT 令牌，
 *   将 [User] 记录缓存在 SQLite数据库 中，并通过 [currentUser] 发布。
 * - 令牌刷新：使用存储的刷新令牌换取新的访问令牌。
 *   如果刷新失败，清除所有本地认证状态（强制重新登录）。
 * - 个人资料：获取/更新用户资料并同步到本地缓存。
 * - 登出：清除令牌、缓存的用户数据和内存状态。
 *
 * ## API 切换
 * [swapApi] 允许在运行时替换底层的 [AuthApi]（例如当用户在远程模式切换时，
 * 在 [MockAuthApi] 和 [RetrofitAuthApi] 之间切换）。
 *
 * ## 用户缓存
 * 构造时，init 代码块从 SQLite数据库 加载用户记录（以 [TokenManager] 中
 * 缓存的用户 ID 为键），因此 [getCurrentUser] 无需网络调用即可立即发送数据。
 */
class AuthRepositoryImpl(
    private var authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager,
    private val db: SourcehubDbHelper
) : AuthRepository {

    /** 在运行时替换底层 API（模拟 <-> 远程）。 */
    fun swapApi(api: AuthApi) { authApi = api }

    /** 以响应式方式发布当前用户的内存持有者。 */
    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        // 构造时从 SQLite数据库 恢复缓存的用户数据，
        // 以便界面无需等待网络往返即可立即获得值。
        kotlinx.coroutines.runBlocking {
            val uid = tokenManager.getUserId()
            if (uid.isNotEmpty()) _currentUser.value = db.getUser(uid)
        }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val resp = authApi.login(LoginRequest(email, password))
            if (resp.code == 200 && resp.data != null) {
                // 持久化令牌和用户 ID，供后续认证请求使用。
                tokenManager.saveTokens(resp.data.accessToken, resp.data.refreshToken)
                tokenManager.saveUserId(resp.data.userId)
                // 构建领域模型并缓存在 SQLite数据库 中以供离线使用。
                val user = User(resp.data.userId, resp.data.userName, resp.data.userEmail, resp.data.avatarUrl)
                db.insertUser(user)
                _currentUser.value = user
                Resource.Success(user)
            } else Resource.Error(resp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "登录失败") }
    }

    override suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val resp = authApi.register(RegisterRequest(name, email, password))
            if (resp.code == 200 && resp.data != null) {
                // 自动登录：立即保存令牌并缓存新用户。
                tokenManager.saveTokens(resp.data.accessToken, resp.data.refreshToken)
                tokenManager.saveUserId(resp.data.userId)
                val user = User(resp.data.userId, resp.data.userName, resp.data.userEmail)
                db.insertUser(user)
                _currentUser.value = user
                Resource.Success(user)
            } else Resource.Error(resp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "注册失败") }
    }

    override suspend fun refreshToken(): Resource<String> {
        return try {
            val rt = tokenManager.getRefreshToken() ?: return Resource.Error("未登录")
            val resp = authApi.refreshToken(rt)
            if (resp.code == 200 && resp.data != null) {
                // 轮换两个令牌 — 旧的刷新令牌被替换。
                tokenManager.saveTokens(resp.data.accessToken, resp.data.refreshToken)
                Resource.Success(resp.data.accessToken)
            } else {
                // 刷新失败 — 清除所有数据并强制重新登录。
                tokenManager.clearTokens(); _currentUser.value = null; Resource.Error(resp.message)
            }
        } catch (e: Exception) { Resource.Error(e.message ?: "刷新失败") }
    }

    override suspend fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun updateProfile(user: User): Resource<User> {
        return try {
            val resp = authApi.updateProfile(UpdateProfileRequest(user.name, user.avatarUrl, user.phone))
            if (resp.code == 200 && resp.data != null) {
                // 从服务器响应重建，以确保服务器端默认值被遵守。
                val u = User(resp.data.userId, resp.data.name, resp.data.email, resp.data.avatarUrl, resp.data.phone, resp.data.createdAt)
                db.insertUser(u)
                _currentUser.value = u
                Resource.Success(u)
            } else Resource.Error(resp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "更新失败") }
    }

    override suspend fun forgotPassword(email: String): Resource<Unit> {
        return try {
            val resp = authApi.forgotPassword(email)
            if (resp.code == 200) Resource.Success(Unit) else Resource.Error(resp.message)
        } catch (e: Exception) { Resource.Error(e.message ?: "请求失败") }
    }

    override suspend fun logout() {
        // 清除所有本地认证状态：令牌、缓存的用户数据和内存流。
        tokenManager.clearTokens()
        db.deleteAllUsers()
        _currentUser.value = null
    }

    override fun isLoggedIn(): Boolean = tokenManager.hasValidToken()
    override fun getUserId(): String = tokenManager.getUserId()
}
