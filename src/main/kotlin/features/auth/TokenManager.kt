package features.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import models.database.tokens.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import utils.toLocal
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

object TokenManager {

    private const val SECRET_KEY = "LTW77pQchduMVecINRAJL6jXNCj1FZ7XJorZAbXzopTxnn4uMUYpeec7jZkKccQ1"

    // Checking

    suspend fun RoutingContext.checkAccessToken(): AccessToken? {
        val accessTokenString = getAccessTokenFromHeader(call.request.headers)
        val accessToken = accessTokenString?.let { parseToken(it) } as? AccessToken

        if (accessToken == null) {
            call.respond(HttpStatusCode.BadRequest, "No access token provided.")
            return null
        }

        val existingAccessToken = transaction {
            AccessTokens.select {
                (AccessTokens.userId eq accessToken.userId) and (AccessTokens.token eq accessToken.token)
            }.singleOrNull()
        }

        if (existingAccessToken == null) return null
        if (accessToken.isExpired()) return null

        return accessToken
    }

    suspend fun RoutingContext.checkRefreshToken(): RefreshToken? {
        val refreshTokenString = getRefreshTokenFromHeader(call.request.headers)
        val refreshToken = refreshTokenString?.let { parseToken(it) } as? RefreshToken

        if (refreshToken == null) {
            call.respond(HttpStatusCode.BadRequest, "No refresh token provided.")
            return null
        }

        val existingRefreshToken = transaction {
            RefreshTokens.select {
                (RefreshTokens.userId eq refreshToken.userId) and (RefreshTokens.token eq refreshToken.token)
            }.singleOrNull()
        }

        if (existingRefreshToken == null) return null
        if (refreshToken.isExpired()) return null

        return refreshToken
    }

    // Getting

    private fun getAccessTokenFromHeader(headers: Headers): String? {
        val authHeader = headers["Authorization"] ?: return null
        val token = authHeader.split(" ").getOrNull(1)

        return token
    }

    private fun getRefreshTokenFromHeader(headers: Headers): String? {
        val authHeader = headers["Refresh-Token"] ?: return null
        val token = authHeader.split(" ").getOrNull(1)

        return token
    }

    @OptIn(InternalAPI::class)
    fun getNewAccessToken(userId: String): AccessToken {
        val algorithm = Algorithm.HMAC512(SECRET_KEY)

        val now = Date()
        val nowPlusDay = Date(now.time + 86_400_000L)

        val accessToken = JWT.create()
            .withSubject(userId)
            .withHeader(mapOf("Type" to "Access"))
            .withIssuedAt(now)
            .withExpiresAt(nowPlusDay)
            .sign(algorithm)

        val accessTokenObject = AccessToken(
            userId = userId,
            token = accessToken,
            expireDate = nowPlusDay.toLocalDateTime()
        )

        return accessTokenObject
    }

    @OptIn(InternalAPI::class)
    fun getNewRefreshToken(userId: String): RefreshToken {
        val algorithm = Algorithm.HMAC512(SECRET_KEY)

        val now = Date()
        val nowPlusMonth = Date(now.time + 2_592_000_000L)

        val refreshToken = JWT.create()
            .withSubject(userId)
            .withHeader(mapOf("Type" to "Refresh"))
            .withIssuedAt(now)
            .withExpiresAt(nowPlusMonth)
            .sign(algorithm)

        val refreshTokenObject = RefreshToken(
            userId = userId,
            token = refreshToken,
            expireDate = nowPlusMonth.toLocalDateTime()
        )

        return refreshTokenObject
    }

    fun getNewTokens(userId: String): Pair<AccessToken, RefreshToken> {
        return getNewAccessToken(userId) to getNewRefreshToken(userId)
    }

    // Finding

    fun findAccessToken(userId: String): AccessToken? {
        val existingToken = transaction {
            AccessTokens.select {
                AccessTokens.userId eq userId
            }.singleOrNull()
        }?.run {
            AccessToken(
                id = this[AccessTokens.id],
                userId = this[AccessTokens.userId],
                token = this[AccessTokens.token],
                expireDate = this[AccessTokens.expireDate]
            )
        }

        return existingToken
    }

    fun findRefreshToken(userId: String): RefreshToken? {
        val existingToken = transaction {
            RefreshTokens.select {
                RefreshTokens.userId eq userId
            }.singleOrNull()
        }?.run {
            RefreshToken(
                id = this[RefreshTokens.id],
                userId = this[RefreshTokens.userId],
                token = this[RefreshTokens.token],
                expireDate = this[RefreshTokens.expireDate]
            )
        }

        return existingToken
    }

    // Parsing

    @OptIn(InternalAPI::class)
    private fun parseToken(token: String): Token? {
        return try {
            val algorithm = Algorithm.HMAC512(SECRET_KEY)
            val verifier = JWT.require(algorithm).build()
            val decodedJWT = verifier.verify(token)

            val type = decodedJWT.getHeaderClaim("Type")?.toString()?.replace("\"", "")

            when (type) {
                "Access" -> {
                    AccessToken(
                        userId = decodedJWT.subject,
                        token = token,
                        expireDate = decodedJWT.expiresAt.toLocalDateTime()
                    )
                }
                "Refresh" -> {
                    RefreshToken(
                        userId = decodedJWT.subject,
                        token = token,
                        expireDate = decodedJWT.expiresAt.toLocalDateTime()
                    )
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}