package features.file

import models.database.files.Files
import models.database.files.RecipeFiles
import models.enums.FileType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import utils.smallHash
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
                Files
                    .select { Files.id eq fileId }
                    .singleOrNull()
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

                if (fileClass is FileClass.RecipeAttachment) {
                    transaction {
                        RecipeFiles.insert {
                            it[RecipeFiles.fileId] = fileId
                            it[recipeId] = fileClass.recipeId
                        }
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
                Files
                    .select { Files.id eq fileId }
                    .singleOrNull()
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

                if (fileClass is FileClass.RecipeAttachment) {
                    transaction {
                        RecipeFiles.deleteWhere {
                            RecipeFiles.fileId eq fileId
                        }
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

    fun deleteRecipeFiles(userId: String, recipeId: String): Boolean {
        return try {
            val dirPath = getRecipePath(userId, recipeId)
            val directory = File(dirPath)

            directory.deleteRecursively()

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getDirPath(userId: String, fileClass: FileClass): String {
        return FILE_ROOT_PATH + "\\user_${userId.smallHash()}" + when (fileClass) {
            FileClass.Profile -> "\\profile"
            is FileClass.Recipe -> "\\recipe_${fileClass.recipeId.smallHash()}\\profile"
            is FileClass.RecipeAttachment -> "\\recipe_${fileClass.recipeId.smallHash()}\\attachments"
            is FileClass.Block -> "\\recipe_${fileClass.recipeId.smallHash()}\\block_${fileClass.blockId.smallHash()}"
        }
    }

    private fun getRecipePath(userId: String, recipeId: String): String {
        return FILE_ROOT_PATH + "\\user_${userId.smallHash()}\\recipe_${recipeId.smallHash()}"
    }
}