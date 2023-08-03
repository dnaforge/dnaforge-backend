package dnaforge.backend.web

import dnaforge.backend.InternalAPI
import dnaforge.backend.sim.JobState
import dnaforge.backend.sim.Jobs
import dnaforge.backend.sim.StageConfigs
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import kotlin.test.*

class ReceiveUpdateTest {

    @OptIn(InternalAPI::class)
    @BeforeTest
    fun `prepare app state`() {
        File("./data").deleteRecursively()
        Jobs.resetState()
    }

    @Test
    fun `receiving normal updates`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.ws("/") {
            sendMessage(WebSocketAuth(bearerToken))

            var frame = incoming.receive()
            assertIs<Frame.Text>(frame)

            var message = frame.toMessage()
            assertIs<WebSocketAuthResponse>(message)
            assertTrue(message.success)

            // submit job
            val job0 = Jobs.submitNewJob(
                mapOf(
                    "title" to "Some Job",
                    "description" to "A very important Job"
                ), StageConfigs.default, top, dat, forces
            )
            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<JobUpdate>(message)
            assertEquals(job0, message.job)

            // get delete update
            Jobs.deleteJob(job0.id)
            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<JobUpdate>(message)
            assertEquals(null, message.job)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `receiving detailed updates`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.ws("/") {
            sendMessage(WebSocketAuth(bearerToken))

            var frame = incoming.receive()
            assertIs<Frame.Text>(frame)

            var message = frame.toMessage()
            assertIs<WebSocketAuthResponse>(message)
            assertTrue(message.success)

            // submit job
            val job0 = Jobs.submitNewJob(
                mapOf(
                    "title" to "Some Job",
                    "description" to "A very important Job"
                ), StageConfigs.default, top, dat, forces
            )
            client.post("/job/subscribe/0") {
                header(HttpHeaders.Authorization, bearerToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<JobUpdate>(message)
            assertEquals(job0, message.job)
            assertEquals(JobState.NEW, message.job?.status)

            // manual execution as automatic execution is disabled
            val scope = CoroutineScope(newSingleThreadContext("TestJobExecutionContext"))
            scope.launch {
                Jobs.getJob(job0.id)!!.execute()
            }

            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<JobUpdate>(message)
            assertEquals(job0, message.job)
            assertEquals(JobState.RUNNING, message.job?.status)

            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<JobUpdate>(message)
            assertEquals(job0, message.job)
            assertEquals(JobState.RUNNING, message.job?.status)

            frame = incoming.receive()
            assertIs<Frame.Text>(frame)
            message = frame.toMessage()
            assertIs<DetailedUpdate>(message)
            assertEquals(job0, message.job)
            assertEquals(JobState.RUNNING, message.job.status)
        }
    }
}
