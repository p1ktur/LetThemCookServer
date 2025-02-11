package features.recipe

import features.auth.TokenManager.checkAccessToken
import features.file.FileClass
import features.file.FileManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.*
import models.database.Recipe.Companion.asRecipeData
import models.database.categories.Categories
import models.database.categories.Category
import models.database.categories.RecipeCategories
import models.database.categories.RecipeCategories.categoryId
import models.database.files.Files
import models.database.files.RecipeFiles
import models.database.files.RecipeFiles.fileId
import models.database.products.Product
import models.database.products.Products
import models.database.products.RecipeProducts
import models.database.products.WeightedProduct
import models.enums.FileType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import utils.decode
import utils.selectPage

// Ideally if request is in different locale
// it is should be that weighted products are decomposed, translated and composed back again
fun Routing.addRecipeRoutes() {
    get("/recipes") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val perPage = call.request.queryParameters["perPage"]?.toIntOrNull()

            val userId = call.request.queryParameters["userId"]
            val searchText = call.request.queryParameters["searchText"]?.decode()

            val recipes = when {
                userId != null -> {
                    transaction {
                        val reviewsCount = Reviews.id.count()

                        Recipes
                            .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                            .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                            .slice(Recipes.columns + reviewsCount + Users.login)
                            .select { Recipes.ownerId eq userId }
                            .groupBy(Recipes.id, Users.login)
                            .orderBy(Recipes.publicationDate to SortOrder.DESC)
                            .toList()
                            .map { it.asRecipeData(reviewsCount) }
                    }
                }
                searchText != null -> {
                    val searchType = SearchType.fromString(call.request.queryParameters["searchType"])
                    val sortType = SortType.fromString(call.request.queryParameters["sortType"])

                    val categories = call.request.queryParameters.getAll("category")
                        ?.map { it.decode() }
                        ?.filterNot { it == "null" || it.isBlank() }
                        ?: emptyList()

                    val products = call.request.queryParameters.getAll("product")
                        ?.map { it.decode() }
                        ?.filterNot { it == "null" || it.isBlank() }
                        ?: emptyList()

                    val recipes = transaction {
                        val reviewsCount = Reviews.id.count()

                        when (searchType) {
                            SearchType.NAME -> {
                                val categoryIds = Categories
                                    .slice(Categories.id)
                                    .select { Categories.name inList categories }
                                    .map { it[Categories.id] }

                                val productIds = Products
                                    .slice(Products.id)
                                    .select { Products.name inList products }
                                    .map { it[Products.id] }

                                Recipes
                                    .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                                    .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                                    .slice(Recipes.columns + reviewsCount + Users.login)
                                    .select {
                                        (Recipes.name like "%$searchText%")
                                            .run {
                                                if (categoryIds.isNotEmpty()) {
                                                    and(
                                                        Recipes.id inSubQuery RecipeCategories
                                                            .slice(RecipeCategories.recipeId)
                                                            .select { categoryId inList categoryIds }
                                                            .groupBy(RecipeCategories.recipeId)
                                                            .having { categoryId.count() greaterEq categoryIds.size.toLong() }
                                                    )
                                                } else {
                                                    this
                                                }
                                            }
                                            .run {
                                                if (productIds.isNotEmpty()) {
                                                    and(
                                                        Recipes.id inSubQuery RecipeProducts
                                                            .slice(RecipeProducts.recipeId)
                                                            .select { RecipeProducts.productId inList productIds }
                                                            .groupBy(RecipeProducts.recipeId)
                                                            .having { RecipeProducts.productId.count() greaterEq productIds.size.toLong() }
                                                    )
                                                } else {
                                                    this
                                                }
                                            }
                                    }
                                    .groupBy(Recipes.id, Users.login)
                                    .orderBy(Recipes.publicationDate to SortOrder.DESC)
                                    .toList()
                                    .selectPage(page, perPage)
                                    .map { it.asRecipeData(reviewsCount) }
                            }
                            SearchType.CATEGORY -> {
                                val categoryIds = Categories
                                    .slice(Categories.id)
                                    .run {
                                        if (categories.isEmpty()) {
                                            select { Categories.name like "%$searchText%" }
                                        } else {
                                            select { (Categories.name like "%$searchText%") or (Categories.name inList categories) }
                                        }
                                    }
                                    .map { it[Categories.id] }

                                if (categoryIds.isEmpty()) {
                                    emptyList()
                                } else {
                                    Recipes
                                        .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                                        .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                                        .slice(Recipes.columns + reviewsCount + Users.login)
                                        .select {
                                            Recipes.id inSubQuery RecipeCategories
                                                .slice(RecipeCategories.recipeId)
                                                .select { categoryId inList categoryIds }
                                                .groupBy(RecipeCategories.recipeId)
                                                .having { categoryId.count() greaterEq categoryIds.size.toLong() }
                                        }
                                        .toList()
                                        .selectPage(page, perPage)
                                        .map { it.asRecipeData(reviewsCount) }
                                }
                            }
                            SearchType.PRODUCTS -> {
                                val productIds = Products
                                    .slice(Products.id)
                                    .run {
                                        if (products.isEmpty()) {
                                            select { Products.name like "%$searchText%" }
                                        } else {
                                            select { (Products.name like "%$searchText%") or (Products.name inList products) }
                                        }
                                    }
                                    .map { it[Products.id] }

                                if (productIds.isEmpty()) {
                                    emptyList()
                                } else {
                                    Recipes
                                        .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                                        .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                                        .slice(Recipes.columns + reviewsCount + Users.login)
                                        .select {
                                            Recipes.id inSubQuery RecipeProducts
                                                .slice(RecipeProducts.recipeId)
                                                .select { RecipeProducts.productId inList productIds }
                                                .groupBy(RecipeProducts.recipeId)
                                                .having { RecipeProducts.productId.count() greaterEq productIds.size.toLong() }
                                        }
                                        .toList()
                                        .selectPage(page, perPage)
                                        .map { it.asRecipeData(reviewsCount) }
                                }
                            }
                        }
                    }

                    when (sortType) {
                        SortType.DATE -> recipes.sortedByDescending { recipe -> recipe.publicationDate }
                        SortType.REVIEWS -> recipes.sortedByDescending { recipe -> recipe.reviewsAmount }
                        SortType.PREPARATIONS -> recipes.sortedByDescending { recipe -> recipe.preparationsAmount }
                        SortType.LIKES -> recipes.sortedByDescending { recipe -> recipe.likesAmount }
                        SortType.POPULARITY -> recipes.sortedByDescending { recipe ->
                            val likes = recipe.likesAmount
                            val dislikes = recipe.dislikesAmount

                            val popularity = (likes.toDouble() / dislikes) * (likes + dislikes) * recipe.viewsAmount

                            popularity
                        }
                    }
                }
                else -> {
                    transaction {
                        val reviewsCount = Reviews.id.count()

                        Recipes
                            .join(Followings, JoinType.LEFT, onColumn = Recipes.ownerId, otherColumn = Followings.followedId)
                            .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                            .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                            .slice(Recipes.columns + reviewsCount + Users.login)
                            .select { (Followings.followerId eq accessToken.userId) or (Recipes.ownerId eq accessToken.userId) }
                            .groupBy(Recipes.id, Users.login)
                            .orderBy(Recipes.publicationDate to SortOrder.DESC)
                            .toList()
                            .selectPage(page, perPage)
                            .map { it.asRecipeData(reviewsCount) }
                    }
                }
            }

            if (recipes.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No recipes found.")
                return@get
            }

            recipes.forEach { recipe ->
                val recipeProducts = transaction {
                    RecipeProducts
                        .join(Products, JoinType.INNER, onColumn = RecipeProducts.productId, otherColumn = Products.id)
                        .select { RecipeProducts.recipeId eq recipe.id }
                        .toList()
                        .map {
                            val product = Product(it[Products.id], it[Products.name])
                            WeightedProduct(product, it[RecipeProducts.weight], it[RecipeProducts.amount])
                        }
                }

                val recipeCategories = transaction {
                    RecipeCategories
                        .join(Categories, JoinType.INNER, onColumn = categoryId, otherColumn = Categories.id)
                        .select { RecipeCategories.recipeId eq recipe.id }
                        .toList()
                        .map { Category(it[Categories.id], it[Categories.name]) }
                }

                val attachmentIds = transaction {
                    RecipeFiles
                        .select { RecipeFiles.recipeId eq recipe.id }
                        .toList()
                        .map { it[fileId] }
                }

                recipe.products = recipeProducts
                recipe.categories = recipeCategories
                recipe.attachmentIds = attachmentIds
            }

            call.respond(HttpStatusCode.OK, recipes)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/recipe") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val recipeId = call.request.queryParameters["recipeId"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@get
            }

            val recipe = transaction {
                val reviewsCount = Reviews.id.count()

                Recipes
                    .join(Reviews, JoinType.LEFT, onColumn = Recipes.id, otherColumn = Reviews.recipeId)
                    .join(Users, JoinType.INNER, onColumn = Recipes.ownerId, otherColumn = Users.id)
                    .slice(Recipes.columns + reviewsCount + Users.login)
                    .select { Recipes.id eq recipeId }
                    .groupBy(Recipes.id, Users.login)
                    .singleOrNull()
                    ?.asRecipeData(reviewsCount)
            }

            if (recipe == null) {
                call.respond(HttpStatusCode.NotFound, "Recipe does not exist.")
                return@get
            }

            val recipeProducts = transaction {
                RecipeProducts
                    .join(Products, JoinType.INNER, onColumn = RecipeProducts.productId, otherColumn = Products.id)
                    .select { RecipeProducts.recipeId eq recipe.id }
                    .toList()
                    .map {
                        val product = Product(it[Products.id], it[Products.name])
                        WeightedProduct(product, it[RecipeProducts.weight], it[RecipeProducts.amount])
                    }
            }

            val recipeCategories = transaction {
                RecipeCategories
                    .join(Categories, JoinType.INNER, onColumn = categoryId, otherColumn = Categories.id)
                    .select { RecipeCategories.recipeId eq recipeId }
                    .toList()
                    .map { Category(it[Categories.id], it[Categories.name]) }
            }

            val attachmentIds = transaction {
                RecipeFiles
                    .select { RecipeFiles.recipeId eq recipeId }
                    .toList()
                    .map { it[fileId] }
            }

            recipe.products = recipeProducts
            recipe.categories = recipeCategories
            recipe.attachmentIds = attachmentIds

            call.respond(HttpStatusCode.OK, recipe)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post<Recipe>("/recipe") { data ->
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            if (accessToken.userId != data.ownerId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@post
            }

            val recipe = transaction {
                Recipes
                    .select { Recipes.id eq data.id }
                    .singleOrNull()
            }

            if (recipe == null) {
                transaction {
                    Recipes.insert {
                        it[id] = data.id
                        it[ownerId] = data.ownerId
                        it[bitmapId] = data.bitmapId
                        it[description] = data.description
                        it[recipeJson] = data.recipeJson
                        it[likesAmount] = data.likesAmount
                        it[dislikesAmount] = data.dislikesAmount
                        it[viewsAmount] = data.viewsAmount
                        it[preparationsAmount] = data.preparationsAmount
                        it[publicationDate] = data.publicationDate
                    }
                }

                transaction {
                    Users.update(
                        where = {
                            Users.id eq data.ownerId
                        },
                        body = {
                            it.update(totalRecipes, totalRecipes + 1)
                        }
                    )
                }

                val products = transaction {
                    data.products.map { weightedProduct ->
                        Products
                            .select { Products.name eq weightedProduct.data.name }
                            .singleOrNull()
                            ?.to(weightedProduct)
                    }
                }

                products.forEach { column ->
                    transaction {
                        if (column != null) {
                            val (product, weightedProduct) = column
                            RecipeProducts.insert {
                                it[productId] = product[Products.id]
                                it[recipeId] = data.id
                                it[weight] = weightedProduct.weight
                                it[amount] = weightedProduct.amount
                            }
                        }
                    }
                }

                val categories = transaction {
                    data.categories.map { category ->
                        Categories
                            .select { Categories.name eq category.name }
                            .singleOrNull()
                    }
                }

                categories.forEach { category ->
                    transaction {
                        if (category != null) {
                            RecipeCategories.insert {
                                it[categoryId] = category[Categories.id]
                                it[recipeId] = data.id
                            }
                        }
                    }
                }
            } else {
                transaction {
                    Recipes.update(
                        where = {
                            Recipes.id eq data.id
                        },
                        body = {
                            it[bitmapId] = data.bitmapId
                            it[name] = data.name
                            it[description] = data.description
                            it[cookingTime] = data.cookingTime
                            it[recipeJson] = data.recipeJson
                        }
                    )
                }

                val products = transaction {
                    RecipeProducts.deleteWhere {
                        recipeId eq data.id
                    }

                    data.products.map { weightedProduct ->
                        Products
                            .select { Products.name eq weightedProduct.data.name }
                            .singleOrNull()
                            ?.to(weightedProduct)
                    }
                }

                products.forEach { column ->
                    transaction {
                        if (column != null) {
                            val (product, weightedProduct) = column
                            RecipeProducts.insert {
                                it[productId] = product[Products.id]
                                it[recipeId] = data.id
                                it[weight] = weightedProduct.weight
                                it[amount] = weightedProduct.amount
                            }
                        }
                    }
                }

                val categories = transaction {
                    RecipeCategories.deleteWhere {
                        recipeId eq data.id
                    }

                    data.categories.map { category ->
                        Categories
                            .select { Categories.name eq category.name }
                            .singleOrNull()
                    }
                }

                categories.forEach { category ->
                    transaction {
                        if (category != null) {
                            RecipeCategories.insert {
                                it[categoryId] = category[Categories.id]
                                it[recipeId] = data.id
                            }
                        }
                    }
                }

                val toDeleteFiles = transaction {
                    RecipeFiles
                        .join(Files, JoinType.INNER, onColumn = fileId, otherColumn = Files.id)
                        .select { (RecipeFiles.recipeId eq data.id) and (fileId notInList data.attachmentIds) }
                        .toList()
                        .map { it[fileId] to FileType.fromString(it[Files.type]) }
                }

                toDeleteFiles.forEach { (fileId, type) ->
                    FileManager.deleteFile(data.ownerId, fileId, type, FileClass.RecipeAttachment(data.id))
                }
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    delete("/recipe") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@delete
            }

            val recipeId = call.request.queryParameters["recipeId"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No Recipe ID provided.")
                return@delete
            }

            val userId = transaction {
                Recipes
                    .select { Recipes.id eq recipeId }
                    .singleOrNull()
                    ?.let { it[Recipes.ownerId] }
            }

            if (accessToken.userId != userId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@delete
            }

            transaction {
                RecipeProducts.deleteWhere {
                    RecipeProducts.recipeId eq recipeId
                }

                RecipeCategories.deleteWhere {
                    RecipeCategories.recipeId eq recipeId
                }

                Reviews.deleteWhere {
                    Reviews.recipeId eq recipeId
                }

                RecipeFiles.deleteWhere {
                    RecipeFiles.recipeId eq recipeId
                }
            }

            FileManager.deleteRecipeFiles(userId, recipeId)

            val rowsAffected = transaction {
                Recipes.deleteWhere { id eq recipeId }
            }

            if (rowsAffected == 0) {
                call.respond(HttpStatusCode.NotFound, "Recipe was not deleted.")
                return@delete
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/recipe_view") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val recipeId = call.request.queryParameters["recipeId"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            transaction {
                Recipes.update(
                    where = {
                        Recipes.id eq recipeId
                    },
                    body = {
                        it.update(viewsAmount, viewsAmount + 1)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/recipe_prepare") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val recipeId = call.request.queryParameters["recipeId"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            transaction {
                Recipes.update(
                    where = {
                        Recipes.id eq recipeId
                    },
                    body = {
                        it.update(preparationsAmount, preparationsAmount + 1)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/recipe_react") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val recipeId = call.request.queryParameters["recipeId"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            val wasLiked = call.request.queryParameters["wasLiked"]?.toBooleanStrictOrNull()
            val liked = call.request.queryParameters["liked"]?.toBooleanStrictOrNull()

            val likedChange = when {
                wasLiked != true && liked != true -> 0
                wasLiked == true && liked == true -> 0
                wasLiked != true && liked == true -> 1
                else -> -1
            }

            val dislikedChange = when {
                wasLiked != false && liked != false -> 0
                wasLiked == false && liked == false -> 0
                wasLiked != false && liked == false -> 1
                else -> -1
            }

            transaction {
                Recipes.update(
                    where = {
                        Recipes.id eq recipeId
                    },
                    body = {
                        it.update(likesAmount, likesAmount + likedChange)
                        it.update(dislikesAmount, dislikesAmount + dislikedChange)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}