package models.database

import org.jetbrains.exposed.sql.Table

data class Recipe(
    val id: String,
    val ownerId: String,
    val imageId: String?,
    val description: String?,
    val recipeJson: String,
    val likesAmount: Int = 0,
    val dislikesAmount: Int = 0,
    val viewsAmount: Int = 0,
    val cookedAmount: Int = 0,
    val products: List<String> = emptyList()
)

object Recipes : Table("recipes") {
    val id = varchar("id", 255).entityId()
    val ownerId = varchar("ownerId", 255).references(Users.id)
    val imageId = varchar("imageId", 255).references(Files.id).nullable()
    val description = varchar("description", 255).nullable()
    val recipeJson = varchar("recipeJson", 255)
    val likesAmount = integer("likesAmount").default(0)
    val dislikesAmount = integer("dislikesAmount").default(0)
    val viewsAmount = integer("viewsAmount").default(0)
    val cookedAmount = integer("cookedAmount").default(0)
}