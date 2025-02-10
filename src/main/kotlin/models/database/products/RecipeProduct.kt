package models.database.products

import org.jetbrains.exposed.sql.Table

object RecipeProducts : Table("recipe_products") {
    val id = integer("id").autoIncrement()
    val productId = integer("productId")
    val recipeId = varchar("recipeId", 36)
    val weight = integer("weight")
    val amount = integer("amount")
}