package models.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

data class Recipe(
    val id: String,
    val ownerId: String,
    val imageId: String?,
    val authorLogin: String, // Not in DB
    val name: String?,
    val description: String?,
    val recipeJson: String,
    val likesAmount: Int = 0,
    val dislikesAmount: Int = 0,
    val viewsAmount: Int = 0,
    val preparationsAmount: Int = 0,
    val reviewsAmount: Int, // Not in DB
    var products: List<String> = emptyList(),
    var categories: List<String> = emptyList(),
    var publicationDate: LocalDateTime
) {
    companion object {
        fun ResultRow.asRecipeData(reviewsAmountColumn: Count): Recipe {
            return Recipe(
                id = this[Recipes.id],
                ownerId = this[Recipes.ownerId],
                imageId = this[Recipes.imageId],
                authorLogin = this[Users.login],
                name = this[Recipes.name],
                description = this[Recipes.description],
                recipeJson = this[Recipes.recipeJson],
                likesAmount = this[Recipes.likesAmount],
                dislikesAmount = this[Recipes.dislikesAmount],
                viewsAmount = this[Recipes.viewsAmount],
                preparationsAmount = this[Recipes.preparationsAmount],
                reviewsAmount = this[reviewsAmountColumn].toInt(),
                publicationDate = this[Recipes.publicationDate]
            )
        }
    }
}

object Recipes : Table("recipes") {
    val id = varchar("id", 36)
    val ownerId = varchar("ownerId", 36).references(Users.id)
    val imageId = varchar("imageId", 255).references(Files.id).nullable()
    val name = varchar("name", 255).nullable()
    val description = varchar("description", 255).nullable()
    val recipeJson = varchar("recipeJson", 255)
    val likesAmount = integer("likesAmount").default(0)
    val dislikesAmount = integer("dislikesAmount").default(0)
    val viewsAmount = integer("viewsAmount").default(0)
    val preparationsAmount = integer("preparationsAmount").default(0)
    val publicationDate = datetime("publicationDate")
}