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
import prettyJson
import web.Clients
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

// TODO consider private setters and a custom de-/serializer
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
    val dir = File(env.dataDir, id.toString())

    @Transient
    private val file = File(dir, jobFileName)

    @Transient
    val topFile = File(dir, "topology.top")

    @Transient
    val startConfFile = File(dir, "conf_start.dat")

    @Transient
    val forcesFile = File(dir, "forces.forces")

    suspend fun execute() = mutex.withLock {
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
    }

    private fun shouldStopExecution(): Boolean = status == JobState.CANCELED || status == JobState.DONE || error != null

    private suspend fun executeNextStep() {
        val nextDir = File(dir, completedSteps.toString())
        prepareFilesForNextStep(nextDir)
        val nextLogFile = File(nextDir, "oxDNA.log")
        val endConfFile = File(nextDir, StepConfig.endConfFileName)

        // run oxDNA
        val pb = ProcessBuilder()
        pb.directory(nextDir)
        pb.command(listOf("oxDNA", "input.properties"))
        // for some reason oxDNA only writes the energy to std out and everything else to std error
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
    }

    private fun prepareFilesForNextStep(nextDir: File) {
        // prepare input file
        val stepFile = File(nextDir, Jobs.stepFileName)
        val stepConfig = StepConfig.fromFile(stepFile)
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

    suspend fun cancel() = cancelMutex.withLock {
        // can't cancel these anymore...
        if (status == JobState.CANCELED || status == JobState.DONE) return@withLock

        status = JobState.CANCELED

        process?.destroy()
        // give it 5s to terminate gracefully
        val ended = process?.waitFor(5, TimeUnit.SECONDS) ?: true
        // kill it if necessary
        if (!ended) process?.destroyForcibly()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toDisk(): Unit = mutex.withLock {
        dir.mkdirs()
        file.outputStream().use { prettyJson.encodeToStream(this, it) }
    }

    suspend fun deleteFiles() = mutex.withLock {
        dir.deleteRecursively()
    }

    companion object {
        private val env = Environment.inst

        private const val jobFileName = "job.json"

        @OptIn(ExperimentalSerializationApi::class)
        @Throws(SerializationException::class, IllegalArgumentException::class)
        fun fromDisk(directory: File): SimJob {
            val file = File(directory, jobFileName)
            return file.inputStream().use { prettyJson.decodeFromStream(it) }
        }
    }
}

enum class JobState {
    NEW,
    RUNNING,
    DONE,
    CANCELED
}
