package dnaforge.backend.sim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val minSelected: SelectedOption = SelectedOption(
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

val mcRelaxSelected: SelectedOption = SelectedOption(
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

val mdRelaxSelected: SelectedOption = SelectedOption(
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

val mdSimSelected: SelectedOption = SelectedOption(
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

val defaultSelected = listOf(minSelected, mcRelaxSelected, mdRelaxSelected, mdSimSelected)


private
val v1InteractionContainer: PropertyContainer = PropertyContainer(
    "Nuclei Acid", listOf(
        Option("DNA", mapOf("interaction_type" to "DNA"), listOf()),
        Option("RNA", mapOf("interaction_type" to "RNA"), listOf())
    )
)

private
val v2InteractionContainer: PropertyContainer = PropertyContainer(
    "Nuclei Acid", listOf(
        Option("DNA", mapOf("interaction_type" to "DNA2"), listOf()),
        Option("RNA", mapOf("interaction_type" to "RNA2"), listOf())
    )
)

private
val relaxForceContainer: PropertyContainer = PropertyContainer(
    "Relaxation Force", listOf(
        Option("Constant Force", mapOf("relax_type" to "constant_force"), listOf()),
        Option("Harmonic Force", mapOf("relax_type" to "harmonic_force"), listOf())
    )
)

private
val relaxNucleiAcidContainer: PropertyContainer = PropertyContainer(
    "Nuclei Acid", listOf(
        Option("DNA", mapOf("interaction_type" to "DNA_relax"), listOf()),
        Option("RNA", mapOf("interaction_type" to "RNA_relax"), listOf())
    )
)

private
val interactionTypeContainer: PropertyContainer = PropertyContainer(
    "Interaction Type",
    listOf(
        Option(
            "Relaxation", mapOf(
                "backend" to "CPU" // not available for relaxation interaction
            ), listOf(
                Property(
                    "Relax Strength",
                    ValueType.Float,
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
                    ValueType.Float,
                    listOf("salt_concentration")
                ),
                v2InteractionContainer
            )
        )
    )
)

private
val thermostatContainer: PropertyContainer = PropertyContainer(
    "Thermostat",
    listOf(
        Option(
            "Brownian Thermostat", mapOf(
                "thermostat" to "brownian"
            ), listOf(
                Property(
                    "Newtonian Steps",
                    ValueType.UInt,
                    listOf("newtonian_steps")
                ),
                Property(
                    "Particle-Momentum-Refresh-Probability",
                    ValueType.Float,
                    listOf("pt")
                ),
                Property(
                    "Base Diffusion Coefficient",
                    ValueType.Float,
                    listOf("diff_coeff")
                )
            )
        )
    )
)

private
val simulationTypeContainer: PropertyContainer =
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
                        ValueType.Float,
                        listOf("delta_translation")
                    ),
                    Property(
                        "ΔRotation",
                        ValueType.Float,
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

val availableOptions: Option = Option(
    "Manual Config", mapOf(
        "time_scale" to "linear",
        "restart_step_counter" to "true",
        "refresh_vel" to "true",
        "trajectory_print_momenta" to "true",
        "rcut" to "2.0"
    ), listOf(
        Property("Steps", ValueType.UInt, listOf("steps")),
        Property("ΔT", ValueType.Float, listOf("dt")),
        Property("Temperature (°C)", ValueType.Float, listOf("T"), suffix = "C"),
        Property("Max. Backbone Force", ValueType.Float, listOf("max_backbone_force")),
        Property("Max. Backbone Force Far", ValueType.Float, listOf("max_backbone_force_far")),
        Property("Verlet Skin", ValueType.Float, listOf("verlet_skin")),
        Property("External Forces", ValueType.Boolean, listOf("external_forces")),
        Property("Print Interval", ValueType.UInt, listOf("print_conf_interval", "print_energy_every")),
        simulationTypeContainer
    )
)

@Serializable
@SerialName("Option")
data class Option(val name: String, val fixedProperties: Map<String, String>, val entries: List<Entry>)

@Serializable
@SerialName("Entry")
sealed interface Entry {
    val name: String
}

@Serializable
@SerialName("Container")
data class PropertyContainer(
    override val name: String,
    val values: List<Option>
) : Entry


@Serializable
@SerialName("Property")
data class Property(
    override val name: String,
    val valueType: ValueType,
    val configNames: List<String>,
    val suffix: String = ""
) : Entry


@Serializable
@SerialName("SelectedOption")
data class SelectedOption(val name: String, val entries: List<SelectedEntry>) {

    fun encodeToMap(level: Option = availableOptions): Map<String, String> = buildMap {
        if (name != level.name) throw IllegalArgumentException("Expected Option named ${level.name}.")
        if (this@SelectedOption.entries.mapTo(HashSet()) { it.name } != level.entries.mapTo(HashSet()) { it.name })
            throw IllegalArgumentException("Invalid or missing Entries in Option ${level.name}.")

        putAll(level.fixedProperties)
        val backend = mutableSetOf<String?>()
        backend.add(this["backend"])

        this@SelectedOption.entries.forEach { entry ->
            val correspondingEntry = level.entries.firstOrNull { it.name == entry.name }
                ?: throw IllegalArgumentException("Entry named ${entry.name} not found.")

            val properties = entry.encodeToMap(correspondingEntry)
            putAll(properties)
            backend.add(this["backend"])
        }

        // only if all Options that include the backend property agree to use the CUDA, will the CUDA be used
        if (backend.contains("CPU"))
            this["backend"] = "CPU"
    }
}

@Serializable
@SerialName("SelectedEntry")
sealed interface SelectedEntry {
    val name: String

    fun encodeToMap(level: Entry): Map<String, String>
}

@Serializable
@SerialName("SelectedContainer")
data class SelectedPropertyContainer(
    override val name: String,
    val value: SelectedOption
) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        // this should be a Property and not a Container
        if (level !is PropertyContainer) throw IllegalArgumentException("Expected Property named ${level.name}.")
        if (name != level.name) throw IllegalArgumentException("Expected Container named ${level.name}.")

        val correspondingOption = level.values.firstOrNull { it.name == value.name }
            ?: throw IllegalArgumentException("Option named ${value.name} not found.")

        val properties = value.encodeToMap(correspondingOption)
        putAll(properties)
    }
}

@Serializable
@SerialName("SelectedProperty")
data class SelectedProperty(
    override val name: String,
    val value: String,
) : SelectedEntry {

    override fun encodeToMap(level: Entry): Map<String, String> = buildMap {
        // this should be a Container and not a Property
        if (level !is Property) throw IllegalArgumentException("Expected Container named ${level.name}.")
        if (name != level.name) throw IllegalArgumentException("Expected Property named ${level.name}.")

        // validate data type
        when (level.valueType) {
            ValueType.Boolean -> {
                value.toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("Expected boolean as value. Got $value.")
            }

            ValueType.UInt -> {
                value.toUIntOrNull()
                    ?: throw IllegalArgumentException("Expected unsigned integer as value. Got $value.")
            }

            ValueType.Float -> {
                value.toFloatOrNull() ?: throw IllegalArgumentException("Expected float as value. Got $value.")
            }
        }

        level.configNames.forEach { key ->
            this[key] = "$value${level.suffix}"
        }
    }
}


enum class ValueType {
    Boolean,
    UInt,
    Float
}
