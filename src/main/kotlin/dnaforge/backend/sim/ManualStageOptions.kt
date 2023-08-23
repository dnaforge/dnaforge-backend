package dnaforge.backend.sim

import dnaforge.backend.Environment
import dnaforge.backend.throwError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object ManualStageOptions {
    val log: Logger = LoggerFactory.getLogger(ManualStageOptions::class.java)


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

    private const val NUCLEIC_ACID = "Nucleic Acid"
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
    private const val PARTICLE_MOMENTUM_REFRESH_PROBABILITY = "Momentum-Refresh-Probability"
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
            SelectedOptionContainer(
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
            SelectedOptionContainer(
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
                        SelectedOptionContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                RELAXATION, listOf(
                                    SelectedProperty(
                                        RELAX_STRENGTH,
                                        "1.0"
                                    ),
                                    SelectedOptionContainer(
                                        NUCLEIC_ACID,
                                        SelectedOption(DNA, listOf())
                                    ),
                                    SelectedOptionContainer(
                                        RELAXATION_FORCE,
                                        SelectedOption(CONSTANT_FORCE, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedOptionContainer(
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
            SelectedOptionContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    MOLECULAR_DYNAMICS_SIMULATION, listOf(
                        SelectedOptionContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                RELAXATION, listOf(
                                    SelectedProperty(
                                        RELAX_STRENGTH,
                                        "100.0"
                                    ),
                                    SelectedOptionContainer(
                                        NUCLEIC_ACID,
                                        SelectedOption(DNA, listOf())
                                    ),
                                    SelectedOptionContainer(
                                        RELAXATION_FORCE,
                                        SelectedOption(CONSTANT_FORCE, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedOptionContainer(
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
            SelectedOptionContainer(
                SIMULATION_TYPE,
                SelectedOption(
                    MOLECULAR_DYNAMICS_SIMULATION, listOf(
                        SelectedOptionContainer(
                            INTERACTION_TYPE,
                            SelectedOption(
                                V1, listOf(
                                    SelectedOptionContainer(
                                        NUCLEIC_ACID,
                                        SelectedOption(DNA, listOf())
                                    )
                                )

                            )
                        ),
                        SelectedOptionContainer(
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

    private val v1InteractionContainer: OptionContainer = OptionContainer(
        NUCLEIC_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA"), listOf())
        )
    )

    private val v2InteractionContainer: OptionContainer = OptionContainer(
        NUCLEIC_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA2"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA2"), listOf())
        )
    )

    private val relaxForceContainer: OptionContainer = OptionContainer(
        RELAXATION_FORCE, listOf(
            Option(CONSTANT_FORCE, mapOf("relax_type" to "constant_force"), listOf()),
            Option(HARMONIC_FORCE, mapOf("relax_type" to "harmonic_force"), listOf())
        )
    )

    private val relaxNucleiAcidContainer: OptionContainer = OptionContainer(
        NUCLEIC_ACID, listOf(
            Option(DNA, mapOf("interaction_type" to "DNA_relax"), listOf()),
            Option(RNA, mapOf("interaction_type" to "RNA_relax"), listOf())
        )
    )

    private val interactionTypeContainer: OptionContainer = OptionContainer(
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

    private val thermostatContainer: OptionContainer = OptionContainer(
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

    private val simulationTypeContainer: OptionContainer =
        OptionContainer(
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
     * Options available for manual stage configuration.
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

    /**
     * Set of all properties available for manual stage configuration.
     */
    val availableProperties: Set<SimpleListProperty> = getAllProperties(availableOptions)

    /**
     * Recursively collects all Properties in the given [Option].
     */
    private fun getAllProperties(
        option: Option,
        set: MutableSet<SimpleListProperty> = mutableSetOf()
    ): MutableSet<SimpleListProperty> {
        option.entries.forEach {
            when (it) {
                is Property -> set.add(SimpleListProperty(it.name, it.valueType))

                is OptionContainer -> {
                    set.add(SimpleListProperty(it.name, ValueType.ENUM, it.values.mapTo(mutableSetOf()) { it.name }))
                    it.values.forEach { getAllProperties(it, set) }
                }

                is Option -> getAllProperties(it, set)
            }
        }

        return set
    }

    fun selectedPropertiesToSelectedOption(set: Set<SelectedProperty>): SelectedOption {
        val available = availableProperties.associateBy { it.name }
        val properties = set.filterTo(mutableSetOf()) { it.value.isNotBlank() }
        if (properties.any { available[it.name] == null })
            log.throwError(IllegalArgumentException("Unknown properties: ${properties.retainAll { available[it.name] == null }}"))

        val selectedOption =
            buildSelectedOptionRecursively(properties.associateByTo(mutableMapOf()) { it.name }, availableOptions)

        if (selectedOption !is SelectedOption)
            log.throwError(IllegalArgumentException("Expected to be able to build a SelectedOption."))

        return selectedOption
    }

    private fun buildSelectedOptionRecursively(
        propertiesLeft: MutableMap<String, SelectedProperty>,
        level: Entry
    ): SelectedEntry? = when (level) {
        is Option -> {
            val entries = level.entries.map { buildSelectedOptionRecursively(propertiesLeft, it) }
            val cleanEntries = entries.mapNotNull { it }
            if (entries.size != cleanEntries.size)
                null
            else
                SelectedOption(level.name, cleanEntries)
        }

        is OptionContainer -> {
            val selectedOption = propertiesLeft.remove(level.name)
                ?.run { level.values.firstOrNull { it.name == this.value } }
                ?.run { buildSelectedOptionRecursively(propertiesLeft, this) }
            if (selectedOption !is SelectedOption)
                null
            else
                SelectedOptionContainer(level.name, selectedOption)
        }

        is Property -> {
            val prop = propertiesLeft.remove(level.name)
            if (prop?.value == null)
                null
            else
                SelectedProperty(prop.name, prop.value)
        }
    }
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
data class Option(
    override val name: String,
    @Transient
    val fixedProperties: Map<String, String> = mapOf(),
    val entries: List<Entry>
) : Entry

/**
 * [Entry] that holds selectable [Option]s.
 */
@Serializable
@SerialName("Container")
data class OptionContainer(
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
    @Transient
    val configNames: List<String> = listOf(),
    @Transient
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
     * Using [ManualStageOptions.availableOptions], this [SelectedEntry] is encoded into a [Map] of properties.
     *
     * @param level the current level/depth in [ManualStageOptions.availableOptions].
     *
     * @return a new [Map] containing all the selected properties.
     */
    fun encodeToMap(level: Entry = ManualStageOptions.availableOptions): Map<String, String>
}

/**
 * [SelectedEntry] that represents a selected option with some sub[entries].
 */
@Serializable
@SerialName("SelectedOption")
data class SelectedOption(override val name: String, val entries: List<SelectedEntry>) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        if (level !is Option)
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected something that isn't an Option named ${level.name}."))
        if (name != level.name)
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected Option named ${level.name}."))
        if (this@SelectedOption.entries.mapTo(HashSet()) { it.name } != level.entries.mapTo(HashSet()) { it.name })
            ManualStageOptions.log.throwError(IllegalArgumentException("Invalid or missing Entries in Option ${level.name}."))

        putAll(level.fixedProperties)
        val backend = mutableSetOf<String?>()
        backend.add(this["backend"])

        this@SelectedOption.entries.forEach { entry ->
            val correspondingEntry = level.entries.firstOrNull { it.name == entry.name }
                ?: ManualStageOptions.log.throwError(IllegalArgumentException("Entry named ${entry.name} not found."))

            val properties = entry.encodeToMap(correspondingEntry)
            putAll(properties)
            backend.add(this["backend"])
        }

        // if CUDA support is not available, CUDA will not be used
        // only if all options that include the backend property agree to use CUDA, CUDA will be used
        if (!Environment.cuda || backend.contains("CPU"))
            this["backend"] = "CPU"
    }
}

/**
 * [SelectedEntry] that holds a [SelectedOption].
 */
@Serializable
@SerialName("SelectedContainer")
data class SelectedOptionContainer(
    override val name: String,
    val value: SelectedOption
) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        // this should be a Property and not a Container
        if (level !is OptionContainer)
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected Property named ${level.name}."))
        if (name != level.name)
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected Container named ${level.name}."))

        val correspondingOption = level.values.firstOrNull { it.name == value.name }
            ?: ManualStageOptions.log.throwError(IllegalArgumentException("Option named ${value.name} not found."))

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
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected Container named ${level.name}."))
        if (name != level.name)
            ManualStageOptions.log.throwError(IllegalArgumentException("Expected Property named ${level.name}."))

        // validate data type
        val valueWithSuffix = when (level.valueType) {
            ValueType.BOOLEAN ->
                value.toBooleanStrictOrNull()?.toString()
                    ?: ManualStageOptions.log.throwError(IllegalArgumentException("Expected boolean as value. Got $value."))

            ValueType.UNSIGNED_INTEGER ->
                value.toUIntOrNull()?.toString()
                    ?: ManualStageOptions.log.throwError(IllegalArgumentException("Expected unsigned integer as value. Got $value."))

            ValueType.FLOAT ->
                value.toFloatOrNull()?.toString()
                    ?: ManualStageOptions.log.throwError(IllegalArgumentException("Expected float as value. Got $value."))

            ValueType.ENUM -> ManualStageOptions.log.throwError(IllegalArgumentException("Enums shouldn't exist here."))
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
    FLOAT,
    ENUM
}


/**
 * Used to represent all available properties.
 * They are needed for [ValueType.ENUM] type properties that represent [Option]s in the usual representation.
 */
@Serializable
@SerialName("Property")
data class SimpleListProperty(
    val name: String,
    val valueType: ValueType,
    val possibleValues: Set<String>? = null,
    val value: String? = null
)
