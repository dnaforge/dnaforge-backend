package dnaforge.backend.sim

import dnaforge.backend.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

/**
 * Default stage list.
 */
val default: List<StageConfig> = listOf(
    ManualConfig(true, ManualStageOptions.min),
    ManualConfig(true, ManualStageOptions.mcRelax),
    ManualConfig(true, ManualStageOptions.mdRelax),
    ManualConfig(false, ManualStageOptions.mdSim)
)


/**
 * Super class for all types of configurations that can be used to configure a simulation or relaxation stage.
 */
@Serializable
sealed class StageConfig {
    abstract val autoExtendStage: Boolean

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
    fun getParameterMap(): Map<String, String> = buildMap {
        this.putAll(encodeToMap())

        // input
        this["topology"] = "../$topologyFileName"
        this["conf_file"] = startConfFileName
        if (this["external_forces"].toBoolean() || this["external_forces"]?.toIntOrNull() == 1)
            this["external_forces_file"] = "../$forcesFileName"

        // output
        this["lastconf_file"] = endConfFileName
        this["trajectory_file"] = trajectoryFileName
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
        file.writeText(getParameterMap().entries.joinToString("\n") { (key, value) -> "$key = $value" })

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
data class FileConfig(override val autoExtendStage: Boolean, val content: String) : StageConfig() {

    override fun encodeToMap(): Map<String, String> = buildMap {
        content.lineSequence()
            .map { it.trim() } // ignore leading or trailing spaces
            .filter { it.isNotBlank() && !it.startsWith('#') } // filter blank and comment lines
            .mapNotNull { // split
                val pos = it.indexOf('=')
                if (pos == -1) null
                else Pair(it.substring(0, pos).trimEnd(), it.substring(pos + 1).trimStart())
            }
            .forEach { // add to map
                this[it.first] = it.second
            }
    }
}

/**
 * [ManualConfig] is a type of [StageConfig]
 * and is to be used to represent an oxDNA input file with a permissive set of options.
 */
@Serializable
@SerialName("ManualConfig")
data class ManualConfig(override val autoExtendStage: Boolean, val options: SelectedOption) : StageConfig() {

    override fun encodeToMap(): Map<String, String> = options.encodeToMap()
}
