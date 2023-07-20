package dnaforge.backend

import dnaforge.backend.sim.Jobs
import dnaforge.backend.web.startWebServer
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("Main")

fun main(): Unit = try {
    // force initialization of Jobs and thus the start of the job worker
    Jobs

    startWebServer()
} catch (e: Throwable) {
    // Make sure that all unexpected exceptions are logged (except for exceptions in the logger).
    log.error(e)
}
