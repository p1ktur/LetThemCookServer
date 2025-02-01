package models.server.auth

import kotlinx.serialization.Serializable
import models.database.User

@Serializable
data class TokenResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)