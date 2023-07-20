package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.default
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteJobTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
        Jobs.inhibitJobExecution()
    }

    @Test
    fun `deleting job doesn't work without auth`() = testApplication {
        val client = prepare()

        client.delete("/job/0").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `deleting job doesn't work with wrong id`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.delete("/job/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `deleting job works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        Jobs.submitNewJob(default, top, dat, forces)

        client.delete("/job/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
