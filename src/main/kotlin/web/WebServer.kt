package web

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Starts the Ktor web server listening on the specified [host] and [port].
 */
fun startWebServer(port: Int, host: String) {
    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}


private fun Application.module() {
    configureWebSocket()
}
