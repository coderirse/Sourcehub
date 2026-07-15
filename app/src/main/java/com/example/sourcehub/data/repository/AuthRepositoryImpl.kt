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

class AuthRepositoryImpl(
    private var authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager,
    private val db: SourcehubDbHelper
) : AuthRepository {

    fun swapApi(api: AuthApi) { authApi = api }

    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        kotlinx.coroutines.runBlocking {
            val uid = tokenManager.getUserId()
            if (uid.isNotEmpty()) _currentUser.value = db.getUser(uid)
        }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val resp = authApi.login(LoginRequest(email, password))
            if (resp.code == 200 && resp.data != null) {
                tokenManager.saveTokens(resp.data.accessToken, resp.data.refreshToken)
                tokenManager.saveUserId(resp.data.userId)
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
                tokenManager.saveTokens(resp.data.accessToken, resp.data.refreshToken)
                Resource.Success(resp.data.accessToken)
            } else { tokenManager.clearTokens(); _currentUser.value = null; Resource.Error(resp.message) }
        } catch (e: Exception) { Resource.Error(e.message ?: "刷新失败") }
    }

    override suspend fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun updateProfile(user: User): Resource<User> {
        return try {
            val resp = authApi.updateProfile(UpdateProfileRequest(user.name, user.avatarUrl, user.phone))
            if (resp.code == 200 && resp.data != null) {
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
        tokenManager.clearTokens()
        db.deleteAllUsers()
        _currentUser.value = null
    }

    override fun isLoggedIn(): Boolean = tokenManager.hasValidToken()
    override fun getUserId(): String = tokenManager.getUserId()
}
