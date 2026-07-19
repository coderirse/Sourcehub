package com.sourcehub.server.routes

import com.sourcehub.server.models.*
import com.sourcehub.server.security.JwtManager
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

fun Routing.orderRoutes(jwtManager: JwtManager, db: Database) {
    authenticate("auth-jwt") {
        route("/api/orders") {

            post {
                val userId = jwtManager.getUserId(call) ?: return@post call.respond(mapOf("code" to 401))
                val req = call.receive<CreateOrderRequest>()
                if (req.items.isEmpty()) return@post call.respond(mapOf("code" to 400, "message" to "Empty order"))

                // Calculate total
                var total = 0.0
                val itemsWithPrice = req.items.map { item ->
                    val product = newSuspendedTransaction(db = db) {
                        Products.select { Products.id eq item.productId }.singleOrNull()
                    } ?: throw Exception("Product ${item.productId} not found")
                    total += product[Products.price] * item.quantity
                    Triple(item, product[Products.price], product[Products.title])
                }

                var discount = 0.0
                if (req.couponCode == "SAVE10") discount = total * 0.1

                val newOrderId = "order_${UUID.randomUUID().toString().take(8)}"
                val now = System.currentTimeMillis()

                newSuspendedTransaction(db = db) {
                    Orders.insert {
                        it[id] = newOrderId
                        it[Orders.userId] = userId
                        it[totalAmount] = total
                        it[discountAmount] = discount
                        it[finalAmount] = total - discount
                        it[status] = "PENDING"
                        it[paymentMethod] = req.paymentMethod
                        it[couponCode] = req.couponCode
                        it[createdAt] = now
                    }
                    itemsWithPrice.forEach { (item, price, title) ->
                        OrderItems.insert {
                            it[id] = "oi_${UUID.randomUUID().toString().take(8)}"
                            it[orderId] = newOrderId
                            it[productId] = item.productId
                            it[productTitle] = title
                            it[unitPrice] = price
                            it[quantity] = item.quantity
                        }
                    }
                }
                call.respond(mapOf("code" to 200, "data" to mapOf(
                    "id" to newOrderId, "totalAmount" to total, "discountAmount" to discount,
                    "finalAmount" to (total - discount), "status" to "PENDING",
                    "createdAt" to now
                )))
            }

            get {
                val userId = jwtManager.getUserId(call) ?: return@get call.respond(mapOf("code" to 401))
                val orders = newSuspendedTransaction(db = db) {
                    Orders.select { Orders.userId eq userId }.orderBy(Orders.createdAt, SortOrder.DESC).map { orderRow ->
                        val items = OrderItems.select { OrderItems.orderId eq orderRow[Orders.id] }.map { itemRow ->
                            mapOf(
                                "id" to itemRow[OrderItems.id], "orderId" to itemRow[OrderItems.orderId],
                                "productId" to itemRow[OrderItems.productId], "productTitle" to itemRow[OrderItems.productTitle],
                                "unitPrice" to itemRow[OrderItems.unitPrice], "quantity" to itemRow[OrderItems.quantity]
                            )
                        }
                        mapOf(
                            "id" to orderRow[Orders.id], "userId" to orderRow[Orders.userId],
                            "totalAmount" to orderRow[Orders.totalAmount], "discountAmount" to orderRow[Orders.discountAmount],
                            "finalAmount" to orderRow[Orders.finalAmount], "status" to orderRow[Orders.status],
                            "paymentMethod" to orderRow[Orders.paymentMethod], "transactionId" to orderRow[Orders.transactionId],
                            "couponCode" to orderRow[Orders.couponCode], "createdAt" to orderRow[Orders.createdAt],
                            "paidAt" to orderRow[Orders.paidAt], "items" to items
                        )
                    }
                }
                call.respond(mapOf("code" to 200, "data" to orders))
            }

            get("/{id}") {
                val orderId = call.parameters["id"] ?: ""
                val order = newSuspendedTransaction(db = db) {
                    Orders.select { Orders.id eq orderId }.singleOrNull()
                } ?: return@get call.respond(mapOf("code" to 404, "message" to "Order not found"))
                val items = newSuspendedTransaction(db = db) {
                    OrderItems.select { OrderItems.orderId eq orderId }.map { row ->
                        mapOf(
                            "id" to row[OrderItems.id], "orderId" to row[OrderItems.orderId],
                            "productId" to row[OrderItems.productId], "productTitle" to row[OrderItems.productTitle],
                            "unitPrice" to row[OrderItems.unitPrice], "quantity" to row[OrderItems.quantity]
                        )
                    }
                }
                call.respond(mapOf("code" to 200, "data" to mapOf(
                    "id" to order[Orders.id], "totalAmount" to order[Orders.totalAmount],
                    "finalAmount" to order[Orders.finalAmount], "status" to order[Orders.status],
                    "createdAt" to order[Orders.createdAt], "items" to items
                )))
            }

            // Cancel a pending order
            post("/{id}/cancel") {
                val orderId = call.parameters["id"] ?: ""
                newSuspendedTransaction(db = db) {
                    Orders.update({ (Orders.id eq orderId) and (Orders.status eq "PENDING") }) {
                        it[status] = "CANCELLED"
                    }
                }
                val updated = newSuspendedTransaction(db = db) {
                    Orders.select { Orders.id eq orderId }.singleOrNull()
                }
                if (updated != null) {
                    call.respond(mapOf("code" to 200, "data" to mapOf(
                        "id" to updated[Orders.id], "status" to updated[Orders.status]
                    )))
                } else {
                    call.respond(mapOf("code" to 404, "message" to "Order not found or cannot be cancelled"))
                }
            }
        }
    }
}

 data class CreateOrderRequest(
    val items: List<OrderItemReq>,
    val couponCode: String = "",
    val paymentMethod: String = "WECHAT"
)
 data class OrderItemReq(val productId: String, val quantity: Int)
