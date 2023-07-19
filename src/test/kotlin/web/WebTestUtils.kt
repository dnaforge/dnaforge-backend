package web

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import simpleJson

/**
 * Prepares the application and creates a client with the required plugins.
 *
 * @return a [HttpClient] for testing.
 */
fun ApplicationTestBuilder.prepare(): HttpClient {
    application {
        module()
    }

    return createClient {
        install(ContentNegotiation) {
            json(simpleJson)
        }
        install(WebSockets)
    }
}

/**
 * Sends a [WebSocketMessage].
 *
 * @param message the [WebSocketMessage] to send.
 */
suspend fun ClientWebSocketSession.sendMessage(message: WebSocketMessage) =
    send(Frame.Text(simpleJson.encodeToString(message)))
