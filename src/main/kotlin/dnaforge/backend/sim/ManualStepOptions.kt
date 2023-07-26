package dnaforge.backend.sim

import dnaforge.backend.error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object ManualStepOptions {
    val log: Logger = LoggerFactory.getLogger(ManualStepOptions::class.java)


    /*
     * Strings
     */

    private const val MANUAL_CONFIG = "Manual Config"

    private const val SIMULATION_TYPE = "Simulation Type"
    private const val POTENTIAL_ENERGY_MINIMIZATION = "Potential Energy Minimization"
    private const val MONTE_CARLO_SIMULATION = "Monte Carlo Simulation"
    private const val MOLECULAR_DYNAMICS_SIMULATION = "Molecular Dynamics Simulation"

    private const val INTERACTION_TYPE = "Interaction Type"
    private const val RELAXATION = "Relaxation"
    private const val V1 = "v1"
    private const val V2 = "v2"

    private const val NUCLEI_ACID = "Nuclei Acid"
    private const val DNA = "DNA"
    private const val RNA = "RNA"

    private const val RELAXATION_FORCE = "Relaxation Force"
    private const val CONSTANT_FORCE = "Constant Force"
    private const val HARMONIC_FORCE = "Harmonic Force"

    private const val THERMOSTAT = "Thermostat"
    private const val BROWNIAN_THERMOSTAT = "Brownian Thermostat"


    private const val STEPS = "Steps"
    private const val D_T = "ΔT"
    private const val TEMPERATURE = "Temperature (°C)"
    private const val MAX_BACKBONE_FORCE = "Max. Backbone Force"
    private const val MAX_BACKBONE_FORCE_FAR = "Max. Backbone Force Far"
    private const val VERLET_SKIN = "Verlet Skin"
    private const val EXTERNAL_FORCES = "External Forces"
    private const val PRINT_INTERVAL = "Print Interval"
    private const val D_TRANSLATION = "ΔTranslation"
    private const val D_ROTATION = "ΔRotation"
    private const val RELAX_STRENGTH = "Relax Strength"
    private const val NEWTONIAN_STEPS = "Newtonian Steps"
    private const val PARTICLE_MOMENTUM_REFRESH_PROBABILITY = "Particle-Momentum-Refresh-Probability"
    private const val BASE_DIFFUSION_COEFFICIENT = "Base Diffusion Coefficient"


    /*
     * Default Options
     */

    /**
     * Default relaxation configuration using the _min_ simulation type.
     */
    val min: SelectedOption = SelectedOption(
        MANUAL_CONFIG, listOf(
            SelectedProperty(STEPS, "2000"),
            SelectedProperty(D_T, "0.005"),
            SelectedProperty(TEMPERATURE, "30.0"),
            SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
            SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
            SelectedProperty(VERLET_SKIN, "0.15"),
            SelectedProperty(EXTERNAL_FORCES, "true"),
            SelectedProperty(PRINT_INTERVAL, "100"),
            SelectedPropertyContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    POTENTIAL_ENERGY_MINIMIZATION, listOf()
                )
            )
        )
    )

    /**
     * Default relaxation configuration using simulation type _MC_ and _DNA_relax_ interaction.
     */
    val mcRelax: SelectedOption = SelectedOption(
        MANUAL_CONFIG, listOf(
            SelectedProperty(STEPS, "100"),
            SelectedProperty(D_T, "0.00001"),
            SelectedProperty(TEMPERATURE, "30.0"),
            SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
            SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
            SelectedProperty(VERLET_SKIN, "0.5"),
            SelectedProperty(EXTERNAL_FORCES, "true"),
            SelectedProperty(PRINT_INTERVAL, "10"),
            SelectedPropertyContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    MONTE_CARLO_SIMULATION, listOf(
                        SelectedProperty(
                            D_TRANSLATION,
                            "0.1"
                        ),
                        SelectedProperty(
                            D_ROTATION,
                            "0.1"
                        ),
                        SelectedPropertyContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                RELAXATION, listOf(
                                    SelectedProperty(
                                        RELAX_STRENGTH,
                                        "1.0"
                                    ),
                                    SelectedPropertyContainer(
                                        NUCLEI_ACID,
                                        SelectedOption(DNA, listOf())
                                    ),
                                    SelectedPropertyContainer(
                                        RELAXATION_FORCE,
                                        SelectedOption(CONSTANT_FORCE, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            THERMOSTAT,
                            SelectedOption(
                                BROWNIAN_THERMOSTAT, listOf(
                                    SelectedProperty(NEWTONIAN_STEPS, "103"),
                                    SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
                                    SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    /**
     * Default relaxation configuration using simulation type _MD_ and _DNA_relax_ interaction.
     */
    val mdRelax: SelectedOption = SelectedOption(
        MANUAL_CONFIG, listOf(
            SelectedProperty(STEPS, 1e3.toUInt().toString()),
            SelectedProperty(D_T, "0.005"),
            SelectedProperty(TEMPERATURE, "30.0"),
            SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
            SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
            SelectedProperty(VERLET_SKIN, "0.5"),
            SelectedProperty(EXTERNAL_FORCES, "true"),
            SelectedProperty(PRINT_INTERVAL, "100"),
            SelectedPropertyContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    MOLECULAR_DYNAMICS_SIMULATION, listOf(
                        SelectedPropertyContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                RELAXATION, listOf(
                                    SelectedProperty(
                                        RELAX_STRENGTH,
                                        "100.0"
                                    ),
                                    SelectedPropertyContainer(
                                        NUCLEI_ACID,
                                        SelectedOption(DNA, listOf())
                                    ),
                                    SelectedPropertyContainer(
                                        RELAXATION_FORCE,
                                        SelectedOption(CONSTANT_FORCE, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            THERMOSTAT,
                            SelectedOption(
                                BROWNIAN_THERMOSTAT, listOf(
                                    SelectedProperty(NEWTONIAN_STEPS, "1"),
                                    SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
                                    SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    /**
     * Default simulation configuration using simulation type _MD_ and _DNA_ interaction.
     */
    val mdSim: SelectedOption = SelectedOption(
        MANUAL_CONFIG, listOf(
            SelectedProperty(STEPS, 1e6.toUInt().toString()),
            SelectedProperty(D_T, "0.005"),
            SelectedProperty(TEMPERATURE, "30.0"),
            SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
            SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
            SelectedProperty(VERLET_SKIN, "0.05"),
            SelectedProperty(EXTERNAL_FORCES, "false"),
            SelectedProperty(PRINT_INTERVAL, "10000"),
            SelectedPropertyContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    MOLECULAR_DYNAMICS_SIMULATION, listOf(
                        SelectedPropertyContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                V1, listOf(
                                    SelectedPropertyContainer(
                                        NUCLEI_ACID,
                                        SelectedOption(DNA, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            THERMOSTAT,
                            SelectedOption(
                                BROWNIAN_THERMOSTAT, listOf(
                                    SelectedProperty(NEWTONIAN_STEPS, "103"),
                                    SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
                                    SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
                                )
                            )
                        )
                    )
                )
            )
        )
    )


    /*
     * Available Options
     */

    private val v1InteractionContainer: PropertyContainer = PropertyContainer(
        NUCLEI_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA"), listOf())
        )
    )

    private val v2InteractionContainer: PropertyContainer = PropertyContainer(
        NUCLEI_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA2"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA2"), listOf())
        )
    )

    private val relaxForceContainer: PropertyContainer = PropertyContainer(
        RELAXATION_FORCE, listOf(
            Option(CONSTANT_FORCE, mapOf("relax_type" to "constant_force"), listOf()),
            Option(HARMONIC_FORCE, mapOf("relax_type" to "harmonic_force"), listOf())
        )
    )

    private val relaxNucleiAcidContainer: PropertyContainer = PropertyContainer(
        NUCLEI_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA_relax"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA_relax"), listOf())
        )
    )

    private val interactionTypeContainer: PropertyContainer = PropertyContainer(
        INTERACTION_TYPE,
        listOf(
            Option(
                RELAXATION, mapOf(
                    "backend" to "CPU" // not available for relaxation interaction
                ), listOf(
                    Property(
                        RELAX_STRENGTH,
                        ValueType.FLOAT,
                        listOf("relax_strength")
                    ),
                    relaxNucleiAcidContainer,
                    relaxForceContainer
                )
            ),
            Option(
                V1, mapOf(), listOf(
                    v1InteractionContainer
                )
            ), Option(
                V2, mapOf(), listOf(
                    Property(
                        "Salt Concentration",
                        ValueType.FLOAT,
                        listOf("salt_concentration")
                    ),
                    v2InteractionContainer
                )
            )
        )
    )

    private val thermostatContainer: PropertyContainer = PropertyContainer(
        THERMOSTAT,
        listOf(
            Option(
                BROWNIAN_THERMOSTAT, mapOf(
                    "thermostat" to "brownian"
                ), listOf(
                    Property(
                        NEWTONIAN_STEPS,
                        ValueType.UNSIGNED_INTEGER,
                        listOf("newtonian_steps")
                    ),
                    Property(
                        PARTICLE_MOMENTUM_REFRESH_PROBABILITY,
                        ValueType.FLOAT,
                        listOf("pt")
                    ),
                    Property(
                        BASE_DIFFUSION_COEFFICIENT,
                        ValueType.FLOAT,
                        listOf("diff_coeff")
                    )
                )
            )
        )
    )

    private val simulationTypeContainer: PropertyContainer =
        PropertyContainer(
            SIMULATION_TYPE, listOf(
                Option(
                    POTENTIAL_ENERGY_MINIMIZATION, mapOf(
                        "sim_type" to "min",
                        "interaction_type" to "DNA",
                        "backend" to "CPU"
                    ), listOf()
                ),
                Option(
                    MONTE_CARLO_SIMULATION, mapOf(
                        "sim_type" to "MC",
                        "backend" to "CPU",
                        "ensemble" to "nvt"
                    ), listOf(
                        Property(
                            D_TRANSLATION,
                            ValueType.FLOAT,
                            listOf("delta_translation")
                        ),
                        Property(
                            D_ROTATION,
                            ValueType.FLOAT,
                            listOf("delta_rotation")
                        ),
                        interactionTypeContainer,
                        thermostatContainer
                    )
                ),
                Option(
                    MOLECULAR_DYNAMICS_SIMULATION, mapOf(
                        "sim_type" to "MD",
                        "backend" to "CUDA"
                    ), listOf(
                        interactionTypeContainer,
                        thermostatContainer
                    )
                )
            )
        )

    /**
     * Options available for manual step configuration.
     */
    val availableOptions: Option = Option(
        MANUAL_CONFIG, mapOf(
            "time_scale" to "linear",
            "restart_step_counter" to "true",
            "refresh_vel" to "true",
            "trajectory_print_momenta" to "true",
            "rcut" to "2.0"
        ), listOf(
            Property(STEPS, ValueType.UNSIGNED_INTEGER, listOf("steps")),
            Property(D_T, ValueType.FLOAT, listOf("dt")),
            Property(TEMPERATURE, ValueType.FLOAT, listOf("T"), suffix = "C"),
            Property(MAX_BACKBONE_FORCE, ValueType.FLOAT, listOf("max_backbone_force")),
            Property(MAX_BACKBONE_FORCE_FAR, ValueType.FLOAT, listOf("max_backbone_force_far")),
            Property(VERLET_SKIN, ValueType.FLOAT, listOf("verlet_skin")),
            Property(EXTERNAL_FORCES, ValueType.BOOLEAN, listOf("external_forces")),
            Property(PRINT_INTERVAL, ValueType.UNSIGNED_INTEGER, listOf("print_conf_interval", "print_energy_every")),
            simulationTypeContainer
        )
    )
}

/**
 * Unit in options available to users.
 */
@Serializable
@SerialName("Entry")
sealed interface Entry {
    val name: String
}

/**
 * [Entry] that represents a (selectable) option with some [fixedProperties] and sub[entries].
 */
@Serializable
@SerialName("Option")
data class Option(override val name: String, val fixedProperties: Map<String, String>, val entries: List<Entry>) : Entry

/**
 * [Entry] that holds selectable [Option]s.
 */
@Serializable
@SerialName("Container")
data class PropertyContainer(
    override val name: String,
    val values: List<Option>
) : Entry

/**
 * [Entry] that represents a particular property.
 */
@Serializable
@SerialName("Property")
data class Property(
    override val name: String,
    val valueType: ValueType,
    val configNames: List<String>,
    val suffix: String = ""
) : Entry


/**
 * Unit in user-selected options.
 */
@Serializable
@SerialName("SelectedEntry")
sealed interface SelectedEntry {
    val name: String

    /**
     * Using [ManualStepOptions.availableOptions], this [SelectedEntry] is encoded into a [Map] of properties.
     *
     * @param level the current level/depth in [ManualStepOptions.availableOptions].
     *
     * @return a new [Map] containing all the selected properties.
     */
    fun encodeToMap(level: Entry = ManualStepOptions.availableOptions): Map<String, String>
}

/**
 * [SelectedEntry] that represents a selected option with some sub[entries].
 */
@Serializable
@SerialName("SelectedOption")
data class SelectedOption(override val name: String, val entries: List<SelectedEntry>) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        if (level !is Option)
            ManualStepOptions.log.error(IllegalArgumentException("Expected something that isn't an Option named ${level.name}."))
        if (name != level.name)
            ManualStepOptions.log.error(IllegalArgumentException("Expected Option named ${level.name}."))
        if (this@SelectedOption.entries.mapTo(HashSet()) { it.name } != level.entries.mapTo(HashSet()) { it.name })
            ManualStepOptions.log.error(IllegalArgumentException("Invalid or missing Entries in Option ${level.name}."))

        putAll(level.fixedProperties)
        val backend = mutableSetOf<String?>()
        backend.add(this["backend"])

        this@SelectedOption.entries.forEach { entry ->
            val correspondingEntry = level.entries.firstOrNull { it.name == entry.name }
                ?: ManualStepOptions.log.error(IllegalArgumentException("Entry named ${entry.name} not found."))

            val properties = entry.encodeToMap(correspondingEntry)
            putAll(properties)
            backend.add(this["backend"])
        }

        // only if all Options that include the backend property agree to use the CUDA, will the CUDA be used
        if (backend.contains("CPU"))
            this["backend"] = "CPU"
    }
}

/**
 * [SelectedEntry] that holds a [SelectedOption].
 */
@Serializable
@SerialName("SelectedContainer")
data class SelectedPropertyContainer(
    override val name: String,
    val value: SelectedOption
) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        // this should be a Property and not a Container
        if (level !is PropertyContainer)
            ManualStepOptions.log.error(IllegalArgumentException("Expected Property named ${level.name}."))
        if (name != level.name)
            ManualStepOptions.log.error(IllegalArgumentException("Expected Container named ${level.name}."))

        val correspondingOption = level.values.firstOrNull { it.name == value.name }
            ?: ManualStepOptions.log.error(IllegalArgumentException("Option named ${value.name} not found."))

        val properties = value.encodeToMap(correspondingOption)
        putAll(properties)
    }
}

/**
 * [SelectedEntry] that represents a particular property with a selected [value].
 */
@Serializable
@SerialName("SelectedProperty")
data class SelectedProperty(
    override val name: String,
    val value: String,
) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        // this should be a Container and not a Property
        if (level !is Property)
            ManualStepOptions.log.error(IllegalArgumentException("Expected Container named ${level.name}."))
        if (name != level.name)
            ManualStepOptions.log.error(IllegalArgumentException("Expected Property named ${level.name}."))

        // validate data type
        val valueWithSuffix = when (level.valueType) {
            ValueType.BOOLEAN -> {
                value.toBooleanStrictOrNull()?.toString()
                    ?: ManualStepOptions.log.error(IllegalArgumentException("Expected boolean as value. Got $value."))
            }

            ValueType.UNSIGNED_INTEGER -> {
                value.toUIntOrNull()?.toString()
                    ?: ManualStepOptions.log.error(IllegalArgumentException("Expected unsigned integer as value. Got $value."))
            }

            ValueType.FLOAT -> {
                value.toFloatOrNull()?.toString()
                    ?: ManualStepOptions.log.error(IllegalArgumentException("Expected float as value. Got $value."))
            }
        } + level.suffix

        // write value for all config names
        level.configNames.forEach { key ->
            this[key] = valueWithSuffix
        }
    }
}


/**
 * Specifies the type of value expected.
 */
enum class ValueType {
    BOOLEAN,
    UNSIGNED_INTEGER,
    FLOAT
}
