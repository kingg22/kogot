package io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver

import io.github.kingg22.godot.codegen.impl.extensionapi.EmptyContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KDocFormatterTest {
    private val context = EmptyContext()

    @Test
    fun `converts inline code`() {
        val input = "Returns [code]true[/code] if successful."
        val expected = "Returns `true` if successful."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts codeblocks`() {
        val input = """
            Example:
            [codeblock]
            var node = Node()
            add_child(node)
            [/codeblock]
        """.trimIndent()

        context(context) {
            val result = KDocFormatter.format(input)!!
            assertTrue(result.contains("```"))
            assertTrue(result.contains("var node = Node()"))
        }
    }

    @Test
    fun `converts bold and italic`() {
        val input = "This is [b]bold[/b] and [i]italic[/i]."
        val expected = "This is **bold** and *italic*."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts param references`() {
        val input = "Sets the value of [param my_property] to 10."
        val expected = "Sets the value of [myProperty] to 10."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts class references`() {
        val input = "Returns a [Node2D] instance."
        val expected = "Returns a [Node2D] instance."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts method references`() {
        val input = "Call [method get_node] to retrieve."
        val expected = "Call [getNode] to retrieve."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts URLs`() {
        val input = "See [url=https://docs.godotengine.org]documentation[/url]."
        val expected = "See [documentation](https://docs.godotengine.org)."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `escapes comment delimiters`() {
        val input = "This has /* and */ in it."
        val expected = "This has /\\* and *\\/ in it."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `handles nested classes`() {
        val input = "Uses [Node2D.PositionMode] for alignment."
        val expected = "Uses [PositionMode][Node2D.PositionMode] for alignment."
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `converts line breaks`() {
        val input = "Line 1[br]Line 2[br]Line 3"
        val expected = "Line 1\nLine 2\nLine 3"
        context(context) {
            assertEquals(expected, KDocFormatter.format(input))
        }
    }

    @Test
    fun `handles complex example`() {
        val input = """
            Returns [code]true[/code] if [param node] is a [Node2D].

            [b]Example:[/b]
            [codeblock]
            if (is_node_2d(my_node)) {
                print("It's a Node2D!")
            }
            [/codeblock]

            See [method get_node] for details.
        """.trimIndent()

        val result = context(context) {
            KDocFormatter.format(input)!!
        }

        assertTrue(result.contains("`true`"), "Output: $result")
        assertTrue(result.contains("[node]"))
        assertTrue(result.contains("[Node2D]"))
        assertTrue(result.contains("**Example:**"))
        assertTrue(result.contains("```"))
        assertTrue(result.contains("[getNode]"))
    }

    @Test
    fun `wraps long lines to max length`() {
        val longLine =
            "This is a very long line that exceeds the maximum line length of 120 characters and should be wrapped into multiple lines automatically by the formatter without breaking words in the middle of them."

        val result = context(context) {
            KDocFormatter.format(longLine)!!
        }

        // Check that no line exceeds the limit
        result.lines().forEach { line ->
            assertTrue(line.length <= 116) { "Line exceeds max length: ${line.length} chars" }
        }

        // Check that content is preserved
        val unwrapped = result.replace("\n", " ")
        assertTrue(unwrapped.contains("very long line"))
        assertTrue(unwrapped.contains("maximum line length"))
    }

    @Test
    fun `does not wrap inside codeblocks`() {
        val input = """
        Example:
        [codeblock]
        val veryLongVariableName = "This is a very long string that would normally be wrapped but should not be wrapped inside a codeblock"
        [/codeblock]
        """.trimIndent()

        val result = context(context) {
            KDocFormatter.format(input)!!
        }

        // The code line should remain intact
        assertTrue(result.contains("val veryLongVariableName"))
        assertTrue(
            !result.lines().any {
                it.contains("veryLongVariableName") && it.contains("\n")
            },
        )
    }

    @Test
    fun `breaks at sentence boundaries when possible`() {
        val input =
            "This is the first sentence. This is the second sentence that is very long and should be on a new line. This is the third sentence."

        val result = context(context) {
            KDocFormatter.format(input)!!
        }
        val lines = result.lines()

        // Should break after sentences
        assertTrue(lines.any { it.trim().startsWith("This is the third") }, "Output: $result")
    }

    @Test
    fun `does not break URLs`() {
        val input =
            "See the documentation at https://docs.godotengine.org/en/stable/tutorials/scripting/gdscript/gdscript_basics.html for more information about this very long topic."

        val result = context(context) {
            KDocFormatter.format(input)!!
        }

        // URL should remain unbroken
        assertTrue(
            result.contains("https://docs.godotengine.org/en/stable/tutorials/scripting/gdscript/gdscript_basics.html"),
        )
    }

    @Test
    fun `does not break BBCode tags`() {
        val input =
            "This is a very long line with a [code]code snippet that should not be broken[/code] in the middle of the tag."

        val result = context(context) {
            KDocFormatter.format(input)!!
        }

        // The code tag should remain intact (will be converted to backticks)
        assertTrue(result.contains("`code snippet that should not be broken`"))
    }
}
