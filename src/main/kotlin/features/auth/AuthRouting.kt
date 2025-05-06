package features.auth

import features.auth.TokenManager.checkAccessToken
import features.auth.TokenManager.checkRefreshToken
import features.auth.TokenManager.findAccessToken
import features.auth.TokenManager.findRefreshToken
import features.auth.TokenManager.getNewAccessToken
import features.auth.TokenManager.getNewRefreshToken
import features.auth.TokenManager.getNewTokens
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.database.User
import models.database.User.Companion.asUserData
import models.database.Users
import models.database.tokens.*
import models.server.auth.LoginData
import models.server.auth.RegistrationData
import models.server.auth.TokenResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Routing.addAuthRoutes() {
    post<RegistrationData>("/register") { data ->
        try {
            data.hashPassword()

            val existingUser = transaction {
                Users
                    .select { (Users.login eq data.login) or (Users.email eq data.email) }
                    .singleOrNull()
            }

            if (existingUser == null) {
                val newUserId = UUID.randomUUID().toString()
                val newUser = User(
                    id = newUserId,
                    login = data.login,
                    email = data.email
                )

                transaction {
                    Users.insert {
                        it[id] = newUser.id
                        it[login] = newUser.login
                        it[email] = newUser.email
                        it[passwordHash] = data.password
                    }
                }

                val (accessToken, refreshToken) = getNewTokens(newUserId)

                transaction {
                    AccessTokens.insert {
                        it[userId] = newUserId
                        it[token] = accessToken.token
                        it[expireDate] = accessToken.expireDate
                    }

                    RefreshTokens.insert {
                        it[userId] = newUserId
                        it[token] = refreshToken.token
                        it[expireDate] = refreshToken.expireDate
                    }
                }

                val response = TokenResponse(newUser, accessToken.token, refreshToken.token)

                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Conflict, "User already exists.")
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post<LoginData>("/login") { data ->
        try {
            val existingUser = when {
                data.login != null -> {
                    transaction {
                        Users
                            .select { Users.login eq data.login }
                            .singleOrNull()
                    }
                }
                data.email != null -> {
                    transaction {
                        Users
                            .select { Users.email eq data.email }
                            .singleOrNull()
                    }
                }
                data.phoneNumber != null -> {
                    transaction {
                        Users
                            .select { Users.phone eq data.phoneNumber }
                            .singleOrNull()
                    }
                }
                else -> {
                    call.respond(HttpStatusCode.Conflict, "User does not exist.")
                    return@post
                }
            }?.run {
                data.hashPassword()
                if (data.password != this[Users.passwordHash]) {
                    call.respond(HttpStatusCode.BadRequest, "Incorred password.")
                    return@post
                }

                asUserData()
            }

            if (existingUser != null) {
                val accessToken = findAccessToken(existingUser.id)

                val newAccessToken = if (accessToken == null) {
                    val newToken = getNewAccessToken(existingUser.id)

                    transaction {
                        AccessTokens.update(
                            where = {
                                AccessTokens.userId eq newToken.userId
                            },
                            body = {
                                it[token] = newToken.token
                                it[expireDate] = newToken.expireDate
                            }
                        )
                    }

                    newToken
                } else {
                    accessToken
                }

                val refreshToken = findRefreshToken(existingUser.id)

                val newRefreshToken = if (refreshToken == null) {
                    val newToken = getNewRefreshToken(existingUser.id)

                    transaction {
                        RefreshTokens.update(
                            where = {
                                RefreshTokens.userId eq newToken.userId
                            },
                            body = {
                                it[token] = newToken.token
                                it[expireDate] = newToken.expireDate
                            }
                        )
                    }

                    newToken
                } else {
                    refreshToken
                }

                val response = TokenResponse(existingUser, newAccessToken.token, newRefreshToken.token)

                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Conflict, "User does not exist.")
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/check_at") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Gone)
                return@get
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/check_rt") {
        try {
            val refreshToken = checkRefreshToken()

            if (refreshToken == null) {
                call.respond(HttpStatusCode.Gone)
                return@get
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/refresh_tokens") {
        try {
            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No user id provided.")
                return@get
            }

            val accessToken = checkAccessToken(true)
            val refreshToken = checkRefreshToken()

            val existingUser = transaction {
                Users
                    .select { Users.id eq userId }
                    .singleOrNull()
                    ?.asUserData()
            }

            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound, "User does not exist.")
                return@get
            }

            if (accessToken == null) {
                val newToken = getNewAccessToken(userId)

                transaction {
                    AccessTokens.update(
                        where = {
                            AccessTokens.userId eq newToken.userId
                        },
                        body = {
                            it[token] = newToken.token
                            it[expireDate] = newToken.expireDate
                        }
                    )
                }
            }

            if (refreshToken == null) {
                val newToken = getNewRefreshToken(userId)

                transaction {
                    RefreshTokens.update(
                        where = {
                            RefreshTokens.userId eq newToken.userId
                        },
                        body = {
                            it[token] = newToken.token
                            it[expireDate] = newToken.expireDate
                        }
                    )
                }
            }

            findAccessToken(userId)?.let { at ->
                findRefreshToken(userId)?.let { rt ->
                    val response = TokenResponse(existingUser, at.token, rt.token)

                    call.respond(HttpStatusCode.OK, response)
                    return@get
                }
            }

            call.respond(HttpStatusCode.InternalServerError)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}