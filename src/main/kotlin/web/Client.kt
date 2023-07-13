package web

import Environment
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import sim.Jobs
import simpleJson

class Client(val session: WebSocketServerSession) {
    private var allowed: Boolean = false

    companion object {
        private val logger = LoggerFactory.getLogger(Client::class.java)
    }

    suspend fun handleMessage(message: Message) {
        logger.info("Handling message.")
        // handle authentication request
        if (message is Authenticate) {
            updateAllowed(message.accessToken)
            return
        }

        // ignore messages from unauthenticated clients
        if (!allowed) return

        // handle other message types
        when (message) {
            is JobNew -> Jobs.submitNewJob(message.job)
            is JobCancel -> Jobs.cancelJob(message.jobId)
            is JobDelete -> Jobs.deleteJob(message.jobId)
            is JobSubscribe -> Clients.subscribe(this, message.jobId)
            else -> {}
        }
    }

    suspend fun sendMessage(message: Message) =
        session.outgoing.send(Frame.Text(Json.encodeToString(message)))

    suspend fun updateAllowed(accessToken: String?) {
        val success = allowed || Environment.inst.allowAccess(accessToken)
        if (success) allowed = true

        // only send message if the client tried to authenticate or authentication was successful
        if (accessToken != null || success)
            sendMessage(AuthenticationResponse(success))

        if (allowed)
            sendMessage(JobList(Jobs.getJobs()))
    }
}
