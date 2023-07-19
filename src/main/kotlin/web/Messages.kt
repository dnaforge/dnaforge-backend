package web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sim.SimJob
import sim.StepConfig

/**
 * Sent by a client.
 * Sends the list of [StepConfig]s along the structure files.
 */
@Serializable
data class JobNew(val configs: List<StepConfig>, val top: String, val dat: String, val forces: String)

/**
 * [WebSocketMessage] is a sealed interface
 * and must be implemented by all possible messages sent between a client and server.
 * This allows automatic (de)serialization.
 */
@Serializable
sealed interface WebSocketMessage

/**
 * Sent by a client via a WebSocket.
 * Authenticates the client's WebSocket with the given [bearerToken].
 * If the [bearerToken] is valid, the WebSocket session will be associated with the correct client.
 */
@Serializable
@SerialName("WebSocketAuth")
data class WebSocketAuth(val bearerToken: String) : WebSocketMessage

/**
 * Sent by a server via a WebSocket.
 * Notifies the client whether its authentication attempt was successful.
 */
@Serializable
@SerialName("WebSocketAuthResponse")
data class WebSocketAuthResponse(val success: Boolean) : WebSocketMessage

/**
 * Sent by the server via a WebSocket.
 * Notifies the client that the [SimJob] with ID [jobId] has undergone a state change.
 * If [job] is `null`, the [SimJob] has been deleted.
 */
@Serializable
@SerialName("JobUpdate")
data class JobUpdate(val jobId: UInt, val job: SimJob?) : WebSocketMessage

/**
 * Sent by the server via a WebSocket.
 * Notifies the client of an updated structure.
 */
@Serializable
@SerialName("DetailedUpdate")
data class DetailedUpdate(val job: SimJob, val conf: String) : WebSocketMessage
