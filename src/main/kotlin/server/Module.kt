package server

import io.ktor.server.application.*

fun Application.module() {
    configureRouting()
    configureContentNegotiation()
}