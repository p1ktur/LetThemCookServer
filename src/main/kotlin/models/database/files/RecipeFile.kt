package models.database.files

import org.jetbrains.exposed.sql.Table

data class RecipeFile(
    val id: String,
    val fileId: String,
    val recipeId: String
)

object RecipeFiles : Table("recipe_files") {
    val id = varchar("id", 36)
    val fileId = varchar("fileId", 36)
    val recipeId = varchar("recipeId", 36)
}