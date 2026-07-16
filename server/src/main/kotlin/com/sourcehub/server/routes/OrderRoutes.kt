/**
 * **订单管理路由：创建和列表**
 *
 * 本包定义了 `/api/orders/*` 路由树。所有端点都需要
 * JWT 认证 —— 用户只能查看和创建自己的订单。
 *
 * ## 端点
 * | 方法   | 路径               | 需要认证? | 描述                             |
 * |--------|-------------------|----------|----------------------------------|
 * | POST   | /api/orders       | 是       | 从商品列表创建新订单              |
 * | GET    | /api/orders       | 是       | 列出当前用户的订单（最新优先）     |
 * | GET    | /api/orders/{id}  | 是       | 获取包含行项目的单个订单          |
 *
 * ## 业务逻辑
 * - **价格计算**：服务器在下单时查询每个产品的当前价格。
 *   客户端价格不被信任 —— 这可以防止客户端价格篡改。
 * - **优惠券码**：仅实现了 `"SAVE10"`（10% 折扣）。在生产环境中，
 *   一个包含有效期、使用次数限制和最低订单金额的 `coupons` 表
 *   应取代此硬编码检查。
 * - **库存**：不跟踪库存数量。每个产品假定为具有无限副本的数字商品。
 *   对于有许可证限制的产品，添加 `inventory` 列并原子性地递减。
 *
 * ## 生产就绪
 * - **幂等性**：创建订单端点没有幂等键。客户端的重试
 *   （如服务器已提交后的网络超时）可能会创建重复订单。
 *   添加客户端提供的幂等键。
 * - **并发**：价格查询和订单插入发生在不同的事务中。
 *   产品价格可能在查询和插入之间发生变化。
 *   在生产数据库中，使用带有行级锁（`SELECT ... FOR UPDATE`）的单一事务。
 * - **授权**：详情端点不验证订单是否属于已认证用户。
 *   任何持有有效令牌的人都可以通过 ID 查看任何订单。添加所有权检查。
 */
package com.sourcehub.server.routes

import com.sourcehub.server.models.*
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

/**
 * 在 `/api/orders` 下挂载所有订单路由，受 JWT 认证保护。
 *
 * @param jwtManager 用于提取当前用户 ID 的 JWT 令牌管理器。
 * @param db 用于执行查询的 Exposed 数据库句柄。
 */
fun Routing.orderRoutes(jwtManager: JwtManager, db: Database) {
    authenticate("auth-jwt") {
        route("/api/orders") {

            /**
             * POST /api/orders
             *
             * 从商品请求列表创建新订单。服务器从数据库中解析每个产品的
             * 价格和标题（客户端提交的价格被忽略），并应用任何有效的
             * 优惠券折扣。
             *
             * 期望 JSON 请求体：[CreateOrderRequest]
             *
             * 状态码：
             * - 200：订单已创建（status = "PENDING"）。
             * - 400：空订单（无商品）。
             * - 401：未认证。
             */
            post {
                val userId = jwtManager.getUserId(call) ?: return@post call.respond(mapOf("code" to 401))
                val req = call.receive<CreateOrderRequest>()
                if (req.items.isEmpty()) return@post call.respond(mapOf("code" to 400, "message" to "Empty order"))

                // 对每个行项目，从数据库获取当前价格和标题。
                // 客户端价格不被信任。
                var total = 0.0
                val itemsWithPrice = req.items.map { item ->
                    val product = newSuspendedTransaction(db = db) {
                        Products.select { Products.id eq item.productId }.singleOrNull()
                    } ?: throw Exception("Product ${item.productId} not found")
                    total += product[Products.price] * item.quantity
                    Triple(item, product[Products.price], product[Products.title])
                }

                // 硬编码的优惠券 —— 在生产环境中替换为数据库支持的
                // 优惠券服务。
                var discount = 0.0
                if (req.couponCode == "SAVE10") discount = total * 0.1

                val newOrderId = "order_${UUID.randomUUID().toString().take(8)}"
                val now = System.currentTimeMillis()

                // 在单个事务中插入订单头部和行项目以保证原子性。
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

            /**
             * GET /api/orders
             *
             * 列出属于已认证用户的所有订单，按创建日期排序（最新优先）。
             * 每个订单包含其行项目。
             */
            get {
                val userId = jwtManager.getUserId(call) ?: return@get call.respond(mapOf("code" to 401))
                val orders = newSuspendedTransaction(db = db) {
                    Orders.select { Orders.userId eq userId }.orderBy(Orders.createdAt, SortOrder.DESC).map { orderRow ->
                        // 为每个订单获取行项目。N+1 查询模式
                        // —— 对于少量订单可接受；在批量场景中使用 join。
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

            /**
             * GET /api/orders/{id}
             *
             * 返回包含行项目的单个订单。
             *
             * **安全说明**：此端点不验证订单是否属于已认证用户。
             * 在生产环境中添加所有权检查。
             */
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
        }
    }
}

/** POST /api/orders 的 JSON 请求体。 */
@Serializable data class CreateOrderRequest(
    val items: List<OrderItemReq>,
    val couponCode: String = "",
    val paymentMethod: String = "WECHAT"
)

/** 创建订单请求中的单条行项目。 */
@Serializable data class OrderItemReq(val productId: String, val quantity: Int)
