package dnaforge.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This object manages parameters supplied by environment variables.
 *
 *
 * _DATADIR_ is mandatory and must be the path to a directory.
 *
 * _ACCESSTOKEN_ can be used to enable simple password protection.
 *
 * _PORT_ may be used to specify a port number. If empty or not an [Int], 8080 is used.
 *
 * _HOST_ may be used to specify a host. If empty, "0.0.0.0" is used.
 */
object Environment {
    private val log = LoggerFactory.getLogger(Environment::class.java)

    private val accessToken: String?

    /**
     * [File] representation of the specified data directory.
     */
    val dataDir: File

    /**
     * Host address used by this application's web server.
     */
    val host: String

    /**
     * Port number used by this application's web server.
     */
    val port: Int

    /**
     * Indicates whether CUDA is available as a backend.
     */
    val cuda: Boolean

    init {
        // update log level
        val logLevel: String? = System.getenv("LOGLEVEL")
        if (logLevel != null) {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            rootLogger.level = Level.toLevel(logLevel, rootLogger.level)
        }

        log.debug("Initializing environment.")

        // Reading environment variables and arguments
        val dataPath: String? = System.getenv("DATADIR")
        var accessToken: String? = System.getenv("ACCESSTOKEN")?.trim()
        if (accessToken?.isBlank() == true) accessToken = null
        Environment.accessToken = accessToken

        port = System.getenv("PORT")?.toIntOrNull() ?: 8080
        host = System.getenv("HOST") ?: "0.0.0.0"

        cuda = System.getenv("CUDA")?.toBooleanStrictOrNull() ?: true

        // DATADIR mustn't be null
        if (dataPath == null)
            log.throwError(IllegalArgumentException("Missing data directory. Set environment variable \"DATADIR\""))

        dataDir = File(dataPath)

        log.info("AccessToken: \"${Environment.accessToken}\"")
        log.info("Data directory: \"$dataDir\"")
        log.info("Address: \"$host:$port\"")
        log.info("CUDA support: $cuda")

        // create data dir if needed and check that it actually exists
        dataDir.mkdirs()
        if (!dataDir.isDirectory)
            log.throwError(
                FileSystemException(
                    dataDir,
                    null,
                    "The given data directory isn't a directory or can't be created."
                )
            )

        log.debug("Environment initialized.")
    }

    /**
     * Tests whether the given [token] gives access to the capabilities of this server.
     * Note that if no [accessToken] is set, any [token] is considered valid.
     *
     * @param token is the token received from a client
     *
     * @return `true` if access is unrestricted or the [token] matches the configured [accessToken]; `false` otherwise
     */
    fun allowAccess(token: String?): Boolean =
        accessToken == null || token?.trim() == accessToken
}
