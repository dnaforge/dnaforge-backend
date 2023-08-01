package dnaforge.backend.sim

import kotlin.test.Test
import kotlin.test.assertIs

class ManualStageOptionsTest {
    @Test
    fun `all default options are valid`() {
        default.forEach {
            assertIs<ManualConfig>(it)
            assertIs<Map<String, String>>(it.options.encodeToMap())
        }
    }
}
