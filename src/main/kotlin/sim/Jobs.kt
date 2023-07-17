package sim

import Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import web.Clients
import java.io.File

/**
 * This object manages jobs in this application.
 */
@OptIn(DelicateCoroutinesApi::class)
object Jobs {
    const val stepFileName = "step.json"

    private val log = LoggerFactory.getLogger(Jobs::class.java)
    private val scope = CoroutineScope(newSingleThreadContext("JobExecutionContext"))
    private var job: Job? = null
    private val mutex = Mutex()

    private val jobs = mutableMapOf<UInt, SimJob>()
    private var nextId: UInt

    /**
     * Create a [List] of [SimJob]s ordered by their [SimJob.id].
     *
     * @return a new [List] with all known [SimJob]s.
     */
    suspend fun getJobs() = mutex.withLock { jobs.toList().sortedBy { it.first }.map { it.second } }

    init {
        runBlocking {
            readAllJobs()

            // determine next ID
            mutex.withLock {
                nextId = jobs.maxOfOrNull { it.key } ?: 0u
            }

            startJobWorker()
        }
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
            jobs[job.id] = job
            Clients.propagateUpdate(job.id, job)
        }

        log.info("New job with ID $id submitted.")

        startJobWorker()
    }

    /**
     * Cancels the execution of the [SimJob] specified by the ID.
     *
     * @param jobId the ID of the [SimJob] to be canceled.
     */
    suspend fun cancelJob(jobId: UInt) = mutex.withLock {
        val job = jobs[jobId] ?: return@withLock

        job.cancel()

        Clients.propagateUpdate(job.id, job)

        log.info("Job with ID $jobId canceled.")
    }

    /**
     * Deletes the [SimJob] specified by the ID.
     *
     * @param jobId the ID of the [SimJob] to be deleted.
     */
    suspend fun deleteJob(jobId: UInt) = mutex.withLock {
        val job = jobs[jobId] ?: return@withLock

        job.cancel()

        jobs.remove(jobId)
        job.deleteFiles()
        Clients.propagateUpdate(job.id, null)

        log.info("Job with ID $jobId deleted.")
    }

    /**
     * Starts a new job worker if needed.
     */
    private suspend fun startJobWorker() = mutex.withLock {

        // Worker is already running
        if (job?.isActive == true) return@withLock

        job = scope.launch {
            println("Starting...")
            var nextJob: SimJob?
            while (getNextJob().also { nextJob = it } != null) {
                nextJob?.execute()
            }

            log.info("No new job found to execute. Worker will stop for now.")
        }
        job?.start()
    }

    /**
     * Calculates the [SimJob] to run next.
     * Partially completed [SimJob]s have a higher priority than new [SimJob]s.
     *
     * @return the next [SimJob] to run or `null` if no [SimJob] is left.
     */
    private suspend fun getNextJob(): SimJob? = mutex.withLock {
        val candidates = jobs.asSequence()
            .filter { it.value.status == JobState.NEW || it.value.status == JobState.RUNNING } // filter for jobs that still need to run
            .sortedBy { it.key } // sort by id
            .map { it.value } // only take job

        // first, try to continue running a partially completed job
        val running = candidates.firstOrNull { it.status == JobState.RUNNING }
        if (running != null) return running

        return candidates.firstOrNull()
    }

    /**
     * Reads all [SimJob]s from the [Environment.dataDir].
     * The [SimJob]s are stored in the [jobs] map.
     */
    private suspend fun readAllJobs() = mutex.withLock {
        (Environment.dataDir.listFiles()?.toList() ?: listOf<File>()).mapNotNull { file: File? ->
            var name: UInt = UInt.MAX_VALUE
            if (file == null || !file.isDirectory || file.name.toUIntOrNull()?.also { name = it } == null) null
            else Pair(name, SimJob.fromDisk(file))
        }.toMap(jobs)
    }
}
