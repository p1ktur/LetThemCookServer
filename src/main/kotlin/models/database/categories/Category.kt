package models.database.categories

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Category(
    val id: Int = 0,
    val name: String
)

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
    val nameUA = varchar("nameUA", 255).uniqueIndex()
}