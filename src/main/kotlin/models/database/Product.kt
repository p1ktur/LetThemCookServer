package models.database

import org.jetbrains.exposed.sql.Table

data class Product(
    val id: Int = 0,
    val name: String
)

object Products : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
}