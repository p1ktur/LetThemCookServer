package models.database.tokens

import java.time.LocalDateTime

interface Token {
    val id: Int
    val userId: String
    val token: String
    val expireDate: LocalDateTime
}