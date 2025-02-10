package models.database.files

import org.jetbrains.exposed.sql.Table

object Files : Table("files") {
    val id = varchar("id", 36)
    val type = varchar("type", 255)
}