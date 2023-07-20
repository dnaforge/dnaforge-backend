package dnaforge.backend.web

import dnaforge.backend.simpleJson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlin.test.assertEquals

val top = object {}::class.java.classLoader.getResource("tetrahedron.top")!!.readText()
val dat = object {}::class.java.classLoader.getResource("tetrahedron.dat")!!.readText()
val forces = object {}::class.java.classLoader.getResource("tetrahedron.forces")!!.readText()

/**
 * Prepares the application and creates a client with the required plugins.
 *
 * @return a [HttpClient] for testing.
 */
fun ApplicationTestBuilder.prepare(): HttpClient {
    application {
        module()
    }

    return createClient {
        install(ContentNegotiation) {
            json(simpleJson)
        }
        install(WebSockets)
    }
}

/**
 * Prepares the application and creates a client with the required plugins.
 * Makes sure the client is authenticated.
 *
 * @return a [HttpClient] for testing along a valid bearer token.
 */
suspend fun ApplicationTestBuilder.prepareWithAuth(): Pair<HttpClient, String> {
    val client = prepare()

    client.get("/auth") {
        header(HttpHeaders.Authorization, "TestToken")
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val bearerToken: String = body()
        assertEquals(32, bearerToken.length)

        return Pair(client, bearerToken)
    }
}

/**
 * Sends a [WebSocketMessage].
 *
 * @param message the [WebSocketMessage] to send.
 */
suspend fun ClientWebSocketSession.sendMessage(message: WebSocketMessage) =
    send(Frame.Text(simpleJson.encodeToString(message)))
