package com.sourcehub.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sourcehub.server.config.AppConfig
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JwtManager(config: AppConfig) {
    private val secret = config.jwtSecret
    private val issuer = config.jwtIssuer
    private val audience = config.jwtAudience
    private val accessLifetime = config.accessTokenLifetimeMs
    private val refreshLifetime = config.refreshTokenLifetimeMs

    val verifier = com.auth0.jwt.JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateAccessToken(userId: String, email: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + accessLifetime))
            .sign(Algorithm.HMAC256(secret))
    }

    fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + refreshLifetime))
            .sign(Algorithm.HMAC256(secret))
    }

    fun getUserId(call: io.ktor.server.application.ApplicationCall): String? {
        val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
        return principal?.payload?.getClaim("userId")?.asString()
    }
}
