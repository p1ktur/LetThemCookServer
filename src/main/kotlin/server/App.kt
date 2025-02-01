package server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun startServer() {
    val server = embeddedServer(
        factory = Netty,
        port = 8080,
        host = "127.0.0.1",
        module = Application::module
    )

    server.start(wait = true)
}