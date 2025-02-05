package models.database.tokens

import kotlinx.serialization.Serializable
import models.database.Users
import models.serializers.LocalDateTimeSerializer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

@Serializable
data class AccessToken(
    override val id: Int = 0,
    override val userId: String,
    override val token: String,
    @Serializable(with = LocalDateTimeSerializer::class) override val expireDate: LocalDateTime
) : Token

object AccessTokens : Table("access_tokens") {
    val id = integer("id").autoIncrement()
    val userId = varchar("userId", 36).references(Users.id)
    val token = varchar("token", 255)
    val expireDate = datetime("expireDate")
}