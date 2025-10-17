package dnaforge.backend.sim

import dnaforge.backend.*
import dnaforge.backend.sim.StageConfigs.log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object StageConfigs {
    val log: Logger = LoggerFactory.getLogger(StageConfigs::class.java)

    /**
     * Default stage list.
     */
    val default: List<StageConfig> = listOf(
        ManualConfig(
            mapOf(
                "title" to "First Relaxation Stage",
                "description" to "Relaxes the structure using a type of potential energy minimization."
            ),
            true,
            true,
            20u,
            ManualStageOptions.minRelax
        ),
        ManualConfig(
            mapOf(
                "title" to "Second Relaxation Stage",
                "description" to "Relaxes the structure with a short Monte Carlo simulation."
            ),
            true,
            true,
            20u,
            ManualStageOptions.mcRelax
        ),
        ManualConfig(
            mapOf(
                "title" to "Third Relaxation Stage",
                "description" to "Relaxes the structure with a short molecular dynamics simulation with a very small ΔT."
            ),
            true,
            false,
            0u,
            ManualStageOptions.mdRelax
        ),
        ManualConfig(
            mapOf(
                "title" to "Simulation Stage",
                "description" to "Simulates the relaxed structure with a molecular dynamics simulation."
            ),
            true,
            false,
            0u,
            ManualStageOptions.mdSim
        )
    )
}


/**
 * Super class for all types of configurations that can be used to configure a simulation or relaxation stage.
 */
@Serializable
sealed class StageConfig {
    abstract val metadata: Map<String, String>
    abstract val createTrajectory: Boolean
    abstract val autoExtendStage: Boolean
    abstract val maxExtensions: UInt

    /**
     * Writes this [StageConfig] to the specified JSON [File].
     *
     * @param file the file to write to. Existing content is overwritten.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun toJsonFile(file: File) = file.outputStream().use { prettyJson.encodeToStream(this, it) }

    /**
     * Encodes this [StageConfig] into a [Map].
     *
     * @return a new [Map] containing the properties of this [StageConfig].
     */
    protected abstract fun encodeToMap(): Map<String, String>

    /**
     * Uses [encodeToMap] and adds some important default values.
     *
     * @return a new [Map] containing the properties of this [StageConfig] and some default values.
     */
    fun toPropertiesMap(): Map<String, String> = buildMap {
        putAll(encodeToMap())

        // input
        this["topology"] = "../$topologyFileName"
        this["conf_file"] = startConfFileName
        if (this["external_forces"].toBoolean() || this["external_forces"]?.toIntOrNull() == 1)
            this["external_forces_file"] = "../$forcesFileName"

        // output
        this["lastconf_file"] = endConfFileName
        this["trajectory_file"] = if (createTrajectory) trajectoryFileName else "/dev/null"
        this["energy_file"] = energyFileName
        this["max_io"] = "100.0"
        this["data_output_1"] = """{
	name = stdout
	linear = true
	print_every = ${this["print_energy_every"]}
	col_1 = {
		type = step
		units = steps
	}
	col_2 = {
		type = potential_energy
		split = false
	}
	col_3 = {
		type = stretched
		print_list = false
	}
}"""
    }

    /**
     * Writes this [StageConfig] to the specified properties [File].
     *
     * @param file the file to write to. Existing content is overwritten.
     */
    fun toPropertiesFile(file: File) =
        file.writeText(toPropertiesMap().entries.joinToString("\n") { (key, value) -> "$key = $value" })

