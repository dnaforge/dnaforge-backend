import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import web.Authenticate
import web.Message
import web.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplicationTest {

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

    @Test
    fun `hello world works`() = testApplication {
        application {
            module()
        }

        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun `authentication works`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        application {
            module()
        }

        client.ws("/") {
            val message: Message = Authenticate("TestToken")
            send(Frame.Text(simpleJson.encodeToString(message)))

            var frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            assertEquals("{\"type\":\"AuthenticationResponse\",\"success\":false}", frame.readText())

            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            assertEquals("{\"type\":\"AuthenticationResponse\",\"success\":true}", frame.readText())
        }
    }
}
