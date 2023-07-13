package sim

import Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import web.Clients
import web.NewJob
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
object Jobs {
    const val stepFileName = "step.json"

    private val env = Environment.inst
    private val logger = LoggerFactory.getLogger(Jobs::class.java)
    private val scope = CoroutineScope(newSingleThreadContext("JobExecutionContext"))
    private var job: Job? = null
    private val mutex = Mutex()

    private val jobs = mutableMapOf<UInt, SimJob>()
    private var nextId: UInt
    suspend fun getJobs() = mutex.withLock { jobs.toList().sortedBy { it.first }.map { it.second } }

    init {
        runBlocking {
            readAllJobs()

            mutex.withLock {
                nextId = jobs.maxOfOrNull { it.key } ?: 0u
            }

            startJobWorker()
        }
    }

    suspend fun submitNewJob(newJob: NewJob) {
        val id = mutex.withLock {
            nextId++
        }
        val job = SimJob(id, newJob.configs.size.toUInt())

        // write files
        job.toDisk()
        job.topFile.writeText(newJob.top.replace("\r\n", "\n"))
        job.startConfFile.writeText(newJob.dat.replace("\r\n", "\n"))
        job.forcesFile.writeText(newJob.forces.replace("\r\n", "\n"))

        for (indexedStep in newJob.configs.withIndex()) {
            val i = indexedStep.index
            val step = indexedStep.value

            val dir = File(job.dir, i.toString())
            dir.mkdirs()
            val stepFile = File(dir, stepFileName)
            step.toFile(stepFile)
        }

        mutex.withLock {
            jobs[job.id] = job
            Clients.propagateUpdate(job.id, job)
        }

        logger.info("New job with id $id submitted.")

        startJobWorker()
    }

    suspend fun cancelJob(jobId: UInt) = mutex.withLock {
        val job = jobs[jobId] ?: return@withLock

        job.cancel()

        Clients.propagateUpdate(job.id, job)
    }

    suspend fun deleteJob(jobId: UInt) = mutex.withLock {
        val job = jobs[jobId] ?: return@withLock

        job.cancel()

        jobs.remove(jobId)
        job.deleteFiles()
        Clients.propagateUpdate(job.id, null)
    }

    private suspend fun startJobWorker() = mutex.withLock {

        // Worker is already running
        if (job?.isActive == true) return@withLock

        job = scope.launch {
            println("Starting...")
            var nextJob: SimJob?
            while (getNextJob().also { nextJob = it } != null) {
                nextJob?.execute()
            }

            logger.info("No new job found to execute. Worker will stop for now.")
        }
        job?.start()
    }

    private suspend fun getNextJob(): SimJob? = mutex.withLock {
        val candidates = jobs.asSequence()
            .filter { it.value.status == JobState.NEW || it.value.status == JobState.RUNNING } // filter for jobs that still need to run
            .sortedBy { it.key } // sort by id
            .map { it.value } // only take job

        // first try to continue running a partially completed job
        val running = candidates.firstOrNull { it.status == JobState.RUNNING }
        if (running != null) return running

        return candidates.firstOrNull()
    }

    private suspend fun readAllJobs() = mutex.withLock {
        (env.dataDir.listFiles()?.toList() ?: listOf<File>()).mapNotNull { file: File? ->
            var name: UInt = UInt.MAX_VALUE
            if (file == null || !file.isDirectory || file.name.toUIntOrNull()?.also { name = it } == null) null
            else Pair(name, SimJob.fromDisk(file))
        }.toMap(jobs)
    }
}
