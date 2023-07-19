package dnaforge.backend.web

import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlin.test.*

class AuthenticationTest {

    @Test
    fun `missing token doesn't work`() = testApplication {
        val client = prepare()

        client.get("/auth").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `wrong token doesn't work`() = testApplication {
        val client = prepare()

        client.get("/auth") {
            bearerAuth("WrongToken")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `correct token works`() = testApplication {
        val client = prepare()

        client.get("/auth") {
            header(HttpHeaders.Authorization, "TestToken")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val response: String = body()
            assertEquals(32, response.length)
        }
    }

    @Test
    fun `ws assignment doesn't work without token`() = testApplication {
        val client = prepare()

        client.ws("/") {
            sendMessage(WebSocketAuth("BearerTokenThatDoesNotExist12345"))

            val frame = incoming.receive()
            assertIs<Frame.Text>(frame)

            val message = frame.toMessage()
            assertIs<WebSocketAuthResponse>(message)
            assertFalse(message.success)
        }
    }

    @Test
    fun `ws assignment doesn't work with incorrect token`() = testApplication {
        val client = prepare()

        val bearerToken: String = client.get("/auth") {
            header(HttpHeaders.Authorization, "TestToken")
        }.run {
            assertEquals(HttpStatusCode.OK, status)
            body()
        }

        client.ws("/") {
            sendMessage(WebSocketAuth(bearerToken + "Wrong"))

            val frame = incoming.receive()
            assertIs<Frame.Text>(frame)

            val message = frame.toMessage()
            assertIs<WebSocketAuthResponse>(message)
            assertFalse(message.success)
        }
    }

    @Test
    fun `ws assignment works with correct token`() = testApplication {
        val client = prepare()

        val bearerToken: String = client.get("/auth") {
            header(HttpHeaders.Authorization, "TestToken")
        }.run {
            assertEquals(HttpStatusCode.OK, status)
            body()
        }

        client.ws("/") {
            sendMessage(WebSocketAuth(bearerToken))

            val frame = incoming.receive()
            assertIs<Frame.Text>(frame)

            val message = frame.toMessage()
            assertIs<WebSocketAuthResponse>(message)
            assertTrue(message.success)
        }
    }
}
