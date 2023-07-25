package dnaforge.backend.web

import dnaforge.backend.sim.ManualStepOptions
import dnaforge.backend.sim.default
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OptionsTest {

    @Test
    fun `getting available options doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/options/available").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting available options works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/options/available") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(ManualStepOptions.availableOptions, body())
        }
    }

    @Test
    fun `getting default options doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/options/default").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting default options works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/options/default") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(default, body())
        }
    }
}