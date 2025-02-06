import database.DatabaseFactory
import server.startServer

// TODO check user, recipe, file ids for uniqueness upon creating new entries
fun main() {
    DatabaseFactory.createAndConnect()

    startServer()
}