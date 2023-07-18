package web

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import simpleJson

fun Application.configureRoutes() {
    // install plugins
    install(ContentNegotiation) {
        json(simpleJson)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/auth") {
            val bearerToken = Clients.authenticate(call.request.authorization())
            if (bearerToken == null)
                call.respond(HttpStatusCode.Unauthorized)
            else
                call.respondMessage(AuthResponse(bearerToken))
        }
    }
}

/**
 * Sends a [Message] in response.
 *
 * @param message is the message to send.
 */
private suspend fun ApplicationCall.respondMessage(message: Message) = this.respond(message)
