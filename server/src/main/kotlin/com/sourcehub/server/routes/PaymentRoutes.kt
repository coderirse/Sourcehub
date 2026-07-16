/**
 * **模拟支付处理路由**
 *
 * 本包定义了 `/api/payment/*` 路由树。**支付完全是模拟的** ——
 * 未集成真实的支付网关。这允许在不设置商户账户的情况下
 * 演示和测试完整的购买流程（浏览 -> 下单 -> 支付 -> 下载）。
 *
 * ## 端点
 * | 方法   | 路径               | 需要认证? | 描述               |
 * |--------|-------------------|----------|--------------------|
 * | POST   | /api/payment/pay  | 是       | 处理订单支付        |
 *
 * ## 模拟行为
 * - **90% 成功率**：调用 `Math.random()` 确定结果。
 *   这模拟了客户端必须处理的偶尔支付失败。
 * - **无金额验证**：[PayRequest.amount] 被接受，不验证其是否
 *   与订单的 [Orders.finalAmount] 一致。在生产环境中，
 *   始终将提交的金额与服务器端订单记录进行比较。
 * - **无幂等性**：重复的 `/pay` 调用将用新的交易 ID 覆盖现有 ID。
 *   在生产环境中，支付网关应提供幂等键，服务器应拒绝重复支付。
 *
 * ## 生产环境集成指南
 * 用真实支付网关集成替换此整个模块：
 * 1. **微信支付 / 支付宝**：使用官方 SDK 生成预支付订单，
 *    并将支付参数（appId、nonceStr、package、signType、
 *    paySign、timestamp）返回给客户端。
 * 2. **Stripe**：在服务器端创建 PaymentIntent 并返回
 *    client_secret；在客户端确认。
 * 3. **Webhook**：监听来自网关的异步支付确认，
 *    仅在收到经过验证的 webhook 后将订单标记为 PAID。
 *    绝不信任客户端报告的支付成功。
 * 4. **退款**：添加 `/api/payment/refund` 端点，
 *    调用网关的退款 API 并更新订单状态。
 */
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

/**
 * 在 `/api/payment` 下挂载模拟支付路由，受 JWT 认证保护。
 *
 * @param jwtManager JWT 令牌管理器（当前未使用，但保留供将来使用，
 *                   例如记录发起支付的用户）。
 * @param db Exposed 数据库句柄。
 */
fun Routing.paymentRoutes(jwtManager: JwtManager, db: Database) {
    authenticate("auth-jwt") {
        route("/api/payment") {

            /**
             * POST /api/payment/pay
             *
             * 模拟处理给定订单的支付。
             *
             * 期望 JSON 请求体：[PayRequest]
             *
             * 成功时（90% 概率）：
             * - 生成模拟的交易 ID。
             * - 将订单状态更新为 "PAID"，附带交易 ID 和支付时间戳。
             *
             * 失败时（10% 概率）：
             * - 返回错误响应，不修改订单。
             *
             * **重要**：这是一个模拟。在生产环境中，
             * 替换为真实的支付网关集成。参见文件级 KDoc 中的生产环境指南。
             */
            post("/pay") {
                val req = call.receive<PayRequest>()
                // 模拟支付：90% 的成功概率。
                // Math.random() 不是加密安全的，但这仅用于演示目的。
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

/** POST /api/payment/pay 的 JSON 请求体。 */
@Serializable data class PayRequest(val orderId: String, val amount: Double, val method: String = "WECHAT")
