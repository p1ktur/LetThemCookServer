package features.file

import features.auth.TokenManager.checkAccessToken
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import models.enums.FileType

fun Routing.addFileRoutes() {
    get("/file") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@get
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@get
            }

            val fileId = call.request.queryParameters["fileId"]

            if (fileId == null) {
                call.respond(HttpStatusCode.BadRequest, "No File ID provided.")
                return@get
            }

            val fileType = call.request.queryParameters["fileType"]

            val type = when (fileType) {
                FileType.IMAGE.value -> FileType.IMAGE
                FileType.VIDEO.value -> FileType.VIDEO
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Wrong or no File Type.")
                    return@get
                }
            }

            val recipeId = call.request.queryParameters["recipeId"]
            val blockId = call.request.queryParameters["blockId"]
            val isAttachment = call.request.queryParameters["isAttachment"]?.toBoolean() ?: false

            val fileClass = when {
                recipeId != null && blockId != null -> FileClass.Block(recipeId, blockId)
                recipeId != null && isAttachment -> FileClass.RecipeAttachment(recipeId)
                recipeId != null -> FileClass.Recipe(recipeId)
                else -> FileClass.Profile
            }

            val file = FileManager.getFile(userId, fileId, type, fileClass)

            if (file?.exists() == true) {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileId).toString()
                )

                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found.")
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/file") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@post
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@post
            }

            val fileId = call.request.queryParameters["fileId"]

            if (fileId == null) {
                call.respond(HttpStatusCode.BadRequest, "No File ID provided.")
                return@post
            }

            val fileType = call.request.queryParameters["fileType"]

            val type = when (fileType) {
                FileType.IMAGE.value -> FileType.IMAGE
                FileType.VIDEO.value -> FileType.VIDEO
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Wrong or no File Type.")
                    return@post
                }
            }

            if (accessToken.userId != userId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@post
            }

            val recipeId = call.request.queryParameters["recipeId"]
            val blockId = call.request.queryParameters["blockId"]
            val isAttachment = call.request.queryParameters["isAttachment"]?.toBoolean() ?: false

            val fileClass = when {
                   recipeId != null && blockId != null -> FileClass.Block(recipeId, blockId)
                   recipeId != null && isAttachment -> FileClass.RecipeAttachment(recipeId)
                   recipeId != null -> FileClass.Recipe(recipeId)
                   else -> FileClass.Profile
            }

            val multipart = call.receiveMultipart()

            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "file") {
                    val fileBytes = part.provider.invoke().toByteArray()
                    val fileMaxSize = when (type) {
                        FileType.IMAGE -> 10 * 1024 * 1024
                        FileType.VIDEO -> 20 * 1024 * 1024
                    }

                    if (fileBytes.size > fileMaxSize) {
                        call.respond(HttpStatusCode.NotAcceptable, "File size must be under 5 MB.")
                        return@forEachPart
                    }

                    val saveResult = FileManager.saveFile(userId, fileId, type, fileBytes, fileClass)

                    if (!saveResult) {
                        call.respond(HttpStatusCode.InternalServerError)
                        return@forEachPart
                    }

                    call.respond(HttpStatusCode.OK)
                    return@forEachPart
                }
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    delete("/file") {
        try {
            val accessToken = checkAccessToken()

            if (accessToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Access token expired or absent.")
                return@delete
            }

            val userId = call.request.queryParameters["userId"]

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "No User ID provided.")
                return@delete
            }

            val fileId = call.request.queryParameters["fileId"]

            if (fileId == null) {
                call.respond(HttpStatusCode.BadRequest, "No File ID provided.")
                return@delete
            }

            val fileType = call.request.queryParameters["fileType"]

            val type = when (fileType) {
                FileType.IMAGE.value -> FileType.IMAGE
                FileType.VIDEO.value -> FileType.VIDEO
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Wrong or no File Type.")
                    return@delete
                }
            }

            if (accessToken.userId != userId) {
                call.respond(HttpStatusCode.Unauthorized, "Wrong access token.")
                return@delete
            }

            val recipeId = call.request.queryParameters["recipeId"]
            val blockId = call.request.queryParameters["blockId"]
            val isAttachment = call.request.queryParameters["isAttachment"]?.toBoolean() ?: false

            val fileClass = when {
                recipeId != null && blockId != null -> FileClass.Block(recipeId, blockId)
                recipeId != null && isAttachment -> FileClass.RecipeAttachment(recipeId)
                recipeId != null -> FileClass.Recipe(recipeId)
                else -> FileClass.Profile
            }

            val deleteResult = FileManager.deleteFile(userId, fileId, type, fileClass)

            if (!deleteResult) {
                call.respond(HttpStatusCode.InternalServerError)
                return@delete
            }

            call.respond(HttpStatusCode.OK)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}