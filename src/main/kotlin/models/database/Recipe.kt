package models.database

import kotlinx.serialization.Serializable
import models.database.categories.Category
import models.database.files.Files
import models.database.products.WeightedProduct
import models.serializers.LocalDateTimeSerializer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

@Serializable
data class Recipe(
    val id: String,
    val ownerId: String,
    val bitmapId: String?,
    val authorLogin: String, // Not in DB
    val name: String?,
    val description: String?,
    val cookingTime: Long?,
    val recipeJson: String?,
    val likesAmount: Int = 0,
    val dislikesAmount: Int = 0,
    val viewsAmount: Int = 0,
    val preparationsAmount: Int = 0,
    val reviewsAmount: Int, // Not in DB
    var products: List<WeightedProduct> = emptyList(), // Not in DB
    var categories: List<Category> = emptyList(), // Not in DB
    var attachmentIds: List<String> = emptyList(), // Not in DB
    @Serializable(with = LocalDateTimeSerializer::class) var publicationDate: LocalDateTime
) {
    companion object {
        fun ResultRow.asRecipeData(reviewsAmountColumn: Count): Recipe {
            return Recipe(
                id = this[Recipes.id],
                ownerId = this[Recipes.ownerId],
                bitmapId = this[Recipes.bitmapId],
                authorLogin = this[Users.login],
                name = this[Recipes.name],
                description = this[Recipes.description],
                cookingTime = this[Recipes.cookingTime],
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
    val bitmapId = varchar("imageId", 255).references(Files.id).nullable()
    val name = varchar("name", 255).nullable()
    val description = varchar("description", 255).nullable()
    val cookingTime = long("cookingTime").nullable()
    val recipeJson = varchar("recipeJson", 255).nullable()
    val likesAmount = integer("likesAmount").default(0)
    val dislikesAmount = integer("dislikesAmount").default(0)
    val viewsAmount = integer("viewsAmount").default(0)
    val preparationsAmount = integer("preparationsAmount").default(0)
    val publicationDate = datetime("publicationDate")
}