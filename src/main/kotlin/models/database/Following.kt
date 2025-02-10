package models.database

import org.jetbrains.exposed.sql.Table

object Followings : Table("followings") {
    val id = integer("id").autoIncrement()
    val followerId = varchar("followerId", 36)
    val followedId = varchar("followedId", 36)
}