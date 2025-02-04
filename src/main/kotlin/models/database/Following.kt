package models.database

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

data class Following(
    val id: Int,
    val followerId: String,
    val followedId: String
) {
    companion object {
        fun ResultRow.asFollowingData(): Following {
            return Following(
                id = this[Followings.id],
                followerId = this[Followings.followerId],
                followedId = this[Followings.followedId]
            )
        }
    }
}

object Followings : Table("followings") {
    val id = integer("id").autoIncrement()
    val followerId = varchar("followerId", 36)
    val followedId = varchar("followedId", 36)
}