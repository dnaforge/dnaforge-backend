package web

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import simpleJson
import java.time.Duration

/**
 * Adds a WebSocket to the web server.
 */
fun Application.configureWebSocket() {
    // install plugin
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
    }

    routing {
        webSocket("/") {
            // handle connect
            var client: Client? = null

            for (frame in incoming) {
                // handling frames that aren't text frames
                if (frame is Frame.Close) break
                if (frame is Frame.Ping) outgoing.send(Frame.Pong(frame.buffer))
                if (frame !is Frame.Text) continue

                val message = try {
                    frame.toMessage()
                } catch (e: Throwable) {
                    continue // ignore invalid messages
                }

                // the client should only send authentication messages over the WebSocket
                // the REST API handles everything else
                if (client == null && message is WebSocketAuth) {
                    client = Clients.authenticate(message.bearerToken, this)

                    val response: WebSocketMessage = WebSocketAuthResponse(client != null)
                    send(Frame.Text(simpleJson.encodeToString(response)))
                }
            }

            // handle disconnect
            client?.let { Clients.disconnect(it) }
        }
    }
}

/**
 * Tries to decode the text in this [Frame.Text] into a [WebSocketMessage].
 *
 * @return a new [WebSocketMessage] instance
 */
fun Frame.Text.toMessage(): WebSocketMessage = simpleJson.decodeFromString(readText())
