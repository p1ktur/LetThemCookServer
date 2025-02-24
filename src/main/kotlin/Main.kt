import database.DatabaseFactory
import server.startServer

fun main() {
    DatabaseFactory.createAndConnect()

    startServer()
}