package web

import io.ktor.server.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import sim.Jobs
import sim.SimJob

/**
 * This object manages clients connected to this application.
 * It is responsible for distributing updates.
 */
object Clients {
    private val log = LoggerFactory.getLogger(Jobs::class.java)
    private val mutex = Mutex()

    private val clients = mutableMapOf<WebSocketServerSession, Client>()
    private val clientSubscribedJobId = mutableMapOf<Client, UInt>()
    private val jobIdSubscribedClient = mutableMapOf<UInt, MutableList<Client>>()


    /**
     * Un-/Subscribes the specified [Client] from the [SimJob] specified by the ID.
     *
     * @param client the [Client] whose subscriptions should be adjusted.
     * @param jobId the ID of the [SimJob] the [Client] should subscribe to; `null` if subscriptions should be unsubscribed.
     */
    suspend fun subscribe(client: Client, jobId: UInt?) {
        mutex.withLock {
            if (jobId == null) {
                // remove subscription
                val oldId = clientSubscribedJobId.remove(client)
                jobIdSubscribedClient[oldId]?.remove(client)

                // Remove entry when no more clients are subscribed
                if (jobIdSubscribedClient[oldId]?.isEmpty() == true)
                    jobIdSubscribedClient.remove(oldId)
                Unit
            } else {
                // add subscription
                jobIdSubscribedClient[jobId]?.add(client) ?: run {
                    jobIdSubscribedClient[jobId] = mutableListOf(client)
                }
            }
        }

        if (jobId == null)
            log.debug("A client has unsubscribed from all jobs.")
        else
            log.debug("A client has subscribed to the job with ID {}", jobId)
    }

    /**
     * Sends a detailed update to all [Client]s subscribed to the [SimJob] specified by the ID.
     *
     * @param jobId the ID of the [SimJob] that encountered an update.
     * @param conf the update data.
     */
    suspend fun propagateDetailedUpdate(jobId: UInt, conf: String) {
        mutex.withLock {
            jobIdSubscribedClient[jobId]?.forEach { it.sendMessage(DetailedUpdate(conf)) }
        }

        log.debug("A detailed update has been propagated to all clients subscribed to the job with ID {}", jobId)
    }

    /**
     * Sends an update to all [Client]s currently connected.
     *
     * @param jobId the ID of the [SimJob] that encountered an update.
     * @param job the updated [SimJob]; `null` if the [SimJob] has been deleted.
     */
    suspend fun propagateUpdate(jobId: UInt, job: SimJob?) {
        mutex.withLock {
            // remove subscriptions if job is deleted
            if (job == null) {
                jobIdSubscribedClient[jobId]?.forEach { clientSubscribedJobId.remove(it) }
                jobIdSubscribedClient.remove(jobId)
            }

            // send update to clients
            clients.values.forEach { it.sendMessage(JobUpdate(jobId, job)) }
        }

        log.debug(
            "An update has been propagated to all clients. The job with ID {} was updated {}",
            jobId,
            if (job == null) "doesn't exist anymore." else "still exists."
        )
    }

    /**
     * Adds a [Client] to the set of connections.
     *
     * @param client the new [Client].
     */
    suspend fun addClient(client: Client) {
        mutex.withLock {
            clients[client.session] = client
        }

        log.info("Client connected.")
    }

    /**
     * Removes a [Client] from the set of connections.
     *
     * @param client the [Client] to be removed.
     */
    suspend fun removeClient(client: Client) {
        mutex.withLock {
            clients.remove(client.session)
        }

        // remove old subscriptions
        subscribe(client, null)

        log.info("Client disconnected.")
    }
}
