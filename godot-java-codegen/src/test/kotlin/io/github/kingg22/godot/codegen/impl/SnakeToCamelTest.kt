package io.github.kingg22.godot.codegen.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val argExpected = mapOf(
    "snake_case" to "snakeCase",
    "get_size" to "getSize",
    "_get_size" to "_getSize",
    "get_node_path" to "getNodePath",
    "_physics_process" to "_physicsProcess",
    "_ready" to "_ready",
    "__ready" to "__ready",
    "_get__sizes" to "_getSizes",
    "get__nodes" to "getNodes",
)

class SnakeToCamelTest {
    @Test
    fun convert() {
        argExpected.forEach { (arg, expected) ->
            assertEquals(expected, arg.snakeCaseToCamelCase())
        }
    }
}
