package web

import io.ktor.server.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import sim.Jobs
import sim.SimJob

object Clients {
    private val logger = LoggerFactory.getLogger(Jobs::class.java)
    private val mutex = Mutex()

    private val clients = mutableMapOf<WebSocketServerSession, Client>()
    private val clientSubscribedJobId = mutableMapOf<Client, UInt>()
    private val jobIdSubscribedClient = mutableMapOf<UInt, MutableList<Client>>()


    suspend fun subscribe(client: Client, jobId: UInt?) = mutex.withLock {
        // remove subscription
        if (jobId == null) {
            val oldId = clientSubscribedJobId.remove(client)
            jobIdSubscribedClient[oldId]?.remove(client)

            // Remove entry when no more clients are subscribed
            if (jobIdSubscribedClient[oldId]?.isEmpty() == true)
                jobIdSubscribedClient.remove(oldId)

            return@withLock
        }

        // add subscription
        jobIdSubscribedClient[jobId]?.add(client) ?: run { jobIdSubscribedClient[jobId] = mutableListOf(client) }
    }

    suspend fun propagateDetailedUpdate(jobId: UInt, conf: String) = mutex.withLock {
        jobIdSubscribedClient[jobId]?.forEach { it.sendMessage(DetailedUpdate(conf)) }
    }

    suspend fun propagateUpdate(jobId: UInt, job: SimJob?) = mutex.withLock {
        // Remove subscriptions when job is deleted
        if (job == null) {
            jobIdSubscribedClient[jobId]?.forEach { clientSubscribedJobId.remove(it) }
            jobIdSubscribedClient.remove(jobId)
        }

        for (client in clients.values)
            client.sendMessage(JobUpdate(jobId, job))
    }

    suspend fun addClient(client: Client) = mutex.withLock {
        clients[client.session] = client
        logger.info("Client connected.")
    }

    suspend fun removeClient(client: Client) {
        mutex.withLock {
            clients.remove(client.session)
            logger.info("Client disconnected.")
        }

        // remove old subscriptions
        subscribe(client, null)
    }
}
