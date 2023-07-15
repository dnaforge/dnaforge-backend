package web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sim.SimJob
import sim.StepConfig

/**
 * [Message] is a sealed interface and must be implemented by all possible messages sent between client and server.
 * This allows automatic (de)serialization.
 */
@Serializable
sealed interface Message

/**
 * Sent by a client.
 * Authenticates the client with the given [accessToken].
 * The server responds with an [AuthenticationResponse] to indicate success or failure.
 */
@Serializable
@SerialName("Authenticate")
data class Authenticate(val accessToken: String) : Message

/**
 * Sent by the server.
 * Notifies the client whether its authentication attempt was successful or not.
 */
@Serializable
@SerialName("AuthenticationResponse")
data class AuthenticationResponse(val success: Boolean) : Message

/**
 * Sent by the server.
 * Tells the client all the [SimJob]s stored on the server.
 * Typically sent after a successful authentication.
 */
@Serializable
@SerialName("JobList")
data class JobList(val jobs: List<SimJob>) : Message

/**
 * Sent by the server.
 * Notifies the client that the [SimJob] with id [jobId] has undergone a state change.
 * If [job] is `null`, the [SimJob] has been deleted.
 */
@Serializable
@SerialName("JobUpdate")
data class JobUpdate(val jobId: UInt, val job: SimJob?) : Message

/**
 * Sent by a client.
 * Sends the list of [StepConfig]s along the structure files.
 */
@Serializable
@SerialName("JobNew")
data class JobNew(val configs: List<StepConfig>, val top: String, val dat: String, val forces: String) : Message

/**
 * Sent by a client.
 * The execution of the [SimJob] with the given [jobId] is canceled.
 */
@Serializable
@SerialName("JobCancel")
data class JobCancel(val jobId: UInt) : Message

/**
 * Sent by a client.
 * The [SimJob] with the given [jobId] is deleted.
 */
@Serializable
@SerialName("JobDelete")
data class JobDelete(val jobId: UInt) : Message

/**
 * Sent by a client.
 * Tells the server that the client wants to receive live updates for the [SimJob] with the given [jobId].
 * If [jobId] is `null`, the last subscription is canceled.
 */
@Serializable
@SerialName("JobSubscribe")
data class JobSubscribe(val jobId: UInt?) : Message

/**
 * Sent by the server.
 * Notifies the client of an updated structure.
 */
@Serializable
@SerialName("DetailedUpdate")
data class DetailedUpdate(val conf: String) : Message
