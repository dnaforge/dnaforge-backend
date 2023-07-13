import org.slf4j.LoggerFactory
import java.io.File

class Environment private constructor(dataPath: String, accessToken: String?) {
    private val accessToken: String?

    val dataDir: File

    companion object {
        private val logger = LoggerFactory.getLogger(Environment::class.java)

        @Volatile
        private var instance: Environment? = null

        val inst: Environment
            get() = synchronized(this) {
                return instance ?: run {
                    logger.error(IllegalStateException("The Environment hasn't been initialized yet."))
                }
            }

        fun init(dataPath: String, accessToken: String?) = instance ?: synchronized(this) {
            instance ?: Environment(dataPath, accessToken).also { instance = it }
        }
    }

    init {
        this.accessToken = accessToken?.trim()
        dataDir = File(dataPath)

        logger.info("AccessToken: \"${this.accessToken}\"")
        logger.info("Data directory: \"$dataDir\"")

        dataDir.mkdirs()
        if (!dataDir.isDirectory)
            logger.error(FileSystemException(dataDir, null, "The given data directory isn't a directory or can't be created."))
    }

    fun allowAccess(token: String?): Boolean =
        accessToken == null || token?.trim() == accessToken
}
