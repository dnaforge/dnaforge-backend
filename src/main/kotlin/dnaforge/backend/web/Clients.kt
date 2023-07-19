package dnaforge.backend.web

import dnaforge.backend.Environment
import io.ktor.server.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.SimJob
import java.security.SecureRandom
import java.util.*
import kotlin.streams.asSequence

// TODO: periodically check for dangling clients that authenticated but then didn't get a WebSocket session
/**
 * This object manages clients connected to this application.
 * It is responsible for distributing updates.
 */
object Clients {
    private val log = LoggerFactory.getLogger(Jobs::class.java)
    private val mutex = Mutex()
    private val tokenCharPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val random: Random = SecureRandom()

    private val tokenClientsMap = mutableMapOf<String, Client>()
    private val clients = mutableMapOf<WebSocketServerSession, Client>()
    private val clientSubscribedJobId = mutableMapOf<Client, UInt>()
    private val jobIdSubscribedClient = mutableMapOf<UInt, MutableList<Client>>()

    /**
     * Unsubscribes the specified [Client] from any [SimJob].
     *
     * @param client the [Client] whose subscriptions should be adjusted.
     */
    suspend fun unsubscribe(client: Client) {
        mutex.withLock {
            // remove subscription
            val oldId = clientSubscribedJobId.remove(client)
            jobIdSubscribedClient[oldId]?.remove(client)

            // Remove entry when no more clients are subscribed
            if (jobIdSubscribedClient[oldId]?.isEmpty() == true)
                jobIdSubscribedClient.remove(oldId)
        }

        log.debug("A client has unsubscribed from all jobs.")
    }

    /**
     * Subscribes the specified [Client] from the [SimJob] specified by the ID.
     *
     * @param client the [Client] whose subscriptions should be adjusted.
     * @param jobId the ID of the [SimJob] the [Client] should subscribe to.
     */
    suspend fun subscribe(client: Client, jobId: UInt) {
        // unsubscribe from old subscriptions first
        unsubscribe(client)

        mutex.withLock {
            // add subscription
            jobIdSubscribedClient[jobId]?.add(client) ?: run {
                jobIdSubscribedClient[jobId] = mutableListOf(client)
            }
        }

        log.debug("A client has subscribed to the job with ID {}", jobId)
    }

    /**
     * Sends a detailed update to all [Client]s subscribed to the [SimJob] specified by the ID.
     *
     * @param job the [SimJob] that encountered an update.
     * @param conf the update data.
     */
    suspend fun propagateDetailedUpdate(job: SimJob, conf: String) {
        mutex.withLock {
            jobIdSubscribedClient[job.id]?.forEach { it.trySendMessage(DetailedUpdate(job, conf)) }
        }

        log.debug("A detailed update has been propagated to all clients subscribed to the job with ID {}", job.id)
    }

    /**
     * Sends an update to all [Client]s currently connected.
     *
     * @param jobId the ID of the [SimJob] that encountered an update.
     * @param job the updated [SimJob]; `null` if the [SimJob] has been deleted.
     */
    suspend fun propagateUpdate(jobId: UInt, job: SimJob?) {
        mutex.withLock {
            // remove subscriptions if the job is deleted
            if (job == null) {
                jobIdSubscribedClient[jobId]?.forEach { clientSubscribedJobId.remove(it) }
                jobIdSubscribedClient.remove(jobId)
            }

            // send update to clients
            clients.values.forEach { it.trySendMessage(JobUpdate(jobId, job)) }
        }

        log.debug(
            "An update has been propagated to all clients. The job with ID {} was updated {}",
            jobId,
            if (job == null) "doesn't exist anymore." else "still exists."
        )
    }

    /**
     * Attempts to authenticate with the specified [accessToken].
     *
     * @param accessToken the token sent by the client.
     *
     * @return a bearer token if the authentication was successful, `null` otherwise.
     */
    suspend fun authenticate(accessToken: String?): String? {
        // wrong token
        if (!Environment.allowAccess(accessToken)) {
            log.debug("Authentication of a client failed.")
            return null
        }

        mutex.withLock {
            // make sure the token doesn't already exist
            var bearerToken: String
            do {
                bearerToken = getRandomString()
            } while (tokenClientsMap.containsKey(bearerToken))

            // create and store client representation
            val client = Client(bearerToken)
            tokenClientsMap[bearerToken] = client
            log.info("A new client has authenticated and been assigned bearer token {}.", bearerToken)
            return bearerToken
        }
    }

    /**
     * Associates a [WebSocketServerSession] with a [Client] if the [bearerToken] is valid.
     *
     * @param bearerToken the token sent by the client.
     * @param session the [WebSocketServerSession] from which the [bearerToken] was sent.
     *
     * @return the [Client] corresponding to the given [bearerToken]; `null` if no such [Client] exists.
     */
    suspend fun authenticate(bearerToken: String, session: WebSocketServerSession): Client? {
        mutex.withLock {
            if (!tokenClientsMap.containsKey(bearerToken)) {
                log.debug("Authentication of a WebSocket session failed.")
                return null
            }

            val client = tokenClientsMap[bearerToken]!!
            client.session = session
            log.info("The client with the bearer token {} has been assigned a WebSocket.", bearerToken)
            return client
        }
    }

    /**
     * Looks up the [Client] corresponding to the given [bearerToken].
     *
     * @param bearerToken the token to look up.
     *
     * @return the corresponding [Client] or `null` if no [Client] was found.
     */
    suspend fun getClientByBearerToken(bearerToken: String?): Client? = mutex.withLock { tokenClientsMap[bearerToken] }

    /**
     * Removes a [Client] from the set of connections.
     *
     * @param client the [Client] to be removed.
     */
    suspend fun disconnect(client: Client) {
        mutex.withLock {
            tokenClientsMap.remove(client.bearerToken)
            clients.remove(client.session)
        }

        // remove subscriptions
        unsubscribe(client)

        log.info("The client with bearer token {} disconnected.", client.bearerToken)
    }


    /**
     * Generates a random [String] of length 32 matching [[a-zA-Z0-9]].
     *
     * @return a new random [String].
     */
    private fun getRandomString() =
        random.ints(32, 0, tokenCharPool.size)
            .asSequence().map(tokenCharPool::get).joinToString("")
}
