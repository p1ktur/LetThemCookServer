package features.user

import features.auth.TokenManager.checkAccessToken
import utils.hash
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.Followings
import models.database.Recipes
import models.database.User
import models.database.User.Companion.asUserData
import models.database.Users
import models.server.RequestPair
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import utils.decode
import utils.selectPage

fun Routing.addUserRoutes() {
    get("/users") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val perPage = call.request.queryParameters["perPage"]?.toIntOrNull()

            val searchText = call.request.queryParameters["searchText"]?.decode() ?: ""

            val users = transaction {
                Users
                    .select { (Users.login like "%$searchText%") and (Users.id neq accessToken.userId) }
                    .toList()
                    .selectPage(page, perPage)
                    .map { it.asUserData() }
            }

            if (users.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No users found.")
                return@get
            }

            call.respond(HttpStatusCode.OK, users)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/user") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@get
            }

            val requestUserId = accessToken.userId

            val user = transaction {
                Users
                    .select { Users.id eq userId }
                    .singleOrNull()
                    ?.asUserData()
            }

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "No such user exists.")
                return@get
            }

            val totalPreparations = transaction {
                Recipes
                    .select { Recipes.ownerId eq userId }
                    .sumOf { it[Recipes.preparationsAmount] }
            }

            val averageRating = transaction {
                val likes = Recipes.likesAmount.sum()
                val dislikes = Recipes.dislikesAmount.sum()

                val result = Recipes
                    .slice(likes, dislikes)
                    .select { Recipes.ownerId eq userId }
                    .singleOrNull()

                val totalLikes = result?.get(likes) ?: 0
                val totalDislikes = result?.get(dislikes) ?: 0

                val ratio = if (totalDislikes == 0) 0.0 else totalLikes.toDouble() / totalDislikes

                val rowCount = Recipes
                    .select { Recipes.ownerId eq userId }
                    .count()

                if (rowCount > 0) ratio.div(rowCount) else 0.0
            }

            val isFollowed = transaction {
                Followings
                    .select { (Followings.followerId eq requestUserId) and (Followings.followedId eq userId) }
                    .singleOrNull()
            }

            user.totalPreparations = totalPreparations
            user.averageRating = averageRating.toFloat()
            user.isFollowed = isFollowed != null

            call.respond(HttpStatusCode.OK, user)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    put<User>("/user") { data ->
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@put
            }

            if (accessToken.userId != data.id) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@put
            }

            transaction {
                Users.update(
                    where = {
                        Users.id eq data.id
                    },
                    body = {
                        it[login] = data.login
                        it[email] = data.email
                        it[phone] = data.phone
                        it[name] = data.name
                        it[surname] = data.surname
                        it[about] = data.about
                        it[birthDate] = data.birthDate
                        it[profileBitmapId] = data.profileBitmapId
                        it[averageRating] = data.averageRating
                        it[totalRecipes] = data.totalRecipes
                        it[totalPreparations] = data.totalPreparations
                        it[totalFollowers] = data.totalFollowers
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/follow") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            transaction {
                Followings.insert {
                    it[followerId] = accessToken.userId
                    it[followedId] = userId
                }

                Users.update(
                    where = {
                        Users.id eq userId
                    },
                    body = {
                        it.update(totalFollowers, totalFollowers + 1)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/unfollow") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            transaction {
                Followings.deleteWhere {
                    (followerId eq accessToken.userId) and (followedId eq userId)
                }

                Users.update(
                    where = {
                        Users.id eq userId
                    },
                    body = {
                        it.update(totalFollowers, totalFollowers - 1)
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    put<RequestPair>("/update_password") { data ->
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@put
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@put
            }

            if (accessToken.userId != userId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@put
            }

            val oldPassword = transaction {
                Users
                    .select { Users.id eq userId }
                    .singleOrNull()
            }?.run {
                this[Users.passwordHash]
            }

            println(oldPassword)

            if (oldPassword != data.first.hash()) {
                call.respond(HttpStatusCode.Forbidden, "Old password is not correct.")
                return@put
            }

            transaction {
                Users.update(
                    where = {
                        Users.id eq userId
                    },
                    body = {
                        it[passwordHash] = data.second.hash()
                    }
                )
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}