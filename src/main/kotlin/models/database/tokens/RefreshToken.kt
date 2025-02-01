package models.database.tokens

import kotlinx.serialization.Serializable
import models.database.Users
import models.serializers.LocalDateSerializer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

@Serializable
data class RefreshToken(
    override val id: Int = 0,
    override val userId: String,
    override val token: String,
    @Serializable(with = LocalDateSerializer::class) override val expireDate: LocalDate
) : Token

object RefreshTokens : Table("refresh_tokens") {
    val id = integer("id").autoIncrement()
    val userId = varchar("userId", 36).references(Users.id)
    val token = varchar("token", 255)
    val expireDate = date("expireDate")
}