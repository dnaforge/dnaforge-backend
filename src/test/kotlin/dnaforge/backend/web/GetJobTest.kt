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

class GetJobTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `getting job doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/job/0").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting job doesn't work with wrong id`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/job/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `getting job works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        val job0 = Jobs.submitNewJob(
            mapOf(
                "title" to "Some Job",
                "description" to "A very important Job"
            ), StageConfigs.default, top, dat, forces
        )

        client.get("/job/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val receivedJob: SimJob = body()
            assertEquals(job0, receivedJob)
        }
    }
}
