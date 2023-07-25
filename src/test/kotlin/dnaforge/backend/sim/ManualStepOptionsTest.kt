package dnaforge.backend.sim

import kotlin.test.Test
import kotlin.test.assertIs

class ManualStepOptionsTest {
    @Test
    fun `all default options are valid`() {
        ManualStepOptions.default.forEach {
            assertIs<Map<String, String>>(it.encodeToMap())
        }
    }
}
