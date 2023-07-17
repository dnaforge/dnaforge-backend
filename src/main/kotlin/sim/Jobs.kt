package sim

import Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import stepFileName
import web.Clients
import java.io.File

/**
 * This object manages jobs in this application.
 */
@OptIn(DelicateCoroutinesApi::class)
object Jobs {
    private val log = LoggerFactory.getLogger(Jobs::class.java)
    private val scope = CoroutineScope(newSingleThreadContext("JobExecutionContext"))
    private val coroutineJob: Job
    private val mutex = Mutex()

    private var nextId: UInt

    // needed since it's not possible to get all queued jobs from the channel
    private val queuedJobs: MutableMap<UInt, SimJob> = LinkedHashMap()
    private val queue = Channel<SimJob>(Channel.UNLIMITED)
    private val finishedJobs: MutableMap<UInt, SimJob> = LinkedHashMap()

    /**
     * Creates a [List] of [SimJob]s.
     *
     * @return a new [List] with all known [SimJob]s.
     */
    suspend fun getJobs() = mutex.withLock { finishedJobs.values + queuedJobs.values }

    init {
        runBlocking {
            readAllJobs()

            // determine next ID
            nextId = getJobs().maxOfOrNull { it.id } ?: 0u
        }

        coroutineJob = scope.launch {
            for (job in queue) {
                if (!mutex.withLock { queuedJobs.containsKey(job.id) }) continue
                job.execute()
            }

            log.warn("No new job found to execute. Worker will stop.")
        }
        coroutineJob.start()

        log.info("Job worker started.")
    }

    /**
     * Creates a new [SimJob] and adds it to the execution queue.
     */
    suspend fun submitNewJob(configs: List<StepConfig>, top: String, dat: String, forces: String) {
        val id = mutex.withLock {
            nextId++
        }
        val job = SimJob(id, configs.size.toUInt())

        // write files
        job.toDisk()
        job.topFile.writeText(top.replace("\r\n", "\n"))
        job.startConfFile.writeText(dat.replace("\r\n", "\n"))
        job.forcesFile.writeText(forces.replace("\r\n", "\n"))

        for (indexedStep in configs.withIndex()) {
            val i = indexedStep.index
            val step = indexedStep.value

            val dir = File(job.dir, i.toString())
            dir.mkdirs()
            val stepFile = File(dir, stepFileName)
            step.toJsonFile(stepFile)
        }

        mutex.withLock {
            queuedJobs[job.id] = job
            queue.send(job)
            Clients.propagateUpdate(job.id, job)
        }

        log.info("New job with ID $id submitted.")
    }

    /**
     * Cancels the execution of the [SimJob] specified by the ID.
     *
     * @param jobId the ID of the [SimJob] to be canceled.
     */
    suspend fun cancelJob(jobId: UInt) {
        mutex.withLock {
            val job = queuedJobs[jobId] ?: return@withLock

            job.cancel()
            queuedJobs.remove(jobId)
            finishedJobs[jobId] = job

            Clients.propagateUpdate(job.id, job)
        }

        log.info("Job with ID $jobId canceled.")
    }

    /**
     * Deletes the [SimJob] specified by the ID.
     *
     * @param jobId the ID of the [SimJob] to be deleted.
     */
    suspend fun deleteJob(jobId: UInt) {
        mutex.withLock {
            if (queuedJobs.containsKey(jobId)) {
                val job = queuedJobs[jobId] ?: return@withLock

                job.cancel()
                queuedJobs.remove(jobId)
                job.deleteFiles()

                Clients.propagateUpdate(job.id, null)
            } else {
                val job = finishedJobs[jobId] ?: return@withLock

                finishedJobs.remove(jobId)
                job.deleteFiles()

                Clients.propagateUpdate(job.id, null)
            }
        }

        log.info("Job with ID $jobId deleted.")
    }

    /**
     * Reads all [SimJob]s from the [Environment.dataDir].
     * The [SimJob]s are stored in the correct [Map]s and [Channel].
     */
    private suspend fun readAllJobs() = mutex.withLock {
        val dirs = Environment.dataDir.listFiles()?.toList() ?: listOf<File>()
        val jobs = dirs.mapNotNull { file: File? ->
            if (file == null || !file.isDirectory || file.name.toUIntOrNull() == null) null
            else SimJob.fromDisk(file)
        }

        val finishedJobs =
            jobs.filter { it.status == JobState.DONE || it.status == JobState.CANCELED }.sortedBy { it.id }
        val runningJobs = jobs.filter { it.status == JobState.RUNNING }.sortedBy { it.id }
        val newJobs = jobs.filter { it.status == JobState.NEW }.sortedBy { it.id }
        val queuedJobs = runningJobs + newJobs

        finishedJobs.associateByTo(this.finishedJobs) { it.id }
        for (job in queuedJobs) {
            this.queuedJobs[job.id] = job
            queue.send(job)
        }
    }
}
