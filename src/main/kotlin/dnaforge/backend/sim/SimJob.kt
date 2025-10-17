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
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.absoluteValue


/**
 * This class represents a job.
 */
@Serializable
data class SimJob(
    val metadata: Map<String, String>,
    val id: UInt,
    val stages: UInt,
    @Transient val configs: List<StageConfig> = configsFromDisk(stages, id)
) {
    var completedStages: UInt = 0u
        private set
    var status: JobState = JobState.NEW
        private set
    private val initialSimSteps: UInt
    private var simSteps: UInt
    private var progress: UInt = 0u
    private val initialStageSimSteps: List<UInt>
    private val stageSimSteps: MutableList<UInt>
    private val stageProgress: MutableList<UInt>
    private val extensions: MutableList<UInt>
    private var error: String? = null

    @Transient
    private val mutex = Mutex()

    // has to be used for changing the status and process
    @Transient
    private val cancelMutex = Mutex()

    @Transient
    private var process: Process? = null

    @Transient
    val dir = baseDir(id)

    @Transient
    private val file = File(dir, jobFileName)

    @Transient
    val topFile = File(dir, topologyFileName)

    @Transient
    val startConfFile = File(dir, startConfFileName)

    @Transient
    val forcesFile = File(dir, forcesFileName)

    /**
     * Creates a new [SimJob] and writes all necessary files.
     */
    constructor(
        metadata: Map<String, String>,
        id: UInt,
        configs: List<StageConfig>,
        top: String,
        dat: String,
        forces: String
    ) : this(metadata, id, configs.size.toUInt(), configs) {
        // write files
        toDisk()
        topFile.writeText(top.replace("\r\n", "\n"))
        startConfFile.writeText(dat.replace("\r\n", "\n"))
        forcesFile.writeText(forces.replace("\r\n", "\n"))

        for (indexedStage in configs.withIndex()) {
            val i = indexedStage.index
            val stage = indexedStage.value

            val dir = File(dir, i.toString())
            dir.mkdirs()
            val stageFile = File(dir, stageFileName)
            stage.toJsonFile(stageFile)
        }
    }

    init {
        stageSimSteps = ArrayList(stages.toInt())
        stageProgress = ArrayList(stages.toInt())
        extensions = ArrayList(stages.toInt())
        initialStageSimSteps = buildList(stages.toInt()) {
            for (i in 0u..<stages) {
                val stageSteps = configs[i.toInt()].toPropertiesMap()["steps"]?.toUIntOrNull() ?: 0u
                this.add(stageSteps)
                stageSimSteps.add(stageSteps)
                stageProgress.add(0u)
                extensions.add(0u)
            }
        }
        initialSimSteps = stageSimSteps.sum()
        simSteps = initialSimSteps
    }

    /**
     * Determines the latest available conf [File].
     * Only completed stages are considered.
     *
     * @return a new [File] instance pointing to the latest known conf [File].
     */
    fun getLatestConfFile(): File {
        if (completedStages == 0u)
            return startConfFile

        val lastCompletedStageDir = File(dir, (completedStages - 1u).toString())
        return File(lastCompletedStageDir, endConfFileName)
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
        toDisk()
        Clients.propagateUpdate(this.id, this)

        while (completedStages < stages) {
            // stop execution if there was an error or the job was canceled
            if (cancelMutex.withLock { shouldStopExecution() }) break

            executeNextStage()
            toDisk()
            Clients.propagateUpdate(this.id, this)
        }

        // set DONE state if everything was fine
        cancelMutex.withLock {
            if (status != JobState.CANCELED)
                status = JobState.DONE
        }
        toDisk()
        Clients.propagateUpdate(this.id, this)

        log.info("Execution of the job with ID $id finished.")
    }

    /**
     * Small helper function that tests if this [SimJob] is [JobState.CANCELED],
     * [JobState.DONE] or has encountered an error.
     */
    private fun shouldStopExecution(): Boolean = status == JobState.CANCELED || status == JobState.DONE || error != null

    /**
     * Executes the next stage of this [SimJob].
     */
    private suspend fun executeNextStage() {
        log.debug("Running stage $completedStages of the job with ID $id.")

        val nextDir = File(dir, completedStages.toString())
        val stageFile = File(nextDir, stageFileName)
        val stageConfig = StageConfig.fromJsonFile(stageFile)
        val nextLogFile = File(nextDir, oxDnaLogFileName)
        val endConfFile = File(nextDir, endConfFileName)

        // catch any exception that might occur
        val success = try {
            prepareFilesForNextStage(nextDir, stageConfig)
            runSimulation(stageConfig.autoExtendStage, stageConfig.maxExtensions, nextDir, nextLogFile, endConfFile)
        } catch (e: Throwable) {
            log.error(e)
            error = error ?: e.message ?: "Exception of type \"${e::class.qualifiedName}\""
            false
        }

        if (success) {
            if (status != JobState.CANCELED)
                completedStages++
        } else {
            error = error ?: nextLogFile.useLines {
                it.firstOrNull { line -> line.startsWith("ERROR:") }?.substring("ERROR: ".length)
            } ?: "An unknown error occurred."

            // abort next stages
            cancelMutex.withLock {
                status = JobState.CANCELED
            }
        }

        log.debug("The execution of a stage of the job with the ID $id is completed.")
    }

    /**
     * Runs oxDNA in the specified [currentDir].
     * Expects all input files to already exist.
     * The simulation may be extended if necessary.
     *
     * @param autoExtendStage determines whether this execution should be automatically extended.
     * @param currentDir the directory in which the simulation will be run.
     * @param currentLogFile the log [File] for the simulation execution.
     * @param endConfFile the [File] to store the final configuration in.
     */
    private suspend fun runSimulation(
        autoExtendStage: Boolean,
        maxExtensions: UInt,
        currentDir: File,
        currentLogFile: File,
        endConfFile: File
    ): Boolean {
        val stageIndex = completedStages.toInt()

        // values for automatic stage extension
        val stepStates = LinkedList<StepState>()

        val stepsFromPreviousStages = stageSimSteps.subList(0, stageIndex).sum()

        // run oxDNA
        val pb = ProcessBuilder()
        pb.directory(currentDir)
        pb.command(listOf("oxDNA", inputFileName))
        // for some reason, oxDNA only writes the energy to std out and everything else to std error
        pb.redirectError(ProcessBuilder.Redirect.appendTo(currentLogFile))

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
        if (cancel) return false

        // we use the fact that the print interval of the trajectory and energy are the same to push detailed updates to clients
        withContext(Dispatchers.IO) {
            var line: String
            while (true) {
                // read custom observables line
                line = try {
                    energyStream?.readLine() ?: break
                } catch (_: IOException) {
                    break
                }

                val state = StepState.fromObservableLine(line)
                stepStates.add(state)
                if (stepStates.size > 5) // retain 5 states
                    stepStates.removeFirst()

                val stepsInCurrentStage = initialStageSimSteps[stageIndex] * extensions[stageIndex] + state.step
                updateProgress(stepsFromPreviousStages, stepsInCurrentStage, stageIndex, endConfFile)

                // read default observables line
                try {
                    energyStream.readLine()
                } catch (_: IOException) {
                }
            }
        }
        // make sure that the progress of the current stage is 100%,
        // even if the print interval doesn't fit the number of steps
        cancelMutex.withLock {
            if (status != JobState.CANCELED) {
                updateProgress(stepsFromPreviousStages, stageSimSteps[stageIndex], stageIndex, endConfFile)
            }
        }

        // blocks job execution scope located in the Jobs object
        // intended behavior, as this prevents multiple jobs from running in parallel
        val exitCode = process?.waitFor()

        var success = exitCode == 0

        // run extension only if the previous run was successful, extensions are allowed,
        // and there have been less than 200 so far
        if (success && autoExtendStage && extensions[stageIndex] < maxExtensions) {
            val potentialEnergyChange =
                (stepStates.first().potentialEnergy - stepStates.last().potentialEnergy).absoluteValue
            val distinctStretchedBonds = stepStates.mapTo(HashSet()) { it.stretchedBonds }.size
            if (// the potential energy still changes a lot
                potentialEnergyChange > 0.01f
                // the number of stretched bonds is still changing
                || distinctStretchedBonds > 1
            ) {
                extensions[stageIndex]++
                simSteps += initialStageSimSteps[stageIndex]
                stageSimSteps[stageIndex] += initialStageSimSteps[stageIndex]
                log.debug(
                    "Running extension {}. Potential Energy change: {}; Distinct Stretched Bonds: {}",
                    extensions[stageIndex], potentialEnergyChange, distinctStretchedBonds
                )
                Clients.propagateUpdate(id, this)
                prepareFilesForExtension(currentDir)
                success = runSimulation(true, maxExtensions, currentDir, currentLogFile, endConfFile)
            }
        }

        return success
    }

    /**
     * Updates the progress of this [SimJob] and propagates a detailed update.
     *
     * @param stepsFromPreviousStages the steps completed in previous stages.
     * @param stepsInCurrentStage the steps completed in the current stage.
     * @param stageIndex the index of the current stage.
     * @endConfFile the endConf [File] of the current stage.
     */
    private suspend fun updateProgress(
        stepsFromPreviousStages: UInt,
        stepsInCurrentStage: UInt,
        stageIndex: Int,
        endConfFile: File
    ) {
        progress = stepsFromPreviousStages + stepsInCurrentStage
        stageProgress[stageIndex] = stepsInCurrentStage
        val currentConf = try {
            endConfFile.readText()
        } catch (_: Throwable) {
            ""
        }
        if (currentConf.isBlank())
            log.debug("Read empty configuration file. Ignoring it.")
        else
            Clients.propagateDetailedUpdate(this@SimJob, currentConf)
    }

    /**
     * Copies the files from the previous stage (or initial files for stage 0) and creates the oxDNA input file.
     *
     * @param nextDir the directory to prepare.
     * @param stageConfig the [StageConfig] of the stage for which the files are to be prepared.
     */
    private fun prepareFilesForNextStage(nextDir: File, stageConfig: StageConfig) {
        // prepare input file
        val inputFile = File(nextDir, inputFileName)
        stageConfig.toPropertiesFile(inputFile)

        // get and copy conf file of last stage
        val oldConfFile =
            if (completedStages == 0u) {
                startConfFile
            } else {
                val oldDir = File(dir, (completedStages - 1u).toString())
                File(oldDir, endConfFileName)
            }
        val startConfFile = File(nextDir, startConfFileName)
        oldConfFile.copyTo(startConfFile, true)
    }

    /**
     * Copies the files from the previous execution of the current stage.
     *
     * @param currentDir the directory to prepare.
     */
    private fun prepareFilesForExtension(currentDir: File) {
        // get and copy conf file from the last run
        val oldConfFile = File(currentDir, endConfFileName)
        val startConfFile = File(currentDir, startConfFileName)
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
    private fun toDisk() {
        dir.mkdirs()
        file.outputStream().use { prettyJson.encodeToStream(this, it) }
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
         * Calculates the base directory for a [SimJob] with the given ID.
         *
         * @param id the ID of the [SimJob] whose base directory is to be calculated.
         */
        fun baseDir(id: UInt) = File(Environment.dataDir, id.toString())

        /**
         * Loads the stage configuration files from disk.
         *
         * @param stages the number of stages.
         * @param id the ID of the [SimJob].
         */
        private fun configsFromDisk(stages: UInt, id: UInt): List<StageConfig> = buildList(stages.toInt()) {
            for (i in 0u..<stages) {
                val stageDir = File(baseDir(id), i.toString())
                val stageFile = File(stageDir, stageFileName)
                this.add(StageConfig.fromJsonFile(stageFile))
            }
        }

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
        return "SimJob(id=$id, stages=$stages, completedSteps=$completedStages, status=$status, progress=$progress, error=$error)"
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

/**
 * Represents some interesting metrics about the structure after a specific step.
 */
data class StepState(val step: UInt, val potentialEnergy: Float, val stretchedBonds: UInt) {
    companion object {

        /**
         * Extracts the needed metrics from the given observables line.
         *
         * @param line a single observables line.
         *
         * @return a new [StepState] instance.
         */
        fun fromObservableLine(line: String): StepState {
            val observables = line.trim().split("\\s+".toRegex())
            return StepState(observables[0].toUInt(), observables[1].toFloat(), observables[2].toUInt())
        }
    }
}
