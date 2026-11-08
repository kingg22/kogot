package io.github.kingg22.kogot.compiler.fir.checkers

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * White box: the checker's [KogotClassIds.VARIANT_TYPES] is a hand-written copy of the runtime conversion rules, and
 * the diagnostics tests run against a hand-written API stand-in. These tests fail as soon as either drifts.
 */
class VariantTypesSyncTest {
    private fun sourceFile(property: String): String =
        File(checkNotNull(System.getProperty(property)) { "System property '$property' is not set" }).readText()

    private val ClassId.simpleName: String get() = shortClassName.asString()

    /** The checker's types, as the simple names the runtime sources use (`Nothing` is the `null` literal, no branch). */
    private val checkerTypes: Set<String> =
        (KogotClassIds.VARIANT_TYPES - StandardClassIds.Nothing).map { it.simpleName }.toSet() +
            KogotClassIds.GODOT_OBJECT.simpleName

    @Test
    fun `checker accepts exactly the types anyToVariantOrNull converts`() {
        val runtimeTypes = Regex("""^\s+is (\w+)(?:<[^>]*>)? ->""", RegexOption.MULTILINE)
            .findAll(sourceFile("kogot.test.convertToVariant"))
            .map { it.groupValues[1] }
            .map { RUNTIME_ALIASES[it] ?: it }
            .toSet() - RUNTIME_REJECTED

        assertEquals(runtimeTypes.sorted(), checkerTypes.sorted())
    }

    @Test
    fun `every narrowing getValueOrNull performs targets a compatible type`() {
        val narrowedTypes = Regex("""T::class == (\w+)::class && value is \w+""")
            .findAll(sourceFile("kogot.test.variantConverter"))
            .map { it.groupValues[1] }
            .toList()

        assertTrue(narrowedTypes.isNotEmpty(), "No narrowing conversion found: did getValueOrNull change shape?")
        assertTrue(checkerTypes.containsAll(narrowedTypes), "Narrowed to types the checker rejects: $narrowedTypes")
    }

    @Test
    fun `the shared test API declares every compatible builtin`() {
        val stubDir = File(checkNotNull(System.getProperty("kogot.test.godotApiStubDir")))
        val declared = Regex("""^(?:open |abstract )?class (\w+)""", RegexOption.MULTILINE)
            .findAll(stubDir.listFiles().orEmpty().filter { it.extension == "kt" }.joinToString("\n") { it.readText() })
            .map { it.groupValues[1] }
            .toSet()
        val builtins = KogotClassIds.VARIANT_TYPES.filter { it.packageFqName.asString().startsWith("io.github") }

        val missing = builtins.map { it.simpleName }.filterNot(declared::contains) +
            listOf(KogotClassIds.GODOT_OBJECT.simpleName).filterNot(declared::contains)
        assertTrue(missing.isEmpty(), "testData/api is missing: $missing")
    }

    private companion object {
        /** Type aliases the runtime uses for checker class names. */
        val RUNTIME_ALIASES = mapOf("AABB" to "Aabb", "RID" to "Rid")

        /** Branches that exist only to reject a type with a clear error. */
        val RUNTIME_REJECTED = setOf("Throwable")
    }
}
