package server

import features.auth.addAuthRoutes
import features.file.addFileRoutes
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
    }
}