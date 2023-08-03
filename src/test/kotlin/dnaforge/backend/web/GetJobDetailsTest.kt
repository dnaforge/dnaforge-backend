package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.StageConfigs
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetJobDetailsTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `getting details doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/job/details/0").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting details doesn't work with wrong id`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/job/details/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `getting details works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        val job0 = Jobs.submitNewJob(
            mapOf(
                "title" to "Some Job",
                "description" to "A very important Job"
            ), StageConfigs.default, top, dat, forces
        )

        client.get("/job/details/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val receivedData: CompleteJob = body()
            assertEquals(job0, receivedData.job)
            assertEquals(top, receivedData.top)
            assertEquals(dat, receivedData.dat)
            assertEquals(forces, receivedData.forces)
        }
    }
}