    companion object {

        /**
         * Reads a [StageConfig] from the specified JSON [File].
         *
         * @param file the [File] to read from.
         *
         * @return a new [StageConfig] instance.
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJsonFile(file: File): StageConfig = file.inputStream().use { prettyJson.decodeFromStream(it) }
    }
}

/**
 * [FileConfig] is a type of [StageConfig] and is to be used to represent a pre-made oxDNA input file.
 */
@Serializable
@SerialName("FileConfig")
data class FileConfig(
    override val metadata: Map<String, String>,
    override val createTrajectory: Boolean,
    override val autoExtendStage: Boolean,
    override val maxExtensions: UInt,
    val content: String
) : StageConfig() {

    override fun encodeToMap(): Map<String, String> = buildMap {
        val interestingLines = content.lines()
            .filter { it.isNotBlank() && !it.startsWith('#') } // ignore blank and comment lines

        var blockStartLine: Int? = null
        var blockStartIndex = 0
        var blockKey = ""
        var blockLevel = 0
        for (i in interestingLines.indices) {
            val line = interestingLines[i]

            if (blockStartLine != null) { // in block: search for an end
                val levelChange = countBrackets(line)
                blockLevel += levelChange

                if (blockLevel <= 0) { // found end
                    val startLine = interestingLines[blockStartLine]
                    val value = (startLine.substring(blockStartIndex + 1) +
                            interestingLines.subList(blockStartLine + 1, i + 1).joinToString("\n")
                            ).trim()
                    this[blockKey] = value

                    blockStartLine = null
                }
            } else { // not in block
                val pos = line.indexOf('=')
                if (pos == -1) continue
                val key = line.take(pos).trim()
                val value = line.substring(pos + 1).trim()
                val levelChange = countBrackets(value)

                if (levelChange <= 0) {
                    this[key] = value
                } else { // block starts in this line
                    blockStartLine = i
                    blockStartIndex = pos
                    blockKey = key
                    blockLevel = levelChange
                }
            }
        }
    }

    private fun countBrackets(text: String): Int =
        text.fold(0) { acc, c -> if (c == '{') acc + 1 else if (c == '}') acc - 1 else acc }
}

/**
 * [ManualConfig] is a type of [StageConfig]
 * and is to be used to represent an oxDNA input file with a permissive set of properties.
 */
@Serializable
@SerialName("ManualConfig")
data class ManualConfig(
    override val metadata: Map<String, String>,
    override val createTrajectory: Boolean,
    override val autoExtendStage: Boolean,
    override val maxExtensions: UInt,
    val properties: Set<SelectedProperty>
) : StageConfig() {

    private fun collectAllProperties(
        propertiesLeft: MutableMap<String, SelectedProperty>,
        level: Entry
    ): Map<String, String> = when (level) {
        is Option -> buildMap {
            putAll(level.fixedProperties)

            val backend = mutableSetOf<String?>()
            backend.add(this["backend"])

            level.entries.forEach { entry ->
                putAll(collectAllProperties(propertiesLeft, entry))
                backend.add(this["backend"])
            }

            // if CUDA support is not available, CUDA will not be used
            // only if all options that include the backend property agree to use CUDA, CUDA will be used
            if (!Environment.cuda || backend.contains("CPU"))
                this["backend"] = "CPU"
        }

        is OptionContainer -> {
            val prop = propertiesLeft.remove(level.name)
                ?: log.throwError(IllegalArgumentException("Expected Property named \"${level.name}\"."))

            level.values.firstOrNull { it.name == prop.value }
                ?.run { collectAllProperties(propertiesLeft, this) }
                ?: log.throwError(IllegalArgumentException("Unknown Option named \"${prop.value}\"."))
        }

        is Property -> {
            val prop = propertiesLeft.remove(level.name)
                ?: log.throwError(IllegalArgumentException("Expected Property named \"${level.name}\"."))

            // validate data type
            val valueWithSuffix = when (level.valueType) {
                ValueType.BOOLEAN ->
                    prop.value.toBooleanStrictOrNull()?.toString()
                        ?: log.throwError(IllegalArgumentException("Expected boolean as value. Got \"${prop.value}\"."))

                ValueType.UNSIGNED_INTEGER ->
                    prop.value.toUIntOrNull()?.toString()
                        ?: log.throwError(IllegalArgumentException("Expected unsigned integer as value. Got \"${prop.value}\"."))

                ValueType.FLOAT ->
                    prop.value.toFloatOrNull()?.toString()
                        ?: log.throwError(IllegalArgumentException("Expected float as value. Got \"${prop.value}\"."))
            } + level.suffix


            // write value for all config names
            level.configNames.associateWith { valueWithSuffix }
        }
    }

    override fun encodeToMap(): Map<String, String> =
        collectAllProperties(properties.associateByTo(mutableMapOf()) { it.name }, ManualStageOptions.availableOptions)
}
