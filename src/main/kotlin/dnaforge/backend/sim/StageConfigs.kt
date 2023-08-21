package dnaforge.backend.sim

import dnaforge.backend.*
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
            ManualStageOptions.min
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
            true,
            20u,
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

    val defaultFiles: List<StageConfig> = default.map {
        when (it) {
            is FileConfig -> it

            is ManualConfig -> FileConfig(
                it.metadata,
                it.createTrajectory,
                it.autoExtendStage,
                it.maxExtensions,
                it.toPropertiesMap().entries.joinToString("\n") { (key, value) -> "$key = $value" }
            )

            is PropertiesConfig -> it
        }
    }

    val defaultProperties: List<StageConfig> = default.map {
        when (it) {
            is FileConfig -> log.throwError(IllegalArgumentException("FileConfigs cannot currently be converted to PropertiesConfigs."))

            is ManualConfig -> PropertiesConfig(
                it.metadata,
                it.createTrajectory,
                it.autoExtendStage,
                it.maxExtensions,
                enhanceSimpleListProperties(getSelectedPropertiesAsSimpleListProperties(it.options))
            )

            is PropertiesConfig -> PropertiesConfig(
                it.metadata,
                it.createTrajectory,
                it.autoExtendStage,
                it.maxExtensions,
                enhanceSimpleListProperties(it.properties)
            )
        }
    }

    /**
     * Makes sure that every available property also appears in the default properties.
     */
    private fun enhanceSimpleListProperties(properties: Set<SimpleListProperty>): Set<SimpleListProperty> {
        val map = properties.associateBy { it.name }
        return ManualStageOptions.availableProperties.mapTo(mutableSetOf()) { map[it.name] ?: it }
    }

    /**
     * Recursively collects all selected Properties in the given [SelectedOption].
     */
    private fun getSelectedPropertiesAsSimpleListProperties(
        option: SelectedOption,
        set: MutableSet<SimpleListProperty> = mutableSetOf()
    ): MutableSet<SimpleListProperty> {
        option.entries.forEach { entry ->
            val related = ManualStageOptions.availableProperties.firstOrNull { it.name == entry.name } ?: return set

            when (entry) {
                is SelectedProperty ->
                    set.add(SimpleListProperty(entry.name, related.valueType, related.possibleValues, entry.value))

                is SelectedOptionContainer -> {
                    set.add(SimpleListProperty(entry.name, related.valueType, related.possibleValues, entry.value.name))
                    getSelectedPropertiesAsSimpleListProperties(entry.value, set)
                }

                is SelectedOption -> getSelectedPropertiesAsSimpleListProperties(entry, set)
            }
        }

        return set
    }
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
        this.putAll(encodeToMap())

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
                val key = line.substring(0, pos).trim()
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
 * and is to be used to represent an oxDNA input file with a permissive set of options.
 */
@Serializable
@SerialName("ManualConfig")
data class ManualConfig(
    override val metadata: Map<String, String>,
    override val createTrajectory: Boolean,
    override val autoExtendStage: Boolean,
    override val maxExtensions: UInt,
    val options: SelectedOption
) : StageConfig() {

    override fun encodeToMap(): Map<String, String> = options.encodeToMap()
}

/**
 * [PropertiesConfig] is a type of [StageConfig]
 * and is to be used to represent an oxDNA input file with a permissive set of properties.
 */
@Serializable
@SerialName("PropertiesConfig")
data class PropertiesConfig(
    override val metadata: Map<String, String>,
    override val createTrajectory: Boolean,
    override val autoExtendStage: Boolean,
    override val maxExtensions: UInt,
    val properties: Set<SimpleListProperty>
) : StageConfig() {

    override fun encodeToMap(): Map<String, String> =
        ManualStageOptions.simpleListPropertiesToSelectedOption(properties).encodeToMap()
}
