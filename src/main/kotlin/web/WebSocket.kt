package web

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
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
            val client = Client(this)
            Clients.addClient(client)

            // authentication might not be required
            client.handleMessage(Authenticate(""))


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

                // forward message to the client
                client.handleMessage(message)
            }

            // handle disconnect
            Clients.removeClient(client)
        }
    }
}

/**
 * Tries to decode the text in this [Frame.Text] into a [Message].
 *
 * @return a new [Message] instance
 */
private fun Frame.Text.toMessage(): Message = simpleJson.decodeFromString(readText())