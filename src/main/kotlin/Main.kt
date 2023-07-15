import org.slf4j.LoggerFactory
import web.startWebServer


private val log = LoggerFactory.getLogger("Main")

fun main(): Unit = try {
    startWebServer()
} catch (e: Throwable) {
    // Make sure that all unexpected exceptions are logged (except for exceptions in the logger).
    log.error(e)
}
