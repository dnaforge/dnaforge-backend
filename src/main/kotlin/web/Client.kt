package web

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import sim.Jobs
import simpleJson

/**
 * This class represents a client connected to this application.
 * [bearerToken] is the corresponding bearer token and [session] is the corresponding [WebSocketServerSession].
 */
data class Client(val bearerToken: String, var session: WebSocketServerSession? = null) {

    companion object {
        private val log = LoggerFactory.getLogger(Client::class.java)
    }

    /**
     * Handles the given [Message] in the context of this [Client].
     *
     * @param message the [Message] to process.
     */
    suspend fun handleMessage(message: Message) {
        log.debug("A message has been received.")

        // handle other message types
        when (message) {
            is JobNew -> Jobs.submitNewJob(message.configs, message.top, message.dat, message.forces)
            is JobCancel -> Jobs.cancelJob(message.jobId)
            is JobDelete -> Jobs.deleteJob(message.jobId)
            is JobSubscribe -> Clients.subscribe(this, message.jobId)
            else -> {}
        }
    }

    /**
     * Sends a [Message] to this [Client] if it has a corresponding WebSocket session.
     *
     * @param message the [Message] to send.
     */
    suspend fun trySendMessage(message: Message) =
        session?.outgoing?.send(Frame.Text(simpleJson.encodeToString(message)))
}
