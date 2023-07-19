package dnaforge.backend.sim

import dnaforge.backend.endConfFileName
import dnaforge.backend.energyFileName
import dnaforge.backend.forcesFileName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import dnaforge.backend.prettyJson
import dnaforge.backend.startConfFileName
import dnaforge.backend.topologyFileName
import dnaforge.backend.trajectoryFileName
import java.io.File

/**
 * Default relaxation config using simulation type [MinConfig].
 */
private val min: StepConfig = ManualConfig(
    steps = 2000u,
    dt = 0.005f,
    temperature = 30.0f,
    maxBackboneForce = 5.0f,
    maxBackboneForceFar = 10.0f,
    verletSkin = 0.15f,
    externalForces = true,
    printInterval = 100u,
    simType = MinConfig
)

/**
 * Default relaxation config using simulation type [McConfig] and [RelaxInteraction].
 */
private val mcRelax: StepConfig = ManualConfig(
    steps = 100u,
    dt = 0.00001f,
    temperature = 30.0f,
    maxBackboneForce = 5.0f,
    maxBackboneForceFar = 10.0f,
    verletSkin = 0.5f,
    externalForces = true,
    printInterval = 10u,
    simType = McConfig(
        deltaTranslation = 0.1f,
        deltaRotation = 0.1f,
        interactionType = RelaxInteraction(
            relaxInteractionType = RelaxInteractionType.DNA_RELAX,
            relaxType = RelaxType.CONSTANT_FORCE,
            relaxStrength = 1.0f
        ),
        thermostat = BrownianThermostat(
            newtonianSteps = 103u,
            pt = 0.99f,
            diffCoeff = 2.5f
        )
    )
)

/**
 * Default relaxation config using simulation type [MdConfig] and [RelaxInteraction].
 */
private val mdRelax: StepConfig = ManualConfig(
    steps = 1e3.toUInt(),
    dt = 0.005f,
    temperature = 30.0f,
    maxBackboneForce = 5.0f,
    maxBackboneForceFar = 10.0f,
    verletSkin = 0.5f,
    externalForces = true,
    printInterval = 100u,
    simType = MdConfig(
        interactionType = RelaxInteraction(
            relaxInteractionType = RelaxInteractionType.DNA_RELAX,
            relaxType = RelaxType.CONSTANT_FORCE,
            relaxStrength = 100.0f
        ),
        thermostat = BrownianThermostat(
            newtonianSteps = 1u,
            pt = 0.99f,
            diffCoeff = 2.5f
        )
    )
)

/**
 * Default simulation config using simulation type [MdConfig] and [V1InteractionType.DNA].
 */
private val mdSim: StepConfig = ManualConfig(
    steps = 1e6.toUInt(), // was 1e8
    dt = 0.005f,
    temperature = 30.0f,
    maxBackboneForce = 5.0f,
    maxBackboneForceFar = 10.0f,
    verletSkin = 0.05f,
    externalForces = false,
    printInterval = 10000u,
    simType = MdConfig(
        interactionType = V1Interaction(
            v1InteractionType = V1InteractionType.DNA
        ),
        thermostat = BrownianThermostat(
            newtonianSteps = 103u,
            pt = 0.99f,
            diffCoeff = 2.5f
        )
    )
)

/**
 * Default step list.
 */
val default: List<StepConfig> = listOf(min, mcRelax, mdRelax, mdSim)


/**
 * Super class for all types of configurations that can be used to configure a simulation or relaxation step.
 */
@Serializable
sealed class StepConfig {

    /**
     * Writes this [StepConfig] to the specified JSON [File].
     *
     * @param file the file to write to. Existing content is overwritten.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun toJsonFile(file: File) = file.outputStream().use { prettyJson.encodeToStream(this, it) }

    /**
     * Encodes this [StepConfig] into a [Map].
     *
     * @return a new [Map] containing the properties of this [StepConfig].
     */
    protected abstract fun encodeToMap(): Map<String, String>

    /**
     * Uses [encodeToMap] and adds some important default values.
     *
     * @return a new [Map] containing the properties of this [StepConfig] and some default values.
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
    }

    /**
     * Writes this [StepConfig] to the specified properties [File].
     *
     * @param file the file to write to. Existing content is overwritten.
     */
    fun toPropertiesFile(file: File) =
        file.writeText(getParameterMap().entries.joinToString("\n") { (key, value) -> "$key = $value" })

