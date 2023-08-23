package dnaforge.backend.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StageConfigsTest {
    @Test
    fun `all default options are valid`() {
        StageConfigs.default.forEach {
            assertIs<ManualConfig>(it)
            assertIs<Map<String, String>>(it.toPropertiesMap())
        }
    }

    @Test
    fun `all default files are valid`() {
        StageConfigs.defaultFiles.forEach {
            assertIs<FileConfig>(it)
            assertIs<Map<String, String>>(it.toPropertiesMap())
        }
    }

    @Test
    fun `all default properties are valid`() {
        StageConfigs.defaultProperties.forEach {
            assertIs<PropertiesConfig>(it)
            assertIs<Map<String, String>>(it.toPropertiesMap())
        }
    }

    @Test
    fun `all default types produce the same configs`() {
        StageConfigs.default.zip(StageConfigs.defaultFiles).forEach {
            assertEquals(it.first.toPropertiesMap(), it.second.toPropertiesMap())
        }
        StageConfigs.default.zip(StageConfigs.defaultProperties).forEach {
            assertEquals(it.first.toPropertiesMap(), it.second.toPropertiesMap())
        }
    }
}
