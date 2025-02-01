package server

import configureRouting
import io.ktor.server.application.*

fun Application.module() {
    configureRouting()
}