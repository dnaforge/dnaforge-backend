package dnaforge.backend.web

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusPageTest {

    @Test
    fun `errors are handled`() = testApplication {
        application {
            module()
        }

        // add route for testing
        routing {
            get("/error") {
                throw Throwable("Oh no, an error occurred!")
            }
        }

        client.get("/error").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertEquals("500: java.lang.Throwable: Oh no, an error occurred!", bodyAsText())
        }
    }
}
