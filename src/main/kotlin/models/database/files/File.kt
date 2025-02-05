package models.database.files

import models.enums.FileType
import org.jetbrains.exposed.sql.Table

data class File(
    val id: String,
    val type: FileType
)

object Files : Table("files") {
    val id = varchar("id", 36)
    val type = varchar("type", 255)
}