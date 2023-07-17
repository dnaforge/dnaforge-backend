package web

import Environment
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import sim.Jobs
import simpleJson

/**
 * This class represents a client connected to this application.
 * [session] is the corresponding [WebSocketServerSession].
 */
data class Client(val session: WebSocketServerSession) {
    private var allowed: Boolean = false

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

        // handle authentication request
        if (message is Authenticate) {
            updateAllowed(message.accessToken)
            return
        }

        // ignore messages from unauthenticated clients
        if (!allowed) return

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
     * Sends a [Message] to this [Client].
     *
     * @param message the [Message] to send.
     */
    suspend fun sendMessage(message: Message) =
        session.outgoing.send(Frame.Text(simpleJson.encodeToString(message)))

    /**
     * Checks if the given [accessToken] is valid and updates the [allowed] status accordingly.
     * If this [Client] has already been authenticated, nothing changes.
     *
     * @param accessToken the token to use for this authentication attempt.
     */
    private suspend fun updateAllowed(accessToken: String?) {
        val success = allowed || Environment.allowAccess(accessToken)
        if (success) allowed = true

        // only send message if the client tried to authenticate or authentication was successful
        if (accessToken != null || success)
            sendMessage(AuthenticationResponse(success))

        if (allowed) {
            sendMessage(JobList(Jobs.getJobs()))
            log.debug("A client authenticated successfully.")
        }
    }
}
