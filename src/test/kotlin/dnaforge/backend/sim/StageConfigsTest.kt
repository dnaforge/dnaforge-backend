package dnaforge.backend.sim

import kotlin.test.Test
import kotlin.test.assertIs

class StageConfigsTest {
    @Test
    fun `all default options are valid`() {
        StageConfigs.default.forEach {
            assertIs<ManualConfig>(it)
            assertIs<Map<String, String>>(it.toPropertiesMap())
        }
    }
}
