package models.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

data class User(
    val id: String,
    val login: String,
    val email: String,
    val phone: String?,
    val passwordHash: String,
    val about: String?,
    val birthDate: LocalDate?,
    val profilePictureId: String?,
    val totalRecipes: Int = 0,
    val totalPreparations: Int = 0,
    val totalFollowers: Int = 0
)

object Users : Table("recipes") {
    val id = varchar("id", 36).entityId()
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