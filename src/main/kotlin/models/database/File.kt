package models.database

import models.enums.FileType
import org.jetbrains.exposed.sql.Table

data class File(
    val id: String,
    val type: FileType,
    val fileOid: Int
)

object Files : Table("files") {
    val id = varchar("id", 255).entityId()
    val type = enumeration("type", FileType::class)
    val fileOid = integer("fileOid")
}