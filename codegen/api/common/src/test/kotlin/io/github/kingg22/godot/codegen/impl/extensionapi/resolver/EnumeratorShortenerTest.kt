package io.github.kingg22.godot.codegen.impl.extensionapi.resolver

import io.github.kingg22.godot.codegen.extensionapi.resolver.EnumeratorShortener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EnumeratorShortenerTest {

    // ── Contract: output size always equals input size ────────────────────────

    @Test
    fun `output list has same size as input - normal case`() {
        val input = listOf("FEED_RGBA_IMAGE", "FEED_YCBCR_IMAGE", "FEED_Y_IMAGE", "FEED_CBCR_IMAGE")
        val output = EnumeratorShortener.shortenEnumeratorNames("CameraServer", "FeedImage", input)
        assertEquals(input.size, output.size) {
            "Shortener must never drop constants. Input=$input Output=$output"
        }
    }

    @Test
    fun `output list has same size as input - single element`() {
        val input = listOf("MY_ONLY")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Solo", input)
        assertEquals(1, output.size)
    }

    @Test
    fun `output list has same size as input - empty list`() {
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Empty", emptyList())
        assertTrue(output.isEmpty())
    }

    // ── Regression: CameraServer.FeedImage ───────────────────────────────────

    /**
     * Regression test for the bug that caused:
     *   "Enum 'CameraServer.FeedImage' has 4 values, but enum shortener returns 2 constants."
     *
     * The old code had a special case:
     *   `if (i == lastIndex && (e.endsWith("_MAX") || e.endsWith("_COUNT"))) return@mapIndexed "MAX"`
     *
     * This incorrectly mapped the last element regardless of whether it actually ended in _MAX/_COUNT.
     * For FeedImage the last element is "FEED_CBCR_IMAGE", which does NOT end in _MAX/_COUNT, so the
     * bug was not in the shortener for this specific enum — the root cause was in EnumConstantResolver.
     * Nonetheless we verify the shortener returns all 4 names correctly for this input.
     */
    @Test
    fun `CameraServer FeedImage - all four constants are returned`() {
        val input = listOf("FEED_RGBA_IMAGE", "FEED_YCBCR_IMAGE", "FEED_Y_IMAGE", "FEED_CBCR_IMAGE")
        val output = EnumeratorShortener.shortenEnumeratorNames("CameraServer", "FeedImage", input)

        assertEquals(4, output.size) {
            "Expected 4 shortened names, got ${output.size}: $output"
        }
        // All results must be valid Kotlin identifiers (not starting with a digit)
        output.forEach { name ->
            assertFalse(name.isEmpty(), "Shortened name must not be empty")
            assertFalse(name[0].isDigit(), "Shortened name '$name' starts with a digit — invalid Kotlin identifier")
        }
        // Common prefix FEED_ should be stripped
        output.forEach { name ->
            assertFalse(name.startsWith("FEED_"), "Prefix 'FEED_' was not stripped from '$name'")
        }
        // Check specific expected values
        assertTrue(output.contains("RGBA_IMAGE"), "Expected RGBA_IMAGE in $output")
        assertTrue(output.contains("CBCR_IMAGE"), "Expected CBCR_IMAGE in $output")
    }

    // ── _MAX / _COUNT sentinel convention ────────────────────────────────────

    /**
     * The old code forced the last element to "MAX" whenever it ended with "_MAX" or "_COUNT".
     * This is wrong when the shortened form (after common-prefix removal) is NOT "MAX"/"COUNT".
     * The new code simply applies the same prefix-stripping and returns whatever that produces.
     */
    @Test
    fun `last element ending in _MAX is shortened via common prefix, not forced to MAX`() {
        // Enum where the last element ends in _MAX and common prefix is MY_
        // Expected: MY_ is stripped → "ALPHA", "BETA", "MAX"
        val input = listOf("MY_ALPHA", "MY_BETA", "MY_MAX")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "MyEnum", input)

        assertEquals(3, output.size)
        assertEquals("ALPHA", output[0])
        assertEquals("BETA", output[1])
        assertEquals("MAX", output[2])
    }

    @Test
    fun `last element that does NOT end in _MAX is not renamed to MAX`() {
        // Regression: previously "FEED_CBCR_IMAGE" (last) would have been returned as "MAX"
        // because the old code only checked endsWith("_MAX") and this particular test ensures
        // a non-_MAX last element keeps its stripped form.
        val input = listOf("STATUS_OK", "STATUS_ERROR", "STATUS_PENDING")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Status", input)

        assertEquals(3, output.size)
        assertNotEquals("MAX", output[2], "Last element should not be renamed to MAX")
        assertEquals("PENDING", output[2])
    }

    @Test
    fun `enum ending in _COUNT sentinel returns COUNT for last element`() {
        val input = listOf("CHANNEL_RED", "CHANNEL_GREEN", "CHANNEL_BLUE", "CHANNEL_COUNT")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Channel", input)
        assertEquals(4, output.size)
        assertEquals("COUNT", output[3])
    }

    // ── Hardcoded prefix exceptions ───────────────────────────────────────────

    @Test
    fun `global Key enum strips KEY_ prefix`() {
        val input = listOf("KEY_NONE", "KEY_ESCAPE", "KEY_TAB")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Key", input)
        assertEquals(listOf("NONE", "ESCAPE", "TAB"), output)
    }

    @Test
    fun `global Error enum strips ERR_ prefix`() {
        val input = listOf("OK", "FAILED", "ERR_UNAVAILABLE")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Error", input)
        // "OK" and "FAILED" don't have ERR_ prefix → returned as-is (tryStripPrefixes fallback)
        assertEquals("OK", output[0])
        assertEquals("FAILED", output[1])
        assertEquals("UNAVAILABLE", output[2])
    }

    @ParameterizedTest
    @CsvSource(
        "RenderingServer,ArrayFormat,ARRAY_FORMAT_VERTEX,VERTEX",
        "RenderingServer,ArrayFormat,ARRAY_VERTEX,VERTEX",
        "Mesh,ArrayFormat,ARRAY_FORMAT_NORMAL,NORMAL",
    )
    fun `ArrayFormat hardcoded prefixes are stripped correctly`(
        className: String,
        enumName: String,
        input: String,
        expected: String,
    ) {
        val output = EnumeratorShortener.shortenEnumeratorNames(className, enumName, listOf(input))
        assertEquals(expected, output[0])
    }

    // ── Digit-starting identifier guard ──────────────────────────────────────

    @Test
    fun `does not produce identifier starting with digit`() {
        // SOURCE_3D → stripping SOURCE_ would yield "3D" which is invalid
        // The shortener should back up and keep "SOURCE_3D" (or the segment before the digit)
        val input = listOf("SOURCE_2D", "SOURCE_3D")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "SourceType", input)
        assertEquals(2, output.size)
        output.forEach { name ->
            assertFalse(name[0].isDigit()) { "Output name '$name' starts with a digit" }
        }
    }

    // ── Heuristic prefix detection ────────────────────────────────────────────

    @Test
    fun `common prefix is stripped for normal enum`() {
        val input = listOf("CAMERA_FEED_UNSPECIFIED", "CAMERA_FEED_RGB", "CAMERA_FEED_YCBCR")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "FeedDataType", input)
        assertEquals(listOf("UNSPECIFIED", "RGB", "YCBCR"), output)
    }

    @Test
    fun `no common prefix - names returned as-is`() {
        val input = listOf("ALPHA", "BETA", "GAMMA")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "Greek", input)
        assertEquals(input, output)
    }

    @Test
    fun `two elements with shared prefix`() {
        val input = listOf("BLEND_ADD", "BLEND_SUBTRACT")
        val output = EnumeratorShortener.shortenEnumeratorNames(null, "BlendMode", input)
        assertEquals(listOf("ADD", "SUBTRACT"), output)
    }
}
