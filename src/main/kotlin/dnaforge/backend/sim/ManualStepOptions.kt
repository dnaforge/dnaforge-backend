package dnaforge.backend.sim

import dnaforge.backend.error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ManualStepOptions {
    val log: Logger = LoggerFactory.getLogger(ManualStepOptions::class.java)


    /*
     * Default Options
     */

    /**
     * Default relaxation configuration using the _min_ simulation type.
     */
    private val min: SelectedOption = SelectedOption(
        "Manual Config", listOf(
            SelectedProperty("Steps", "2000"),
            SelectedProperty("ΔT", "0.005"),
            SelectedProperty("Temperature (°C)", "30.0"),
            SelectedProperty("Max. Backbone Force", "5.0"),
            SelectedProperty("Max. Backbone Force Far", "10.0"),
            SelectedProperty("Verlet Skin", "0.15"),
            SelectedProperty("External Forces", "true"),
            SelectedProperty("Print Interval", "100"),
            SelectedPropertyContainer(
                "Simulation Type",
                SelectedOption(
                    "Potential Energy Minimization", listOf()
                )
            )
        )
    )

    /**
     * Default relaxation configuration using simulation type _MC_ and _DNA_relax_ interaction.
     */
    private val mcRelax: SelectedOption = SelectedOption(
        "Manual Config", listOf(
            SelectedProperty("Steps", "100"),
            SelectedProperty("ΔT", "0.00001"),
            SelectedProperty("Temperature (°C)", "30.0"),
            SelectedProperty("Max. Backbone Force", "5.0"),
            SelectedProperty("Max. Backbone Force Far", "10.0"),
            SelectedProperty("Verlet Skin", "0.5"),
            SelectedProperty("External Forces", "true"),
            SelectedProperty("Print Interval", "10"),
            SelectedPropertyContainer(
                "Simulation Type",
                SelectedOption(
                    "Monte Carlo Simulation", listOf(
                        SelectedProperty(
                            "ΔTranslation",
                            "0.1"
                        ),
                        SelectedProperty(
                            "ΔRotation",
                            "0.1"
                        ),
                        SelectedPropertyContainer(
                            "Interaction Type",
                            SelectedOption(
                                "Relaxation", listOf(
                                    SelectedProperty(
                                        "Relax Strength",
                                        "1.0"
                                    ),
                                    SelectedPropertyContainer(
                                        "Nuclei Acid",
                                        SelectedOption("DNA", listOf())
                                    ),
                                    SelectedPropertyContainer(
                                        "Relaxation Force",
                                        SelectedOption("Constant Force", listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            "Thermostat",
                            SelectedOption(
                                "Brownian Thermostat", listOf(
                                    SelectedProperty("Newtonian Steps", "103"),
                                    SelectedProperty("Particle-Momentum-Refresh-Probability", "0.99"),
                                    SelectedProperty("Base Diffusion Coefficient", "2.5")
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
    private val mdRelax: SelectedOption = SelectedOption(
        "Manual Config", listOf(
            SelectedProperty("Steps", 1e3.toUInt().toString()),
            SelectedProperty("ΔT", "0.005"),
            SelectedProperty("Temperature (°C)", "30.0"),
            SelectedProperty("Max. Backbone Force", "5.0"),
            SelectedProperty("Max. Backbone Force Far", "10.0"),
            SelectedProperty("Verlet Skin", "0.5"),
            SelectedProperty("External Forces", "true"),
            SelectedProperty("Print Interval", "100"),
            SelectedPropertyContainer(
                "Simulation Type",
                SelectedOption(
                    "Molecular Dynamics Simulation", listOf(
                        SelectedPropertyContainer(
                            "Interaction Type",
                            SelectedOption(
                                "Relaxation", listOf(
                                    SelectedProperty(
                                        "Relax Strength",
                                        "100.0"
                                    ),
                                    SelectedPropertyContainer(
                                        "Nuclei Acid",
                                        SelectedOption("DNA", listOf())
                                    ),
                                    SelectedPropertyContainer(
                                        "Relaxation Force",
                                        SelectedOption("Constant Force", listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            "Thermostat",
                            SelectedOption(
                                "Brownian Thermostat", listOf(
                                    SelectedProperty("Newtonian Steps", "1"),
                                    SelectedProperty("Particle-Momentum-Refresh-Probability", "0.99"),
                                    SelectedProperty("Base Diffusion Coefficient", "2.5")
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
    private val mdSim: SelectedOption = SelectedOption(
        "Manual Config", listOf(
            SelectedProperty("Steps", 1e6.toUInt().toString()),
            SelectedProperty("ΔT", "0.005"),
            SelectedProperty("Temperature (°C)", "30.0"),
            SelectedProperty("Max. Backbone Force", "5.0"),
            SelectedProperty("Max. Backbone Force Far", "10.0"),
            SelectedProperty("Verlet Skin", "0.05"),
            SelectedProperty("External Forces", "false"),
            SelectedProperty("Print Interval", "10000"),
            SelectedPropertyContainer(
                "Simulation Type",
                SelectedOption(
                    "Molecular Dynamics Simulation", listOf(
                        SelectedPropertyContainer(
                            "Interaction Type",
                            SelectedOption(
                                "v1", listOf(
                                    SelectedPropertyContainer(
                                        "Nuclei Acid",
                                        SelectedOption("DNA", listOf())
                                    )
                                )

                            )
                        ),
                        SelectedPropertyContainer(
                            "Thermostat",
                            SelectedOption(
                                "Brownian Thermostat", listOf(
                                    SelectedProperty("Newtonian Steps", "103"),
                                    SelectedProperty("Particle-Momentum-Refresh-Probability", "0.99"),
                                    SelectedProperty("Base Diffusion Coefficient", "2.5")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    /**
     * Default step config list.
     */
    val default = listOf(min, mcRelax, mdRelax, mdSim)


    /*
     * Available Options
     */

    private val v1InteractionContainer: PropertyContainer = PropertyContainer(
        "Nuclei Acid", listOf(
            Option("DNA", mapOf("interaction_type" to "DNA"), listOf()),
            Option("RNA", mapOf("interaction_type" to "RNA"), listOf())
        )
    )

    private val v2InteractionContainer: PropertyContainer = PropertyContainer(
        "Nuclei Acid", listOf(
            Option("DNA", mapOf("interaction_type" to "DNA2"), listOf()),
            Option("RNA", mapOf("interaction_type" to "RNA2"), listOf())
        )
    )

    private val relaxForceContainer: PropertyContainer = PropertyContainer(
        "Relaxation Force", listOf(
            Option("Constant Force", mapOf("relax_type" to "constant_force"), listOf()),
            Option("Harmonic Force", mapOf("relax_type" to "harmonic_force"), listOf())
        )
    )

    private val relaxNucleiAcidContainer: PropertyContainer = PropertyContainer(
        "Nuclei Acid", listOf(
            Option("DNA", mapOf("interaction_type" to "DNA_relax"), listOf()),
            Option("RNA", mapOf("interaction_type" to "RNA_relax"), listOf())
        )
    )

    private val interactionTypeContainer: PropertyContainer = PropertyContainer(
        "Interaction Type",
        listOf(
            Option(
                "Relaxation", mapOf(
                    "backend" to "CPU" // not available for relaxation interaction
                ), listOf(
                    Property(
                        "Relax Strength",
                        ValueType.FLOAT,
                        listOf("relax_strength")
                    ),
                    relaxNucleiAcidContainer,
                    relaxForceContainer
                )
            ),
            Option(
                "v1", mapOf(), listOf(
                    v1InteractionContainer
                )
            ), Option(
                "v2", mapOf(), listOf(
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
        "Thermostat",
        listOf(
            Option(
                "Brownian Thermostat", mapOf(
                    "thermostat" to "brownian"
                ), listOf(
                    Property(
                        "Newtonian Steps",
                        ValueType.UNSIGNED_INTEGER,
                        listOf("newtonian_steps")
                    ),
                    Property(
                        "Particle-Momentum-Refresh-Probability",
                        ValueType.FLOAT,
                        listOf("pt")
                    ),
                    Property(
                        "Base Diffusion Coefficient",
                        ValueType.FLOAT,
                        listOf("diff_coeff")
                    )
                )
            )
        )
    )

    private val simulationTypeContainer: PropertyContainer =
        PropertyContainer(
            "Simulation Type", listOf(
                Option(
                    "Potential Energy Minimization", mapOf(
                        "sim_type" to "min",
                        "interaction_type" to "DNA",
                        "backend" to "CPU"
                    ), listOf()
                ),
                Option(
                    "Monte Carlo Simulation", mapOf(
                        "sim_type" to "MC",
                        "backend" to "CPU",
                        "ensemble" to "nvt"
                    ), listOf(
                        Property(
                            "ΔTranslation",
                            ValueType.FLOAT,
                            listOf("delta_translation")
                        ),
                        Property(
                            "ΔRotation",
                            ValueType.FLOAT,
                            listOf("delta_rotation")
                        ),
                        interactionTypeContainer,
                        thermostatContainer
                    )
                ),
                Option(
                    "Molecular Dynamics Simulation", mapOf(
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
        "Manual Config", mapOf(
            "time_scale" to "linear",
            "restart_step_counter" to "true",
            "refresh_vel" to "true",
            "trajectory_print_momenta" to "true",
            "rcut" to "2.0"
        ), listOf(
            Property("Steps", ValueType.UNSIGNED_INTEGER, listOf("steps")),
            Property("ΔT", ValueType.FLOAT, listOf("dt")),
            Property("Temperature (°C)", ValueType.FLOAT, listOf("T"), suffix = "C"),
            Property("Max. Backbone Force", ValueType.FLOAT, listOf("max_backbone_force")),
            Property("Max. Backbone Force Far", ValueType.FLOAT, listOf("max_backbone_force_far")),
            Property("Verlet Skin", ValueType.FLOAT, listOf("verlet_skin")),
            Property("External Forces", ValueType.BOOLEAN, listOf("external_forces")),
            Property("Print Interval", ValueType.UNSIGNED_INTEGER, listOf("print_conf_interval", "print_energy_every")),
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
