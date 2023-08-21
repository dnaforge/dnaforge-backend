package dnaforge.backend.web

import dnaforge.backend.sim.*
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
            assertEquals(simplifyOption(ManualStageOptions.availableOptions), body())
        }
    }

    /**
     * fixedProperties, configNames and suffix are not sent by the webserver
     * to avoid sending information that is irrelevant to the frontend.
     */
    private fun simplifyOption(option: Option): Option {
        return Option(option.name, mapOf(), option.entries.map { entry ->
            when (entry) {
                is Option -> simplifyOption(entry)
                is OptionContainer -> OptionContainer(entry.name, entry.values.map { option -> simplifyOption(option) })
                is Property -> Property(entry.name, entry.valueType)
            }
        })
    }

    @Test
    fun `getting available properties doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/options/available/properties").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting available properties works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/options/available/properties") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(ManualStageOptions.availableProperties, body())
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
            assertEquals(StageConfigs.default, body())
        }
    }

    @Test
    fun `getting default files doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/options/default/files").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting default files works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/options/default/files") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(StageConfigs.defaultFiles, body())
        }
    }

    @Test
    fun `getting default properties doesn't work without auth`() = testApplication {
        val client = prepare()

        client.get("/options/default/properties").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `getting default properties works`() = testApplication {
        val (client, bearerToken) = prepareWithAuth()

        client.get("/options/default/properties") {
            header(HttpHeaders.Authorization, bearerToken)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(StageConfigs.defaultProperties, body())
        }
    }
}
