package com.sourcehub.server.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.sourcehub.server.models.Users
import com.sourcehub.server.security.JwtManager
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import java.util.*

fun Routing.authRoutes(jwtManager: JwtManager, db: Database) {
    route("/api/auth") {

        post("/register") {
            val req = call.receive<RegisterRequest>()
            // Validate
            if (req.email.isBlank() || req.password.length < 6 || req.name.isBlank()) {
                call.respond(mapOf("code" to 400, "message" to "Invalid input"))
                return@post
            }
            // Check duplicate
            val existing = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.select { Users.email eq req.email }.singleOrNull()
            }
            if (existing != null) {
                call.respond(mapOf("code" to 409, "message" to "Email already registered"))
                return@post
            }
            val userId = "u_${UUID.randomUUID().toString().take(8)}"
            val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
            org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.insert {
                    it[id] = userId
                    it[name] = req.name
                    it[email] = req.email
                    it[passwordHash] = hash
                    it[createdAt] = System.currentTimeMillis()
                }
            }
            val accessToken = jwtManager.generateAccessToken(userId, req.email)
            val refreshToken = jwtManager.generateRefreshToken(userId)
            call.respond(mapOf(
                "code" to 200, "message" to "success",
                "data" to mapOf(
                    "userId" to userId, "userName" to req.name, "userEmail" to req.email,
                    "accessToken" to accessToken, "refreshToken" to refreshToken,
                    "avatarUrl" to ""
                )
            ))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.select { Users.email eq req.email }.singleOrNull()
            }
            if (row == null) {
                call.respond(mapOf("code" to 401, "message" to "Invalid email or password"))
                return@post
            }
            val hash = row[Users.passwordHash]
            val valid = BCrypt.verifyer().verify(req.password.toCharArray(), hash)
            if (!valid.verified) {
                call.respond(mapOf("code" to 401, "message" to "Invalid email or password"))
                return@post
            }
            val accessToken = jwtManager.generateAccessToken(row[Users.id], row[Users.email])
            val refreshToken = jwtManager.generateRefreshToken(row[Users.id])
            call.respond(mapOf(
                "code" to 200, "message" to "success",
                "data" to mapOf(
                    "userId" to row[Users.id], "userName" to row[Users.name],
                    "userEmail" to row[Users.email], "avatarUrl" to (row[Users.avatarUrl] ?: ""),
                    "accessToken" to accessToken, "refreshToken" to refreshToken
                )
            ))
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            try {
                val payload = JWT.decode(req.refreshToken)
                if (payload.getClaim("type").asString() != "refresh") {
                    call.respond(mapOf("code" to 401, "message" to "Invalid refresh token"))
                    return@post
                }
                val userId = payload.getClaim("userId").asString() ?: ""
                val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.select { Users.id eq userId }.singleOrNull()
                }
                if (row == null) {
                    call.respond(mapOf("code" to 401, "message" to "User not found"))
                    return@post
                }
                val accessToken = jwtManager.generateAccessToken(userId, row[Users.email])
                val refreshToken = jwtManager.generateRefreshToken(userId)
                call.respond(mapOf("code" to 200, "data" to mapOf("accessToken" to accessToken, "refreshToken" to refreshToken)))
            } catch (e: Exception) {
                call.respond(mapOf("code" to 401, "message" to "Invalid token"))
            }
        }

        authenticate("auth-jwt") {
            get("/profile") {
                val userId = jwtManager.getUserId(call) ?: return@get call.respond(mapOf("code" to 401))
                val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.select { Users.id eq userId }.singleOrNull()
                } ?: return@get call.respond(mapOf("code" to 404))
                call.respond(mapOf("code" to 200, "data" to mapOf(
                    "userId" to row[Users.id], "name" to row[Users.name], "email" to row[Users.email],
                    "avatarUrl" to (row[Users.avatarUrl] ?: ""), "phone" to (row[Users.phone] ?: ""),
                    "createdAt" to row[Users.createdAt]
                )))
            }

            put("/profile") {
                val userId = jwtManager.getUserId(call) ?: return@put call.respond(mapOf("code" to 401))
                val req = call.receive<UpdateProfileRequest>()
                org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.update({ Users.id eq userId }) {
                        req.name?.let { n -> it[name] = n }
                        req.avatarUrl?.let { a -> it[avatarUrl] = a }
                        req.phone?.let { p -> it[phone] = p }
                    }
                }
                call.respond(mapOf("code" to 200, "message" to "updated"))
            }
        }
    }
}

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val name: String, val email: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class UpdateProfileRequest(val name: String? = null, val avatarUrl: String? = null, val phone: String? = null)
