import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import sim.default
import web.*
import java.io.File


private val logger = LoggerFactory.getLogger("Main")

suspend fun main(): Unit = try {
    // Reading environment variables and arguments
    val dataPath: String? = System.getenv("DATADIR")
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 80
    val host: String = System.getenv("HOST") ?: "0.0.0.0"
    val accessToken: String? = System.getenv("ACCESSTOKEN")

    // dataPath mustn't be null
    if (dataPath == null)
        logger.error(IllegalArgumentException("Missing data directory. Set environment variable \"DATADIR\""))

    // Initiate the environment using the arguments passed
    Environment.init(dataPath, accessToken)

    startWebServer(port, host)
} catch (e: Throwable) {
    // Make sure that all unexpected exceptions are logged (except for exceptions in the logger).
    logger.error(e)
}
