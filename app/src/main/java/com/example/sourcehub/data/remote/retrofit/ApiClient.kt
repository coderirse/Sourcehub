package com.example.sourcehub.data.remote.retrofit

import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.security.TokenManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 单例 Retrofit 客户端构建器。
 *
 * 创建一个 [OkHttpClient]，实现：
 * 1. **通过认证拦截器将 JWT 访问令牌附加到每个请求**。
 * 2. **401 自动刷新**：同步调用刷新令牌端点，
 *    并使用新令牌重试原始请求。
 * 3. **通过 [HttpLoggingInterceptor] 记录请求/响应体**以便调试。
 *
 * ## 基础 URL
 * 默认为 `http://10.0.2.2:8080/`（Android 模拟器访问宿主机 localhost 的别名）。
 * 生产构建时可覆盖 [baseUrl]。
 *
 * ## 令牌管理
 * 访问令牌和刷新令牌从 [TokenManager] 读取，
 * [TokenManager] 从应用级 [AppContainer] 获取。在收到 401 响应时，
 * 拦截器会先尝试同步刷新令牌，然后再重试。
 * 如果刷新也失败了，会清除令牌并将原始 401 传播出去，
 * 以便界面层重定向到登录页面。
 *
 * ## 超时
 * - 连接: 15 秒
 * - 读取/写入: 各 30 秒
 */
object ApiClient {

    /** 所有 Retrofit 服务的基础 URL。默认: 模拟器 -> 宿主机 localhost。 */
    var baseUrl: String = "http://10.0.2.2:8080/" // Android 模拟器 → 宿主机

    /** 从全局应用容器延迟获取 [TokenManager]。 */
    private val tokenManager: TokenManager
        get() = SourcehubApplication.instance.appContainer.tokenManager

    /**
     * 构建一个完整配置的 [Retrofit] 实例，包含认证、日志记录
     * 和 Gson 转换器。
     */
    fun build(): Retrofit {
        // 记录完整的请求/响应体用于开发调试。
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 附加 Bearer 令牌并处理 401 刷新的拦截器。
        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getAccessToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request() // 无令牌 — 以未认证方式继续。
            }
            val response = chain.proceed(request)

            // 401 自动刷新: 访问令牌可能已过期。
            if (response.code == 401) {
                response.close()
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    // 执行同步刷新调用。这会阻塞当前
                    // 请求线程，但比排队重试更简单。
                    val refreshClient = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .build()
                    val refreshRequest = okhttp3.Request.Builder()
                        .url("$baseUrl/api/auth/refresh")
                        .post(okhttp3.RequestBody.create(
                            "application/json".toMediaType(),
                            """{"refreshToken":"$refreshToken"}"""
                        ))
                        .build()
                    val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body?.string()
                        val newAccessToken = body?.let { extractToken(it) }
                        if (newAccessToken != null) {
                            // 持久化新的令牌对并重试原始请求。
                            tokenManager.saveTokens(newAccessToken, refreshToken)
                            val retryRequest = chain.request().newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            return@Interceptor chain.proceed(retryRequest)
                        }
                    }
                }
                // 刷新失败 — 清除令牌，以便界面重定向到登录页面。
                tokenManager.clearTokens()
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 从 JSON 响应体中提取 `accessToken` 字段。
     *
     * 预期响应格式：
     * `{"code":200,"data":{"accessToken":"...","refreshToken":"..."}}`
     *
     * 使用 Gson 的 [JsonParser] 进行轻量解析，无需完整的
     * 反序列化模型。
     */
    private fun extractToken(json: String): String? {
        return try {
            com.google.gson.JsonParser.parseString(json)
                .asJsonObject
                .getAsJsonObject("data")
                ?.get("accessToken")
                ?.asString
        } catch (e: Exception) { null }
    }
}
