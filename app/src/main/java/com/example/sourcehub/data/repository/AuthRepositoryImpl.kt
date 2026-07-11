package com.example.sourcehub.data.repository

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
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    private var cachedUser: User? = null

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.code == 200 && response.data != null) {
                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                tokenManager.saveUserId(response.data.userId)
                val user = User(
                    id = response.data.userId,
                    name = response.data.userName,
                    email = response.data.userEmail,
                    avatarUrl = response.data.avatarUrl
                )
                cachedUser = user
                _currentUser.value = user
                Resource.Success(user)
            } else {
                Resource.Error(response.message)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "登录失败")
        }
    }

    override suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val response = authApi.register(RegisterRequest(name, email, password))
            if (response.code == 200 && response.data != null) {
                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                tokenManager.saveUserId(response.data.userId)
                val user = User(id = response.data.userId, name = response.data.userName, email = response.data.userEmail)
                cachedUser = user
                _currentUser.value = user
                Resource.Success(user)
            } else Resource.Error(response.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "注册失败")
        }
    }

    override suspend fun refreshToken(): Resource<String> {
        return try {
            val rt = tokenManager.getRefreshToken() ?: return Resource.Error("未登录")
            val response = authApi.refreshToken(rt)
            if (response.code == 200 && response.data != null) {
                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                Resource.Success(response.data.accessToken)
            } else {
                tokenManager.clearTokens()
                _currentUser.value = null
                Resource.Error(response.message)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "刷新令牌失败")
        }
    }

    override suspend fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun updateProfile(user: User): Resource<User> {
        return try {
            val resp = authApi.updateProfile(UpdateProfileRequest(name = user.name, avatarUrl = user.avatarUrl, phone = user.phone))
            if (resp.code == 200 && resp.data != null) {
                val u = User(resp.data.userId, resp.data.name, resp.data.email, resp.data.avatarUrl, resp.data.phone, resp.data.createdAt)
                cachedUser = u
                _currentUser.value = u
                Resource.Success(u)
            } else Resource.Error(resp.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "更新失败")
        }
    }

    override suspend fun forgotPassword(email: String): Resource<Unit> {
        return try {
            val resp = authApi.forgotPassword(email)
            if (resp.code == 200) Resource.Success(Unit) else Resource.Error(resp.message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "请求失败")
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
        cachedUser = null
        _currentUser.value = null
    }

    override fun isLoggedIn(): Boolean = tokenManager.hasValidToken()
    override fun getUserId(): String = tokenManager.getUserId()
}
