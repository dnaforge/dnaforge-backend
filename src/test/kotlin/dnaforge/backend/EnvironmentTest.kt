package dnaforge.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentTest {

    @Test
    fun `data directory correct`() {
        assertEquals("./data", Environment.dataDir.path)
    }

    @Test
    fun `host correct`() {
        assertEquals("0.0.0.0", Environment.host)
    }

    @Test
    fun `port correct`() {
        assertEquals(8080, Environment.port)
    }

    @Test
    fun `cuda correct`() {
        assertEquals(true, Environment.cuda)
    }

    @Test
    fun `logging level correct`() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        assertEquals(Level.ALL, rootLogger.level)
    }
}
