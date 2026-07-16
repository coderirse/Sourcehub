/**
 * **SourceHub 数据库表结构的 Exposed ORM 表定义**
 *
 * 本文件使用 JetBrains [Exposed](https://github.com/JetBrains/Exposed) 作为
 * 类型安全的 SQL DSL 声明数据库表结构。每个对象与一个数据库表一一映射，
 * 并将其列暴露为类型化属性。框架将这些声明转换为 DDL（通过 [SchemaUtils.create]）
 * 和类型检查的查询表达式。
 *
 * ## 为什么选择 Exposed？
 * 选择 Exposed 而非原始 JDBC 或完整 ORM（Hibernate）的原因：
 * - 它为列引用提供**编译时类型安全**（查询中不使用字符串列名）。
 * - 它**轻量级** —— DSL 接近 SQL 语义，没有 JPA 实体图、懒加载或会话管理的复杂性。
 * - 通过 `newSuspendedTransaction` / `transaction` 块与 Ktor 协程自然集成。
 * - 支持多种数据库（H2、PostgreSQL、MySQL），只需极少的更改。
 *
 * ## 为什么本地开发使用 H2？
 * H2 是用 Java 编写的**嵌入式、零配置**数据库。对于本地开发：
 * - 无需外部数据库进程 —— 数据库存在于文件中（`./data/sourcehub.mv.db`）。
 * - 启动时即时创建表结构。
 * - `AUTO_SERVER=TRUE` 允许多个 JVM 进程（如服务器和数据库检查工具）同时访问同一文件。
 * 在生产环境中，将 `DB_DRIVER` 和 `DB_URL` 切换为 PostgreSQL/MySQL。
 *
 * ## 表设计说明
 * - 使用**字符串 ID**（如 `"u_abc12345"`）而非自增整数，
 *   以便 ID 可以在客户端或应用程序代码中生成，无需往返数据库。
 * - **时间戳**存储为 `Long`（epoch 毫秒）而非数据库 `TIMESTAMP` 类型，
 *   以避免不同数据库引擎之间的时区陷阱。
 * - **外键**通过 [Orders.userId] 和 [OrderItems.orderId] 上的 `references` 声明。
 *   Exposed **不会**在数据库级别强制执行这些约束（H2 需要显式约束），
 *   但 DSL 将其用于连接推断和文档。
 * - 提供了**默认值**（如 `avatarUrl` 上的 `default("")`、`status = "PENDING"`），
 *   以便插入语句可以省略可选列。
 *
 * ## 生产就绪
 * - 在频繁查询的列上添加**索引**：`Users.email`、
 *   `Products.category`、`Orders.userId`、`Orders.status`。
 * - 如果迁移到 PostgreSQL，考虑使用 `uuid` 类型配合原生 UUID 列，而非 VARCHAR(64)。
 * - [Products] 上的 `file_path` 列存储相对路径。在生产环境中，
 *   应指向对象存储键（S3、MinIO）而非本地文件系统路径。
 */
package com.sourcehub.server.models

import org.jetbrains.exposed.sql.*

/**
 * SourceHub 市场的注册用户。
 *
 * 密码存储为 BCrypt 哈希（12 轮）—— 参见 [com.sourcehub.server.routes.AuthRoutes]。
 */
