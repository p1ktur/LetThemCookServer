package models.database

import kotlinx.serialization.Serializable
import models.serializers.LocalDateSerializer
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

@Serializable
data class User(
    val id: String,
    val login: String,
    val email: String,
    val phone: String? = null,
    val about: String? = null,
    @Serializable(with = LocalDateSerializer::class) val birthDate: LocalDate? = null,
    val profilePictureId: String? = null,
    val totalRecipes: Int = 0,
    val totalPreparations: Int = 0,
    val totalFollowers: Int = 0
) {
    companion object {
        fun ResultRow.asUser(): User {
            return User(
                id = this[Users.id],
                login = this[Users.login],
                email = this[Users.email]
            )
        }
    }
}

object Users : Table("users") {
    val id = varchar("id", 36)
    val login = varchar("login", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 255).nullable()
    val passwordHash = varchar("passwordHash", 255)
    val about = varchar("about", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val profilePictureId = varchar("profilePictureId", 255).references(Files.id).nullable()
    val totalRecipes = integer("totalRecipes").default(0)
    val totalPreparations = integer("totalPreparations").default(0)
    val totalFollowers = integer("totalFollowers").default(0)
}