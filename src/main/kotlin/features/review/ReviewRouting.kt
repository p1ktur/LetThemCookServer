package features.review

import features.auth.TokenManager.checkAccessToken
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.Recipes
import models.database.Review
import models.database.Reviews
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import utils.selectPage

fun Routing.addReviewRoutes() {
    get("/reviews") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val perPage = call.request.queryParameters["perPage"]?.toIntOrNull()

            val recipeId = call.request.queryParameters["id"]

            if (recipeId == null) {
                call.respond(HttpStatusCode.BadRequest, "No Recipe Id provided.")
                return@get
            }

            val reviews = Reviews
                .select { Reviews.recipeId eq recipeId }
                .toList()
                .selectPage(page, perPage)
                .map {
                    Review(
                        id = it[Reviews.id],
                        authorId = it[Reviews.authorId],
                        recipeId = it[Reviews.recipeId],
                        reviewText = it[Reviews.reviewText],
                        likesAmount = it[Reviews.likesAmount]
                    )
                }

            if (reviews.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No reviews found.")
                return@get
            }

            call.respond(HttpStatusCode.OK, reviews)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post<Review>("/review") { data ->
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            if (accessToken.userId != data.authorId) {
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
                    Reviews.insert {
                        it[id] = data.id
                        it[authorId] = data.authorId
                        it[recipeId] = data.recipeId
                        it[reviewText] = data.reviewText
                        it[likesAmount] = data.likesAmount
                    }
                }
            } else {
                transaction {
                    Reviews.update(
                        where = {
                            Reviews.id eq data.id
                        },
                        body = {
                            it[reviewText] = data.reviewText
                        }
                    )
                }
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/review_like") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val reviewId = call.request.queryParameters["reviewId"]

            if (reviewId == null) {
                call.respond(HttpStatusCode.BadRequest, "No Review Id provided.")
                return@post
            }

            val liked = call.request.queryParameters["liked"]?.toBooleanStrictOrNull()

            if (liked == null) {
                call.respond(HttpStatusCode.BadRequest, "No Like Status provided.")
                return@post
            }

            transaction {
                Reviews.update(
                    where = {
                        Reviews.id eq reviewId
                    },
                    body = {
                        it.update(likesAmount, likesAmount + if (liked) 1 else -1)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}