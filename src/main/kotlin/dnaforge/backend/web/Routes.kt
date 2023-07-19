package dnaforge.backend.web

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.SimJob
import dnaforge.backend.simpleJson

/**
 * Adds some endpoints to the web server.
 */
fun Application.configureRoutes() {
    // install plugins
    install(ContentNegotiation) {
        json(simpleJson)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/auth") {
            val bearerToken = Clients.authenticate(call.request.authorization())
            if (bearerToken == null)
                call.respond(HttpStatusCode.Unauthorized)
            else
                call.respond(bearerToken)
        }


        route("/job") {
            get {
                ifAuthorized {
                    call.respond(Jobs.getJobs())
                }
            }

            get("/{id?}") {
                ifAuthorized {
                    withJob {
                        call.respond(it)
                    }
                }
            }

            post {
                ifAuthorized {
                    val newJob: JobNew = call.receive()
                    Jobs.submitNewJob(newJob.configs, newJob.top, newJob.dat, newJob.forces)
                    call.respond(SimJob)
                }
            }

            delete("/{id?}") {
                ifAuthorized {
                    withJob {
                        Jobs.deleteJob(it.id)
                        ok()
                    }
                }
            }

            patch("/cancel/{id?}") {
                ifAuthorized {
                    withJob {
                        Jobs.cancelJob(it.id)
                        ok()
                    }
                }
            }

            post("/unsubscribe") {
                ifAuthorized {
                    Clients.unsubscribe(it)
                    ok()
                }
            }

            post("/subscribe/{id?}") {
                ifAuthorized { client ->
                    withJob {
                        Clients.subscribe(client, it.id)
                        ok()
                    }
                }
            }
        }
    }
}


/**
 * Checks if an ID parameter has been passed and if a [SimJob] matching this ID exists.
 * If so, the given body is executed with access to the [SimJob].
 *
 * @param body the code to be executed if a valid ID was passed.
 */
private suspend inline fun PipelineContext<Unit, ApplicationCall>.withJob(body: PipelineContext<Unit, ApplicationCall>.(SimJob) -> Unit) {
    val job = call.parameters["id"]?.toUIntOrNull()?.run { Jobs.getJob(this) }
    if (job == null)
        call.respond(HttpStatusCode.NotFound)
    else
        body(this, job)
}

/**
 * Checks if a correct bearer token has been supplied
 * and then executes the given body with access to the corresponding [Client].
 *
 * @param body the code to execute if an authenticated [Client] is identified.
 */
private suspend inline fun PipelineContext<Unit, ApplicationCall>.ifAuthorized(body: PipelineContext<Unit, ApplicationCall>.(Client) -> Unit) {
    val client = Clients.getClientByBearerToken(call.request.authorization())
    if (client == null)
        call.respond(HttpStatusCode.Unauthorized)
    else
        body(this, client)
}

/**
 * Sends [HttpStatusCode.OK] in response.
 */
private suspend inline fun PipelineContext<Unit, ApplicationCall>.ok() {
    call.respond(HttpStatusCode.OK)
}