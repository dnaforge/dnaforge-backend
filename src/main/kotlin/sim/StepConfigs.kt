package sim

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import prettyJson
import java.io.File

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

val default: List<StepConfig> = listOf(min, mcRelax, mdRelax, mdSim)


@Serializable
sealed class StepConfig {

    private fun toParameterMap(): Map<String, String> = buildMap {
        this.putAll(encodeToMap())

        // input
        this["topology"] = topologyFileName
        this["conf_file"] = startConfFileName
        if (this["external_forces"].toBoolean() || this["external_forces"]?.toIntOrNull() == 1)
            this["external_forces_file"] = forcesFileName

        // output
        this["lastconf_file"] = endConfFileName
        this["trajectory_file"] = trajectoryFileName
        this["energy_file"] = energyFileName
        this["max_io"] = "100.0"
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun toFile(file: File) = file.outputStream().use { prettyJson.encodeToStream(this, it) }

    fun toPropertiesFile(file: File) =
        file.writeText(toParameterMap().entries.joinToString("\n") { (key, value) -> "$key = $value" })

    protected abstract fun encodeToMap(): Map<String, String>

    companion object {
        const val inputFileName = "input.properties"
        const val topologyFileName = "../topology.top"
        const val startConfFileName = "conf_start.dat"
        const val forcesFileName = "../forces.forces"

        const val endConfFileName = "conf_end.dat"
        const val trajectoryFileName = "trajectory.dat"
        const val energyFileName = "energy.dat"

        @OptIn(ExperimentalSerializationApi::class)
        fun fromFile(file: File): StepConfig = file.inputStream().use { prettyJson.decodeFromStream(it) }
    }
}


@Serializable
sealed interface SubConfig {
    fun encodeToMap(): Map<String, String>
}

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

@Serializable
sealed interface SimTypeConfig : SubConfig

@Serializable
sealed interface InteractionTypeConfig : SubConfig {
    val allowsCuda: Boolean
}

@Serializable
sealed interface ThermostatConfig : SubConfig

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
        this["trajectory_print_momenta"] = true.toString()
        this["rcut"] = 2.0f.toString()
    }
}

@Serializable
@SerialName("MinConfig")
data object MinConfig : SimTypeConfig {
    override fun encodeToMap(): Map<String, String> = buildMap {
        this["sim_type"] = SimType.MIN.configName
        this["interaction_type"] = V1InteractionType.DNA.configName
        this["backend"] = "CPU"
    }
}

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

@Serializable
@SerialName("MdConfig")
data class MdConfig(
    val interactionType: InteractionTypeConfig,
    val thermostat: ThermostatConfig
) : SimTypeConfig {

    override fun encodeToMap(): Map<String, String> = buildMap {
        this.putAll(interactionType.encodeToMap())
        this.putAll(thermostat.encodeToMap())

        this["sim_type"] = SimType.MC.configName
        this["backend"] = if (interactionType.allowsCuda) "CUDA" else "CPU"
        this["ensemble"] = "nvt"
    }
}

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


enum class SimType(val configName: String) {
    MIN("min"),
    MC("MC"),
    MD("MD")
}

enum class RelaxInteractionType(val configName: String) {
    DNA_RELAX("DNA_relax"),
    RNA_RELAX("RNA_relax")
}

enum class V1InteractionType(val configName: String) {
    DNA("DNA"),
    RNA("RNA"),
}

enum class V2InteractionType(val configName: String) {
    DNA2("DNA2"),
    RNA2("RNA2"),
}

enum class RelaxType(val configName: String) {
    CONSTANT_FORCE("constant_force"),
    HARMONIC_FORCE("harmonic_force")
}
