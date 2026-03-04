package io.github.kingg22.godot.codegen.impl

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class ScreamingSnakeCaseTest {
    @Test
    fun `test conversion cases`() {
        assertAll(
            testCasesToScreamingCase.map { (input, expected) ->
                val result = input.toScreamingSnakeCase()
                Executable { assertEquals(expected, result, "Input: $input") }
            },
        )
    }

    @Test
    fun `test screaming to pascal case conversion`() {
        testCasesToPascalCase.forEach { (input, expected) ->
            val result = input.screamingToPascalCase()
            assertEquals(expected, result, "Error convirtiendo $input")
        }
    }
}

private val testCasesToScreamingCase = mapOf(
    "InlineAlignment" to "INLINE_ALIGNMENT",
    "camelCase" to "CAMEL_CASE",
    "User1Login" to "USER_1_LOGIN",
    "HTTPResponseCode" to "HTTP_RESPONSE_CODE",
    "simple" to "SIMPLE",
    "Already_Snake" to "ALREADY_SNAKE",
    "A" to "A",
    "" to "",
    "   " to "",
    "My99Variables" to "MY_99_VARIABLES",
    "XMLParser" to "XML_PARSER",
)

private val testCasesToPascalCase = mapOf(
    "TYPE_NIL" to "Nil",
    "TYPE_BOOL" to "Bool",
    "TYPE_INT" to "Int",
    "TYPE_STRING" to "String",
    "TYPE_VECTOR2" to "Vector2",
    "TYPE_VECTOR2I" to "Vector2i",
    "TYPE_RECT2" to "Rect2",
    "TYPE_TRANSFORM2D" to "Transform2D",
    "TYPE_AABB" to "Aabb",
    "TYPE_STRING_NAME" to "StringName",
    "TYPE_NODE_PATH" to "NodePath",
    "TYPE_RID" to "Rid",
    "TYPE_PACKED_BYTE_ARRAY" to "PackedByteArray",
    "TYPE_PACKED_INT32_ARRAY" to "PackedInt32Array",
)
