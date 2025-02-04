package models.database.categories

import org.jetbrains.exposed.sql.Table

data class RecipeCategory(
    val id: Int = 0,
    val recipeId: String,
    val categoryId: Int
)

object RecipeCategories : Table("recipe_categories") {
    val id = integer("id").autoIncrement()
    val recipeId = varchar("recipeId", 36)
    val categoryId = integer("categoryId")
}