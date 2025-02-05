package models.database

import kotlinx.serialization.Serializable
import models.database.Users.averageRating
import models.database.Users.birthDate
import models.database.Users.phone
import models.database.Users.surname
import models.database.Users.totalFollowers
import models.database.Users.totalPreparations
import models.database.files.Files
import models.serializers.LocalDateTimeSerializer
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

@Serializable
data class User(
    val id: String,
    val login: String,
    val email: String,
    val phone: String? = null,
    val name: String? = null,
    val surname: String? = null,
    val about: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val birthDate: LocalDateTime? = null,
    val profileBitmapId: String? = null,
    val averageRating: Float = 0f, // TODO count rating before returning it in requests
    val totalRecipes: Int = 0,
    val totalPreparations: Int = 0,
    val totalFollowers: Int = 0,
    var isFollowed: Boolean = false
) {
    companion object {
        fun ResultRow.asUserData(): User {
            return User(
                id = this[Users.id],
                login = this[Users.login],
                email = this[Users.email],
                phone = this[phone],
                name = this[Users.name],
                surname = this[surname],
                about = this[Users.about],
                birthDate = this[birthDate],
                profileBitmapId = this[Users.profileBitmapId],
                averageRating = this[averageRating],
                totalRecipes = this[Users.totalRecipes],
                totalPreparations = this[totalPreparations],
                totalFollowers = this[totalFollowers],
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
    val name = varchar("name", 255).nullable()
    val surname = varchar("surname", 255).nullable()
    val about = varchar("about", 255).nullable()
    val birthDate = datetime("birthDate").nullable()
    val profileBitmapId = varchar("profilePictureId", 255).references(Files.id).nullable()
    val averageRating = float("averageRating").default(0f)
    val totalRecipes = integer("totalRecipes").default(0)
    val totalPreparations = integer("totalPreparations").default(0)
    val totalFollowers = integer("totalFollowers").default(0)
}