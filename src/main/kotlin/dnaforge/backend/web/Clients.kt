package dnaforge.backend.web

import dnaforge.backend.Environment
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.SimJob
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.streams.asSequence
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * This object manages clients connected to this application.
 * It is responsible for distributing updates.
 */
@OptIn(DelicateCoroutinesApi::class)
object Clients {
    private val log = LoggerFactory.getLogger(Jobs::class.java)
    private val mutex = Mutex()
    private val tokenCharPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val random: Random = SecureRandom()

    private val tokenClientsMap = mutableMapOf<String, Client>()
    private val clientSubscribedJobId = mutableMapOf<Client, UInt>()
    private val jobIdSubscribedClient = mutableMapOf<UInt, MutableList<Client>>()

    init {
        val scope = CoroutineScope(newSingleThreadContext("JobExecutionContext"))
        val coroutineJob = scope.launch {
            // run cleanup once every minute
            while (true) {
                delay(1.toDuration(DurationUnit.MINUTES))

                val inactiveClients: Map<String, Client>
                mutex.withLock {
                    // Purge clients that do not have an active WebSocket session
                    // and have not interacted in the last 5 minutes.
                    inactiveClients = tokenClientsMap.filterValues {
                        it.session == null && it.lastInteraction.isBefore(
                            Instant.now().minus(5, ChronoUnit.MINUTES)
                        )
                    }

                    inactiveClients.forEach {
                        tokenClientsMap.remove(it.key)
                    }
                }

                if (inactiveClients.isEmpty()) continue

                // remove subscriptions
                inactiveClients.forEach {
                    unsubscribe(it.value)
                }

                log.info("Purged {} inactive clients.", inactiveClients.size)
            }
        }
        coroutineJob.start()
    }

    /**
     * Looks up which [SimJob] the given [Client] is currently subscribed to.
     *
     * @param client the [client] whose subscription to look up.
     *
     * @return the ID of the [SimJob] the [Client] is subscribed to; `null` if the [Client] has no subscription.
     */
    suspend fun getSubscription(client: Client): UInt? {
        return mutex.withLock {
            clientSubscribedJobId[client]
        }
    }

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
            clientSubscribedJobId[client] = jobId
        }

        log.debug("A client has subscribed to the job with ID {}", jobId)
    }

    /**
     * Sends a detailed update to all [Client]s subscribed to the [SimJob] specified by the ID.
     * Sends a normal update to all other Clients.
     *
     * @param job the [SimJob] that encountered an update.
     * @param dat the update data.
     */
    suspend fun propagateDetailedUpdate(job: SimJob, dat: String) {
        mutex.withLock {
            jobIdSubscribedClient[job.id]?.forEach {
                it.trySendMessage(DetailedUpdate(job, job.topFile.readText(), dat))
            }

            // send normal updates to all other clients
            tokenClientsMap.asSequence().map { it.value }.filter { clientSubscribedJobId[it] != job.id }
                .forEach { it.trySendMessage(JobUpdate(job.id, job)) }
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
            if (job == null)
                jobIdSubscribedClient.remove(jobId)?.forEach { clientSubscribedJobId.remove(it) }

            // send update to clients
            tokenClientsMap.asSequence().map { it.value }.forEach { it.trySendMessage(JobUpdate(jobId, job)) }
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
