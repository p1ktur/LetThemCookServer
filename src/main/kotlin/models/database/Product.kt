package models.database

import org.jetbrains.exposed.sql.Table

data class Product(
    val id: Int,
    val name: String
)

object Products : Table("products") {
    val id = integer("id").entityId()
    val name = varchar("name", 255).uniqueIndex()
}