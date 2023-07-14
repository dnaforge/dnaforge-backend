package web

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import simpleJson
import java.time.Duration

fun Application.configureWebSocket() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
    }

    routing {
        webSocket("/") {
            // handle connect
            val client = Client(this)
            Clients.addClient(client)

            // authentication might not be required
            client.updateAllowed(null)


            for (frame in incoming) {
                if (frame is Frame.Close) break
                if (frame is Frame.Ping) outgoing.send(Frame.Pong(frame.buffer))
                if (frame !is Frame.Text) continue

                val message = try {
                    frame.toMessage()
                } catch (e: Throwable) {
                    continue // ignore invalid messages
                }

                client.handleMessage(message)
            }

            // handle disconnect
            Clients.removeClient(client)
        }
    }
}


fun Frame.Text.toMessage(): Message = simpleJson.decodeFromString(readText())
