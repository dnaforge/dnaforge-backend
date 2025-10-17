package dnaforge.backend

import dnaforge.backend.sim.Jobs
import dnaforge.backend.web.startWebServer
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("Main")

fun main(): Unit = try {
    // start job worker
    Jobs.enableAutomaticJobExecution()

    startWebServer()
} catch (e: Throwable) {
    // make sure that all unexpected exceptions are logged (except for exceptions in the logger).
    log.throwError(e)
}
