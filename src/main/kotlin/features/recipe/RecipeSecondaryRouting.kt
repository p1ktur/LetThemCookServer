package features.recipe

import features.auth.TokenManager.checkAccessToken
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.categories.Categories
import models.database.categories.Category
import models.database.products.Product
import models.database.products.Products
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.decode
import utils.selectPage

fun Routing.addRecipeSecondaryRoutes() {
    get("/categories") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val perPage = call.request.queryParameters["perPage"]?.toIntOrNull()
            val language = call.request.queryParameters["language"] ?: "us"

            val searchText = call.request.queryParameters["searchText"]?.decode()

            val categories = when {
                searchText != null -> {
                    transaction {
                        Categories
                            .select { (Categories.name like "%$searchText%") or (Categories.nameUA like "%$searchText%") }
                            .toList()
                            .selectPage(page, perPage)
                            .map {
                                Category(
                                    id = it[Categories.id],
                                    name = if (language == "uk") it[Categories.nameUA] else it[Categories.name]
                                )
                            }
                    }
                }
                else -> {
                    transaction {
                        Categories
                            .selectAll()
                            .toList()
                            .selectPage(page, perPage)
                            .map {
                                Category(
                                    id = it[Categories.id],
                                    name = if (language == "uk") it[Categories.nameUA] else it[Categories.name]
                                )
                            }
                    }
                }
            }

            if (categories.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No categories found.")
                return@get
            }

            call.respond(HttpStatusCode.OK, categories)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/products") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val perPage = call.request.queryParameters["perPage"]?.toIntOrNull()
            val language = call.request.queryParameters["language"] ?: "us"

            val searchText = call.request.queryParameters["searchText"]?.decode()

            val products = when {
                searchText != null -> {
                    transaction {
                        Products
                            .select { (Products.name like "%$searchText%") or (Products.nameUA like "%$searchText%") }
                            .toList()
                            .selectPage(page, perPage)
                            .map {
                                Product(
                                    id = it[Products.id],
                                    name = if (language == "uk") it[Products.nameUA] else it[Products.name]
                                )
                            }
                    }
                }
                else -> {
                    transaction {
                        Products
                            .selectAll()
                            .toList()
                            .selectPage(page, perPage)
                            .map {
                                Product(
                                    id = it[Products.id],
                                    name = if (language == "uk") it[Products.nameUA] else it[Products.name]
                                )
                            }
                    }
                }
            }

            if (products.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No products found.")
                return@get
            }

            call.respond(HttpStatusCode.OK, products)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}