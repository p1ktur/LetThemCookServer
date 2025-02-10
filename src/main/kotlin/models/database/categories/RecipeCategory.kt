package models.database.categories

import org.jetbrains.exposed.sql.Table

object RecipeCategories : Table("recipe_categories") {
    val id = integer("id").autoIncrement()
    val recipeId = varchar("recipeId", 36)
    val categoryId = integer("categoryId")
}