package web

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import simpleJson

/**
 * This class represents a client connected to this application.
 * [bearerToken] is the corresponding bearer token and [session] is the corresponding [WebSocketServerSession].
 */
data class Client(val bearerToken: String, var session: WebSocketServerSession? = null) {

    /**
     * Sends a [WebSocketMessage] to this [Client] if it has a corresponding WebSocket session.
     *
     * @param message the [WebSocketMessage] to send.
     */
    suspend fun trySendMessage(message: WebSocketMessage) =
        session?.outgoing?.send(Frame.Text(simpleJson.encodeToString(message)))
}
