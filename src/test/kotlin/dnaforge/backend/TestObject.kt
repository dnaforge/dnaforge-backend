package dnaforge.backend

import kotlinx.serialization.Serializable

@Serializable
data class TestObject(val name: String, val age: Int = 10)
