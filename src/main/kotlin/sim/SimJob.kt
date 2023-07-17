package sim

import Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.LoggerFactory
import prettyJson
import web.Clients
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

// TODO consider private setters and a custom de-/serializer
/**
 * This class represents a job.
 */
@Serializable
class SimJob(
    val id: UInt,
    val steps: UInt,
    var completedSteps: UInt = 0u,
    var status: JobState = JobState.NEW,
    val statusMessage: String? = null,
    val progress: Float = 0.0f,
    var error: String? = null
) {
    @Transient
    private val mutex = Mutex()

    // has to be used for changing the status and process
    @Transient
    private val cancelMutex = Mutex()

    @Transient
    private var process: Process? = null

    @Transient
    val dir = File(Environment.dataDir, id.toString())

    @Transient
    private val file = File(dir, jobFileName)

    @Transient
    val topFile = File(dir, "topology.top")

    @Transient
    val startConfFile = File(dir, "conf_start.dat")

    @Transient
    val forcesFile = File(dir, "forces.forces")

    /**
     * Runs this [SimJob] and updates its status whenever needed.
     * Also initiates the propagation of updates to clients.
     */
    suspend fun execute() = mutex.withLock {
        log.info("Executing the job with ID $id.")

        // set running state if not already canceled
        val cancel = cancelMutex.withLock {
            if (shouldStopExecution()) true
            else {
                status = JobState.RUNNING
                false
            }
        }
        if (cancel) return@withLock
        Clients.propagateUpdate(this.id, this)

        while (completedSteps < steps) {
            // stop execution if there was an error or the job was canceled
            if (cancelMutex.withLock { shouldStopExecution() }) break

            executeNextStep()
            Clients.propagateUpdate(this.id, this)
        }

        // set DONE state if everything was fine
        cancelMutex.withLock {
            if (status != JobState.CANCELED)
                status = JobState.DONE
        }
        Clients.propagateUpdate(this.id, this)

        log.info("Execution of the job with ID $id finished.")
    }

    /**
     * Small helper function that tests if this [SimJob] is [JobState.CANCELED],
     * [JobState.DONE] or has encountered an error.
     */
    private fun shouldStopExecution(): Boolean = status == JobState.CANCELED || status == JobState.DONE || error != null

    /**
     * Executes the next step of this [SimJob].
     */
    private suspend fun executeNextStep() {
        log.debug("Running step $completedSteps of the job with ID $id.")
        val nextDir = File(dir, completedSteps.toString())
        prepareFilesForNextStep(nextDir)
        val nextLogFile = File(nextDir, "oxDNA.log")
        val endConfFile = File(nextDir, StepConfig.endConfFileName)

        // run oxDNA
        val pb = ProcessBuilder()
        pb.directory(nextDir)
        pb.command(listOf("oxDNA", "input.properties"))
        // for some reason, oxDNA only writes the energy to std out and everything else to std error
        pb.redirectError(ProcessBuilder.Redirect.appendTo(nextLogFile))

        var energyStream: BufferedReader? = null
        val cancel = cancelMutex.withLock {
            if (status == JobState.CANCELED) {
                true
            } else {
                process = pb.start()
                energyStream = BufferedReader(InputStreamReader(process!!.inputStream))
                false
            }
        }
        if (cancel) return

        // we use the fact that the print interval of the trajectory and energy are the same to push detailed updates to clients
        withContext(Dispatchers.IO) {
            var line: String? = ""
            while (line != null) {
                line = try {
                    energyStream?.readLine()
                } catch (e: IOException) {
                    null
                }
                if (line != null)
                    Clients.propagateDetailedUpdate(id, endConfFile.readText())
            }
        }

        val exitCode = process?.waitFor()

        val success = exitCode == 0

        if (success) {
            completedSteps++
        } else {
            error = nextLogFile.useLines {
                it.filter { it.startsWith("ERROR:") }.firstOrNull()?.substring("ERROR: ".length)
            }

            // abort next steps
            cancelMutex.withLock {
                if (status != JobState.CANCELED)
                    status = JobState.CANCELED
            }
        }

        log.debug("The execution of a step of the job with the ID $id is completed.")
    }

    /**
     * Copies the files from the previous step (or initial files for step 0) and creates the oxDNA input file.
     *
     * @param nextDir the directory to prepare.
     */
    private fun prepareFilesForNextStep(nextDir: File) {
        // prepare input file
        val stepFile = File(nextDir, Jobs.stepFileName)
        val stepConfig = StepConfig.fromJsonFile(stepFile)
        val inputFile = File(nextDir, StepConfig.inputFileName)
        stepConfig.toPropertiesFile(inputFile)

        // get and copy conf file of last step
        val oldConfFile =
            if (completedSteps == 0u) {
                startConfFile
            } else {
                val oldDir = File(dir, (completedSteps - 1u).toString())
                File(oldDir, "conf_end.dat")
            }
        val startConfFile = File(nextDir, "conf_start.dat")
        oldConfFile.copyTo(startConfFile, true)
    }

    /**
     * Cancels the execution of this [SimJob].
     */
    suspend fun cancel() {
        log.debug("Attempting to cancel the job with ID $id.")

        cancelMutex.withLock {
            // can't cancel these anymore...
            if (status == JobState.CANCELED || status == JobState.DONE) return@withLock

            status = JobState.CANCELED

            process?.destroy()
            // give it 5s to terminate gracefully
            val ended = process?.waitFor(5, TimeUnit.SECONDS) ?: true
            // kill it if necessary
            if (!ended) process?.destroyForcibly()
        }

        log.info("The job with ID $id has been canceled.")
    }

    /**
     * Writes the current state of this [SimJob] to disk.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toDisk() {
        mutex.withLock {
            dir.mkdirs()
            file.outputStream().use { prettyJson.encodeToStream(this, it) }
        }

        log.debug("Wrote the job with ID $id to disk.")
    }

    /**
     * Deletes all files related to this [SimJob].
     */
    suspend fun deleteFiles() {
        mutex.withLock {
            dir.deleteRecursively()
        }

        log.info("All files of the job with ID $id have been deleted.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimJob::class.java)
        private const val jobFileName = "job.json"

        /**
         * Reads a [SimJob] from the specified directory [File].
         *
         * @param directory the [File] to read from.
         *
         * @return a new [SimJob] instance.
         */
        @OptIn(ExperimentalSerializationApi::class)
        @Throws(SerializationException::class, IllegalArgumentException::class)
        fun fromDisk(directory: File): SimJob {
            val file = File(directory, jobFileName)
            return file.inputStream().use { prettyJson.decodeFromStream(it) }
        }
    }
}

/**
 * States a [SimJob] can be in.
 */
enum class JobState {
    NEW,
    RUNNING,
    DONE,
    CANCELED
}
