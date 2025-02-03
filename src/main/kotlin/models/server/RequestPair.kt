package models.server

import kotlinx.serialization.Serializable

@Serializable
data class RequestPair(
    val first: String,
    val second: String
)
