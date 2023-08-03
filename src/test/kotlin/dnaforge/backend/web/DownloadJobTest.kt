package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.StageConfigs
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadJobTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `download doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/job/download/0").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `download doesn't work with wrong id`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/job/download/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `download works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        Jobs.submitNewJob(StageConfigs.default, top, dat, forces)

        client.get("/job/download/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(ContentType.Application.Zip, contentType())
        }
    }
}
