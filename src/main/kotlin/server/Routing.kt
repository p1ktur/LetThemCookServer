package server

import features.auth.addAuthRoutes
import features.file.addFileRoutes
import features.recipe.addRecipeRoutes
import features.recipe.addRecipeSecondaryRoutes
import features.review.addReviewRoutes
import features.user.addUserRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        addAuthRoutes()
        addUserRoutes()
        addFileRoutes()
        addRecipeRoutes()
        addRecipeSecondaryRoutes()
        addReviewRoutes()
    }
}