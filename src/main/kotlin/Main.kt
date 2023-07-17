import org.slf4j.LoggerFactory
import sim.Jobs
import web.startWebServer


private val log = LoggerFactory.getLogger("Main")

fun main(): Unit = try {
    // force initialization of Jobs and thus the start of the job worker
    Jobs

    startWebServer()
} catch (e: Throwable) {
    // Make sure that all unexpected exceptions are logged (except for exceptions in the logger).
    log.error(e)
}
