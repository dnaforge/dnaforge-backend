import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import web.Authenticate
import web.Message
import web.configureWebSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplicationTest {
    @Test
    fun authenticationTest() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        application {
            configureWebSocket()
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
