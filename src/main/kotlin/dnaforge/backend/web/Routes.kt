package dnaforge.backend.web

import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.ManualStageOptions
import dnaforge.backend.sim.SimJob
import dnaforge.backend.sim.StageConfigs
import dnaforge.backend.simpleJson
import dnaforge.backend.zipFileName
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Adds some endpoints to the web server.
 */
fun Application.configureRoutes() {
    // install plugins
    install(PartialContent)
    install(AutoHeadResponse)
    install(ContentNegotiation) {
        json(simpleJson)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(CORS) {
        allowHost("127.0.0.1:8080", schemes = listOf("http", "https"))
        allowHost("localhost:8080", schemes = listOf("http", "https"))
        allowHost("dnaforge.org", schemes = listOf("http", "https"))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }

    routing {
        get("/auth") {
            val bearerToken = Clients.authenticate(call.request.authorization())
            if (bearerToken == null)
                call.respond(HttpStatusCode.Unauthorized)
            else
                call.respond(bearerToken)
        }


        route("/options") {
            get("/available") {
                ifAuthorized {
                    call.respond(ManualStageOptions.availableOptions)
                }
            }

            get("/default") {
                ifAuthorized {
                    call.respond(StageConfigs.default)
                }
            }
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

            get("/details/{id?}") {
                ifAuthorized {
                    withJob {
                        val data = withContext(Dispatchers.IO) {
                            CompleteJob(
                                it,
                                it.topFile.readText(),
                                it.getLatestConfFile().readText(),
                                it.forcesFile.readText()
                            )
                        }
                        call.respond(data)
                    }
                }
            }

            get("/download/{id?}") {
                ifAuthorized {
                    withJob {
                        val zip = withContext(Dispatchers.IO) {
                            it.prepareDownload()
                        }
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName,
                                zipFileName
                            ).toString()
                        )
                        call.respondFile(zip)
                    }
                }
            }

            post {
                ifAuthorized {
                    val newJob: JobNew = call.receive()

                    // check that all configs are valid
                    newJob.configs.forEach { it.toPropertiesMap() }

                    val job = Jobs.submitNewJob(newJob.metadata, newJob.configs, newJob.top, newJob.dat, newJob.forces)
                    call.respond(job)
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

            patch("/{id?}") {
                ifAuthorized {
                    withJob {
                        Jobs.cancelJob(it.id)
                        ok()
                    }
                }
            }

            route("/subscribe") {
                get {
                    ifAuthorized {
                        val id = Clients.getSubscription(it)
                        if (id == null)
                            call.respond(HttpStatusCode.NoContent)
                        else
                            call.respond(id)
                    }
                }

                post("/{id?}") {
                    ifAuthorized { client ->
                        withJob {
                            Clients.subscribe(client, it.id)
                            ok()
                        }
                    }
                }

                delete {
                    ifAuthorized {
                        Clients.unsubscribe(it)
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
    else {
        client.lastInteraction = Instant.now()
        body(this, client)
    }
}

/**
 * Sends [HttpStatusCode.OK] in response.
 */
private suspend inline fun PipelineContext<Unit, ApplicationCall>.ok() {
    call.respond(HttpStatusCode.OK)
}
