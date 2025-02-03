package features.file

import features.auth.smallHash
import models.database.Files
import models.database.Files.type
import models.enums.FileType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File

object FileManager {

    private const val FILE_ROOT_PATH = "C:\\LetThemCookFiles"

    private val fileExtensions = mapOf(
        FileType.IMAGE to ".jpg",
        FileType.VIDEO to ".mp4"
    )

    fun saveFile(userId: String, fileId: String, type: FileType, bytes: ByteArray, fileClass: FileClass): Boolean {
        return try {
            val dirPath = getDirPath(userId, fileClass)
            val directory = File(dirPath)

            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) return false
            }

            val extension = fileExtensions[type] ?: ".txt"
            val file = File(dirPath, "${fileId}${extension}")

            file.writeBytes(bytes)

            val existingFile = transaction {
                Files.select {
                    Files.id eq fileId
                }.singleOrNull()
            }

            if (existingFile != null) {
                transaction {
                    Files.update(
                        where = {
                            Files.id eq fileId
                        },
                        body = {
                            it[Files.type] = type.value
                        }
                    )
                }
            } else {
                transaction {
                    Files.insert {
                        it[id] = fileId
                        it[Files.type] = type.value
                    }
                }
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    fun getFile(userId: String, fileId: String, type: FileType, fileClass: FileClass): File? {
        return try {
            val existingFile = transaction {
                Files.select {
                    Files.id eq fileId
                }.singleOrNull()
            }

            if (existingFile == null) return null

            val dirPath = getDirPath(userId, fileClass)
            val directory = File(dirPath)

            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) return null
            }

            val extension = fileExtensions[type] ?: ".txt"
            val file = File(dirPath, "${fileId}${extension}")

            file
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFile(userId: String, fileId: String, type: FileType, fileClass: FileClass): Boolean {
        return try {
            val dirPath = getDirPath(userId, fileClass)
            val directory = File(dirPath)

            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) return false
            }

            val extension = fileExtensions[type] ?: ".txt"
            val file = File(dirPath, "${fileId}${extension}")

            if (file.delete()) {
                transaction {
                    Files.deleteWhere {
                        id eq fileId
                    }
                }

                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getDirPath(userId: String, fileClass: FileClass): String {
        var dirPath = FILE_ROOT_PATH + "\\user_${userId.smallHash()}"
        when (fileClass) {
            FileClass.Profile -> dirPath += "\\profile"
            is FileClass.Recipe -> dirPath += "\\recipe_${fileClass.recipeId.smallHash()}\\profile"
            is FileClass.RecipeAttachment -> "\\recipe_${fileClass.recipeId.smallHash()}\\attachments"
            is FileClass.Block -> "\\recipe_${fileClass.recipeId.smallHash()}\\block_${fileClass.blockId.smallHash()}"
        }

        return dirPath
    }
}