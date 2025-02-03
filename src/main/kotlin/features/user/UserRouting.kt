package features.user

import features.auth.TokenManager.checkAccessToken
import features.auth.hash
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.User
import models.database.User.Companion.asUser
import models.database.Users
import models.server.RequestPair
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Routing.addUserRoutes() {
    get("/user") {
        try {
            if (checkAccessToken() == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val userId = call.request.queryParameters["id"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@get
            }

            val user = transaction {
                Users.select {
                    Users.id eq userId
                }.singleOrNull()
            }?.asUser()

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "No such user exists.")
                return@get
            }

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
                        it[profilePictureId] = data.profilePictureId
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

    put<RequestPair>("/update_password") { data ->
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@put
            }

            val userId = call.request.queryParameters["id"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@put
            }

            if (accessToken.userId != userId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@put
            }

            val oldPassword = transaction {
                Users.select {
                    Users.id eq userId
                }.singleOrNull()
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