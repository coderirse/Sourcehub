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
 * Retrofit client builder. Creates authenticated OkHttp clients that
 * attach JWT access tokens and auto-refresh on 401 responses.
 */
object ApiClient {

    // Default to localhost for dev; override in production
    var baseUrl: String = "http://10.0.2.2:8080/" // Android emulator → host

    private val tokenManager: TokenManager
        get() = SourcehubApplication.instance.appContainer.tokenManager

    fun build(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getAccessToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)

            // Auto-refresh on 401
            if (response.code == 401) {
                response.close()
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    // Synchronous refresh (blocks this request)
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
                            tokenManager.saveTokens(newAccessToken, refreshToken)
                            // Retry original request with new token
                            val retryRequest = chain.request().newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            return@Interceptor chain.proceed(retryRequest)
                        }
                    }
                }
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
