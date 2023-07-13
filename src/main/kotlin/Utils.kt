import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.slf4j.Logger

/**
 * Logs the given [Throwable] at _ERROR_ level and then throws it.
 *
 * @param exception the [Throwable] to be processed.
 */
fun Logger.error(exception: Throwable): Nothing {
    error(exception.message ?: "Exception of type \"${exception::class.qualifiedName}\"", exception)
    throw exception
}


@OptIn(ExperimentalSerializationApi::class)
val simpleJson = Json {
    encodeDefaults = true
    explicitNulls = true
}

@OptIn(ExperimentalSerializationApi::class)
val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = true
}
