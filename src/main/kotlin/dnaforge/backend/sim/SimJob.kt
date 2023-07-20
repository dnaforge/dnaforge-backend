package dnaforge.backend.sim

import dnaforge.backend.*
import dnaforge.backend.web.Clients
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
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * This class represents a job.
 */
@Serializable
data class SimJob(
    val id: UInt,
    val steps: UInt
) {
    private var completedSteps: UInt = 0u
    var status: JobState = JobState.NEW
        private set
    private var progress: Float = 0.0f
    private var error: String? = null

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
    val topFile = File(dir, topologyFileName)

    @Transient
    val startConfFile = File(dir, startConfFileName)

    @Transient
    val forcesFile = File(dir, forcesFileName)

    private val simSteps: List<UInt> by lazy {
        buildList {
            for (i in 0u..<steps) {
                val stepDir = File(dir, i.toString())
                val stepFile = File(stepDir, stepFileName)
                val stepConfig = StepConfig.fromJsonFile(stepFile)
                this.add(stepConfig.getParameterMap()["steps"]?.toUIntOrNull() ?: 0u)
            }
        }
    }

    /**
     * Determines the latest available conf [File].
     * Only completed steps are considered.
     *
     * @return a new [File] instance pointing to the latest known conf [File].
     */
    suspend fun getLatestConfFile(): File = mutex.withLock {
        if (completedSteps == 0u)
            return startConfFile

        val lastCompletedStepDir = File(dir, (completedSteps - 1u).toString())
        return File(lastCompletedStepDir, endConfFileName)
    }

    fun prepareDownload(): File {
        val zipFile = File(dir, zipFileName)
        FileOutputStream(zipFile).use { zipFis ->
            ZipOutputStream(zipFis).use { out ->
                dir.listFiles { file: File -> file != zipFile }?.forEach { file ->
                    zipFile(out, file)
                }
            }
        }
        return zipFile
    }

    /**
     * Adds the specified [fileToZip] to the [ZipOutputStream].
     * If [fileToZip] is a directory, this function is called recursively for all children.
     *
     * @param zipOut the [ZipOutputStream] to write to.
     * @param fileToZip the [File] to add to [zipOut].
     * @param prefix used for the recursive call.
     */
    private fun zipFile(zipOut: ZipOutputStream, fileToZip: File, prefix: String = "") {
        if (fileToZip.isDirectory) {
            zipOut.putNextEntry(ZipEntry("$prefix${fileToZip.name}/"))
            zipOut.closeEntry()

            fileToZip.listFiles()?.forEach { childFile ->
                zipFile(zipOut, childFile, "$prefix${fileToZip.name}${File.separatorChar}")
            }
        } else {
            FileInputStream(fileToZip).use { fis ->
                val entry = ZipEntry("$prefix${fileToZip.name}")
                zipOut.putNextEntry(entry)

                val bytes = ByteArray(1024)
                var length: Int
                while (fis.read(bytes).also { length = it } >= 0) {
                    zipOut.write(bytes, 0, length)
                }
                zipOut.closeEntry()
            }
        }
    }

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
        unlockedToDisk()
        Clients.propagateUpdate(this.id, this)

        while (completedSteps < steps) {
            // stop execution if there was an error or the job was canceled
            if (cancelMutex.withLock { shouldStopExecution() }) break

            executeNextStep()
            unlockedToDisk()
            Clients.propagateUpdate(this.id, this)
        }

        // set DONE state if everything was fine
        cancelMutex.withLock {
            if (status != JobState.CANCELED)
                status = JobState.DONE
        }
        unlockedToDisk()
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
        val nextLogFile = File(nextDir, oxDnaLogFileName)
        val endConfFile = File(nextDir, endConfFileName)

        // run oxDNA
        val pb = ProcessBuilder()
        pb.directory(nextDir)
        pb.command(listOf("oxDNA", inputFileName))
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
            var line: String?
            while (true) {
                line = try {
                    energyStream?.readLine()
                } catch (e: IOException) {
                    null
                }
                if (line == null) break

                val currentConf = endConfFile.readText()
                val completedInCurrentConf =
                    currentConf.lineSequence().firstOrNull { it.startsWith("t = ") }?.substring("t = ".length)
                        ?.toUIntOrNull() ?: 0u
                val totalSimSteps = simSteps.sum()
                val completedSimSteps = simSteps.subList(0, completedSteps.toInt()).sum() + completedInCurrentConf
                this@SimJob.progress = completedSimSteps.toFloat() / totalSimSteps.toFloat()
                Clients.propagateDetailedUpdate(this@SimJob, currentConf)
            }
        }

        val exitCode = process?.waitFor()

        val success = exitCode == 0

        if (success) {
            completedSteps++
        } else {
            error = nextLogFile.useLines {
                it.firstOrNull { line -> line.startsWith("ERROR:") }?.substring("ERROR: ".length)
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
        val stepFile = File(nextDir, stepFileName)
        val stepConfig = StepConfig.fromJsonFile(stepFile)
        val inputFile = File(nextDir, inputFileName)
        stepConfig.toPropertiesFile(inputFile)

        // get and copy conf file of last step
        val oldConfFile =
            if (completedSteps == 0u) {
                startConfFile
            } else {
                val oldDir = File(dir, (completedSteps - 1u).toString())
                File(oldDir, endConfFileName)
            }
        val startConfFile = File(nextDir, startConfFileName)
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
    private fun unlockedToDisk() {
        dir.mkdirs()
        file.outputStream().use { prettyJson.encodeToStream(this, it) }
    }

    /**
     * Writes the current state of this [SimJob] to disk.
     */
    suspend fun toDisk() {
        mutex.withLock {
            unlockedToDisk()
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

    override fun toString(): String {
        return "SimJob(id=$id, steps=$steps, completedSteps=$completedSteps, status=$status, progress=$progress, error=$error)"
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
