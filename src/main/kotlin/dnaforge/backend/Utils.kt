package dnaforge.backend

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.slf4j.Logger

/*
 * File names
 */
const val jobFileName = "job.json"
const val stepFileName = "step.json"

const val topologyFileName = "topology.top"
const val startConfFileName = "conf_start.dat"
const val forcesFileName = "forces.forces"

const val endConfFileName = "conf_end.dat"
const val trajectoryFileName = "trajectory.dat"
const val energyFileName = "energy.dat"

const val inputFileName = "input.properties"

const val oxDnaLogFileName = "oxDNA.log"


/**
 * [Json] with default and `null` encoding.
 * Ignores unknown keys.
 */
@OptIn(ExperimentalSerializationApi::class)
val simpleJson = Json {
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
}

/**
 * [Json] with default and `null` encoding.
 * Ignores unknown keys.
 * Pretty print is enabled.
 */
@OptIn(ExperimentalSerializationApi::class)
val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
}


/**
 * Logs the given [Throwable] at _ERROR_ level and then throws it.
 *
 * @param exception the [Throwable] to be processed.
 */
fun Logger.error(exception: Throwable): Nothing {
    error(exception.message ?: "Exception of type \"${exception::class.qualifiedName}\"", exception)
    throw exception
}

/**
 * Indicates an internal API that should not be used without knowing exactly what it does.
 * Typically used to provide functions needed for some tests.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal and should not be used!"
)
@Target(
    AnnotationTarget.FUNCTION
)
annotation class InternalAPI
