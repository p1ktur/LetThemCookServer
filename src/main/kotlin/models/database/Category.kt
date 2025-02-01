package models.database

import org.jetbrains.exposed.sql.Table

data class Category(
    val id: Int,
    val name: String
)

object Categories : Table("categories") {
    val id = integer("id").entityId()
    val name = varchar("name", 255).uniqueIndex()
}