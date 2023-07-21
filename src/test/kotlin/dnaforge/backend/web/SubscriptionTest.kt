package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.default
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `getting subscription doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/job/subscribe").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting no subscription works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/job/subscribe") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }

    @Test
    fun `unsubscribing doesn't work without auth`() = testApplication {
        val client = prepare()

        client.delete("/job/subscribe").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `subscribing job doesn't work without auth`() = testApplication {
        val client = prepare()

        client.post("/job/subscribe/0").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `subscribing job doesn't work with wrong id`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.post("/job/subscribe/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `subscribing job and getting subscription works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        Jobs.submitNewJob(default, top, dat, forces)

        client.post("/job/subscribe/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/job/subscribe") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val id: UInt = body()
            assertEquals(0u, id)
        }
    }

    @Test
    fun `unsubscribing job works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        Jobs.submitNewJob(default, top, dat, forces)

        client.post("/job/subscribe/0") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.delete("/job/subscribe") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/job/subscribe") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }
    }
}
