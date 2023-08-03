package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.SimJob
import dnaforge.backend.sim.StageConfigs
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubmitJobTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `submitting jobs doesn't work without auth`() = testApplication {
        val client = prepare()

        client.post("/job").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `posting jobs works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.post("/job") {
            header(HttpHeaders.Authorization, bearerToken)
            contentType(ContentType.Application.Json)
            setBody(
                JobNew(
                    mapOf(
                        "title" to "Some Job",
                        "description" to "A very important Job"
                    ), StageConfigs.default, top, dat, forces
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val job0: SimJob = body()
            val expected = SimJob(
                mapOf(
                    "title" to "Some Job",
                    "description" to "A very important Job"
                ), 0u, StageConfigs.default.size.toUInt()
            )
            assertEquals(expected, job0)
            val jobs = Jobs.getJobs()
            assertEquals(1, jobs.size)
            assertEquals(expected, jobs[0])
        }

        client.post("/job") {
            header(HttpHeaders.Authorization, bearerToken)
            contentType(ContentType.Application.Json)
            setBody(
                JobNew(
                    mapOf(
                        "title" to "Some Job",
                        "description" to "A very important Job"
                    ), StageConfigs.default, top, dat, forces
                )
            )
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val job0: SimJob = body()
            val expected = SimJob(
                mapOf(
                    "title" to "Some Job",
                    "description" to "A very important Job"
                ), 1u, StageConfigs.default.size.toUInt()
            )
            assertEquals(expected, job0)
            val jobs = Jobs.getJobs()
            assertEquals(2, jobs.size)
            assertEquals(expected, jobs[1])
        }
    }
}
