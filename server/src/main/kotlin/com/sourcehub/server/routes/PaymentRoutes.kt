package com.sourcehub.server.routes

import com.sourcehub.server.models.Orders
import com.sourcehub.server.security.JwtManager
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

fun Routing.paymentRoutes(jwtManager: JwtManager, db: Database) {
    authenticate("auth-jwt") {
        route("/api/payment") {

            post("/pay") {
                val req = call.receive<PayRequest>()
                // Simulate payment (90% success)
                val success = Math.random() > 0.1
                if (success) {
                    val txnId = "txn_${UUID.randomUUID().toString().take(12)}"
                    val now = System.currentTimeMillis()
                    newSuspendedTransaction(db = db) {
                        Orders.update({ Orders.id eq req.orderId }) {
                            it[status] = "PAID"
                            it[transactionId] = txnId
                            it[paidAt] = now
                        }
                    }
                    call.respond(mapOf("code" to 200, "data" to mapOf(
                        "transactionId" to txnId, "orderId" to req.orderId,
                        "status" to "SUCCESS", "amount" to req.amount, "timestamp" to now
                    )))
                } else {
                    call.respond(mapOf("code" to 500, "message" to "Payment failed", "data" to mapOf(
                        "status" to "FAILED", "errorCode" to "PAYMENT_FAILED", "errorMessage" to "Payment processing failed"
                    )))
                }
            }
        }
    }
}

@Serializable data class PayRequest(val orderId: String, val amount: Double, val method: String = "WECHAT")
