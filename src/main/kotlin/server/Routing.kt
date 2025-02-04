package server

import features.auth.addAuthRoutes
import features.file.addFileRoutes
import features.recipe.addRecipeRoutes
import features.recipe.addRecipeSecondaryRoutes
import features.user.addUserRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.NonCancellable.children

fun Application.configureRouting() {
    routing {
        addAuthRoutes()
        addUserRoutes()
        addFileRoutes()
        addRecipeRoutes()
        addRecipeSecondaryRoutes()
    }
}