package dnaforge.backend.web

import dnaforge.backend.simpleJson
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Instant

/**
 * This class represents a client connected to this application.
 * [bearerToken] is the corresponding bearer token and [session] is the corresponding [WebSocketServerSession].
 * To periodically purge inactive clients, the time of the last interaction is stored.
 */
data class Client(val bearerToken: String) {
    var lastInteraction: Instant = Instant.now()
    var session: WebSocketServerSession? = null

    /**
     * Sends a [WebSocketMessage] to this [Client] if it has a corresponding WebSocket session.
     *
     * @param message the [WebSocketMessage] to send.
     */
    suspend fun trySendMessage(message: WebSocketMessage) =
        session?.outgoing?.send(Frame.Text(simpleJson.encodeToString(message)))
}
