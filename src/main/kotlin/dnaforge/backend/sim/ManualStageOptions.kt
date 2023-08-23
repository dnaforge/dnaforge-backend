package dnaforge.backend.sim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


object ManualStageOptions {
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
    val minRelax: Set<SelectedProperty> = setOf(
        SelectedProperty(STEPS, "2000"),
        SelectedProperty(D_T, "0.005"),
        SelectedProperty(TEMPERATURE, "30.0"),
        SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
        SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
        SelectedProperty(VERLET_SKIN, "0.15"),
        SelectedProperty(EXTERNAL_FORCES, "true"),
        SelectedProperty(PRINT_INTERVAL, "100"),

        SelectedProperty(SIMULATION_TYPE, POTENTIAL_ENERGY_MINIMIZATION)
    )

    /**
     * Default relaxation configuration using simulation type _MC_ and _DNA_relax_ interaction.
     */
    val mcRelax: Set<SelectedProperty> = setOf(
        SelectedProperty(STEPS, "100"),
        SelectedProperty(D_T, "0.00001"),
        SelectedProperty(TEMPERATURE, "30.0"),
        SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
        SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
        SelectedProperty(VERLET_SKIN, "0.5"),
        SelectedProperty(EXTERNAL_FORCES, "true"),
        SelectedProperty(PRINT_INTERVAL, "10"),

        SelectedProperty(SIMULATION_TYPE, MONTE_CARLO_SIMULATION),
        SelectedProperty(D_TRANSLATION, "0.1"),
        SelectedProperty(D_ROTATION, "0.1"),

        SelectedProperty(INTERACTION_TYPE, RELAXATION),
        SelectedProperty(RELAX_STRENGTH, "1.0"),
        SelectedProperty(NUCLEIC_ACID, DNA),
        SelectedProperty(RELAXATION_FORCE, CONSTANT_FORCE),

        SelectedProperty(THERMOSTAT, BROWNIAN_THERMOSTAT),
        SelectedProperty(NEWTONIAN_STEPS, "103"),
        SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
        SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
    )

    /**
     * Default relaxation configuration using simulation type _MD_ and _DNA_relax_ interaction.
     */
    val mdRelax: Set<SelectedProperty> = setOf(
        SelectedProperty(STEPS, 1e3.toUInt().toString()),
        SelectedProperty(D_T, "0.005"),
        SelectedProperty(TEMPERATURE, "30.0"),
        SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
        SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
        SelectedProperty(VERLET_SKIN, "0.5"),
        SelectedProperty(EXTERNAL_FORCES, "true"),
        SelectedProperty(PRINT_INTERVAL, "100"),

        SelectedProperty(SIMULATION_TYPE, MOLECULAR_DYNAMICS_SIMULATION),

        SelectedProperty(INTERACTION_TYPE, RELAXATION),
        SelectedProperty(RELAX_STRENGTH, "100.0"),
        SelectedProperty(NUCLEIC_ACID, DNA),
        SelectedProperty(RELAXATION_FORCE, CONSTANT_FORCE),

        SelectedProperty(THERMOSTAT, BROWNIAN_THERMOSTAT),
        SelectedProperty(NEWTONIAN_STEPS, "1"),
        SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
        SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
    )

    /**
     * Default simulation configuration using simulation type _MD_ and _DNA_ interaction.
     */
    val mdSim: Set<SelectedProperty> = setOf(
        SelectedProperty(STEPS, 1e6.toUInt().toString()),
        SelectedProperty(D_T, "0.005"),
        SelectedProperty(TEMPERATURE, "30.0"),
        SelectedProperty(MAX_BACKBONE_FORCE, "5.0"),
        SelectedProperty(MAX_BACKBONE_FORCE_FAR, "10.0"),
        SelectedProperty(VERLET_SKIN, "0.05"),
        SelectedProperty(EXTERNAL_FORCES, "false"),
        SelectedProperty(PRINT_INTERVAL, "10000"),

        SelectedProperty(SIMULATION_TYPE, MOLECULAR_DYNAMICS_SIMULATION),

        SelectedProperty(INTERACTION_TYPE, V1),
        SelectedProperty(NUCLEIC_ACID, DNA),

        SelectedProperty(THERMOSTAT, BROWNIAN_THERMOSTAT),
        SelectedProperty(NEWTONIAN_STEPS, "103"),
        SelectedProperty(PARTICLE_MOMENTUM_REFRESH_PROBABILITY, "0.99"),
        SelectedProperty(BASE_DIFFUSION_COEFFICIENT, "2.5")
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
 * Represents a particular property with a selected [value].
 */
@Serializable
@SerialName("SelectedProperty")
data class SelectedProperty(
    val name: String,
    val value: String,
)


/**
 * Specifies the type of value expected.
 */
enum class ValueType {
    BOOLEAN,
    UNSIGNED_INTEGER,
    FLOAT
}
