package com.example.sourcehub.data.remote.dto

/**
 * 所有 API 响应的通用信封。
 *
 * 对应标准后端响应格式：
 * ```json
 * {"code": 200, "message": "success", "data": { ... }}
 * ```
 *
 * @param code HTTP 风格的状态码（200 = 成功，4xx = 客户端错误，5xx = 服务端错误）。
 * @param message 人类可读的消息，始终填充（即使成功时也有）。
 * @param data 类型化的载荷；当响应不携带数据体时为 null。
 */
data class ApiResponse<T>(
    val code: Int = 200,
    val message: String = "success",
    val data: T? = null
)