object Users : Table("users") {
    /** 唯一用户标识，如 `"u_a1b2c3d4"`。 */
    val id = varchar("id", 64)
    /** 显示名称（可能不唯一）。 */
    val name = varchar("name", 128)
    /** 用于登录的邮箱地址；必须唯一（目前尚未在数据库层面强制）。 */
    val email = varchar("email", 256)
    /** 用户密码的 BCrypt 哈希。 */
    val passwordHash = varchar("password_hash", 256)
    /** 用户头像图片的可选 URL。 */
    val avatarUrl = varchar("avatar_url", 512).default("")
    /** 可选的手机号码。 */
    val phone = varchar("phone", 32).default("")
    /** 账户创建时间戳（epoch 毫秒）。 */
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

/**
 * 可供购买的数字产品（简历模板、PPT 演示文稿、
 * 电子书、学习笔记、商业文档）。
 */
object Products : Table("products") {
    /** 唯一产品标识，如 `"p1"`（种子数据）或 `"prod_xxx"`（动态）。 */
    val id = varchar("id", 64)
    /** 显示在产品卡片和详情页的标题。 */
    val title = varchar("title", 256)
    /** 完整描述（可能很长 —— 存储为 TEXT）。 */
    val description = text("description")
    /** 创作者/作者署名。 */
    val author = varchar("author", 128)
    /** 当前售价。 */
    val price = double("price")
    /** 原价/标价（用于显示 "原价 ~~¥X~~" 折扣标识）。 */
    val originalPrice = double("original_price")
    /** 封面/缩略图图片的 URL。 */
    val coverUrl = varchar("cover_url", 512).default("")
    /** 可下载文件的相对路径（或对象存储键）。 */
    val filePath = varchar("file_path", 512).default("")
    /** 文件格式："PDF"、"DOCX"、"PPTX" 等。 */
    val fileType = varchar("file_type", 16).default("PDF")
    /** 页数（对文档有意义；其他类型为 0）。 */
    val pageCount = integer("page_count").default(0)
    /** 文件大小，单位字节。 */
    val fileSize = long("file_size").default(0)
    /** 用于筛选的分类标识，如 "简历模板"、"电子书"。 */
    val category = varchar("category", 64).default("")
    /** 累计付费购买次数（用于按热度排序）。 */
    val salesCount = integer("sales_count").default(0)
    /** 平均评分（0.0 - 5.0）。 */
    val rating = float("rating").default(0f)
    /** 逗号分隔的标签列表，如 "简历,创意,设计"。 */
    val tags = varchar("tags", 512).default("")
    /** 产品上架创建时间戳（epoch 毫秒）。 */
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

/**
 * 客户订单。每个订单包含一个或多个 [OrderItems]，
 * 并与单个 [Users.id] 关联。
 */
object Orders : Table("orders") {
    /** 唯一订单标识，如 `"order_a1b2c3d4"`。 */
    val id = varchar("id", 64)
    /** 购买者的用户 ID（引用 [Users.id]）。 */
    val userId = varchar("user_id", 64) references Users.id
    /** 折扣前所有商品价格的总和。 */
    val totalAmount = double("total_amount")
    /** 已应用的折扣（优惠券、促销）。 */
    val discountAmount = double("discount_amount").default(0.0)
    /** 实际收费金额 = [totalAmount] - [discountAmount]。 */
    val finalAmount = double("final_amount")
    /** 订单生命周期："PENDING" -> "PAID" -> "CANCELLED"（等）。 */
    val status = varchar("status", 32).default("PENDING")
    /** 支付渠道："WECHAT"、"ALIPAY"、"CARD"。 */
    val paymentMethod = varchar("payment_method", 32).default("WECHAT")
    /** 支付网关返回的外部交易 ID。 */
    val transactionId = varchar("transaction_id", 128).default("")
    /** 已应用的优惠券码，如 "SAVE10"。 */
    val couponCode = varchar("coupon_code", 64).default("")
    /** 订单创建时间戳（epoch 毫秒）。 */
    val createdAt = long("created_at")
    /** 支付完成时间戳（未支付则为 0）。 */
    val paidAt = long("paid_at").default(0)
    override val primaryKey = PrimaryKey(id)
}

/**
 * 订单中的行项目。每行将 [productId] 链接到 [orderId]，
 * 包含数量和购买时的单价（快照）。
 */
object OrderItems : Table("order_items") {
    /** 唯一行项目标识。 */
    val id = varchar("id", 64)
    /** 父订单（引用 [Orders.id]）。 */
    val orderId = varchar("order_id", 64) references Orders.id
    /** 已购买的产品 ID（引用 [Products.id]，不作为外键强制执行）。 */
    val productId = varchar("product_id", 64)
    /** 购买时的产品标题（反规范化以便显示）。 */
    val productTitle = varchar("product_title", 256)
    /** 购买时的单价。 */
    val unitPrice = double("unit_price")
    /** 购买的份数/单位数。 */
    val quantity = integer("quantity")
    override val primaryKey = PrimaryKey(id)
}

/**
 * 下载审计日志。每次用户请求已购买产品的下载 URL 时创建一条记录。
 * 这允许将来实现速率限制和下载次数限制。
 */
object Downloads : Table("downloads") {
    /** 唯一下载记录标识。 */
    val id = varchar("id", 64)
    /** 发起下载的用户。 */
    val userId = varchar("user_id", 64)
    /** 购买该产品所用的订单。 */
    val orderId = varchar("order_id", 64)
    /** 正在下载的产品。 */
    val productId = varchar("product_id", 64)
    /** 下载的建议文件名。 */
    val fileName = varchar("file_name", 256)
    /** 下载请求时间戳（epoch 毫秒）。 */
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
