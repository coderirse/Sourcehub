package com.sourcehub.server.plugins

import com.sourcehub.server.config.AppConfig
import com.sourcehub.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    fun init(config: AppConfig): Database {
        File("data").mkdirs()
        File(config.uploadDir).mkdirs()

        val db = Database.connect(
            url = config.dbUrl,
            driver = config.dbDriver,
            user = config.dbUser,
            password = config.dbPassword
        )

        transaction(db) {
            SchemaUtils.create(Users, Products, Orders, OrderItems, Downloads)
        }

        // Seed demo data
        transaction(db) {
            val count = Products.selectAll().count()
            if (count == 0L) SeedData.insertAll()
        }

        return db
    }
}
