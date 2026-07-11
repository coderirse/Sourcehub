package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.User
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(name: String, email: String, password: String): Resource<User>
    suspend fun refreshToken(): Resource<String>
    suspend fun getCurrentUser(): Flow<User?>
    suspend fun updateProfile(user: User): Resource<User>
    suspend fun forgotPassword(email: String): Resource<Unit>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getUserId(): String
}
