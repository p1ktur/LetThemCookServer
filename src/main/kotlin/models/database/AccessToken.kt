package models.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

data class AccessToken(
    val id: Int,
    val userId: String,
    val token: String,
    val expireDate: LocalDate
)

object AccessTokens : Table("access_tokens") {
    val id = integer("id").entityId().autoIncrement()
    val userId = varchar("userId", 255).references(Users.id)
    val token = varchar("token", 255)
    val expireDate = date("expireDate")
}