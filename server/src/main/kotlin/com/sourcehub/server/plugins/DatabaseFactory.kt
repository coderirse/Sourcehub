/**
 * **数据库初始化与表结构管理**
 *
 * 此包级插件负责：
 * 1. 创建本地数据目录（`./data/`）和上传目录。
 * 2. 使用 [AppConfig] 中的 JDBC URL/驱动连接到数据库。
 * 3. 运行 [SchemaUtils.create] 确保所有 Exposed 表定义在数据库中存在
 *    （幂等 —— 仅在不存在时创建）。
 * 4. 如果 products 表为空，通过 [SeedData.insertAll] 填充演示产品数据。
 *
 * ## 为什么使用 `SchemaUtils.create`（而非迁移）？
 * 对于原型/早期阶段的项目，在启动时自动创建表很方便。
 * 然而，这种方法有局限性：
 * - 无法处理表结构**变更**（列重命名、类型更改等）。
 * - 如果手动执行了 DDL，可能会静默地创建与预期略有不同的表结构。
 *
 * **生产环境建议**：将 [SchemaUtils.create] 替换为合适的迁移工具，
 * 如 **Flyway** 或 **Liquibase**。这确保表结构变更被版本化、审查并确定性地应用。
 *
 * ## 为什么使用 `transaction`（而非 `newSuspendedTransaction`）？
 * 数据库初始化发生在服务器启动时，在接受任何 HTTP 请求之前。
 * 此时不需要协程挂起，因此使用更简单的阻塞式 [transaction] API 是合适的。
 * 挂起式变体（[newSuspendedTransaction]）用于路由处理器中，
 * 因为调用协程不得阻塞事件循环线程。
 *
 * ## 生产就绪
 * - 添加连接池（HikariCP）。Exposed 可以使用 HikariCP
 *   `DataSource` 而非原始 JDBC URL。
 * - 将 `File("data").mkdirs()` 移至适当的启动钩子，或使用 Docker
 *   卷确保目录在 JVM 启动前存在。
 * - 在启动时记录（脱敏后的）数据库 URL 和驱动版本，以便运维可见性。
 */
package com.sourcehub.server.plugins

import com.sourcehub.server.config.AppConfig
import com.sourcehub.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * 初始化并持有数据库连接的单例。
 *
 * [init] 返回的 [Database] 实例被传递给每个路由处理器，
 * 以便所有数据库操作共享同一个连接池。
 */
object DatabaseFactory {
    /**
     * 初始化数据库：
     * - 创建本地目录（`data/` 和配置的上传目录）。
     * - 通过 Exposed 的 [Database.connect] 打开 JDBC 连接。
     * - 创建 [com.sourcehub.server.models] 中定义的所有表（如果尚不存在）。
     * - 如果 [Products] 表为空，填充演示产品数据。
     *
     * @param config 提供 JDBC URL、驱动、凭据和上传目录路径的应用程序配置。
     * @return 已连接的 [Database] 对象，供路由处理器使用。
     */
    fun init(config: AppConfig): Database {
        // 确保所需的目录在数据库尝试在其中创建文件之前存在。
        File("data").mkdirs()
        File(config.uploadDir).mkdirs()

        val db = Database.connect(
            url = config.dbUrl,
            driver = config.dbDriver,
            user = config.dbUser,
            password = config.dbPassword
        )

        // SchemaUtils.create 是幂等的：仅在表不存在时创建。
        // 在生产环境中，替换为 Flyway/Liquibase。
        transaction(db) {
            SchemaUtils.create(Users, Products, Orders, OrderItems, Downloads)
        }

        // 仅在首次运行时填充演示数据（products 表为空时）。
        transaction(db) {
            val count = Products.selectAll().count()
            if (count == 0L) SeedData.insertAll()
        }

        return db
    }
}
