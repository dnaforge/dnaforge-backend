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

class GetJobsTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `getting jobs doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/job").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting jobs works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/job") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val jobs: List<SimJob> = body()
            assertEquals(0, jobs.size)
        }

        val job0 = Jobs.submitNewJob(
            mapOf(
                "title" to "Some Job",
                "description" to "A very important Job"
            ), StageConfigs.default, top, dat, forces
        )

        client.get("/job") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val jobs: List<SimJob> = body()
            assertEquals(1, jobs.size)
            assertEquals(job0, jobs[0])
        }

        val job1 = Jobs.submitNewJob(
            mapOf(
                "title" to "Some Job",
                "description" to "A very important Job"
            ), StageConfigs.default, top, dat, forces
        )

        client.get("/job") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val jobs: List<SimJob> = body()
            assertEquals(2, jobs.size)
            assertEquals(job0, jobs[0])
            assertEquals(job1, jobs[1])
        }
    }
}
