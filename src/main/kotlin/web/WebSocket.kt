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

                    val response: Message = WebSocketAuthResponse(client != null)
                    send(Frame.Text(simpleJson.encodeToString(response)))
                }

                // TODO: migrate to REST API
                client?.handleMessage(message)
            }

            // handle disconnect
            client?.let { Clients.disconnect(it) }
        }
    }
}

/**
 * Tries to decode the text in this [Frame.Text] into a [Message].
 *
 * @return a new [Message] instance
 */
fun Frame.Text.toMessage(): Message = simpleJson.decodeFromString(readText())