    companion object {

        /**
         * Reads a [StepConfig] from the specified JSON [File].
         *
         * @param file the [File] to read from.
         *
         * @return a new [StepConfig] instance.
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJsonFile(file: File): StepConfig = file.inputStream().use { prettyJson.decodeFromStream(it) }
    }
}

/**
 * [FileConfig] is a type of [StepConfig] and is to be used to represent a pre-made oxDNA input file.
 */
@Serializable
@SerialName("FileConfig")
data class FileConfig(val content: String) : StepConfig() {

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
 * This interface exists to enforce that the [encodeToMap] function exists for all parts of a [StepConfig].
 */
@Serializable
sealed interface SubConfig {

    /**
     * Encodes this [SubConfig] into a [Map].
     *
     * @return a new [Map] containing the properties of this [SubConfig].
     */
    fun encodeToMap(): Map<String, String>
}

/**
 * Specifies the simulation type.
 */
@Serializable
sealed interface SimTypeConfig : SubConfig

/**
 * Specifies the type of particle interaction for some simulation types.
 */
@Serializable
sealed interface InteractionTypeConfig : SubConfig {

    /**
     * Whether this interaction type allows the use of CUDA.
     */
    val allowsCuda: Boolean
}

/**
 * Specifies the type of thermostat for some simulation types.
 */
@Serializable
sealed interface ThermostatConfig : SubConfig

/**
 * [ManualConfig] is a type of [StepConfig]
 * and is to be used to represent an oxDNA input file with a permissive set of options.
 */
@Serializable
@SerialName("ManualConfig")
data class ManualConfig(
    val steps: UInt,
    val dt: Float,
    val temperature: Float,
    val maxBackboneForce: Float,
    val maxBackboneForceFar: Float,
    val verletSkin: Float,
    val externalForces: Boolean,
    val printInterval: UInt,
    val simType: SimTypeConfig
) : StepConfig() {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this.putAll(simType.encodeToMap())

        this["steps"] = steps.toString()
        this["dt"] = dt.toString()
        this["T"] = "${temperature}C"
        this["max_backbone_force"] = maxBackboneForce.toString()
        this["max_backbone_force_far"] = maxBackboneForceFar.toString()
        this["verlet_skin"] = verletSkin.toString()
        this["external_forces"] = externalForces.toString()
        this["print_conf_interval"] = printInterval.coerceAtLeast(1u).toString()
        this["print_energy_every"] = printInterval.coerceAtLeast(1u).toString()

        // some default options
        this["time_scale"] = "linear"
        this["restart_step_counter"] = true.toString()
        this["refresh_vel"] = true.toString()
        this["trajectory_print_momenta"] = true.toString()
        this["rcut"] = 2.0f.toString()
    }
}

/**
 * Type of simulation that attempts to minimize potential energy.
 */
@Serializable
@SerialName("MinConfig")
data object MinConfig : SimTypeConfig {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this["sim_type"] = SimType.MIN.configName
        this["interaction_type"] = V1InteractionType.DNA.configName
        this["backend"] = "CPU"
    }
}

/**
 * Monte Carlo simulation.
 */
@Serializable
@SerialName("McConfig")
data class McConfig(
    val deltaTranslation: Float,
    val deltaRotation: Float,
    val interactionType: InteractionTypeConfig,
    val thermostat: ThermostatConfig
) : SimTypeConfig {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this.putAll(interactionType.encodeToMap())
        this.putAll(thermostat.encodeToMap())

        this["sim_type"] = SimType.MC.configName
        this["backend"] = "CPU"
        this["ensemble"] = "nvt"

        this["delta_translation"] = deltaTranslation.toString()
        this["delta_rotation"] = deltaRotation.toString()
    }
}

/**
 * Molecular dynamics simulation.
 */
@Serializable
@SerialName("MdConfig")
data class MdConfig(
    val interactionType: InteractionTypeConfig,
    val thermostat: ThermostatConfig
) : SimTypeConfig {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this.putAll(interactionType.encodeToMap())
        this.putAll(thermostat.encodeToMap())

        this["sim_type"] = SimType.MD.configName
        this["backend"] = if (interactionType.allowsCuda) "CUDA" else "CPU"
    }
}

/**
 * Specifies relaxation interaction properties.
 */
@Serializable
@SerialName("RelaxInteraction")
data class RelaxInteraction(
    val relaxInteractionType: RelaxInteractionType,
    val relaxType: RelaxType,
    val relaxStrength: Float
) : InteractionTypeConfig {
    override val allowsCuda: Boolean = false

    override fun encodeToMap(): Map<String, String> = buildMap {
        this["interaction_type"] = relaxInteractionType.configName
        this["relax_type"] = relaxType.configName
        this["relax_strength"] = relaxStrength.toString()
    }
}

/**
 * Specifies v1 interaction properties.
 */
@Serializable
@SerialName("V1Interaction")
data class V1Interaction(
    val v1InteractionType: V1InteractionType
) : InteractionTypeConfig {
    override val allowsCuda: Boolean = true

    override fun encodeToMap(): Map<String, String> = buildMap {
        this["interaction_type"] = v1InteractionType.configName
    }
}

/**
 * Specifies v2 interaction properties.
 */
@Serializable
@SerialName("V2Interaction")
data class V2Interaction(
    val v2InteractionType: V2InteractionType,
    val saltConcentration: Float
) : InteractionTypeConfig {
    override val allowsCuda: Boolean = true

    override fun encodeToMap(): Map<String, String> = buildMap {
        this["interaction_type"] = v2InteractionType.configName
        this["salt_concentration"] = saltConcentration.toString()
    }
}


/**
 * Specifies Brownian thermostat properties.
 */
@Serializable
@SerialName("BrownianThermostat")
data class BrownianThermostat(
    val newtonianSteps: UInt,
    val pt: Float,
    val diffCoeff: Float
) : ThermostatConfig {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this["thermostat"] = "brownian"
        this["newtonian_steps"] = newtonianSteps.toString()
        this["pt"] = pt.toString()
        this["diff_coeff"] = diffCoeff.toString()
    }
}

/**
 * Supported simulation types.
 */
enum class SimType(val configName: String) {
    MIN("min"),
    MC("MC"),
    MD("MD")
}

/**
 * Supported relaxation types.
 */
enum class RelaxInteractionType(val configName: String) {
    DNA_RELAX("DNA_relax"),
    RNA_RELAX("RNA_relax")
}

/**
 * Supported v1 interaction types.
 */
enum class V1InteractionType(val configName: String) {
    DNA("DNA"),
    RNA("RNA"),
}

/**
 * Supported v2 interaction types.
 */
enum class V2InteractionType(val configName: String) {
    DNA2("DNA2"),
    RNA2("RNA2"),
}

enum class RelaxType(val configName: String) {
    CONSTANT_FORCE("constant_force"),
    HARMONIC_FORCE("harmonic_force")
}
