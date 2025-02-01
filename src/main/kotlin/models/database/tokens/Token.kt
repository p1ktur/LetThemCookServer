package models.database.tokens

import java.time.LocalDate

interface Token {
    val id: Int
    val userId: String
    val token: String
    val expireDate: LocalDate
}

fun Token.isExpired(): Boolean {
    return expireDate.isBefore(LocalDate.now())
}