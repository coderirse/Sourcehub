package com.example.sourcehub.domain.repository

import com.example.sourcehub.domain.model.User
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 认证/授权操作的契约接口。
 *
 * 实现类负责处理登录、注册、令牌管理、个人资料
 * 获取和注销。该仓库抽象了后端是真实 Ktor 服务器
 * （通过网络层）还是本地模拟实现。
 */
interface AuthRepository {

    /**
     * 使用邮箱和密码进行认证/授权。
     * 成功后将 JWT 令牌对存储起来，并将用户缓存到 SQLite数据库。
     * @return 包含已登录[User]的 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun login(email: String, password: String): Resource<User>

    /**
     * 创建新账户。
     * 成功后自动登录（令牌已存储，用户已缓存）。
     * @return 包含新创建的[User]的 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun register(name: String, email: String, password: String): Resource<User>

    /**
     * 用已存储的刷新令牌换取新的访问令牌。
     * 如果刷新令牌缺失或已过期，则登出用户。
     * @return 包含新访问令牌的 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun refreshToken(): Resource<String>

    /**
     * 以响应式方式观察当前已登录的用户。
     * 当没有用户通过认证/授权时，发出 null。
     */
    suspend fun getCurrentUser(): Flow<User?>

    /**
     * 更新当前用户的个人资料（姓名、头像、电话）。
     * 更新后的个人资料会同时持久化到后端和本地 SQLite数据库 缓存。
     * @return 包含更新后[User]的 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun updateProfile(user: User): Resource<User>

    /**
     * 为指定邮箱地址请求密码重置邮件。
     * @return 发送成功时返回 [Resource.Success]，或 [Resource.Error]。
     */
    suspend fun forgotPassword(email: String): Resource<Unit>

    /** 清除所有本地认证/授权状态：令牌、缓存的用户以及内存中的状态。 */
    suspend fun logout()

    /** 快速检查：是否存在有效（未过期）的访问令牌？ */
    fun isLoggedIn(): Boolean

    /** 从令牌管理器中返回缓存的用户 ID。 */
    fun getUserId(): String
}
