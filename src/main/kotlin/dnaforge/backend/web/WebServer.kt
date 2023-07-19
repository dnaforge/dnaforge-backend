package dnaforge.backend.web

import dnaforge.backend.Environment
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Starts the Ktor web server on the [Environment.host] and [Environment.port].
 */
fun startWebServer() {
    embeddedServer(Netty, port = Environment.port, host = Environment.host, module = Application::module)
        .start(wait = true)
}

/**
 * Registers modules on a Ktor web server.
 */
fun Application.module() {
    configureRoutes()
    configureWebSocket()
}
