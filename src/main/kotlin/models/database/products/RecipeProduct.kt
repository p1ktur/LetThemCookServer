package models.database.products

import org.jetbrains.exposed.sql.Table

data class RecipeProduct(
    val id: Int = 0,
    val productId: Int,
    val recipeId: String,
    val weight: Int,
    val amount: Int
)

object RecipeProducts : Table("recipe_products") {
    val id = integer("id").autoIncrement()
    val productId = integer("productId")
    val recipeId = varchar("recipeId", 36)
    val weight = integer("weight")
    val amount = integer("amount")
}