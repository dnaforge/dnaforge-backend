package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.SimJob
import dnaforge.backend.sim.default
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSingleJobTest {

    @OptIn(InternalAPI::class)
    @BeforeEach
    fun `delete data directory`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
        Jobs.inhibitJobExecution()
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

        val job0 = Jobs.submitNewJob(default, "a", "b", "c")

        client.get("/job/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val receivedJob: SimJob = body()
            assertEquals(job0, receivedJob)
        }
    }
}
