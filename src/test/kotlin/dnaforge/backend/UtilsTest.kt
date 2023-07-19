package dnaforge.backend

import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class UtilsTest {

    @Test
    fun `simple JSON works`() {
        val testObject = TestObject("Test")
        val testObjectString = "{\"name\":\"Test\",\"age\":10}"
        val testObjectExtraString = "{\"name\":\"Test\",\"age\":10,\"color\":\"green\"}"

        assertEquals(testObjectString, simpleJson.encodeToString(testObject))
        assertEquals(testObject, simpleJson.decodeFromString(testObjectString))

        // ignore unknown keys
        assertEquals(testObject, simpleJson.decodeFromString(testObjectExtraString))
    }

    @Test
    fun `pretty JSON works`() {
        val testObject = TestObject("Test")
        val testObjectString = "{\n    \"name\": \"Test\",\n    \"age\": 10\n}"
        val testObjectExtraString = "{\"name\":\"Test\",\"age\":10,\"color\":\"green\"}"

        assertEquals(testObjectString, prettyJson.encodeToString(testObject))
        assertEquals(testObject, prettyJson.decodeFromString(testObjectString))

        // ignore unknown keys
        assertEquals(testObject, prettyJson.decodeFromString(testObjectExtraString))
    }

    @Test
    fun `error throwing works`() {
        val log = LoggerFactory.getLogger("TestLogger")

        assertFails {
            log.error(Throwable("Oh no, an error occurred!"))
        }
    }
}
