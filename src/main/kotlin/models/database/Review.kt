package models.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Review(
    val id: String,
    val authorId: String,
    val recipeId: String,
    val reviewText: String?,
    val likesAmount: Int = 0
)

object Reviews : Table("reviews") {
    val id = varchar("id", 255)
    val authorId = varchar("authorId", 36).references(Users.id)
    val recipeId = varchar("recipeId", 36).references(Recipes.id)
    val reviewText = varchar("reviewText", 255).nullable()
    val likesAmount = integer("likesAmount").default(0)
}